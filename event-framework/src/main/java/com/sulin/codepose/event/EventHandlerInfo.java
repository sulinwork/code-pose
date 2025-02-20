package com.sulin.codepose.event;

import com.sulin.codepose.event.enums.EventGroupMainHandleResult;
import com.sulin.codepose.event.enums.EventHandleResult;
import com.sulin.codepose.event.enums.EventHandleStatus;
import com.sulin.codepose.event.enums.EventHandlerMode;
import com.sulin.codepose.event.eventinfo.EventHandlerContext;
import com.sulin.codepose.event.handler.EventDelayHandler;
import com.sulin.codepose.event.handler.EventHandler;
import com.sulin.codepose.kit.json.Gsons;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 订单事件处理信息
 */
@Data
@Accessors(chain = true)
@Slf4j
public class EventHandlerInfo {

    /**
     * 事件处理类
     */
    private String eventHandler;
    /**
     * 事件处理类
     */
    private String parentGroupHandler;
    /**
     * 事件信息
     */
    private String context;

    /**
     * 处理状态，-1:初始化、0:待处理、1:已处理、2:处理中
     */
    private EventHandleStatus handleStatus;
    /**
     * 重试次数
     */
    private Integer retryNum;
    /**
     * 期望执行时间，有延迟时间，依赖xxl job
     */
    private LocalDateTime expectedExecuteTime;

    /**
     * 乐观锁
     */
    private Integer version;

    //handler模式  0:普通  1:未来
    private EventHandlerMode handlerMode;

    /**
     * 初始化订单事件处理信息
     */
    public static EventHandlerInfo init(Event Event, EventHandler<? extends Event> orderEventHandler, List<? extends EventHandlerContext> eventHandlerContexts) {
        EventHandlerInfo handlerInfo = new EventHandlerInfo();
        ;
        String context = matchContext(orderEventHandler, eventHandlerContexts);
        handlerInfo.setContext(context);
        handlerInfo.setEventHandler(orderEventHandler.getClass().getSimpleName());
        if (orderEventHandler.getParentGroupHandler() != null) {
            handlerInfo.setParentGroupHandler(orderEventHandler.getParentGroupHandler().getClass().getSimpleName());
        }
        handlerInfo.setHandleStatus(EventHandleStatus.PROCESSING_STATUS);
        handlerInfo.setHandlerMode(EventHandlerMode.NORMAL);
        //获取handler执行时间
        LocalDateTime executeTime = LocalDateTime.now();
        if (orderEventHandler instanceof EventDelayHandler) {
            EventDelayHandler orderEventDelayHandler = (EventDelayHandler) orderEventHandler;
            executeTime = orderEventDelayHandler.getExecuteTime(Event, handlerInfo);
            handlerInfo.setHandleStatus(EventHandleStatus.PENDING_STATUS);
            handlerInfo.setHandlerMode(EventHandlerMode.FUTURE);
        }
        handlerInfo.setExpectedExecuteTime(executeTime);
        handlerInfo.setRetryNum(0);
        return handlerInfo;
    }

    private static String matchContext(EventHandler<? extends Event> eventHandler, List<? extends EventHandlerContext> eventInfos) {
        // 过滤事件处理者关注的eventInfo
        List<EventHandlerContext> concernEventHandlerContexts = eventInfos.stream().filter(ei -> Objects.equals(eventHandler.concernEventHandlerContextUniqueCode(), ei.contextUniqueCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(concernEventHandlerContexts)) return null;
        return Gsons.GSON.toJson(concernEventHandlerContexts.get(0));
    }

    public void incrRetryNum() {
        retryNum++;
    }

    /**
     * 通过事件处理结果更新处理状态
     */
    public void updateByHandleResult(EventHandleResult result) {
        if (EventHandleResult.FINISHED == result) {
            handleStatus = EventHandleStatus.FINISHED_STATUS;
        } else if (EventHandleResult.GROUP_MAIN_FINISHED == result) {
            handleStatus = EventHandleStatus.GROUP_MAIN_FINISHED_STATUS;
        } else if (EventHandleResult.GROUP_MAIN_FINISHED_SUB_ABORT == result) {
            handleStatus = EventHandleStatus.GROUP_MAIN_FINISHED_SUB_ABORT_STATUS;
        } else if (EventHandleResult.FAIL == result) {
            handleStatus = EventHandleStatus.PENDING_STATUS;
            incrRetryNum();
        } else if (EventHandleResult.ABORT == result) {
            handleStatus = EventHandleStatus.ABORT_STATUS;
        } else {
            throw new IllegalArgumentException("illegal state, EventHandleResult: " + result);
        }
    }

    /**
     * 通过事件处理结果更新处理状态
     */
    public void updateByHandleResultNotIncrRetryNum(EventHandleResult result) {
        if (EventHandleResult.FINISHED == result) {
            handleStatus = EventHandleStatus.FINISHED_STATUS;
        } else if (EventHandleResult.GROUP_MAIN_FINISHED == result) {
            handleStatus = EventHandleStatus.GROUP_MAIN_FINISHED_STATUS;
        } else if (EventHandleResult.GROUP_MAIN_FINISHED_SUB_ABORT == result) {
            handleStatus = EventHandleStatus.GROUP_MAIN_FINISHED_SUB_ABORT_STATUS;
        } else if (EventHandleResult.FAIL == result) {
            handleStatus = EventHandleStatus.PENDING_STATUS;
        } else if (EventHandleResult.ABORT == result) {
            handleStatus = EventHandleStatus.ABORT_STATUS;
        } else {
            throw new IllegalArgumentException("illegal state, EventHandleResult: " + result);
        }
    }

    /**
     * 事件处理异常时更新处理状态
     */
    public void updateByHandleException() {
        handleStatus = EventHandleStatus.PENDING_STATUS;
        incrRetryNum();
    }

    /**
     * 根据状态判断是否需要执行事件处理
     */
    public boolean isNeedHandle() {
        return EventHandleStatus.PENDING_STATUS == handleStatus
                || EventHandleStatus.PROCESSING_STATUS == handleStatus
                || EventHandleStatus.GROUP_MAIN_FINISHED_STATUS == handleStatus
                || EventHandleStatus.GROUP_MAIN_FINISHED_SUB_ABORT_STATUS == handleStatus;
    }

    /**
     * 延迟任务是否到达执行时间
     */
    public boolean arrayExecuteTime() {
        LocalDateTime now = LocalDateTime.now();
        if (Objects.nonNull(expectedExecuteTime) && now.isBefore(expectedExecuteTime)) {
            log.info("延迟任务还未到执行时间:{},now:{}", eventHandler, now);
            return false;
        }
        return true;
    }

    /**
     * 根据状态判断是否需要执行事件分组主处理
     */
    public boolean isNeedGroupMainHandle() {
        return EventHandleStatus.PENDING_STATUS == handleStatus
                || EventHandleStatus.PROCESSING_STATUS == handleStatus;
    }

    /**
     * 根据状态获取事件分组处理结果
     */
    public EventGroupMainHandleResult getGroupMainResultFromStatus() {
//        Checks.notNull(handleStatus);

        if (EventHandleStatus.PENDING_STATUS == handleStatus || EventHandleStatus.PROCESSING_STATUS == handleStatus) {
            return EventGroupMainHandleResult.FAIL;
        } else if (EventHandleStatus.ABORT_STATUS == handleStatus) {
            return EventGroupMainHandleResult.ABORT;
        } else if (EventHandleStatus.GROUP_MAIN_FINISHED_STATUS == handleStatus) {
            return EventGroupMainHandleResult.GROUP_MAIN_FINISHED;
        } else if (EventHandleStatus.GROUP_MAIN_FINISHED_SUB_ABORT_STATUS == handleStatus) {
            return EventGroupMainHandleResult.GROUP_MAIN_FINISHED_SUB_ABORT;
        } else {
            throw new IllegalArgumentException("illegal state, handleStatus: " + handleStatus);
        }
    }

    public <T extends EventHandlerContext> T parseContext(Class<T> t) {
        return Optional.ofNullable(this.context).map(c -> Gsons.GSON.fromJson(c, t)).orElse(null);
    }
}
