package com.sulin.codepose.order.handler;

import com.sulin.codepose.event.enums.EnumEventContextType;
import com.sulin.codepose.event.handler.EventContextWarpHandler;
import com.sulin.codepose.order.EnumOrderEventContextType;
import com.sulin.codepose.order.event.OrderPaidEvent;
import com.sulin.codepose.order.vo.CouponResourceContext;
import org.springframework.stereotype.Component;


@Component
public class OrderPaidConfirmCouponHandler extends EventContextWarpHandler<OrderPaidEvent, CouponResourceContext> {

    @Override
    public void handler(OrderPaidEvent event, CouponResourceContext ctx) {

    }

    @Override
    protected EnumEventContextType getContextType() {
        return EnumOrderEventContextType.COUPON;
    }
}
