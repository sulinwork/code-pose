package com.sulin.codepose.event.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


@Getter
@AllArgsConstructor
public enum EventHandleStatus {
    // 待处理
    PENDING_STATUS(0),
    // 已处理
    FINISHED_STATUS(1),
    // 处理中
    PROCESSING_STATUS(2),
    // 终止处理
    ABORT_STATUS(3),
    // 分组主处理者已处理，子处理者待处理
    GROUP_MAIN_FINISHED_STATUS(4),
    // 分组主处理者已处理，子处理者待终止处理
    GROUP_MAIN_FINISHED_SUB_ABORT_STATUS(5),

    ;

    private static final Map<Integer, EventHandleStatus> statusMap = Arrays.stream(values()).collect(Collectors.toMap(EventHandleStatus::getStatus, Function.identity(), (k1, k2) -> k1));

    private final Integer status;

    public static EventHandleStatus getByStatus(Integer status) {
        return Optional.ofNullable(statusMap.get(status)).orElseThrow(() -> new IllegalArgumentException("无效的订单事件处理状态"));
    }
}
