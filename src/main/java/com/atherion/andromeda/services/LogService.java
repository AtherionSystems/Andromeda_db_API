package com.atherion.andromeda.services;

import com.atherion.andromeda.model.Log;
import com.atherion.andromeda.repositories.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogService {
    private final LogRepository logRepository;

    public List<Log> search(Long projectId, Long taskId, Long userId, LocalDateTime fromDate, LocalDateTime toDate) {
        return logRepository.search(projectId, taskId, userId, fromDate, toDate);
    }

    public List<Log> findByProjectId(Long projectId) {
        return logRepository.search(projectId, null, null, null, null);
    }

    public Log save(Log log) {
        return logRepository.save(log);
    }
}