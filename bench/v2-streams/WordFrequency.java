package skoreiko.chnu.edu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * @author Rout
 * @version 1.0.0
 * @project Software quality
 * @class WordFrequency
 * @since 19.10.2026 - 23.24
 */
public final class WordFrequency {

    /** A word together with the number of its occurrences. */
    public record WordCount(String word, long count) {
    }

    /** Common function words that usually dominate the top of the list. */
    public static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "from", "had",
            "has", "have", "he", "her", "him", "his", "i", "in", "into", "is", "it", "its",
            "me", "my", "of", "on", "or", "she", "so", "that", "the", "their", "them", "then",
            "there", "they", "this", "to", "up", "was", "we", "were", "what", "with", "you",
            "your");

    private static final int READ_BUFFER_SIZE = 64 * 1024;
    private static final int INITIAL_TABLE_SIZE = 1 << 12;
    private static final int INITIAL_WORD_SIZE = 32;

    private final boolean skipStopWords;

    // Open-addressing hash table. A slot is free when keys[slot] == null.
    // counts[slot] == 0 marks a stop word that must be ignored.
    private byte[][] keys = new byte[INITIAL_TABLE_SIZE][];
    private long[] counts = new long[INITIAL_TABLE_SIZE];
    private int usedSlots;

    private long totalWords;
    private int distinctWords;

    // State of the word that is being read; reused for every word.
    private byte[] word = new byte[INITIAL_WORD_SIZE];
    private int wordLength;
    private int wordHash;
    private boolean pendingApostrophe;

    /**
     * @param skipStopWords if true, words from {@link #STOP_WORDS} are not counted
     */
    public WordFrequency(boolean skipStopWords) {
        this.skipStopWords = skipStopWords;
    }

    /** Reads the stream to the end and counts its words. The stream is not closed. */
    public void addAll(InputStream in) throws IOException {
        addAll(in, READ_BUFFER_SIZE);
    }

    /** Same as {@link #addAll(InputStream)} with a custom chunk size (used by the tests). */
    void addAll(InputStream in, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int read;
        while ((read = in.read(buffer)) != -1) {
            for (int i = 0; i < read; i++) {
                accept(buffer[i]);
            }
        }
        endWord();
    }

    /** Number of counted words (stop words excluded when skipping is on). */
    public long totalWords() {
        return totalWords;
    }

    /** Number of distinct counted words. */
    public int distinctWords() {
        return distinctWords;
    }

    /**
     * Returns the most frequent words, most frequent first.
     * Words with equal counts are ordered alphabetically, so the result is deterministic.
     * Works in O(distinct * log limit) time and O(limit) extra memory.
     *
     * @param limit maximum number of words to return, must not be negative
     */
    public WordCount[] top(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }
        int size = Math.min(limit, distinctWords);
        if (size == 0) {
            return new WordCount[0];
        }

        // Min-heap of slot numbers: the root is the worst of the best `size` words.
        Comparator<Integer> byRank = (a, b) -> compareRank(a, b);
        PriorityQueue<Integer> heap = new PriorityQueue<>(size + 1, byRank);
        for (int slot = 0; slot < keys.length; slot++) {
            if (keys[slot] == null || counts[slot] == 0) {
                continue;
            }
            if (heap.size() < size) {
                heap.add(slot);
            } else if (compareRank(slot, heap.peek()) > 0) {
                heap.poll();
                heap.add(slot);
            }
        }

        WordCount[] result = new WordCount[heap.size()];
        for (int i = result.length - 1; i >= 0; i--) {
            int slot = heap.poll();
            result[i] = new WordCount(
                    new String(keys[slot], StandardCharsets.ISO_8859_1), counts[slot]);
        }
        return result;
    }

    /** Positive if the word in slot {@code a} ranks higher (is "better") than the one in {@code b}. */
    private int compareRank(int a, int b) {
        int byCount = Long.compare(counts[a], counts[b]);
        if (byCount != 0) {
            return byCount;
        }
        // Equal counts: the alphabetically smaller word ranks higher.
        return Arrays.compare(keys[b], keys[a]);
    }

    private void accept(byte b) {
        if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z')) {
            if (pendingApostrophe) {
                append((byte) '\'');
                pendingApostrophe = false;
            }
            append((byte) (b | 0x20)); // to lower case
        } else if (b == '\'' && wordLength > 0 && !pendingApostrophe) {
            // Keep the apostrophe only if a letter follows.
            pendingApostrophe = true;
        } else {
            endWord();
        }
    }

    private void append(byte b) {
        if (wordLength == word.length) {
            word = Arrays.copyOf(word, wordLength * 2);
        }
        word[wordLength++] = b;
        wordHash = 31 * wordHash + b;
    }

    private void endWord() {
        if (wordLength > 0) {
            record();
        }
        wordLength = 0;
        wordHash = 0;
        pendingApostrophe = false;
    }

    private void record() {
        int mask = keys.length - 1;
        int slot = spread(wordHash) & mask;
        while (keys[slot] != null) {
            byte[] key = keys[slot];
            if (key.length == wordLength && Arrays.equals(key, 0, wordLength, word, 0, wordLength)) {
                if (counts[slot] > 0) {
                    counts[slot]++;
                    totalWords++;
                }
                return;
            }
            slot = (slot + 1) & mask;
        }

        // First time this word is seen: the only place that allocates.
        byte[] key = Arrays.copyOf(word, wordLength);
        keys[slot] = key;
        usedSlots++;
        boolean ignored = skipStopWords
                && STOP_WORDS.contains(new String(key, StandardCharsets.ISO_8859_1));
        if (!ignored) {
            counts[slot] = 1;
            totalWords++;
            distinctWords++;
        }
        if (usedSlots * 2 > keys.length) {
            grow();
        }
    }

    private void grow() {
        byte[][] oldKeys = keys;
        long[] oldCounts = counts;
        keys = new byte[oldKeys.length * 2][];
        counts = new long[oldKeys.length * 2];
        int mask = keys.length - 1;
        for (int i = 0; i < oldKeys.length; i++) {
            byte[] key = oldKeys[i];
            if (key == null) {
                continue;
            }
            int slot = spread(hash(key)) & mask;
            while (keys[slot] != null) {
                slot = (slot + 1) & mask;
            }
            keys[slot] = key;
            counts[slot] = oldCounts[i];
        }
    }

    private static int hash(byte[] key) {
        int h = 0;
        for (byte b : key) {
            h = 31 * h + b;
        }
        return h;
    }

    /** Mixes the high bits into the low ones, because the table size is a power of two. */
    private static int spread(int h) {
        return h ^ (h >>> 16);
    }
}
