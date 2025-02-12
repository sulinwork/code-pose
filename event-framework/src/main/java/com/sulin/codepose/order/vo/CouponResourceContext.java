package com.sulin.codepose.order.vo;

import com.sulin.codepose.event.EventContext;
import lombok.Data;

@Data
public class CouponResourceContext implements EventContext {
    private Long couponId;
}
