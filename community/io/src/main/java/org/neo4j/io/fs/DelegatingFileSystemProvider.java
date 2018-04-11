package org.neo4j.io.fs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class DelegatingFileSystemProvider extends FileSystemProvider
{
    private final FileSystemProvider delegate;

    public DelegatingFileSystemProvider( FileSystemProvider delegate )
    {
        this.delegate = delegate;
    }

    public FileSystemProvider getDelegate()
    {
        return delegate;
    }

    @Override
    public String getScheme()
    {
        return delegate.getScheme();
    }

    @Override
    public FileSystem newFileSystem( URI uri, Map<String, ?> env ) throws IOException
    {
        return delegate.newFileSystem( uri, env );
    }

    @Override
    public FileSystem getFileSystem( URI uri )
    {
        return delegate.getFileSystem( uri );
    }

    @Override
    public Path getPath( URI uri )
    {
        return wrapPath( delegate.getPath( uri ) );
    }

    @Override
    public SeekableByteChannel newByteChannel( Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs ) throws IOException
    {
        return delegate.newByteChannel( getDelegate( path ), options, attrs );
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream( Path dir, DirectoryStream.Filter<? super Path> filter ) throws IOException
    {
        final DirectoryStream<Path> stream = delegate.newDirectoryStream( getDelegate( dir ), filter );
        return new DirectoryStream<Path>()
        {
            @Override
            public Iterator<Path> iterator()
            {
                final Iterator<Path> iterator = stream.iterator();
                return new Iterator<Path>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        return iterator.hasNext();
                    }

                    @Override
                    public Path next()
                    {
                        return wrapPath( iterator.next() );
                    }

                    @Override
                    public void remove()
                    {
                        iterator.next();
                    }
                };
            }

            @Override
            public void close() throws IOException
            {
                stream.close();
            }
        };
    }

    @Override
    public void createDirectory( Path dir, FileAttribute<?>... attrs ) throws IOException
    {
        delegate.createDirectory( getDelegate( dir ), attrs );
    }

    @Override
    public void delete( Path path ) throws IOException
    {
        delegate.delete( getDelegate( path ) );
    }

    @Override
    public void copy( Path source, Path target, CopyOption... options ) throws IOException
    {
        delegate.copy( getDelegate( source ), getDelegate( target ), options );
    }

    @Override
    public void move( Path source, Path target, CopyOption... options ) throws IOException
    {
        delegate.move( getDelegate( source ), getDelegate( target ), options );
    }

    @Override
    public boolean isSameFile( Path path, Path path2 ) throws IOException
    {
        return delegate.isSameFile( getDelegate( path ), getDelegate( path2 ) );
    }

    @Override
    public boolean isHidden( Path path ) throws IOException
    {
        return delegate.isHidden( getDelegate( path ) );
    }

    @Override
    public FileStore getFileStore( Path path ) throws IOException
    {
        return delegate.getFileStore( getDelegate( path ) );
    }

    @Override
    public void checkAccess( Path path, AccessMode... modes ) throws IOException
    {
        delegate.checkAccess( getDelegate( path ), modes );
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView( Path path, Class<V> type, LinkOption... options )
    {
        return delegate.getFileAttributeView( getDelegate( path ), type, options );
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes( Path path, Class<A> type, LinkOption... options ) throws IOException
    {
        return delegate.readAttributes( getDelegate( path ), type, options );
    }

    @Override
    public Map<String, Object> readAttributes( Path path, String attributes, LinkOption... options ) throws IOException
    {
        return delegate.readAttributes( path, attributes, options );
    }

    @Override
    public void setAttribute( Path path, String attribute, Object value, LinkOption... options ) throws IOException
    {
        delegate.setAttribute( getDelegate( path ), attribute, value, options );
    }

    private Path getDelegate( Path path )
    {
        return DelegatingPath.getDelegate( path );
    }

    private Path wrapPath( Path path )
    {
        return (path == null) ? null : createDelegate( path );
    }

    protected abstract Path createDelegate( Path path );
}
