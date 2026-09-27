package com.payflow.backend.service;

import com.payflow.backend.entity.AuditLog;
import com.payflow.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(UUID actorId, String action, String targetType, UUID targetId, Map<String, Object> metadata) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .metadata(metadata)
                .build();
        auditLogRepository.save(auditLog);
    }
}
