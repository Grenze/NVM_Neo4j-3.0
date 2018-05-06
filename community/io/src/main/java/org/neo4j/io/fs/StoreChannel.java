package org.neo4j.io.fs;

import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public interface StoreChannel
        extends Flushable, SeekableByteChannel, GatheringByteChannel, ScatteringByteChannel, InterruptibleChannel {
    /**
     * Attempts to acquire an exclusive lock on this channel's file.
     * @return A lock object representing the newly-acquired lock, or null if the lock could not be acquired.
     * @throws IOException If an I/O error occurs.
     * @throws java.nio.channels.ClosedChannelException if the channel is closed.
     */
    boolean tryLock() throws IOException;

    /**
     * NOTE: If you want to write bytes to disk, use #writeAll(), this does not guarantee all bytes will be written,
     * and you are responsible for handling the return value of this call (which tells you how many bytes were written).
     */
    int write( ByteBuffer src, long position ) throws IOException;

    /**
     * Same as #write(), except this method will write the full contents of the buffer in chunks if the OS fails to
     * write it all at once.
     */
    void writeAll( ByteBuffer src, long position ) throws IOException;

    /**
     * Same as #write(), except this method will write the full contents of the buffer in chunks if the OS fails to
     * write it all at once.
     */
    void writeAll( ByteBuffer src ) throws IOException;

    /**
     * @see java.nio.channels.FileChannel#read(java.nio.ByteBuffer, long)
     */
    int read( ByteBuffer dst, long position ) throws IOException;

    void force( boolean metaData ) throws IOException;

    StoreChannel position( long newPosition ) throws IOException;

    StoreChannel truncate( long size ) throws IOException;

    void release();
}
