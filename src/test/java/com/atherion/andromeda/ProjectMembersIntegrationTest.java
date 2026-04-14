package com.atherion.andromeda;

import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.ProjectMember;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.model.UserType;
import com.atherion.andromeda.repositories.ProjectMemberRepository;
import com.atherion.andromeda.repositories.ProjectRepository;
import com.atherion.andromeda.repositories.UserRepository;
import com.atherion.andromeda.repositories.UserTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ProjectMembersIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserTypeRepository userTypeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private UserType seedUserType(String suffix) {
        UserType userType = new UserType();
        userType.setUserType("pm_role_" + suffix);
        userType.setDescription("Project member test role");
        return userTypeRepository.save(userType);
    }

    private User seedUser(UserType userType, String suffix) {
        User user = new User();
        user.setUserType(userType);
        user.setName("User " + suffix);
        user.setUsername("pm_user_" + suffix);
        user.setPasswordHash("hash-" + suffix);
        user.setEmail("pm_" + suffix + "@test.com");
        user.setPhone("1234567890");
        return userRepository.save(user);
    }

    private Project seedProject(String suffix) {
        Project project = new Project();
        project.setName("Project " + suffix);
        project.setDescription("Project for ProjectMembers endpoint tests");
        return projectRepository.save(project);
    }

    private ProjectMember seedMember(Project project, User user, String role) {
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        return projectMemberRepository.save(member);
    }

    @Test
    void getById_existing_returns200() throws Exception {
        UserType userType = seedUserType("get_by_id");
        User user = seedUser(userType, "get_by_id");
        Project project = seedProject("get_by_id");
        ProjectMember member = seedMember(project, user, "member");

        mockMvc.perform(get("/api/project-members/{id}", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId()))
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.userId").value(user.getId()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/project-members/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project member not found"));
    }

    @Test
    void create_success_returns201() throws Exception {
        UserType userType = seedUserType("create_ok");
        User user = seedUser(userType, "create_ok");
        Project project = seedProject("create_ok");

        String body = """
                {
                  "projectId": %d,
                  "userId": %d,
                  "role": "manager"
                }
                """.formatted(project.getId(), user.getId());

        mockMvc.perform(post("/api/project-members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.role").value("manager"));
    }

    @Test
    void create_duplicateMember_returns409() throws Exception {
        UserType userType = seedUserType("create_dup");
        User user = seedUser(userType, "create_dup");
        Project project = seedProject("create_dup");
        seedMember(project, user, "member");

        String body = """
                {
                  "projectId": %d,
                  "userId": %d,
                  "role": "member"
                }
                """.formatted(project.getId(), user.getId());

        mockMvc.perform(post("/api/project-members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("User is already a member of this project"));
    }

    @Test
    void update_role_returns200() throws Exception {
        UserType userType = seedUserType("update");
        User user = seedUser(userType, "update");
        Project project = seedProject("update");
        ProjectMember member = seedMember(project, user, "member");

        String body = """
                {
                  "role": "owner"
                }
                """;

        mockMvc.perform(put("/api/project-members/{id}", member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId()))
                .andExpect(jsonPath("$.role").value("owner"));
    }

    @Test
    void delete_existing_thenDeleteAgain_returns404() throws Exception {
        UserType userType = seedUserType("delete");
        User user = seedUser(userType, "delete");
        Project project = seedProject("delete");
        ProjectMember member = seedMember(project, user, "member");

        mockMvc.perform(delete("/api/project-members/{id}", member.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/project-members/{id}", member.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project member not found"));
    }
}

