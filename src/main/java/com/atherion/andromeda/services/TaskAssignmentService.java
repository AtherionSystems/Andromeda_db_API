// TaskAssignmentService.java
package com.atherion.andromeda.services;

import com.atherion.andromeda.model.TaskAssignment;
import com.atherion.andromeda.repositories.TaskAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskAssignmentService {
    private final TaskAssignmentRepository taskAssignmentRepository;

    public List<TaskAssignment> findAll() { return taskAssignmentRepository.findAll(); }
    public Optional<TaskAssignment> findById(Long id) { return taskAssignmentRepository.findById(id); }
    public TaskAssignment save(TaskAssignment taskAssignment) { return taskAssignmentRepository.save(taskAssignment); }
    public void deleteById(Long id) { taskAssignmentRepository.deleteById(id); }
}