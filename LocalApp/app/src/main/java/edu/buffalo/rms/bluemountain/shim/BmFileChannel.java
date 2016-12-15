package edu.buffalo.rms.bluemountain.shim;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by sharath on 10/16/16.
 */

public class BmFileChannel extends FileChannel {
    long mPosition;

    public BmFileChannel() {
        mPosition = 0;
    }

    @Override
    public void force(boolean metadata) throws IOException {
        throw new IOException();
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        throw new IOException();
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        throw new IOException();
    }

    @Override
    public long position() throws IOException {
        return mPosition;
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        mPosition = newPosition;
        return this;
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        throw new IOException();
    }

    @Override
    public int read(ByteBuffer buffer, long position) throws IOException {
        throw new IOException();
    }

    @Override
    public long read(ByteBuffer[] buffers, int start, int number) throws IOException {
        throw new IOException();
    }

    @Override
    public long size() throws IOException {
        throw new IOException();
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        throw new IOException();
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        throw new IOException();
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        throw new IOException();
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        throw new IOException();
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new IOException();
    }

    @Override
    public int write(ByteBuffer buffer, long position) throws IOException {
        throw new IOException();
    }

    @Override
    public long write(ByteBuffer[] buffers, int offset, int length) throws IOException {
        throw new IOException();
    }

    @Override
    protected void implCloseChannel() throws IOException {
        throw new IOException();
    }
}
