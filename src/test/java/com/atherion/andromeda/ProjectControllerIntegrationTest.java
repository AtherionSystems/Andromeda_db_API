package com.atherion.andromeda;

import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.repositories.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ProjectControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ProjectRepository projectRepository;

    private MockMvc mockMvc;
    private final List<Long> createdProjectIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanup() {
        for (Long id : createdProjectIds) {
            if (projectRepository.existsById(id)) {
                projectRepository.deleteById(id);
            }
        }
        createdProjectIds.clear();
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

    @Test
    void getAll_returns200AndContainsCreatedProject() throws Exception {
        Project p = createProject("project_all_" + System.nanoTime());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(p.getId().intValue())));
    }

    @Test
    void getById_existingProject_returns200() throws Exception {
        Project p = createProject("project_get_" + System.nanoTime());

        mockMvc.perform(get("/api/projects/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId()))
                .andExpect(jsonPath("$.name").value(p.getName()))
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void getById_missingProject_returns404() throws Exception {
        mockMvc.perform(get("/api/projects/{id}", 999999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void create_validPayload_returns201AndDefaultActiveStatus() throws Exception {
        String payload = """
                {
                  "name": "API Project Create",
                  "description": "Created from test"
                }
                """;

        String response = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("API Project Create"))
                .andExpect(jsonPath("$.status").value("active"))
                .andReturn().getResponse().getContentAsString();

        Long id = Long.valueOf(response.replaceAll(".*\"id\":(\\d+).*", "$1"));
        createdProjectIds.add(id);
    }

    @Test
    void create_missingName_returns400() throws Exception {
        String payload = """
                {
                  "description": "Invalid project"
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_existingProject_returns200AndUpdatesFields() throws Exception {
        Project p = createProject("project_patch_" + System.nanoTime());

        String payload = """
                {
                  "name": "Patched Project",
                  "status": "paused",
                  "description": "Updated description"
                }
                """;

        mockMvc.perform(patch("/api/projects/{id}", p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId()))
                .andExpect(jsonPath("$.name").value("Patched Project"))
                .andExpect(jsonPath("$.status").value("paused"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void patch_missingProject_returns404() throws Exception {
        String payload = """
                {
                  "name": "Not Found"
                }
                """;

        mockMvc.perform(patch("/api/projects/{id}", 999999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void delete_existingProject_returns204() throws Exception {
        Project p = createProject("project_delete_" + System.nanoTime());

        mockMvc.perform(delete("/api/projects/{id}", p.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", p.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_missingProject_returns404() throws Exception {
        mockMvc.perform(delete("/api/projects/{id}", 999999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }
}