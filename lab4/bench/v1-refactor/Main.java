package skoreiko.chnu.edu;


/**
 * @author Rout
 * @version 1.0.0
 * @project Default (Template) Project
 * @class ${NAME}
 * @since 08.10.2026 - 17.01
 */
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Prints the most frequent words of a text file.
 */
public class Main {

    private static final String DEFAULT_FILE = "src/txt/harry.txt";
    private static final int DEFAULT_TOP = 30;

    public static void main(String[] args) {
        Path file = Path.of(args.length > 0 ? args[0] : DEFAULT_FILE);
        int top = args.length > 1 ? parseTop(args[1]) : DEFAULT_TOP;

        long start = System.nanoTime();
        try {
            // The file is not valid UTF-8, so the charset is set explicitly.
            String text = Files.readString(file, StandardCharsets.ISO_8859_1);

            List<String> words = WordFrequency.tokenize(text);
            Map<String, Long> counts = WordFrequency.count(words);

            WordFrequency.top(counts, top)
                    .forEach(entry -> System.out.println(entry.word() + " " + entry.count()));

            System.out.println("------");
            System.out.println("Words: " + words.size() + ", distinct: " + counts.size());
        } catch (IOException e) {
            System.err.println("Cannot read file " + file + ": " + e);
            System.exit(1);
        }
        System.out.println("Time, ms: " + Duration.ofNanos(System.nanoTime() - start).toMillis());
    }

    private static int parseTop(String value) {
        try {
            int top = Integer.parseInt(value);
            if (top > 0) {
                return top;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        System.err.println("top-n must be a positive integer, got: " + value);
        System.exit(1);
        return DEFAULT_TOP;
    }
}