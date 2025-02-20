package com.sulin.codepose.kit.bit;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum EnumDefaultMarkBit implements MarkSubBit {

    NO(0),
    YES(1);

    private final Integer value;

    static final Map<Integer, EnumDefaultMarkBit> valueMap = Arrays.stream(values()).collect(Collectors.toMap(EnumDefaultMarkBit::getValue, Function.identity()));

    public static EnumDefaultMarkBit getByValue(int value) {
        return Optional.ofNullable(valueMap.get(value)).orElseThrow(() -> new IllegalArgumentException("invalid value"));
    }

    public static final List<MarkSubBit> DEFAULT_MARK_SUB_BIT_LIST = Arrays.asList(values());
}
