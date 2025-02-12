package com.sulin.codepose.order.chain;

import com.sulin.codepose.event.chain.AbstractEventHandlerChain;
import com.sulin.codepose.event.chain.EventHandlerHolder;
import com.sulin.codepose.event.enums.EnumEventType;
import com.sulin.codepose.order.EnumOrderEventType;
import com.sulin.codepose.order.event.OrderPaidEvent;
import com.sulin.codepose.order.handler.OrderPaidConfirmCouponHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderPaidEventChain extends AbstractEventHandlerChain<OrderPaidEvent> {
    @Override
    public void appendHandler(EventHandlerHolder<OrderPaidEvent> holder) {
        holder.add(OrderPaidConfirmCouponHandler.class);
    }

    @Override
    public EnumEventType getEventType() {
        return EnumOrderEventType.PAID;
    }
}
