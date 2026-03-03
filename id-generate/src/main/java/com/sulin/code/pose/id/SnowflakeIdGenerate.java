package com.sulin.code.pose.id;

import com.sulin.code.pose.id.api.IdGenerate;
import com.sulin.code.pose.id.api.Sequence;
import com.sulin.code.pose.id.api.WorkerIdHolder;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SnowflakeIdGenerate implements IdGenerate {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final long workerId;

    private final int maxWorkerStrLen;

    private final Sequence sequence;

    public SnowflakeIdGenerate(WorkerIdHolder workerIdHolder, Sequence sequence) {
        this.workerId = workerIdHolder.getWorkerId();
        this.sequence = sequence;
        this.maxWorkerStrLen = String.valueOf(workerIdHolder.getMaxWorkerId()).length();
    }

    @Override
    public String getId() {
        String time = LocalDateTime.now().format(dateTimeFormatter);
        String workId = StringUtils.leftPad(String.valueOf(workerId), maxWorkerStrLen, "0");
        return time
                + workId
                + sequence.getSequence();

    }
}
