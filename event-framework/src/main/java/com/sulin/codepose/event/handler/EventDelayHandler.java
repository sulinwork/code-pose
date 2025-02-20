package com.sulin.codepose.event.handler;



import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.EventHandlerInfo;

import java.time.LocalDateTime;


public interface EventDelayHandler<T extends Event> {


    LocalDateTime getExecuteTime(T event, EventHandlerInfo handlerInfo);

}
