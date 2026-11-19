package skoreiko.chnu.edu;

/**
 * @author Rout
 * @version 1.0.0
 * @project Software quality
 * @class Main
 * @since 19.11.2026 - 12.00
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point: reads the text, counts word frequency and prints the top N.
 *
 * <p>Usage: {@code java skoreiko.chnu.edu.Main [file_path] [top_n]}
 *
 * <p>No lambdas, string concatenation or {@code java.time}: each of them triggers
 * {@code invokedynamic} bootstrap, which costs more than counting a 440 KB file in a short-lived JVM.
 */
public final class Main {

    private static final String DEFAULT_FILE = "src/txt/harry.txt";
    private static final int DEFAULT_TOP_N = 30;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path file = Path.of(args.length > 0 ? args[0] : DEFAULT_FILE);
        int topN = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_TOP_N;

        long start = System.nanoTime();

        FastWordCounter counter = FastWordCounter.countFile(file);
        List<WordCount> top = counter.top(topN);

        StringBuilder out = new StringBuilder(1024);
        for (WordCount wc : top) {
            out.append(wc.word()).append(' ').append(wc.count()).append('\n');
        }
        out.append("------\n");
        out.append("Words: ").append(counter.totalWords()).append('\n');
        out.append("Distinct: ").append(counter.distinctCount()).append('\n');
        out.append("Time, ms: ").append((System.nanoTime() - start) / 1_000_000).append('\n');

        PrintStream stdout = System.out;
        stdout.print(out);
        stdout.flush();
    }
}
