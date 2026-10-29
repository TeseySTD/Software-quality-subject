import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Compares all versions of the word counter.
 * <p>
 * Usage (from the project root, JDK 17 or newer):
 * <pre>
 *   java bench/Benchmark.java [runs]
 * </pre>
 * Every measurement runs each version in a fresh JVM (SerialGC). The versions are run in turns,
 * so a slow moment of the machine does not hit only one of them.
 * <ol>
 *   <li>The book (src/txt/harry.txt, 0.4 MB), {@code runs} cold runs (default 15), heap
 *       -Xms16m -Xmx64m. Reported: the median of
 *       <ul>
 *         <li>time - time spent in {@code Main.main}, ms (JVM start-up is not included);</li>
 *         <li>alloc - memory allocated by the main thread, MB (how much garbage the version makes);</li>
 *         <li>GCs, GC - number of garbage collections and total GC time, ms;</li>
 *         <li>process - wall time of the whole java process, ms (JVM start-up is included);</li>
 *       </ul>
 *       and the CRC of the printed top-30 list, to see which versions give the same answer.</li>
 *   <li>The smallest -Xmx with which each version still finishes the book.</li>
 *   <li>The book repeated 50 times (about 21 MB), 3 runs, heap -Xms64m -Xmx1g, and the smallest
 *       -Xmx again. v0 is skipped: its running time grows with the square of the input.</li>
 * </ol>
 */
public class Benchmark {

    private static final Path BOOK = Path.of("src/txt/harry.txt");
    private static final List<String> JVM_FLAGS = List.of("-XX:+UseSerialGC", "-Xshare:auto");

    private record Version(String name, Path sources) {
    }

    /** One run of {@link RunOnce}. */
    private record Sample(double timeMs, double allocMb, double gcCount, double gcMs,
                          String crc, double processMs) {
    }

    private static final Version V0 = new Version("v0-original", Path.of("bench/v0-original"));
    private static final Version V1 = new Version("v1-refactor", Path.of("bench/v1-refactor"));
    private static final Version V2 = new Version("v2-streams", Path.of("bench/v2-streams"));
    private static final Version V3 = new Version("v3-optimized", Path.of("src/main/java"));

    public static void main(String[] args) throws Exception {
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : 15;
        if (!Files.exists(BOOK)) {
            System.err.println("Run from the project root: " + BOOK + " not found");
            System.exit(1);
        }

        Path out = Files.createTempDirectory("sorcerstone-bench");
        try {
            compile(Path.of("bench/RunOnce.java"), out.resolve("runonce"));
            for (Version v : List.of(V0, V1, V2, V3)) {
                compile(v.sources(), out.resolve(v.name()));
            }

            System.out.println("== 1. The book (0.4 MB): " + runs + " cold runs per version, median ==");
            measure(out, BOOK, runs, List.of("-Xms16m", "-Xmx64m"), List.of(V0, V1, V2, V3));

            System.out.println();
            System.out.println("== 2. Smallest -Xmx that still works, the book ==");
            for (Version v : List.of(V0, V1, V2, V3)) {
                smallestHeap(out, BOOK, v, 64, 48, 32, 24, 16, 12, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
            }

            System.out.println();
            System.out.println("== 3. The book repeated 50 times: 3 runs per version, median ==");
            Path big = out.resolve("big.txt");
            byte[] book = Files.readAllBytes(BOOK);
            try (OutputStream os = Files.newOutputStream(big)) {
                for (int i = 0; i < 50; i++) {
                    os.write(book);
                }
            }
            System.out.printf(Locale.ROOT, "file size: %.1f MB%n", Files.size(big) / 1048576.0);
            measure(out, big, 3, List.of("-Xms64m", "-Xmx1g"), List.of(V1, V2, V3));

            System.out.println();
            System.out.println("== 3b. Smallest -Xmx that still works, the big file ==");
            for (Version v : List.of(V1, V2, V3)) {
                smallestHeap(out, big, v, 2048, 1536, 1024, 768, 512, 384, 256, 192, 128, 96, 64, 48,
                        32, 24, 16, 12, 8, 6, 4, 2, 1);
            }
        } finally {
            deleteRecursively(out);
        }
    }

    /** Compiles a single file or all .java files of a directory into {@code outDir}. */
    private static void compile(Path source, Path outDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No compiler found: run with a JDK, not a JRE");
        }
        List<String> arguments = new ArrayList<>(List.of("-encoding", "UTF-8", "-d", outDir.toString()));
        if (Files.isDirectory(source)) {
            try (Stream<Path> files = Files.list(source)) {
                files.filter(f -> f.toString().endsWith(".java")).sorted()
                        .forEach(f -> arguments.add(f.toString()));
            }
        } else {
            arguments.add(source.toString());
        }
        Files.createDirectories(outDir);
        if (compiler.run(null, null, null, arguments.toArray(new String[0])) != 0) {
            throw new IllegalStateException("Compilation failed: " + source);
        }
    }

    private static void measure(Path out, Path file, int runs, List<String> heap, List<Version> versions)
            throws Exception {
        Map<String, List<Sample>> samples = new LinkedHashMap<>();
        versions.forEach(v -> samples.put(v.name(), new ArrayList<>()));

        for (int i = 0; i < runs; i++) {
            for (Version v : versions) {
                samples.get(v.name()).add(runOnce(out, file, heap, v));
            }
        }

        System.out.printf(Locale.ROOT, "%-14s %9s %10s %6s %7s %11s  %s%n",
                "version", "time,ms", "alloc,MB", "GCs", "GC,ms", "process,ms", "top-30 CRC");
        for (Version v : versions) {
            List<Sample> list = samples.get(v.name());
            System.out.printf(Locale.ROOT, "%-14s %9s %10s %6s %7s %11s  %s%n", v.name(),
                    format(median(list, Sample::timeMs)),
                    format(median(list, Sample::allocMb)),
                    format(median(list, Sample::gcCount)),
                    format(median(list, Sample::gcMs)),
                    format(median(list, Sample::processMs)),
                    String.join(",", new TreeSet<>(list.stream().map(Sample::crc).toList())));
        }
    }

    /** Runs {@link RunOnce} for one version in a fresh JVM. */
    private static Sample runOnce(Path out, Path file, List<String> heap, Version v) throws Exception {
        List<String> command = javaCommand(heap, out.resolve("runonce"), out.resolve(v.name()),
                "RunOnce", file);
        long start = System.nanoTime();
        Result result = run(command);
        double processMs = (System.nanoTime() - start) / 1e6;
        if (result.exitCode() != 0) {
            throw new IllegalStateException(v.name() + " failed: " + result.output());
        }
        // RunOnce prints: time_ms allocated_kb gc_count gc_ms crc
        String[] parts = result.output().trim().split("\\s+");
        return new Sample(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]) / 1024,
                Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), parts[4], processMs);
    }

    /** Prints the smallest of the given heap sizes (MB, in descending order) that still works. */
    private static void smallestHeap(Path out, Path file, Version v, int... sizesMb) throws Exception {
        String smallest = "none";
        for (int size : sizesMb) {
            List<String> command = javaCommand(List.of("-Xmx" + size + "m"), out.resolve(v.name()),
                    null, "Main", file);
            if (run(command).exitCode() != 0) {
                break;
            }
            smallest = size + "m";
        }
        System.out.printf(Locale.ROOT, "%-14s %s%n", v.name(), smallest);
    }

    private static List<String> javaCommand(List<String> heap, Path classPath1, Path classPath2,
                                            String mainClass, Path file) {
        String classPath = classPath2 == null
                ? classPath1.toString()
                : classPath1 + System.getProperty("path.separator") + classPath2;
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(JVM_FLAGS);
        command.addAll(heap);
        command.addAll(List.of("-cp", classPath, mainClass, file.toString()));
        return command;
    }

    private record Result(int exitCode, String output) {
    }

    private static Result run(List<String> command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        return new Result(process.waitFor(), output);
    }

    private static double median(List<Sample> samples, java.util.function.ToDoubleFunction<Sample> metric) {
        double[] values = samples.stream().mapToDouble(metric).sorted().toArray();
        int n = values.length;
        return n % 2 == 1 ? values[n / 2] : (values[n / 2 - 1] + values[n / 2]) / 2;
    }

    private static String format(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.format(Locale.ROOT, "%.1f", value);
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}
