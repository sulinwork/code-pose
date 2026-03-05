package com.sulin.code.pose.id;

import com.sulin.code.pose.id.api.Sequence;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

public class SequentialRandomSequence implements Sequence {

    private final int bufferCount;
    private final int factor;
    private final int cycleLen;
    private final int maxValue;
    private final AtomicLong globalSequence = new AtomicLong(0);

    public SequentialRandomSequence(int bufferCount, int factor) {
        this(bufferCount, factor, 2);
    }

    public SequentialRandomSequence(int bufferCount, int factor, int cycleLen) {
        this.bufferCount = bufferCount;
        this.factor = factor;
        this.cycleLen = cycleLen;
        this.maxValue = (int) Math.pow(10, factor);
    }

    @Override
    public String getSequence() {
        long seq = globalSequence.getAndIncrement();
        
        int bufferId = (int) (seq % bufferCount);
        long value = (seq / bufferCount) % maxValue;
        long cycle = (seq / (bufferCount * maxValue)) % (long) Math.pow(10, cycleLen);
        
        return bufferId + 
               StringUtils.leftPad(String.valueOf(cycle), cycleLen, "0") + 
               StringUtils.leftPad(String.valueOf(value), factor, "0");
    }
}
