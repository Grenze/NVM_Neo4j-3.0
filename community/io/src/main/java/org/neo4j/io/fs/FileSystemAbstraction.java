package org.neo4j.io.fs;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.zip.ZipOutputStream;
import java.io.IOException;

public interface FileSystemAbstraction {
    StoreChannel open(File fileName, String mode ) throws IOException;

    OutputStream openAsOutputStream(File fileName, boolean append ) throws IOException;

    InputStream openAsInputStream(File fileName ) throws IOException;

    Reader openAsReader(File fileName, Charset charset ) throws IOException;

    Writer openAsWriter( File fileName, Charset charset, boolean append ) throws IOException;

    StoreChannel create(File fileName ) throws IOException;

    boolean fileExists( File fileName )  ;

    boolean mkdir( File fileName ) ;

    void mkdirs( File fileName ) throws IOException;

    long getFileSize( File fileName ) ;

    boolean deleteFile( File fileName );

    void deleteRecursively( File directory ) throws IOException;

    boolean renameFile( File from, File to ) throws IOException;

    File[] listFiles( File directory ) ;

    File[] listFiles( File directory, FilenameFilter filter );

    boolean isDirectory( File file ) ;

    void moveToDirectory( File file, File toDirectory ) throws IOException;

    void copyFile( File from, File to ) throws IOException;

    void copyRecursively( File fromDirectory, File toDirectory ) throws IOException;

    <K extends FileSystemAbstraction.ThirdPartyFileSystem> K getOrCreateThirdPartyFileSystem(Class<K> clazz, Function<Class<K>, K> creator );

    void truncate( File path, long size ) throws IOException;

    interface ThirdPartyFileSystem extends Closeable
    {
        void close();

        void dumpToZip(ZipOutputStream zip, byte[] scratchPad ) throws IOException;
    }

}
