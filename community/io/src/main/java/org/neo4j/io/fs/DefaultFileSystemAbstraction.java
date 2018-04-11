package org.neo4j.io.fs;


import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DefaultFileSystemAbstraction implements FileSystemAbstraction {

    //mode not used yet
    @Override
    public StoreFileChannel open(File fileName, String mode) throws IOException {
        FileUtils.nvmMkDirs(fileName, true, false);//ensure there is NvmFilDir with fileName
        //FileUtils.printDirectory();

        return new StoreFileChannel(NvmFilDir.getNvmFilDir(fileName));
    }

    /*method below not supported yet, supported by FileUtils*/
    @Override
    public OutputStream openAsOutputStream(File fileName, boolean append) throws FileNotFoundException {
        return new FileOutputStream( fileName, append );
    }

    @Override
    public InputStream openAsInputStream(File fileName) throws FileNotFoundException {
        return new FileInputStream( fileName );
    }


    @Override
    public Reader openAsReader( File fileName, Charset charset ) throws IOException
    {
        return new InputStreamReader( new FileInputStream( fileName ), charset );
    }

    @Override
    public Writer openAsWriter( File fileName, Charset charset, boolean append ) throws IOException
    {
        return new OutputStreamWriter( new FileOutputStream( fileName, append ), charset );
    }

    @Override
    public StoreFileChannel create(File fileName) throws IOException {
        return open(fileName, "rw");
    }

    @Override
    public boolean mkdir(File fileName)  {
        if(fileName.toString().split("/").length>1){
            return false;
        }
        FileUtils.nvmMkDirs(fileName, false, true);
        return true;
    }

    @Override
    public void mkdirs(File fileName) throws IOException {
        FileUtils.nvmMkDirs(fileName, false, true);
        //FileUtils.printDirectory();
    }

    @Override
    public boolean fileExists(File fileName)  {
        //FileUtils.printDirectory();
        return NvmFilDir.exists(fileName);
    }

    @Override
    public long getFileSize(File fileName) {
        return NvmFilDir.getNvmFilDir(fileName).getSize();
    }

    @Override
    public boolean deleteFile(File fileName)  {
        return FileUtils.deleteFile(fileName);
    }

    @Override
    public void deleteRecursively(File directory) throws IOException {
        FileUtils.deleteRecursively(directory);
    }

    @Override
    public boolean renameFile(File from, File to) throws IOException {
        return FileUtils.renameFile(from, to);
    }

    @Override
    public File[] listFiles(File directory){
        return NvmFilDir.listLocalFiles(directory, null);
    }

    @Override
    public File[] listFiles(File directory, FilenameFilter filter)  {
        return NvmFilDir.listLocalFiles(directory, filter);
    }

    @Override
    public boolean isDirectory(File file)  {
        return NvmFilDir.isDirectory(file);
    }

    @Override
    public void moveToDirectory(File file, File toDirectory) throws IOException {
        FileUtils.moveFileToDirectory(file, toDirectory);
    }

    @Override
    public void copyFile(File from, File to) throws IOException {
        FileUtils.copyFile(from, to);
    }

    @Override
    public void copyRecursively(File fromDirectory, File toDirectory) throws IOException {
        FileUtils.copyRecursively(fromDirectory, toDirectory);
    }

    private final Map<Class<? extends ThirdPartyFileSystem>, ThirdPartyFileSystem> thirdPartyFileSystems =
            new HashMap<>();

    @Override
    public synchronized <K extends ThirdPartyFileSystem> K getOrCreateThirdPartyFileSystem(Class<K> clazz, Function<Class<K>, K> creator )
    {
        ThirdPartyFileSystem fileSystem = thirdPartyFileSystems.get( clazz );
        if (fileSystem == null)
        {
            thirdPartyFileSystems.put( clazz, fileSystem = creator.apply( clazz ) );
        }
        return clazz.cast( fileSystem );
    }


    @Override
    public void truncate(File path, long size) throws IOException {
        FileUtils.truncateFile(path, size);
    }

    /*used not only here*/
    protected StoreFileChannel getStoreFileChannel( StoreFileChannel channel )
    {
        return new StoreFileChannel( channel );
    }

}
