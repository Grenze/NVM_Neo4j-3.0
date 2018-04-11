package org.neo4j.io.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;

public class AbstractStoreChannel implements StoreChannel{
    @Override
    public FileLock tryLock() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int write( ByteBuffer src, long position ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeAll( ByteBuffer src, long position ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeAll( ByteBuffer src ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read( ByteBuffer dst, long position ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void force( boolean metaData ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long write( ByteBuffer[] srcs, int offset, int length ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long write( ByteBuffer[] srcs ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long read( ByteBuffer[] dsts, int offset, int length ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long read( ByteBuffer[] dsts ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read( ByteBuffer dst ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int write( ByteBuffer src ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long position() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public StoreChannel position( long newPosition ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long size() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public StoreChannel truncate( long size ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() throws IOException
    {
        force( false );
    }
}
