package com.atherion.andromeda;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.atherion.andromeda.model.TaskAssignment;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.repositories.TaskAssignmentRepository;
import com.atherion.andromeda.repositories.TasksRepository;
import com.atherion.andromeda.repositories.UserRepository;

@SpringBootTest
class TaskAssignmentControllerTest {

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private TasksRepository tasksRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void writeAndReadAssignment() {
        User user = userRepository.findAll().get(0); 
        Tasks task = tasksRepository.findAll().get(0);

        TaskAssignment assignment = new TaskAssignment();
        assignment.setUser(user);
        assignment.setTask(task);

        TaskAssignment saved = taskAssignmentRepository.save(assignment);
        assertNotNull(saved.getId());

        Optional<TaskAssignment> found = taskAssignmentRepository.findById(saved.getId());
        assertTrue(found.isPresent());

        taskAssignmentRepository.deleteById(saved.getId());
        assertFalse(taskAssignmentRepository.findById(saved.getId()).isPresent());
    }
}