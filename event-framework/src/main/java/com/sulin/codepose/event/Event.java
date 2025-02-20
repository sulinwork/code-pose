package com.sulin.codepose.event;



import com.sulin.codepose.event.enums.EventType;
import com.sulin.codepose.event.handler.EventHandler;
import com.sulin.codepose.event.repository.EventRepository;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;


@Data
@Accessors(chain = true)
public abstract class Event {

    @Resource
    protected EventRepository eventRepository;

    protected String bizCode;
    protected Long bizId;

    protected EventType<?> eventType;

    /** 订单事件处理信息 **/
    protected List<EventHandlerInfo> eventHandlerInfoList;

    /**
     * 根据事件处理类获取订单事件处理信息
     */
    public EventHandlerInfo getEventHandlerInfo(EventHandler<? extends Event> eventHandler) {
        if (!CollectionUtils.isEmpty(eventHandlerInfoList)) {
            for (EventHandlerInfo handlerInfo : eventHandlerInfoList) {
                if (eventHandler.getClass().getSimpleName().equals(handlerInfo.getEventHandler())) {
                    return handlerInfo;
                }
            }
        }
        return null;
    }

    /**
     * 保存事件
     */
    public void saveEvent() {
        // 没有事件处理者，不保存事件记录
        if (!CollectionUtils.isEmpty(eventHandlerInfoList)) {
            eventRepository.addEvent(this);
        }
    }

    /**
     * 保存事件（独立事务提交，不受父事务影响）
     */
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveEventIndependent() {
        this.saveEvent();
    }
}
