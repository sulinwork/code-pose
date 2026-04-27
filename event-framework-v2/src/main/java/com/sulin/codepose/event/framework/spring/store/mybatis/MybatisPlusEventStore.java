package com.sulin.codepose.event.framework.spring.store.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.api.store.ReplayScanRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MybatisPlusEventStore extends ServiceImpl<DomainEventRecordMapper, DomainEventRecordEntity> implements EventStore {

    private static final List<ExecutionStatus> RETRYABLE_STATUSES = Arrays.asList(
            ExecutionStatus.PENDING,
            ExecutionStatus.PROCESSING,
            ExecutionStatus.GROUP_MAIN_FINISHED,
            ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT
    );


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void append(DomainEvent event, List<HandlerExecutionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<DomainEventRecordEntity> entities = records.stream().map(DomainEventRecordConverter::toEntity).collect(Collectors.toList());
        boolean re = this.saveBatch(entities);
        if (!re) {
            throw new IllegalStateException("save event error");
        }
    }


    @Override
    public boolean update4VersionCas(HandlerExecutionRecord record) {
        if (record == null || record.getId() == null) {
            return false;
        }
        Long oldVersion = record.getVersion();
        DomainEventRecordEntity entity = DomainEventRecordConverter.toEntity(record);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setVersion(oldVersion + 1L);
        return this.update(entity, Wrappers.lambdaUpdate(DomainEventRecordEntity.class)
                .eq(DomainEventRecordEntity::getId, record.getId())
                .eq(DomainEventRecordEntity::getVersion, oldVersion)
        );
    }

    @Override
    public List<HandlerExecutionRecord> scanRetryable(ReplayScanRequest request) {
        LambdaQueryWrapper<DomainEventRecordEntity> warp = Wrappers.lambdaQuery(DomainEventRecordEntity.class)
                .in(DomainEventRecordEntity::getStatus, retryableStatusNames())
                .in(!CollectionUtils.isEmpty(request.bizCodes()), DomainEventRecordEntity::getBizCode, request.bizCodes())
                .in(Objects.nonNull(request.lastId()), DomainEventRecordEntity::getId, request.lastId())
                .in(Objects.nonNull(request.maxRetryNum()), DomainEventRecordEntity::getRetryNum, request.maxRetryNum())
                .in(Objects.nonNull(request.createdBefore()), DomainEventRecordEntity::getCreatedAt, request.createdBefore())
                .in(Objects.nonNull(request.executeBefore()), DomainEventRecordEntity::getExecuteTime, request.executeBefore())
                .last(Objects.nonNull(request.limit()) && request.limit() > 0, " limit " + request.limit())

                .orderByAsc(DomainEventRecordEntity::getId);

        return toRecords(this.list(warp));
    }

    @Override
    public List<HandlerExecutionRecord> loadByEventKey(String getEventKey) {
        return toRecords(this.list(Wrappers.lambdaQuery(DomainEventRecordEntity.class).eq(DomainEventRecordEntity::getEventKey, getEventKey).orderByAsc(DomainEventRecordEntity::getId)));
    }


    private IllegalStateException duplicateRecordException(HandlerExecutionRecord record, Exception ex) {
        return new IllegalStateException(
                "Duplicate handler record for getEventKey=" + record.getEventKey() + ", handlerCode=" + record.getHandlerCode(),
                ex
        );
    }

    private List<String> retryableStatusNames() {
        List<String> statusNames = new ArrayList<String>(RETRYABLE_STATUSES.size());
        for (ExecutionStatus status : RETRYABLE_STATUSES) {
            statusNames.add(status.name());
        }
        return statusNames;
    }

    private List<HandlerExecutionRecord> toRecords(List<DomainEventRecordEntity> entities) {
        List<HandlerExecutionRecord> records = new ArrayList<HandlerExecutionRecord>(entities.size());
        for (DomainEventRecordEntity entity : entities) {
            records.add(DomainEventRecordConverter.toRecord(entity));
        }
        return records;
    }


    private LocalDateTime toStorageTime(java.time.Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
