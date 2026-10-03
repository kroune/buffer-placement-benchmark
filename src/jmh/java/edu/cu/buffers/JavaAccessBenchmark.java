package edu.cu.buffers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Pure Java sequential access: the exact workload BufferPlacementDemo measures
 * (fill + sum through a LongBuffer view), plus variants that isolate reads,
 * absolute getLong access, and a plain long[] baseline.
 *
 * One op = one full pass over the buffer (fill+sum moves 2 * bufferBytes,
 * sum-only variants move bufferBytes).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class JavaAccessBenchmark {

    @Param({"4194304"})
    int bufferBytes;

    ByteBuffer heapBuffer;
    LongBuffer heapView;
    ByteBuffer directBuffer;
    LongBuffer directView;
    long[] plainArray;

    @Setup(Level.Trial)
    public void setup() {
        heapBuffer = ByteBuffer.allocate(bufferBytes).order(ByteOrder.nativeOrder());
        heapView = heapBuffer.asLongBuffer();
        directBuffer = ByteBuffer.allocateDirect(bufferBytes).order(ByteOrder.nativeOrder());
        directView = directBuffer.asLongBuffer();
        plainArray = new long[bufferBytes / Long.BYTES];
        for (int i = 0; i < plainArray.length; i++) {
            long value = 0x5A5A5A5A00000000L ^ i;
            heapView.put(i, value);
            directView.put(i, value);
            plainArray[i] = value;
        }
    }

    @Benchmark
    public long fillAndSumHeapView() {
        return fillAndSum(heapView, 17);
    }

    @Benchmark
    public long fillAndSumDirectView() {
        return fillAndSum(directView, 17);
    }

    @Benchmark
    public long sumHeapView() {
        long checksum = 0;
        for (int i = 0; i < heapView.capacity(); i++) {
            checksum += heapView.get(i);
        }
        return checksum;
    }

    @Benchmark
    public long sumDirectView() {
        long checksum = 0;
        for (int i = 0; i < directView.capacity(); i++) {
            checksum += directView.get(i);
        }
        return checksum;
    }

    @Benchmark
    public long sumHeapGetLong() {
        long checksum = 0;
        for (int i = 0; i < bufferBytes; i += Long.BYTES) {
            checksum += heapBuffer.getLong(i);
        }
        return checksum;
    }

    @Benchmark
    public long sumDirectGetLong() {
        long checksum = 0;
        for (int i = 0; i < bufferBytes; i += Long.BYTES) {
            checksum += directBuffer.getLong(i);
        }
        return checksum;
    }

    @Benchmark
    public long sumPlainArray() {
        long checksum = 0;
        for (int i = 0; i < plainArray.length; i++) {
            checksum += plainArray[i];
        }
        return checksum;
    }

    @Benchmark
    public long fillHeapPutLong() {
        for (int i = 0; i < bufferBytes; i += Long.BYTES) {
            heapBuffer.putLong(i, (17L << 32) ^ i);
        }
        return heapBuffer.getLong(0);
    }

    @Benchmark
    public long fillDirectPutLong() {
        for (int i = 0; i < bufferBytes; i += Long.BYTES) {
            directBuffer.putLong(i, (17L << 32) ^ i);
        }
        return directBuffer.getLong(0);
    }

    static long fillAndSum(LongBuffer buffer, int seed) {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(i, ((long) seed << 32) ^ i);
        }
        long checksum = 0;
        for (int i = 0; i < buffer.capacity(); i++) {
            checksum += buffer.get(i);
        }
        return checksum;
    }
}
