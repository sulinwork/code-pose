package com.sulin.codepose.kit.bit;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class MarkBitHolder {
    private StringBuilder bit;

    public MarkBitHolder() {
        this.bit = new StringBuilder();
    }

    public MarkBitHolder(String bit) {
        this.bit = new StringBuilder(Optional.ofNullable(bit).orElse(""));
    }

    public void set(@NonNull MarkBit markBit) {
        setBit(markBit, EnumDefaultMarkBit.YES);
    }

    public boolean eq(@NonNull MarkBit markBit) {
        return eq(markBit, EnumDefaultMarkBit.YES);
    }

    public boolean eq(@NonNull MarkBit markBit, MarkSubBit subBit) {
        return getBit(markBit) == subBit.getValue();
    }

    public int getBit(@NonNull MarkBit markBit) {
        int index = markBit.getIndex();
        if (index > this.bit.length()) {
            return 0;
        }
        char result = this.bit.charAt(index - 1);
        if (result < '0' || result > '9') {
            throw new IllegalArgumentException("invalid value");
        }
        return this.bit.charAt(index - 1) - '0';
    }

    public int getBit(@NonNull MarkBit markBit, int defaultValue) {
        if (markBit.getIndex() > this.bit.length()) {
            return defaultValue;
        }
        return getBit(markBit);
    }

    public void setBit(@NonNull MarkBit markBit, MarkSubBit markSubBit) {
        if (!markBit.getMarkSubBitList().contains(markSubBit) || markSubBit.getValue() < 0 || markSubBit.getValue() > 9) {
            throw new IllegalArgumentException("markSubBit invalid");
        }
        int index = markBit.getIndex();
        //不足补0
        if (index > this.bit.length()) {
            this.bit = new StringBuilder(StringUtils.rightPad(this.bit.toString(), index, "0"));
        }
        this.bit.replace(index - 1, index, String.valueOf(markSubBit.getValue()));
    }

    public String getBit() {
        return bit.toString();
    }
}
