package skoreiko.chnu.edu;


/**
 * @author Rout
 * @version 1.0.0
 * @project Default (Template) Project
 * @class ${NAME}
 * @since 08.10.2026 - 17.01
 */
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Prints the most frequent words of a text file.
 */
public class Main {

    private static final String DEFAULT_FILE = "src/txt/harry.txt";
    private static final int DEFAULT_TOP = 30;
    private static final String SKIP_STOP_WORDS = "--skip-stop-words";

    public static void main(String[] args) {
        List<String> positional = new ArrayList<>(List.of(args));
        boolean skipStopWords = positional.remove(SKIP_STOP_WORDS);

        Path file = Path.of(positional.size() > 0 ? positional.get(0) : DEFAULT_FILE);
        int top = positional.size() > 1 ? parseTop(positional.get(1)) : DEFAULT_TOP;

        long start = System.nanoTime();
        WordFrequency counter = new WordFrequency(skipStopWords);
        // The file is streamed in chunks, it is never loaded into memory as a whole.
        try (InputStream in = Files.newInputStream(file)) {
            counter.addAll(in);
        } catch (IOException e) {
            System.err.println("Cannot read file " + file + ": " + e);
            System.exit(1);
        }

        for (WordFrequency.WordCount entry : counter.top(top)) {
            System.out.println(entry.word() + " " + entry.count());
        }
        System.out.println("------");
        System.out.println("Words: " + counter.totalWords() + ", distinct: " + counter.distinctWords());
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