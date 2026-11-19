package skoreiko.chnu.edu;

/**
 * @author Rout
 * @version 1.0.0
 * @project Software quality
 * @class FastWordCounter
 * @since 19.11.2026 - 12.00
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Single-pass, allocation-light word counter tuned for cold-start speed.
 *
 * <ul>
 *   <li>The whole file is read with one call into a {@code byte[]}; words are never copied,
 *       the table keeps only the offset of the first occurrence in that array.</li>
 *   <li>A 256-entry table maps a byte to its lowercase letter or 0 (separator), so classification
 *       and lowercasing (in place) are one lookup.</li>
 *   <li>The hash is computed in the same pass that finds word boundaries.</li>
 *   <li>Open-addressing table in one interleaved {@code int[]} (hash, offset, length, count):
 *       one lookup touches one cache line, no boxing, no {@code HashMap}.</li>
 *   <li>The table is sized from the input length, so it normally never has to grow.</li>
 *   <li>{@link #top} keeps a small insertion-sorted array: no lambdas, no {@code PriorityQueue}.</li>
 * </ul>
 *
 * <p>A word is a maximal run of ASCII letters ({@code [A-Za-z]+}); every other byte is a separator.
 */
final class FastWordCounter {

    private static final int STRIDE = 4;
    private static final int HASH = 0, OFFSET = 1, LENGTH = 2, COUNT = 3;
    private static final int MIN_SLOTS = 1 << 10;
    private static final int MAX_SLOTS = 1 << 22;

    /** byte -> lowercase letter, or 0 for any non-letter. */
    private static final byte[] LOWER = new byte[256];

    static {
        for (int c = 'a'; c <= 'z'; c++) {
            LOWER[c] = (byte) c;
            LOWER[c - 'a' + 'A'] = (byte) c;
        }
    }

    private byte[] data;
    private int[] table;
    private int mask;
    private int size;
    private long totalWords;

    static FastWordCounter countFile(Path file) throws IOException {
        byte[] bytes;
        try (FileInputStream in = new FileInputStream(file.toFile())) {
            bytes = in.readAllBytes();
        }
        FastWordCounter counter = new FastWordCounter();
        counter.count(bytes, bytes.length);
        return counter;
    }

    /** Counts words of {@code buf[0..n)}. The buffer is lowercased in place and kept by this object. */
    void count(byte[] buf, int n) {
        data = buf;
        // About one distinct word per 10 input bytes at most, kept under 50% load.
        int slots = Integer.highestOneBit(Math.max(MIN_SLOTS, Math.min(MAX_SLOTS, n / 10))) << 1;
        table = new int[slots * STRIDE];
        mask = slots - 1;
        size = 0;

        final byte[] lower = LOWER;
        long words = 0;
        int i = 0;
        while (i < n) {
            int b = lower[buf[i] & 0xFF];
            if (b == 0) {
                i++;
                continue;
            }
            final int start = i;
            int h = 0;
            do {
                buf[i] = (byte) b;
                h = 31 * h + b;
                i++;
            } while (i < n && (b = lower[buf[i] & 0xFF]) != 0);
            add(start, i - start, h ^ (h >>> 16));
            words++;
        }
        totalWords = words;
    }

    private void add(int start, int len, int h) {
        final int[] t = table;
        final byte[] d = data;
        int slot = h & mask;
        while (true) {
            int base = slot * STRIDE;
            int l = t[base + LENGTH];
            if (l == 0) {
                t[base + HASH] = h;
                t[base + OFFSET] = start;
                t[base + LENGTH] = len;
                t[base + COUNT] = 1;
                if (++size > (mask + 1) >> 1) {
                    grow();
                }
                return;
            }
            if (t[base + HASH] == h && l == len
                    && Arrays.equals(d, t[base + OFFSET], t[base + OFFSET] + len, d, start, start + len)) {
                t[base + COUNT]++;
                return;
            }
            slot = (slot + 1) & mask;
        }
    }

    private void grow() {
        final int[] old = table;
        final int newSlots = (mask + 1) * 2;
        final int[] t = new int[newSlots * STRIDE];
        final int newMask = newSlots - 1;
        for (int base = 0; base < old.length; base += STRIDE) {
            if (old[base + LENGTH] == 0) {
                continue;
            }
            int slot = old[base + HASH] & newMask;
            while (t[slot * STRIDE + LENGTH] != 0) {
                slot = (slot + 1) & newMask;
            }
            System.arraycopy(old, base, t, slot * STRIDE, STRIDE);
        }
        table = t;
        mask = newMask;
    }

    long totalWords() {
        return totalWords;
    }

    int distinctCount() {
        return size;
    }

    /** The {@code n} most frequent words; ties are ordered alphabetically. */
    List<WordCount> top(int n) {
        if (n <= 0 || size == 0) {
            return List.of();
        }
        final int[] t = table;
        final int[] best = new int[Math.min(n, size)];   // table bases, best first
        int filled = 0;
        for (int base = 0; base < t.length; base += STRIDE) {
            if (t[base + LENGTH] == 0) {
                continue;
            }
            if (filled == best.length) {
                if (!better(base, best[filled - 1])) {
                    continue;
                }
                filled--;                                   // evict the worst
            }
            int pos = filled++;
            while (pos > 0 && better(base, best[pos - 1])) {
                best[pos] = best[pos - 1];
                pos--;
            }
            best[pos] = base;
        }
        List<WordCount> result = new ArrayList<>(filled);
        for (int k = 0; k < filled; k++) {
            int base = best[k];
            result.add(new WordCount(
                    new String(data, t[base + OFFSET], t[base + LENGTH], StandardCharsets.ISO_8859_1),
                    t[base + COUNT]));
        }
        return result;
    }

    /** Frequency descending, then alphabetically (words are ASCII, so byte order equals String order). */
    private boolean better(int a, int b) {
        final int[] t = table;
        int ca = t[a + COUNT], cb = t[b + COUNT];
        if (ca != cb) {
            return ca > cb;
        }
        int oa = t[a + OFFSET], ob = t[b + OFFSET];
        return Arrays.compare(data, oa, oa + t[a + LENGTH], data, ob, ob + t[b + LENGTH]) < 0;
    }
}
