package com.sulin.code.pose.id.api;

public interface Sequence {
    String getSequence();



    default void close(){
        //pass
    }
}
