import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Runs {@code Main.main(args)} of the version found on the class path and prints one line:
 * <pre>time_ms allocated_kb gc_count gc_ms result_crc</pre>
 * Every version is measured in its own JVM, so the numbers describe a cold single run,
 * which is how the command-line program is really used.
 * <p>
 * The CRC covers the printed top list (everything before the "------" line), so it shows
 * whether two versions give the same answer.
 */
public class RunOnce {

    public static void main(String[] args) throws Exception {
        PrintStream realOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));

        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().threadId();
        Method main = Class.forName("Main").getMethod("main", String[].class);

        long allocatedBefore = threads.getThreadAllocatedBytes(threadId);
        long start = System.nanoTime();
        main.invoke(null, (Object) args);
        long elapsed = System.nanoTime() - start;
        long allocated = threads.getThreadAllocatedBytes(threadId) - allocatedBefore;

        long gcCount = 0;
        long gcMillis = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += gc.getCollectionCount();
            gcMillis += gc.getCollectionTime();
        }

        String output = captured.toString(StandardCharsets.UTF_8);
        String top = output.substring(0, output.indexOf("------"));
        CRC32 crc = new CRC32();
        crc.update(top.getBytes(StandardCharsets.UTF_8));

        realOut.printf(java.util.Locale.ROOT, "%.1f %d %d %d %08x%n", elapsed / 1e6, allocated / 1024, gcCount, gcMillis,
                crc.getValue());
    }
}
