package com.sulin.codepose.order.event;

import com.sulin.codepose.event.Event;
import com.sulin.codepose.order.EnumOrderEventType;
import lombok.Data;

@Data
public abstract class OrderEvent implements Event<EnumOrderEventType> {
    private String orderNo;
    private Long userId;


}
