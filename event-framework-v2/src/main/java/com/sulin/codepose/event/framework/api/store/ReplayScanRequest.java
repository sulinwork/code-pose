package com.sulin.codepose.event.framework.api.store;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public final class ReplayScanRequest {

    private final List<String> bizCodes;
    private final Long lastId;
    private final Integer limit;
    private final Integer maxRetryNum;
    private final Instant createdBefore;
    private final Instant executeBefore;

    public ReplayScanRequest(
            List<String> bizCodes,
            Long lastId,
            Integer limit,
            Integer maxRetryNum,
            Instant createdBefore,
            Instant executeBefore
    ) {
        this.bizCodes = bizCodes == null ? Collections.emptyList() : Collections.unmodifiableList(bizCodes);
        this.lastId = lastId;
        this.limit = limit;
        this.maxRetryNum = maxRetryNum;
        this.createdBefore = createdBefore;
        this.executeBefore = executeBefore;
    }

    public List<String> bizCodes() {
        return bizCodes;
    }

    public Long lastId() {
        return lastId;
    }

    public Integer limit() {
        return limit;
    }

    public Integer maxRetryNum() {
        return maxRetryNum;
    }

    public Instant createdBefore() {
        return createdBefore;
    }

    public Instant executeBefore() {
        return executeBefore;
    }
}
