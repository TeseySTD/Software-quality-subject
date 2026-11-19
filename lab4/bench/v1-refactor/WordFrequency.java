package skoreiko.chnu.edu;

/**
 * @author Rout
 * @version 1.0.0
 * @project Software quality
 * @class WordFrequency
 * @since 19.10.2026 - 23.24
 */
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Counts how often each word occurs in a text.
 * A word is a sequence of Latin letters; everything else is a separator.
 */
public final class WordFrequency {

    private static final Pattern NON_LETTERS = Pattern.compile("[^a-z]+");

    /** A word together with the number of its occurrences. */
    public record WordCount(String word, long count) {
    }

    private WordFrequency() {
    }

    /**
     * Splits the text into lower-case words.
     *
     * @param text any text
     * @return the words in the order they appear in the text
     */
    public static List<String> tokenize(String text) {
        return Arrays.stream(NON_LETTERS.split(text.toLowerCase(Locale.ROOT)))
                .filter(word -> !word.isEmpty())
                .toList();
    }

    /**
     * Counts occurrences of every distinct word in a single pass.
     *
     * @param words words to count
     * @return map from word to its number of occurrences
     */
    public static Map<String, Long> count(List<String> words) {
        return words.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /**
     * Returns the most frequent words.
     * Words with equal counts are ordered alphabetically, so the result is deterministic.
     *
     * @param counts map from word to its number of occurrences
     * @param limit  maximum number of words to return
     * @return the top words, most frequent first
     */
    public static List<WordCount> top(Map<String, Long> counts, int limit) {
        return counts.entrySet().stream()
                .map(entry -> new WordCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(WordCount::count).reversed()
                        .thenComparing(WordCount::word))
                .limit(limit)
                .toList();
    }
}