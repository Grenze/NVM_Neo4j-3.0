package org.neo4j.io.fs;

public class StoreFileChannelUnwrapper {

    public static StoreFileChannel unwrap( StoreChannel channel )
    {
        return StoreFileChannel.unwrap( channel );
    }

}
