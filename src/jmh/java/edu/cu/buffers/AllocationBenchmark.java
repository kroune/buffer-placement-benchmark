package edu.cu.buffers;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Allocation cost of heap vs direct buffers, plain and with first-touch
 * of every 4 KiB page (what the demo includes in its "allocation" metric).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 3, jvmArgs = {"-Xms1g", "-Xmx1g"})
public class AllocationBenchmark {

    @Param({"4194304"})
    int bufferBytes;

    @Benchmark
    public ByteBuffer allocateHeap() {
        return ByteBuffer.allocate(bufferBytes);
    }

    @Benchmark
    public ByteBuffer allocateDirect() {
        return ByteBuffer.allocateDirect(bufferBytes);
    }

    @Benchmark
    public ByteBuffer allocateHeapTouched() {
        return touch(ByteBuffer.allocate(bufferBytes));
    }

    @Benchmark
    public ByteBuffer allocateDirectTouched() {
        return touch(ByteBuffer.allocateDirect(bufferBytes));
    }

    private static ByteBuffer touch(ByteBuffer buffer) {
        for (int i = 0; i < buffer.capacity(); i += 4096) {
            buffer.put(i, (byte) i);
        }
        buffer.put(buffer.capacity() - 1, (byte) 1);
        return buffer;
    }
}
