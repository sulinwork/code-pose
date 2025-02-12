package com.sulin.codepose.order.event;

import com.sulin.codepose.order.EnumOrderEventType;

public class OrderPaidEvent extends OrderEvent {

    @Override
    public EnumOrderEventType eventType() {
        return EnumOrderEventType.PAID;
    }
}
