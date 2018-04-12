package org.neo4j.io.fs;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;


public class StoreFileChannel implements StoreChannel
{
    private NvmFilDir nvmFile;
    private sharePosition locate;

    private class sharePosition{
        public int position;
        sharePosition(){
            position=0;
        }
    }

    //new channel connect to nvmFilDir with position 0
    public StoreFileChannel(NvmFilDir file)
    {
        this.nvmFile = file;
        this.locate = new sharePosition();

    }

    //copy and new channel connect to nvmFilDir with copied's position
    public StoreFileChannel(StoreFileChannel nvmchannel)
    {
        this.nvmFile = nvmchannel.nvmFile;
        this.locate = nvmchannel.locate;
    }

    //convert ByteBuffer to String
    private String byteBufferToString( ByteBuffer buf ){
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes, 0, bytes.length);
        return DatatypeConverter.printHexBinary(bytes);
        //return new String(bytes);
    }

    //convert ByteBuffers to String
    private String  byteBuffersToString( ByteBuffer[] bufs, int offset, int length ){
        String bufsStr = "";
        for(int i=0; i<length; i++){
            bufsStr += byteBufferToString(bufs[offset+i]);
        }
        return bufsStr;
    }



    /*write the content of ByteBuffer to the position of channel, position init 0*/
    @Override
    public int write( ByteBuffer src )throws IOException {
        int temp = nvmFile.write( byteBufferToString(src) , locate.position);
        locate.position += temp;
        return temp;
    }

    /*write the content of every ByteBuffer to the position of channel in order*/
    @Override
    public long write( ByteBuffer[] srcs ) throws IOException{
        int temp = nvmFile.write( byteBuffersToString(srcs, 0, srcs.length),  locate.position);
        locate.position += temp;
        return temp;
    }

    /*write the content of ByteBuffer to the position of channel, position required*/
    @Override
    public int write( ByteBuffer src, long position )throws IOException {
        int temp = nvmFile.write( byteBufferToString(src), Math.toIntExact(position));
        locate.position = Math.toIntExact(position) + temp;
        return temp;
    }

    /*write the content of ByteBuffer[offset:offset+length(<=ByteBuffer.length)] to the position of channel, params>=0, nothing will be written if length == 0*/
    @Override
    public long write( ByteBuffer[] srcs, int offset, int length )throws IOException {
        int temp = nvmFile.write( byteBuffersToString(srcs, offset, length ), locate.position);
        locate.position += temp;
        return temp;
    }

    /*guarantee all bytes will be written*/
    @Override
    public void writeAll( ByteBuffer src, long position ) throws IOException{
        write( src, Math.toIntExact(position) );
        /*long filePosition = position;
        //be sure ByteBuffer.flip() executed
        long expectedEndPosition = filePosition + src.limit() - src.position();
        int bytesWritten;
        while((filePosition += (bytesWritten = write( src, filePosition ))) < expectedEndPosition)
        {
            if( bytesWritten < 0 )
            {
                throw new IOException( "Unable to write to disk, reported bytes written was " + bytesWritten );
            }
        }*/
    }

    @Override
    public void writeAll( ByteBuffer src )throws IOException {
        write(src);
        /*long bytesToWrite = src.limit() - src.position();
        int bytesWritten;
        while((bytesToWrite -= (bytesWritten = write( src ))) > 0)
        {
            if( bytesWritten < 0 )
            {
                throw new IOException( "Unable to write to disk, reported bytes written was " + bytesWritten );
            }
        }*/
    }

    /*truncate from the position*/
    @Override
    public StoreFileChannel truncate( long size )throws IOException {
        nvmFile.truncate( Math.toIntExact(size) );
        if(locate.position>Math.toIntExact(size)){
            locate.position = Math.toIntExact(size);
        }
        return this;
    }

    private static byte[] stringToBytes(String str){
        return DatatypeConverter.parseHexBinary(str);
        //return str.getBytes();
    }

    @Override
    public int read( ByteBuffer dst )throws IOException {
        String getString = nvmFile.read(dst.remaining(),locate.position);
        if(getString.length()==0){
            return -1;
        }
        locate.position += getString.length();
        dst.put(stringToBytes(getString));
        return getString.length()/2;
    }

    @Override
    public long read( ByteBuffer[] dsts )throws IOException {
        return read(dsts, 0, dsts.length);
    }

    @Override
    public int read( ByteBuffer dst, long position ) throws IOException{
        String getString = nvmFile.read(dst.remaining(), Math.toIntExact(position));
        if(getString.length()==0){
            return -1;
        }
        locate.position = Math.toIntExact(position) + getString.length();
        //System.out.println(dst.remaining()+"?="+getString.length());
        dst.put(stringToBytes(getString));
        return getString.length()/2;
    }

    @Override
    public long read( ByteBuffer[] dsts, int offset, int length )throws IOException {
        int sumCapacity = 0;
        for(int i=0; i<length; i++){
            sumCapacity += dsts[offset+i].remaining();
        }
        String getString = nvmFile.read(sumCapacity, locate.position);
        if(getString.length()==0){
            return -1;
        }
        locate.position += getString.length();
        int len = getString.length();
        int bufOrder = offset;
        while(getString.length()>dsts[bufOrder].remaining() && bufOrder<offset+length){
            dsts[bufOrder].put(stringToBytes(getString.substring(0, dsts[bufOrder].remaining())));
            //sub the current bufString then change order
            getString = getString.substring(dsts[bufOrder].remaining());
            bufOrder++;
        }
        dsts[bufOrder].put(stringToBytes(getString));
        return len/2;
    }

    /*position init at 0 when open the channel
     *and move to the newPosition using position method may cause hole in file
     * support method chain
     */
    @Override
    public StoreFileChannel position( long newPosition )throws IOException {
        locate.position = Math.toIntExact(newPosition);
        return this;
    }

    /*return the current position*/
    @Override
    public long position() throws IOException{
        return locate.position;
    }

    /*pcj Transaction provide lock in lower layer, delete the tryLock()
    *    public void addMovie(PersistentString movie) {
         Transaction.run(() -> {
             movies.add(movie);
             movieIndex.add(movie);
         });
        }
    *
    *
    * */
    @Override
    public FileLock tryLock() throws IOException{

        return null;
    }

    @Override
    public boolean isOpen()
    {
        return nvmFile != null;
    }

    @Override
    public void close()throws IOException {
        if(this.nvmFile == null){return;}
        nvmFile.force(true);
        this.nvmFile = null;
        this.locate = null;
        return;
    }

    /*size of file*/
    @Override
    public long size() {
        return nvmFile.getSize();
    }

    /*sync memory to disk*/
    @Override
    public void force( boolean metaData )throws IOException {
        nvmFile.force( metaData );
    }

    @Override
    public void flush() throws IOException{
        force( false );
    }

    /*only used in StoreFileChannelUnwrapper.java*/
    static StoreFileChannel unwrap( StoreChannel channel )
    {
        StoreFileChannel sfc = (StoreFileChannel) channel;
        return sfc;
    }
}
