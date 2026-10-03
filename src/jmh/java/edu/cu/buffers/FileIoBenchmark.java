package edu.cu.buffers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * FileChannel transfer with heap vs direct buffers. The file is pre-filled
 * and fits the OS page cache, so this measures the JVM-side path
 * (buffer handling, copying), not the disk.
 *
 * One op = ioMiB mebibytes written (overwrite in place) or read.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class FileIoBenchmark {

    private static final int MEBIBYTE = 1024 * 1024;

    @Param({"4194304"})
    int bufferBytes;

    @Param({"64"})
    int ioMiB;

    ByteBuffer heapBuffer;
    ByteBuffer directBuffer;
    Path file;
    FileChannel channel;
    long totalBytes;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        heapBuffer = ByteBuffer.allocate(bufferBytes);
        directBuffer = ByteBuffer.allocateDirect(bufferBytes);
        for (int i = 0; i < bufferBytes; i += Long.BYTES) {
            long value = 0x5A5A5A5A00000000L ^ i;
            heapBuffer.putLong(i, value);
            directBuffer.putLong(i, value);
        }
        totalBytes = (long) ioMiB * MEBIBYTE;
        file = Files.createTempFile("jmh-buffer-placement-", ".bin");
        channel = FileChannel.open(file,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        writeFully(directBuffer);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        channel.close();
        Files.deleteIfExists(file);
    }

    @Benchmark
    public long writeHeap() throws IOException {
        return writeFully(heapBuffer);
    }

    @Benchmark
    public long writeDirect() throws IOException {
        return writeFully(directBuffer);
    }

    @Benchmark
    public long readHeap() throws IOException {
        return readFully(heapBuffer);
    }

    @Benchmark
    public long readDirect() throws IOException {
        return readFully(directBuffer);
    }

    private long writeFully(ByteBuffer buffer) throws IOException {
        channel.position(0);
        long written = 0;
        while (written < totalBytes) {
            buffer.position(0);
            buffer.limit((int) Math.min(buffer.capacity(), totalBytes - written));
            while (buffer.hasRemaining()) {
                written += channel.write(buffer);
            }
        }
        return written;
    }

    private long readFully(ByteBuffer buffer) throws IOException {
        channel.position(0);
        long read = 0;
        while (read < totalBytes) {
            buffer.clear();
            buffer.limit((int) Math.min(buffer.capacity(), totalBytes - read));
            while (buffer.hasRemaining()) {
                int count = channel.read(buffer);
                if (count < 0) {
                    throw new IOException("unexpected end of file");
                }
                read += count;
            }
        }
        return read;
    }
}
