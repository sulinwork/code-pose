package com.sulin.codepose.event;

import com.sulin.codepose.event.enums.EventGroupMainHandleResult;
import com.sulin.codepose.event.enums.EventHandleResult;
import com.sulin.codepose.event.enums.EventHandleStatus;
import com.sulin.codepose.event.eventinfo.EventInfo;
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
    private String eventInfo;
    /**
     * 订单事件信息类型
     */
    private String eventInfoType;
    /**
     * 处理状态，-1:初始化、0:待处理、1:已处理、2:处理中
     */
    private EventHandleStatus handleStatus;
    /**
     * 重试次数
     */
    private Integer retryNum;
    /**
     * 执行时间，有分钟级别的延迟时间，依赖重试任务，现在重试任务的时间周期是1分钟，并且查询的是1分钟之前的数据
     * 所以使用这个功能的前提是，时间的延迟时间尽量大于1分钟，并且能接受分钟级别的误差
     */
    private LocalDateTime executeTime;

    /**
     * 初始化订单事件处理信息
     */
    public static EventHandlerInfo init(Event Event, EventHandler<? extends Event> orderEventHandler, List<? extends EventInfo> eventInfos) {
        EventHandlerInfo handlerInfo = null;
        String eventInfo = getValidEventInfo(orderEventHandler, eventInfos);
        // 有事件信息才初始对象
        if (StringUtils.isNoneBlank(eventInfo)) {
            handlerInfo = new EventHandlerInfo();
            handlerInfo.setEventInfo(eventInfo);
            handlerInfo.setEventInfoType(orderEventHandler.concernEventInfoType().getEventInfoType());
            handlerInfo.setEventHandler(orderEventHandler.getClass().getSimpleName());
            if (orderEventHandler.getParentGroupHandler() != null) {
                handlerInfo.setParentGroupHandler(orderEventHandler.getParentGroupHandler().getClass().getSimpleName());
            }
            handlerInfo.setHandleStatus(EventHandleStatus.PROCESSING_STATUS);
            //获取handler执行时间
            LocalDateTime executeTime = LocalDateTime.now();
            if (orderEventHandler instanceof EventDelayHandler) {
                EventDelayHandler orderEventDelayHandler = (EventDelayHandler) orderEventHandler;
                executeTime = orderEventDelayHandler.getExecuteTime(Event, handlerInfo);
                handlerInfo.setHandleStatus(EventHandleStatus.PENDING_STATUS);
            }
            handlerInfo.setExecuteTime(executeTime);
            handlerInfo.setRetryNum(0);
        }
        return handlerInfo;
    }

    private static String getValidEventInfo(EventHandler<? extends Event> eventHandler, List<? extends EventInfo> eventInfos) {
        // 过滤事件处理者关注的eventInfo
        List<EventInfo> concernEventInfos = eventInfos.stream().filter(ei -> eventHandler.concernEventInfoType() == ei.eventInfoType()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(concernEventInfos)) {
            if (concernEventInfos.size() == 1) {
                return Gsons.GSON.toJson(concernEventInfos.get(0));
            } else {
                return  Gsons.GSON.toJson(concernEventInfos);
            }
        }
        return null;
    }

//    /**
//     * 获取事件信息订单对象，带EventExtendInfo
//     */
//    public EventInfo genEventInfo(TypeToken type) {
//        return eventInfoType.getEventInfo(eventInfo, type);
//    }
//
//    /**
//     * 获取事件信息订单对象
//     */
//    public EventInfo genEventInfo() {
//        return genEventInfo(null);
//    }

//    public List<EventInfoResource> getEventInfoResources() {
//        return eventInfoType.getEventInfoResources(eventInfo);
//    }

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
     *
     * @return
     */
    public boolean arrayExecuteTime() {
        LocalDateTime now = LocalDateTime.now();
        if (Objects.nonNull(executeTime) && now.isBefore(executeTime)) {
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
}
