package com.sulin.code.pose.id;

import com.sulin.code.pose.id.api.Sequence;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

public class MillisecondIncrSequence implements Sequence {
    private static final long MAX_SEQUENCE = 9999; // 最大序列号限制
    // 高32位存秒级时间戳，低32位存序列号
    private final AtomicLong state = new AtomicLong(0);

    @Override
    public String getSequence() {
        while (true) {
            long current = state.get();
            long currentSecond = System.currentTimeMillis() / 1000;
            long lastSecond = current >>> 32;
            long seq = current & 0xFFFFFFFFL;

            long newState;
            if (currentSecond == lastSecond) {
                if (seq >= MAX_SEQUENCE) {
                    // 序列号耗尽，忙等下一秒
                    Thread.yield();
                    continue;
                }
                newState = (currentSecond << 32) | (seq + 1);
            } else {
                newState = currentSecond << 32;
            }

            if (state.compareAndSet(current, newState)) {
                return StringUtils.leftPad(String.valueOf(seq + 1), 4, "0");
            }
        }
    }
}
