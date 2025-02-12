package com.sulin.codepose.order;

import com.sulin.codepose.event.EventContext;
import com.sulin.codepose.event.enums.EnumEventContextType;
import com.sulin.codepose.event.enums.EnumEventType;
import com.sulin.codepose.kit.json.Gsons;
import com.sulin.codepose.order.vo.CouponResourceContext;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public enum EnumOrderEventContextType implements EnumEventContextType {
    COUPON("coupon"){
        @Override
        public <T extends EventContext> T decodeContext(String context) {
            return (T) Gsons.GSON.fromJson(context, CouponResourceContext.class);
        }
    };
    private final String eventContextType;
}
