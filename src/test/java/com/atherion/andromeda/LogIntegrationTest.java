package com.atherion.andromeda;

import com.atherion.andromeda.model.Log;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.model.UserType;
import com.atherion.andromeda.repositories.LogRepository;
import com.atherion.andromeda.repositories.ProjectRepository;
import com.atherion.andromeda.repositories.TasksRepository;
import com.atherion.andromeda.repositories.UserRepository;
import com.atherion.andromeda.repositories.UserTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class LogIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private LogRepository logRepository;
    @Autowired private TasksRepository tasksRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserTypeRepository userTypeRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private final List<Long> createdLogIds = new ArrayList<>();
    private final List<Long> createdTaskIds = new ArrayList<>();
    private final List<Long> createdProjectIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdUserTypeIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanup() {
        for (Long id : createdLogIds) {
            logRepository.deleteByIdJpql(id);
        }
        for (Long id : createdTaskIds) {
            if (tasksRepository.existsById(id)) {
                tasksRepository.deleteById(id);
            }
        }
        for (Long id : createdUserIds) {
            userRepository.deleteByIdJpql(id);
        }
        for (Long id : createdUserTypeIds) {
            userTypeRepository.deleteByIdJpql(id);
        }
        for (Long id : createdProjectIds) {
            if (projectRepository.existsById(id)) {
                projectRepository.deleteById(id);
            }
        }

        createdLogIds.clear();
        createdTaskIds.clear();
        createdProjectIds.clear();
        createdUserIds.clear();
        createdUserTypeIds.clear();
    }

    private Project createProject(String name) {
        Project project = new Project();
        project.setName(name);
        project.setDescription("Desc " + name);
        project.setStatus("active");
        Project saved = projectRepository.save(project);
        createdProjectIds.add(saved.getId());
        return saved;
    }

    private Tasks createTask(Project project, String title) {
        Tasks task = new Tasks();
        task.setProject(project);
        task.setTitle(title);
        task.setStatus("todo");
        task.setPriority("medium");
        Tasks saved = tasksRepository.save(task);
        createdTaskIds.add(saved.getId());
        return saved;
    }

    private User createUser() {
        UserType userType = new UserType();
        userType.setUserType("log_tester_" + System.nanoTime());
        userType.setDescription("Log integration test role");
        UserType savedType = userTypeRepository.save(userType);
        createdUserTypeIds.add(savedType.getId());

        User user = new User();
        user.setUserType(savedType);
        user.setName("Log Tester");
        user.setUsername("log_user_" + System.nanoTime());
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user.setEmail("log" + System.nanoTime() + "@test.com");
        user.setPhone("123456789");
        User savedUser = userRepository.save(user);
        createdUserIds.add(savedUser.getId());
        return savedUser;
    }

    private Log createLog(User user, String entity, Long entityId, String action, LocalDateTime logDate) {
        Log log = new Log();
        log.setUser(user);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetail("detail-" + action);
        log.setLogDate(logDate);
        Log saved = logRepository.save(log);
        createdLogIds.add(saved.getId());
        return saved;
    }

    @Test
    void getLogs_withProjectFilter_returnsProjectAndTaskLogs() throws Exception {
        Project projectA = createProject("project_a_" + System.nanoTime());
        Project projectB = createProject("project_b_" + System.nanoTime());
        Tasks taskA = createTask(projectA, "task_a");
        Tasks taskB = createTask(projectB, "task_b");
        User user = createUser();

        createLog(user, "project", projectA.getId(), "project_created", LocalDateTime.of(2026, 1, 1, 10, 0));
        createLog(user, "task", taskA.getId(), "task_updated", LocalDateTime.of(2026, 1, 1, 11, 0));
        createLog(user, "task", taskB.getId(), "task_updated_other_project", LocalDateTime.of(2026, 1, 1, 12, 0));

        mockMvc.perform(get("/api/logs")
                        .param("projectId", projectA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getProjectLogs_returnsOnlyLogsForGivenProject() throws Exception {
        Project projectA = createProject("project_logs_" + System.nanoTime());
        Project projectB = createProject("project_other_" + System.nanoTime());
        Tasks taskA = createTask(projectA, "task_project_a");
        Tasks taskB = createTask(projectB, "task_project_b");
        User user = createUser();

        createLog(user, "project", projectA.getId(), "project_event", LocalDateTime.of(2026, 1, 2, 10, 0));
        createLog(user, "task", taskA.getId(), "task_event", LocalDateTime.of(2026, 1, 2, 11, 0));
        createLog(user, "task", taskB.getId(), "other_task_event", LocalDateTime.of(2026, 1, 2, 12, 0));

        mockMvc.perform(get("/api/projects/{projectId}/logs", projectA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void postLog_validPayload_returns201() throws Exception {
        User user = createUser();

        String payload = """
                {
                  "userId": %d,
                  "entity": "project",
                  "entityId": 100,
                  "action": "created",
                  "detail": "Created project from test"
                }
                """.formatted(user.getId());

        String response = mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.entity").value("project"))
                .andExpect(jsonPath("$.action").value("created"))
                .andReturn().getResponse().getContentAsString();

        Long id = Long.valueOf(response.replaceAll(".*\"id\":(\\d+).*", "$1"));
        createdLogIds.add(id);
    }

    @Test
    void postLog_userNotFound_returns404() throws Exception {
        String payload = """
                {
                  "userId": 999999999,
                  "entity": "project",
                  "entityId": 100,
                  "action": "created",
                  "detail": "Invalid user"
                }
                """;

        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }
}
