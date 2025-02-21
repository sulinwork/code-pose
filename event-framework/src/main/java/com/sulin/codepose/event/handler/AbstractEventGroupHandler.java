package com.sulin.codepose.event.handler;


import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.EventHandlerInfo;
import com.sulin.codepose.event.chain.EventChainContext;
import com.sulin.codepose.event.enums.EventGroupMainHandleResult;
import com.sulin.codepose.event.enums.EventHandleResult;
import com.sulin.codepose.event.enums.EventHandleStatus;
import com.sulin.codepose.event.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单事件分组处理者
 */
@Slf4j
@Component
public abstract class AbstractEventGroupHandler<T extends Event> extends AbstractEventHandler<T> implements EventGroupHandler<T> {

    @Resource
    private EventRepository eventRepository;

    protected List<EventHandler<T>> subEventHandlers = new ArrayList<>();

    @Override
    public EventGroupHandler<T> addSubHandlers(List<EventHandler<T>> subEventHandlers) {
        if (!CollectionUtils.isEmpty(subEventHandlers)) {
            checkDelayHandler(subEventHandlers);
            this.subEventHandlers.addAll(subEventHandlers);
        }
        return this;
    }

    private static <T extends Event> void checkDelayHandler(List<EventHandler<T>> subEventHandlers) {
        //暂时不支持子handler是延迟的
        subEventHandlers.forEach(handler->{
            if(handler instanceof EventDelayHandler){
                throw new IllegalArgumentException("分组handler中，子handler不能是延迟handler，原因：暂未实现");
            }
        });
    }


    @Override
    public EventHandleResult handle(T event, EventHandlerInfo handlerInfo, EventChainContext context) {
        // 分组主处理者执行事件处理
        EventGroupMainHandleResult mainResult = mainHandle(event, handlerInfo, context);

        // 分组子处理者执行事件处理
        boolean allSubFinished = subHandle(event, context, mainResult);

        // 当子处理器有失败的情况，主处理器的失败重试次数需要和子处理器一样增加
        if(!allSubFinished) handlerInfo.incrRetryNum();

        return allSubFinished ? EventHandleResult.FINISHED : mainResult.toEventHandleResult();
    }

    private EventGroupMainHandleResult mainHandle(T event, EventHandlerInfo handlerInfo, EventChainContext context) {
        if (handlerInfo.isNeedGroupMainHandle()) {
            try {
                EventGroupMainHandleResult mainResult = doMainHandle(event, handlerInfo, context);
                handlerInfo.updateByHandleResult(mainResult.toEventHandleResult());
                eventRepository.updateEventHandleStatus(event, handlerInfo);
                return mainResult;
            } catch (Exception e) {
                log.error("Group Main Handler {} exception, bizId: {}", this.getClass().getSimpleName(), event.getBizId(), e);
                handlerInfo.updateByHandleException();
                eventRepository.updateEventHandleStatus(event, handlerInfo);
                return EventGroupMainHandleResult.FAIL;
            }
        } else {
            // 根据状态获取事件分组处理结果
            return handlerInfo.getGroupMainResultFromStatus();
        }
    }

    private boolean subHandle(T event, EventChainContext context, EventGroupMainHandleResult mainResult) {

        // 分组主处理者结果不是失败才执行子处理者事件处理
        if (EventGroupMainHandleResult.FAIL != mainResult) {
            boolean allSubFinished = true;
            // 遍历所有事件处理类并进行处理
            for (EventHandler<T> subEventHandler : subEventHandlers) {
                // 获取当前事件处理类信息
                EventHandlerInfo subHandlerInfo = event.getEventHandlerInfo(subEventHandler);
                if (subHandlerInfo == null) {
                    // 事件重试时会出现该情况（只把没处理的处理者查出来执行）
                    continue;
                }
                try {
                    // 根据处理状态决定是否执行事件处理
                    if (subHandlerInfo.isNeedHandle()) {
                        // 分组主处理者为已完成，子处理者执行事件处理
                        if (EventGroupMainHandleResult.GROUP_MAIN_FINISHED == mainResult) {
                            log.info("SubHandler {} handling, bizId: {}", subEventHandler.getClass().getSimpleName(), event.getBizId());
                            // 执行事件处理
                            EventHandleResult subResult = subEventHandler.handle(event, subHandlerInfo, context);
                            context.addHandleResult(subHandlerInfo.getEventHandler(), subResult);
                            subHandlerInfo.updateByHandleResult(subResult);
                            eventRepository.updateEventHandleStatus(event, subHandlerInfo);
                            // 处理结果不是已完成或终止处理，标识所有子处理者未完成
                            if (! subResult.isFinished()) {
                                allSubFinished = false;
                            }
                        } else if (EventGroupMainHandleResult.ABORT == mainResult
                                || EventGroupMainHandleResult.GROUP_MAIN_FINISHED_SUB_ABORT == mainResult) {
                            // 分组主处理者为终止执行或终止子处理者执行，设置所有子处理者为终止执行
                            updateAllSubHandlersAbort(subEventHandler, event);
                        } else {
                            throw new IllegalArgumentException("illegal state, mainResult: " + mainResult);
                        }
                    }
                } catch (Exception e) {
                    allSubFinished = false;
                    log.error("SubHandler {} exception, bizId: {}", subEventHandler.getClass().getSimpleName(), event.getBizId(), e);
                    // 更新本地事件异常状态
                    subHandlerInfo.updateByHandleException();
                    eventRepository.updateEventHandleStatus(event, subHandlerInfo);
                }
            }
            return allSubFinished;
        }
        return false;
    }

    private void updateAllSubHandlersAbort(EventHandler<T> eventHandler, T event) {
        if (eventHandler instanceof EventGroupHandler) {
            List<EventHandler<T>> subHandlers = ((EventGroupHandler<T>) eventHandler).getSubEventHandlers();
            for (EventHandler<T> subHandler : subHandlers) {
                updateAllSubHandlersAbort(subHandler, event);
            }
        }
        EventHandlerInfo subHandlerInfo = event.getEventHandlerInfo(eventHandler);
        subHandlerInfo.setHandleStatus(EventHandleStatus.ABORT_STATUS);
        eventRepository.updateEventHandleStatus(event, subHandlerInfo);
    }

    /**
     * 分组主处理者处理逻辑
     */
    protected abstract EventGroupMainHandleResult doMainHandle(T event, EventHandlerInfo handlerInfo, EventChainContext context);

    @Override
    public List<EventHandler<T>> getSubEventHandlers() {
        return subEventHandlers;
    }
}
