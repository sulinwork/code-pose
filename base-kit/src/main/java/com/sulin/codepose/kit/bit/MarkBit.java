package com.sulin.codepose.kit.bit;

import java.util.List;

public interface MarkBit {
    // 下标由右到左从1开始，如4个标记位分别为 true,false,ture,false >> 1010 >> db保存'1010'
    int getIndex();
    // 子标志位的列表集合
    List<MarkSubBit> getMarkSubBitList();
}
