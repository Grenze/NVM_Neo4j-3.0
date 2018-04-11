package org.neo4j.io.fs;

import lib.util.persistent.ObjectDirectory;
import lib.util.persistent.ObjectPointer;
import lib.util.persistent.PersistentObject;
import lib.util.persistent.PersistentString;
import lib.util.persistent.types.BooleanField;
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.StringField;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static lib.util.persistent.Util.persistent;

public class NvmFilDir  extends PersistentObject{
    private static final StringField GLOBALID = new StringField();
    private static final StringField LOCALINDEX = new StringField();
    private static final StringField FILECONTENT = new StringField();
    private static final BooleanField ISFILE = new BooleanField();
    private static final BooleanField ISDIRECTORY = new BooleanField();

    private static final ObjectType<NvmFilDir> TYPE = ObjectType.withFields(NvmFilDir.class, GLOBALID, LOCALINDEX, FILECONTENT, ISFILE, ISDIRECTORY);
    //only this param about File is String, convert to canonicalPath before pass in
    public NvmFilDir(String uniqueFileName, boolean isFile, boolean isDirectory){
        super(TYPE);
        setGlobalId(uniqueFileName);
        setLocalIndex("");
        setFileContent("");
        setIsFile(isFile);
        setIsDirectory(isDirectory);
    }

    private NvmFilDir(ObjectPointer<NvmFilDir> p){
        super(p);
    }
    //copy
    public NvmFilDir(NvmFilDir nvmFilDir){
        super(TYPE);
        setGlobalId(nvmFilDir.getGlobalId());
        setLocalIndex(nvmFilDir.getLocalIndex());
        setFileContent(nvmFilDir.getFileContent());
        setIsFile(nvmFilDir.getIsFile());
        setIsDirectory(nvmFilDir.getIsDirectory());
    }

    private void setGlobalId(String globalId){
        setObjectField(GLOBALID, persistent(globalId));
    }

    private String getGlobalId(){
        return getObjectField(GLOBALID).toString();
    }


    private void setLocalIndex(String localIndex){
        synchronized(LOCALINDEX) {

            setObjectField(LOCALINDEX, persistent(localIndex));
        }
    }

    private String getLocalIndex(){
        synchronized(LOCALINDEX) {

            return getObjectField(LOCALINDEX).toString();
        }
    }


    private void setFileContent(String fileContent){
        synchronized (FILECONTENT) {
            System.out.println("Write " + getGlobalId() + " From DRAM To NVM");
            System.out.println(fileContent);
            setObjectField(FILECONTENT, persistent(fileContent));
        }
    }

    private String getFileContent(){
        synchronized (FILECONTENT) {
            System.out.println("Read " + getGlobalId() + " From NVM To DRAM");
            System.out.println(getObjectField(FILECONTENT).toString());
            return getObjectField(FILECONTENT).toString();
        }
    }


    private void setIsFile(boolean isFile){
        setBooleanField(ISFILE, isFile);
    }

    private boolean getIsFile(){
        return getBooleanField(ISFILE);
    }


    private void setIsDirectory(boolean isDirectory){
        setBooleanField(ISDIRECTORY, isDirectory);
    }

    private boolean getIsDirectory(){
        return getBooleanField(ISDIRECTORY);
    }



    /*above set/get method*/


    public int write(String src, int position){
        int positionLocal = position * 2;
        if(src.length() == 0 || positionLocal < 0){return 0;}
        String originContent = getFileContent();
        int offset = positionLocal - originContent.length();
        if(offset > 0){
            setFileContent(originContent + new String(new byte[offset])+ src);
        }
        else{
            if(positionLocal+src.length()<=originContent.length()){
                setFileContent(originContent.substring(0, positionLocal) + src + originContent.substring(positionLocal + src.length()));
            }
            else{
                setFileContent(originContent.substring(0, positionLocal) + src);
            }
        }
        return src.length()/2;
    }

    public void write(String text, boolean append){
        if(append){
            write(text, getFileContent().length()/2);
        }
        else{
            write(text, 0);
        }
    }

    public String readAll(){
            return getFileContent();
    }



    public void truncate(int size){
            String originContent = getFileContent();
            setFileContent(originContent.substring(0, size*2));
    }

    public String read(int length, int position){
        int positionLocal = position * 2;
        int lengthLocal = length * 2;
        String originContent = getFileContent();
            if (positionLocal >= originContent.length() || positionLocal < 0 || lengthLocal <= 0) {
                return "";
            }
            if (positionLocal + lengthLocal <= originContent.length()) {
                return originContent.substring(positionLocal, positionLocal + lengthLocal);
            } else {
                return originContent.substring(positionLocal);
            }
    }

    public long getSize(){
            return getFileContent().length()/2;
    }
    //remained to complete
    public void force(boolean metadata){
            if (ObjectDirectory.get(getGlobalId(), NvmFilDir.class) == null) {
                ObjectDirectory.put(getGlobalId(), this);
            }

            FileUtils.printDirectory();
    }



    //"/"not used in file's name
    public void increaseLocalIndex(File newLocalSub){
            setLocalIndex(getLocalIndex()+"/"+newLocalSub.getName());
    }

    public void decreaseLocalIndex(File oldLocalSub){
            setLocalIndex(getLocalIndex().replace(("/" + oldLocalSub.getName()), ""));
    }
    //remove the first "/"
    public String[] getSubList(){
            if (getLocalIndex().length() == 0) {
                return null;
            }
            return getLocalIndex().substring(1).split("/");
    }

    public void renameSelf(File src, File dst) throws IOException{
            setGlobalId(dst.getCanonicalPath());
            NvmFilDir.putNvmFilDir(dst, NvmFilDir.removeNvmFilDir(src));
    }


    /*below are static methods*/



    public static boolean exists(File file)  {

        try {
            return (ObjectDirectory.get(file.getCanonicalPath(), NvmFilDir.class)!=null);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static NvmFilDir removeNvmFilDir(File file) throws IOException{

        return ObjectDirectory.remove(file.getCanonicalPath(), NvmFilDir.class);
    }

    public static NvmFilDir getNvmFilDir(File file) {

        try {
            return ObjectDirectory.get(file.getCanonicalPath(), NvmFilDir.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void putNvmFilDir(File file, NvmFilDir nvmFilDir) throws IOException{

        ObjectDirectory.put(file.getCanonicalPath(), nvmFilDir);
        //PrintDirectory(NvmFilDir.class);
    }

    public static void copyNvmFilDir(File src, File dst)throws IOException{
        NvmFilDir dstNvmFilDir = new NvmFilDir(NvmFilDir.getNvmFilDir(src));
        dstNvmFilDir.setGlobalId(dst.getCanonicalPath());
        NvmFilDir.putNvmFilDir(dst, dstNvmFilDir);
    }

    public static boolean isEmpty(File file) throws IOException {

        return ObjectDirectory.get(file.getCanonicalPath(), NvmFilDir.class).getLocalIndex().length() == 0;
    }

    public static boolean isFile(File file) throws IOException{

        if(ObjectDirectory.get(file.getCanonicalPath(), NvmFilDir.class)==null){
            return false;
        }
        return ObjectDirectory.get(file.getCanonicalPath(), NvmFilDir.class).getIsFile();
    }

    public static boolean isDirectory(File file) {

        try {
            if(ObjectDirectory.get(file.getCanonicalPath(), NvmFilDir.class)==null){
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return  ObjectDirectory.get(file.getCanonicalPath(), NvmFilDir.class).getIsDirectory();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static File[] listLocalFiles(File directory, FilenameFilter filter ){
        String[] subs = NvmFilDir.getNvmFilDir(directory).getSubList();
        if(subs == null){
            return null;
        }
        ArrayList temp = new ArrayList();
        for(String sub: subs){
            if(filter == null || filter.accept(directory, sub)){
                temp.add(new File(directory, sub));
            }
        }
        return (File[])temp.toArray(new File[temp.size()]);
    }


    //ObjectDirectory's HashMap's key format: mark + class.getName()
    public static List<String> getNvmFilDirDirectory(String reMove){
        List<String> keyList = new ArrayList<>();
        String nvmClass = NvmFilDir.class.getName();
        for(PersistentString key: ObjectDirectory.getDirectory()){
            if(key.toString().endsWith(nvmClass)){
                keyList.add(key.toString().replace(nvmClass,""));
            }
        }
        keyList.remove(reMove);//remove safely
        return keyList;
    }

    //Print ObjectDirectory's HashMap's key Set
    public static void PrintDirectory(Class cls){
        List<PersistentString> keyList = new ArrayList<>(ObjectDirectory.getDirectory());
        Collections.sort(keyList, new Comparator<PersistentString>() {
            @Override
            public int compare(PersistentString s1, PersistentString s2) {
                //if(s1.toString().split("/").length == s2.toString().split("/").length){
                    return s1.toString().replace(cls.getName(),"").compareTo(s2.toString().replace(cls.getName(),""));
                //}
                //return s1.toString().split("/").length - s2.toString().split("/").length;
            }
        });
        System.out.println("\n------"+cls.getName()+"------\n");
        for(PersistentString key: keyList){
            System.out.println(key.toString().replace(cls.getName(),""));
        }
        System.out.println("\n------"+cls.getName()+"------\n");
    }




}
