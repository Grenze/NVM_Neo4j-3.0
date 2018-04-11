package org.neo4j.io.fs;


import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.*;

public class FileUtils {

    //parent itself child
    private static void deleteNvmFilDir(File file) throws IOException {
        if ( !NvmFilDir.exists(file) )
        {
            return;
        }
        File parentDirectory = getCanonicalParentSafely(file);
        NvmFilDir.getNvmFilDir(parentDirectory).decreaseLocalIndex(file);

        //NvmFilDir.removeNvmFilDir(file); //is included in the loop underneath
        if(NvmFilDir.isFile(file) || NvmFilDir.isEmpty(file)){
            NvmFilDir.removeNvmFilDir(file); //is included in the loop underneath
            return;
        }

        for(String key: NvmFilDir.getNvmFilDirDirectory(null)){
            if(key.startsWith(file.getCanonicalPath())){
                NvmFilDir.removeNvmFilDir(new File(key));
            }
        }

    }

    //delete the directory(file)'s content including itself
    public static void deleteRecursively( File directory ) throws IOException{
        deleteNvmFilDir( directory );
    }

    //delete the path(file)'s content including itself
    public static void deletePathRecursively( Path path ) throws IOException{
        deleteNvmFilDir(path.toFile());
    }

    //only able to delete file or empty directory
    public static boolean deleteFile(File file) {
        if ( !NvmFilDir.exists(file) )
        {
            return true;
        }
        try {
            if( NvmFilDir.isEmpty(file)){
                deleteNvmFilDir(file);
                return true;
            }
            else{
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void moveFile( File toMove, File target ) throws IOException {
        renameNvmFilDir(toMove, target);
    }

    //parent itself child
    private static void renameNvmFilDir(File src, File dst) throws IOException {
        if (!NvmFilDir.exists(src)) {
            throw new FileNotFoundException("Source file[" + src.getCanonicalPath() + "] not found");
        }
        if (NvmFilDir.exists(dst)) {
            throw new FileNotFoundException("Target file[" + dst.getCanonicalPath() + "] already exists");
        }
        NvmFilDir.getNvmFilDir(getCanonicalParentSafely(src)).decreaseLocalIndex(src);
        nvmMkDirs(getCanonicalParentSafely(dst), false, true);
        NvmFilDir.getNvmFilDir(getCanonicalParentSafely(dst)).increaseLocalIndex(dst);

        //NvmFilDir srcFilDir = NvmFilDir.getNvmFilDir(src);//is included in the loop underneath
        //srcFilDir.renameSelf(src, dst);//index changed from src to dst, inner globalId changed too

        if(dst.getCanonicalPath().startsWith(src.getCanonicalPath())){
            throw new IOException("Don't move a directory to a sub directory!");
        }
        if(NvmFilDir.isFile(src) || NvmFilDir.isEmpty(src)){
            NvmFilDir srcFilDir = NvmFilDir.getNvmFilDir(src);//is included in the loop underneath
            srcFilDir.renameSelf(src, dst);//index changed from src to dst, inner globalId changed too
            return;
        }
        for(String key: NvmFilDir.getNvmFilDirDirectory(null)){
            if(key.startsWith(src.getCanonicalPath())){
                NvmFilDir subFilDir = NvmFilDir.getNvmFilDir(new File(key));
                subFilDir.renameSelf(new File(key), new File(dst, key.substring(src.getCanonicalPath().length())));
            }
        }
    }

    //mk current layer then make or prove higher and finally connect them
    public static void nvmMkDirs(File file, boolean isFile, boolean isDirectory) {
        if(!nvmMkFilDir(file, isFile, isDirectory)){
            return;
        }
        File parentFile = null;
        try {
            parentFile = getCanonicalParentSafely(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(parentFile==null){return;}//reach the top layer of nvm file system
        if(!NvmFilDir.exists(parentFile)){
            nvmMkDirs(parentFile, false, true);
        }
        NvmFilDir.getNvmFilDir(parentFile).increaseLocalIndex(file);
    }

    //if already exists, MkFilDir failed
    public static boolean nvmMkFilDir(File file, boolean isFile, boolean isDirectory) {
        if(NvmFilDir.exists(file)){
            return false;
        }

        NvmFilDir nfd = null;
        try {
            nfd = new NvmFilDir(file.getCanonicalPath(), isFile, isDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
        nfd.force(true);
        return true;
    }

    /*specified moveFile, targetDirectory must exist, so renameTo only, keep its origin name*/
    public static File moveFileToDirectory( File toMove, File targetDirectory ) throws IOException {
        if (!NvmFilDir.isDirectory(targetDirectory)) {
            throw new IllegalArgumentException(
                    "Move target must be a directory, not " + targetDirectory.getCanonicalPath());
        }

        File target = new File( targetDirectory, toMove.getName() );
        moveFile(toMove, target);
        return target;
    }

    /*bound to call renameTo, but possible locked file, return true when succeed*/
    public static boolean renameFile( File srcFile, File renameToFile ) throws IOException {
        if (!NvmFilDir.isDirectory(getCanonicalParentSafely(renameToFile))) {
            throw new FileNotFoundException("Target directory[" + getCanonicalParentSafely(renameToFile) + "] does not exists");
        }
        renameNvmFilDir(srcFile, renameToFile);
        return true;
    }

    /*realised by StoreFileChannel*/
    public static void truncateFile( StoreFileChannel fileChannel, long position ) throws IOException{
        fileChannel.truncate(position);
    }
    /*realised by StoreFileChannel*/
    public static void truncateFile( File file, long position ) throws IOException
    {
        if(NvmFilDir.isFile(file)){
            StoreFileChannel access = new StoreFileChannel(NvmFilDir.getNvmFilDir(file));
            truncateFile( access, position );
        }
        else{
            throw new IllegalArgumentException("Parameter first must be a file exist");
        }
    }

    /*
     * fixSeparatorsInPath for Settings.java --remained
     */
    public static String fixSeparatorsInPath( String path )
    {
        String fileSeparator = System.getProperty( "file.separator" );
        if ( "\\".equals( fileSeparator ) )
        {
            path = path.replace( '/', '\\' );
        }
        else if ( "/".equals( fileSeparator ) )
        {
            path = path.replace( '\\', '/' );
        }
        return path;
    }

    /*copy fromDirectory to the toDirectory, name changed*/
    private static void copyNvmFilDir(File srcFile, File dstFile, FileFilter filter) throws IOException{
        if (!NvmFilDir.exists(srcFile)) {
            throw new FileNotFoundException("Source file[" + srcFile.getCanonicalPath() + "] not found");
        }
        if (NvmFilDir.exists(dstFile)) {
            throw new FileNotFoundException("Target file[" + dstFile.getCanonicalPath() + "] already exists");
        }
        nvmMkDirs(getCanonicalParentSafely(dstFile), false, true);
        NvmFilDir.getNvmFilDir(getCanonicalParentSafely(dstFile)).increaseLocalIndex(dstFile);

        //NvmFilDir.copyNvmFilDir(srcFile, dstFile); //is included in the loop underneath

        if(NvmFilDir.isFile(srcFile) || NvmFilDir.isEmpty(srcFile)){
            NvmFilDir.copyNvmFilDir(srcFile, dstFile); //is included in the loop underneath
            return;
        }

        for(String key: NvmFilDir.getNvmFilDirDirectory(null)){
            if(key.startsWith(srcFile.getCanonicalPath()) && (filter==null || filter.accept(new File(key)))){
                NvmFilDir.copyNvmFilDir(new File(key), new File(dstFile, key.substring(srcFile.getCanonicalPath().length())) );
            }
        }
    }

    //ensure src and dst already exist
    private static void copyDirectoryContent(File srcDirectory, File dstDirectory, FileFilter filter) throws IOException{
        for(String key: NvmFilDir.getNvmFilDirDirectory(srcDirectory.getCanonicalPath())){
            if(key.startsWith(srcDirectory.getCanonicalPath())
                    && !NvmFilDir.exists(new File(dstDirectory, key.substring(srcDirectory.getCanonicalPath().length())))
                    && (filter==null || filter.accept(new File(key)))){
                NvmFilDir.copyNvmFilDir(new File(key), new File(dstDirectory, key.substring(srcDirectory.getCanonicalPath().length())) );
            }
        }
    }

    //FileUtils.copyDirectory used in AbstractInProcessServerBuilder
    /*
     * srcDirectory must exist, dstDirectory can be created if not exist
     * copy srcDirectory's content to the dstDirectory's under path
     * do not copy the same file into dstDirectory
     *
     * */

    public static void copyDirectory(File srcDirectory, File dstDirectory) throws IOException{
        if(srcDirectory == null || dstDirectory == null){
            throw new NullPointerException("Parameter must not be null");
        }
        if(srcDirectory.getCanonicalPath()==dstDirectory.getCanonicalPath()){
            throw new IOException("Source '" + srcDirectory + "' and destination '" + dstDirectory + "' are the same");
        }
        if(!NvmFilDir.exists(srcDirectory) || !NvmFilDir.isDirectory(srcDirectory)){
            throw new IOException("srcDirectory "+srcDirectory.getCanonicalPath()+" must exist, also not a file");
        }
        if(NvmFilDir.isFile(dstDirectory)){
            throw new IOException("dstDirectory "+dstDirectory.getCanonicalPath()+" is not a directory");
        }
        nvmMkDirs(dstDirectory, false, true);
        copyDirectoryContent(srcDirectory, dstDirectory, null);
    }



    /*copy srcFile to the path of dstFile's ParentFile, name changed*/
    public static void copyFile( File srcFile, File dstFile ) throws IOException{
        if( !NvmFilDir.isFile(srcFile )){
            throw new IllegalArgumentException(
                    "Source file must be a file, not " + srcFile.getCanonicalPath());
        }
        NvmFilDir.copyNvmFilDir(srcFile, dstFile);
    }

    /*keep its origin name, so there should not be the same name file(Dir) under the toDirectory*/
    public static void copyRecursively( File fromDirectory, File toDirectory ) throws IOException
    {
        copyRecursively( fromDirectory, toDirectory, null );
    }
    /*keep its origin name, so there should not be the same name file(Dir) under the toDirectory*/
    public static void copyRecursively( File fromDirectory, File toDirectory, FileFilter filter) throws IOException
    {
        copyNvmFilDir(fromDirectory, new File(toDirectory, fromDirectory.getName()), filter);
    }

    /*override or append, create if not exist, createNewFile need its ParentFile exist*/
    public static void writeToFile( File target, String text, boolean append ) throws IOException
    {
        if(!NvmFilDir.exists(target)){
            nvmMkDirs(getCanonicalParentSafely(target), false, true);
            NvmFilDir.putNvmFilDir(target, new NvmFilDir(target.getCanonicalPath(), true, false));
            NvmFilDir.getNvmFilDir(getCanonicalParentSafely(target)).increaseLocalIndex(target);
        }
        if(NvmFilDir.isFile(target)){
            String temp = new String(text.getBytes(),StandardCharsets.UTF_8);
            NvmFilDir.getNvmFilDir(target).write(DatatypeConverter.printHexBinary(temp.getBytes()), append);
            //NvmFilDir.getNvmFilDir(target).write(new String(text.getBytes(),StandardCharsets.UTF_8), append);
        }
    }


    /*below method keep origin*/
    public static File path( String root, String... path )
    {
        return path( new File( root ), path );
    }

    public static File path( File root, String... path )
    {
        for ( String part : path )
        {
            root = new File( root, part );
        }
        return root;
    }

    public interface FileOperation
    {
        void perform() throws IOException;
    }

    public static void windowsSafeIOOperation( FileOperation operation ) throws IOException
    {
        IOException storedIoe = null;
        for ( int i = 0; i < 10; i++ )
        {
            try
            {
                operation.perform();
                return;
            }
            catch ( IOException e )
            {
                storedIoe = e;
                System.gc();
            }
        }
        throw storedIoe;
    }


    private static boolean mayBeWindowsMemoryMappedFileReleaseProblem( IOException e )
    {
        return e.getMessage().contains( "The process cannot access the file because it is being used by another process." );
    }

    public static class MaybeWindowsMemoryMappedFileReleaseProblem extends IOException
    {
        public MaybeWindowsMemoryMappedFileReleaseProblem( IOException e )
        {
            super(e);
        }
    }

    /*above method keep origin*/

    //return with the content of file extend with a "\n"*/
    public static String readTextFile( File file, Charset charset ) throws IOException
    {
        if (NvmFilDir.isDirectory(file)) {
            throw new IllegalArgumentException(
                    "Source must be a file, not " + file.getCanonicalPath());
        }
        String temp = NvmFilDir.getNvmFilDir(file).readAll();
        return (new String(DatatypeConverter.parseHexBinary(temp), charset)+"\n");
        //return (new String(NvmFilDir.getNvmFilDir(file).readAll().getBytes(), charset)+"\n");
    }

    /**
     * Given a directory and a path under it, return filename of the path
     * relative to the directory.
     *
     * @param baseDir The base directory, containing the storeFile
     * @param storeFile The store file path, must be contained under
     * <code>baseDir</code>
     * @return The relative path of <code>storeFile</code> to
     * <code>baseDir</code>
     * @throws IOException As per {@link File#getCanonicalPath()}
     */
    /*example: Dir_test, Dir_test/File/F, File/F returned*/
    public static String relativePath( File baseDir, File storeFile )
            throws IOException
    {
        String prefix = baseDir.getCanonicalPath();
        String path = storeFile.getCanonicalPath();
        if ( !path.startsWith( prefix ) )
        {
            throw new FileNotFoundException();
        }
        path = path.substring( prefix.length() );
        if ( path.startsWith( File.separator ) )
        {
            return path.substring( 1 );
        }
        return path;
    }

    // TODO javadoc what this one does. It comes from Serverutil initially.
    public static File getMostCanonicalFile( File file )
    {
        try
        {
            return file.getCanonicalFile().getAbsoluteFile();
        }
        catch ( IOException e )
        {
            return file.getAbsoluteFile();
        }
    }

    public static void writeAll(StoreFileChannel nvmChannel, ByteBuffer src, long position ) throws IOException {
        nvmChannel.writeAll(src, position);
    }

    public static void writeAll(StoreFileChannel nvmChannel, ByteBuffer src) throws IOException {
        nvmChannel.writeAll(src);
    }

    public static OpenOption[] convertOpenMode(String mode )
    {
        OpenOption[] options;
        switch ( mode )
        {
            case "r": options = new OpenOption[]{READ}; break;
            case "rw": options = new OpenOption[] {CREATE, READ, WRITE}; break;
            case "rws": options = new OpenOption[] {CREATE, READ, WRITE, SYNC}; break;
            case "rwd": options = new OpenOption[] {CREATE, READ, WRITE, DSYNC}; break;
            default: throw new IllegalArgumentException( "Unsupported mode: " + mode );
        }
        return options;
    }

    /*only used in DelegateFileSystemAbstraction.java, supported by StoreFileChannel
    * mode not used yet
    * */
    public static StoreFileChannel open( Path path, String mode ) throws IOException
    {
        FileUtils.nvmMkDirs(path.toFile(), true, false);//ensure there is NvmFilDir with path
        return new StoreFileChannel(NvmFilDir.getNvmFilDir(path.toFile()));
    }

    /**
     * Check if directory is empty.
     * @param directory - directory to check
     * @return false if directory exists and not empty, true otherwise.
     * @throws IllegalArgumentException if specified directory represent a file
     * @throws IOException if some problem encountered during reading directory content
     */
    public static boolean isEmptyDirectory( File directory ) throws IOException
    {
        return NvmFilDir.isEmpty(directory);
    }


    private static File getCanonicalParentSafely(File file) throws IOException {
        if(file.getCanonicalFile().getParentFile()==null || file==null){
            return null;
        }
        return file.getCanonicalFile().getParentFile();
    }






    /*********not supported yet, will be designed to work when connected to the main module*/



     //construct the InputStream for nvm, let its method to be used by other class
    public static InputStream openAsInputStream( Path path ) throws IOException
    {
        return Files.newInputStream( path, READ );
    }


    //construct the OutputStream for nvm, let its method to be used by other class
    public static OutputStream openAsOutputStream( Path path, boolean append ) throws IOException
    {
        OpenOption[] options;
        if ( append )
        {
            options = new OpenOption[] {CREATE, WRITE, APPEND};
        }
        else
        {
            options = new OpenOption[] {CREATE, WRITE};
        }
        return Files.newOutputStream( path, options );
    }

    //drawback: read all of the file's content
    public static BufferedReader newBufferedFileReader(File file, Charset charset ) throws IOException
    {
        return new BufferedReader( new InputStreamReader( new FileInputStream( file ), charset) );
        //return new BufferedReader( new InputStreamReader( new ByteArrayInputStream(NvmFilDir.getNvmFilDir(file).readAll().getBytes(charset)), charset) );
    }

    public static PrintWriter newFilePrintWriter( File file, Charset charset ) throws FileNotFoundException {
        return new PrintWriter( new OutputStreamWriter( new FileOutputStream( file, true ), charset) );
    }

    /*public static void newFilePrintWriterAppend(File file, String text, Charset charset) throws IOException {
        nvmMkDirs(file, true, false);//ensure file exist
        NvmFilDir.getNvmFilDir(file).write(text, true);
    }*/





    public interface LineListener
    {
        void line( String line );
    }


    public static LineListener echo(final PrintStream target )
    {
        return new LineListener()
        {
            @Override
            public void line( String line )
            {
                target.println( line );
            }
        };
    }

    public static void readTextFile( File file, LineListener listener ) throws IOException
    {
        try(BufferedReader reader = new BufferedReader( new FileReader( file ) ))
        {
            String line;
            while ( (line = reader.readLine()) != null )
            {
                listener.line( line );
            }
        }
    }





    public static void printDirectory(){
        NvmFilDir.PrintDirectory(NvmFilDir.class);
    }







}
