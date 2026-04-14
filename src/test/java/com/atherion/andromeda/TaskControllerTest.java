package com.atherion.andromeda;

import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.repositories.ProjectRepository;
import com.atherion.andromeda.repositories.TasksRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TasksControllerTest {

    @Autowired
    private TasksRepository tasksRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void writeAndReadTask() {
        Project project = new Project();
        project.setName("Test Project");
        project.setDescription("Project for integration test");
        project.setStatus("active");
        Project savedProject = projectRepository.save(project);

        Tasks task = new Tasks();
        task.setTitle("Test Integration Task");
        task.setDescription("Testing Oracle connection");
        task.setPriority("high");
        task.setStatus("todo");
        task.setProject(savedProject); // Vinculamos al proyecto que acabamos de crear
        
        Tasks savedTask = tasksRepository.save(task);

        assertNotNull(savedTask.getId());
        System.out.println("Tarea guardada con ID: " + savedTask.getId());

        Optional<Tasks> found = tasksRepository.findById(savedTask.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Integration Task", found.get().getTitle());

        tasksRepository.deleteById(savedTask.getId());
        projectRepository.deleteById(savedProject.getId());
        
        assertFalse(tasksRepository.findById(savedTask.getId()).isPresent());
    }
}