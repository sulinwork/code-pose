package com.sulin.codepose.event.chain;

import com.sulin.codepose.event.enums.EnumEventType;
import com.sulin.codepose.event.enums.EnumOrderEventType;
import org.springframework.stereotype.Component;

@Component
public class OrderPaidEventHandlerChain extends AbstractEventHandlerChain {
    @Override
    public void appendHandler(EventHandlerHolder holder) {
        holder.add(null)
                .addWithChild(null,
                        null, null);
    }


    @Override
    public EnumEventType getEventType() {
        return EnumOrderEventType.PAID;
    }
}
