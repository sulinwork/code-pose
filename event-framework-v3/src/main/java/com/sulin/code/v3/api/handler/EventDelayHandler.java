package com.sulin.code.v3.api.handler;





import com.sulin.code.v3.api.Event;
import com.sulin.code.v3.api.EventHandlerInfo;

import java.time.LocalDateTime;


public interface EventDelayHandler<T extends Event> {


    LocalDateTime getExecuteTime(T event, EventHandlerInfo handlerInfo);

}
