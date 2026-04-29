package com.atherion.andromeda.telegram;

import com.atherion.andromeda.services.AiService;
import com.atherion.andromeda.dto.SprintTaskRow;
import com.atherion.andromeda.model.*;
import com.atherion.andromeda.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BotCommandHandler {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private static final BigDecimal MAX_TASK_HOURS = new BigDecimal("4.0");

    private static final Set<String> VALID_PROJECT_STATUSES =
            Set.of("active", "paused", "completed", "cancelled");
    private static final Set<String> VALID_TASK_STATUSES =
            Set.of("todo", "in_progress", "review", "done");
    private static final Set<String> VALID_TASK_PRIORITIES =
            Set.of("low", "medium", "high", "critical");
    private static final Set<String> VALID_SPRINT_STATUSES =
            Set.of("planned", "active", "completed");
    private static final Set<String> VALID_MEMBER_ROLES =
            Set.of("owner", "manager", "member");

    private final ProjectService        projectService;
    private final TasksService          tasksService;
    private final UserService           userService;
    private final ProjectMemberService  projectMemberService;
    private final SprintService         sprintService;
    private final SprintStoryAssignmentService sprintStoryAssignmentService;
    private final TaskAssignmentService taskAssignmentService;
    private final CapabilityService     capabilityService;
    private final FeatureService        featureService;
    private final UserStoryService      userStoryService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AiService             aiService;

    // ── entry point ──────────────────────────────────────────────────────────

    @Transactional
    public String handle(String rawText, Long telegramUserId) {
        String text  = rawText.trim();
        String[] parts = text.split("\\s+", 2);

        String cmd = parts[0];
        if (cmd.contains("@")) cmd = cmd.substring(0, cmd.indexOf('@'));
        String args = parts.length > 1 ? parts[1].trim() : "";

        return switch (cmd.toLowerCase()) {
            // ── info ──────────────────────────────────────────────────────
            case "/help"          -> handleHelp();
            case "/ping"          -> "Pong! Andromeda API is up and running.";
            case "/health"        -> "Status: OK\nService: Andromeda Backend API\nBot: Connected";
            // ── auth ──────────────────────────────────────────────────────
            case "/link"          -> handleLink(args, telegramUserId);
            // ── read ──────────────────────────────────────────────────────
            case "/projects"        -> handleProjects();
            case "/project"         -> handleProject(args);
            case "/capabilities"    -> handleCapabilities(args);
            case "/capability"      -> handleCapability(args);
            case "/features"        -> handleFeatures(args);
            case "/feature"         -> handleFeature(args);
            case "/projectstories"  -> handleProjectStories(args);
            case "/userstories"     -> handleUserStories(args);
            case "/userstory"       -> handleUserStory(args);
            case "/tasks"           -> handleTasks(args);
            case "/task"            -> handleTask(args);
            case "/members"         -> handleMembers(args);
            case "/sprints"         -> handleSprints(args);
            case "/sprinttasks"     -> handleSprintTasks(args);
            case "/users"           -> handleUsers();
            case "/user"            -> handleUser(args);
            // ── write ─────────────────────────────────────────────────────
            case "/newproject"    -> handleNewProject(args, telegramUserId);
            case "/newsprint"     -> handleNewSprint(args, telegramUserId);
            case "/newtask"       -> handleNewTask(args, telegramUserId);
            case "/assigntask"    -> handleAssignTask(args, telegramUserId);
            case "/addsprinttask" -> handleAssignTask(args, telegramUserId);
            case "/completetask"  -> handleCompleteTask(args, telegramUserId);
            case "/taskstatus"    -> handleTaskStatus(args, telegramUserId);
            case "/taskpriority"  -> handleTaskPriority(args, telegramUserId);
            case "/projectstatus" -> handleProjectStatus(args, telegramUserId);
            case "/addmember"     -> handleAddMember(args, telegramUserId);
            // ── AI ────────────────────────────────────────────────────────────
            case "/pingai"        -> handlePingAi();
            case "/suggest"       -> handleSuggest(args);
            case "/analyze"       -> handleAnalyze(args);
            case "/fix"           -> handleFix(args);
            default               -> null;
        };
    }

    // ── help ─────────────────────────────────────────────────────────────────

    private String handleHelp() {
        return """
                Andromeda Bot — Commands
                ════════════════════════

                SETUP
                /link <username> <password>   Link your account

                READ  (hierarchy: Project → Capabilities → Features → User Stories → Tasks)
                /projects                     List all projects
                /project <id>                 Project details
                /capabilities <projectId>     Capabilities in a project
                /capability <id>              Capability details
                /features <capabilityId>      Features in a capability
                /feature <id>                 Feature details
                /projectstories <projectId>   All user stories in a project
                /userstories <featureId>      User stories in a feature
                /userstory <id>               User story details
                /tasks <projectId>            Tasks in a project
                /task <id>                    Task details
                /members <projectId>          Project members
                /sprints <projectId>          Project sprints
                /sprinttasks <projectId>      Sprint board (last 2 sprints)
                /users                        List all users
                /user <id>                    User details

                WRITE  (requires /link)
                /newproject <name> [| desc] [| status]
                /newsprint <projectId> | <name> [| goal] [| status] [| startDate] [| dueDate]
                /newtask <projectId> | <title> | <estimatedHours> [| priority]
                /assigntask <sprintId> <taskId>
                /addsprinttask <sprintId> <taskId>
                /completetask <taskId> <actualHours>
                /taskstatus <taskId> <status>
                /taskpriority <taskId> <priority>
                /projectstatus <projectId> <status>
                /addmember <projectId> <userId> [role]

                AI
                /pingai                       Test AI backend connectivity
                /suggest <projectId>          AI improvement suggestions
                /analyze <projectId>          AI health analysis
                /fix <taskId>                 AI task resolution guidance

                TIP: You can also message me in plain English!
                e.g. "show me tasks in project 2" or "what's the status of task 5"

                VALUES
                Project status : active · paused · completed · cancelled
                Sprint status  : planned · active · completed
                Task status    : todo · in_progress · review · done
                Task priority  : low · medium · high · critical
                Member role    : owner · manager · member
                Max est. hours : 4.0 h per task""";
    }

    // ── auth ─────────────────────────────────────────────────────────────────

    /**
     * /link <username> <password>
     * Authenticates the user with BCrypt and stores their Telegram ID.
     */
    private String handleLink(String args, Long telegramUserId) {
        if (telegramUserId == null)
            return "Cannot identify your Telegram account. Please try again.";

        String[] tokens = args.split("\\s+", 2);
        if (tokens.length < 2 || tokens[0].isBlank() || tokens[1].isBlank())
            return "Usage: /link <username> <password>";

        String username = tokens[0].trim();
        String password = tokens[1].trim();

        User user = userService.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash()))
            return "Invalid username or password.";

        // already linked to this same account — idempotent
        if (telegramUserId.equals(user.getTelegramId()))
            return "Already linked to @" + user.getUsername() + ". Welcome back, " + user.getName() + "!";

        // Telegram ID claimed by a different account
        if (userService.telegramIdExists(telegramUserId))
            return "This Telegram account is already linked to a different user. " +
                   "Contact an admin to unlink it.";

        user.setTelegramId(telegramUserId);
        userService.save(user);

        return "Linked! Welcome, " + user.getName() + " (@" + user.getUsername() + ").\n" +
               "You can now use all write commands.";
    }

    /** Resolves the calling Telegram user to a DB User, or returns null. */
    private Optional<User> linkedUser(Long telegramUserId) {
        if (telegramUserId == null) return Optional.empty();
        return userService.findByTelegramId(telegramUserId);
    }

    private static final String NOT_LINKED =
            "You must link your Telegram account first.\nUse: /link <username> <password>";

    // ── read commands ─────────────────────────────────────────────────────────

    private String handleProjects() {
        List<Project> projects = projectService.findAll();
        if (projects.isEmpty()) return "No projects found.";
        StringBuilder sb = new StringBuilder("Projects (" + projects.size() + ")\n───────────────────────\n");
        for (Project p : projects)
            sb.append(String.format("[%d] %s — %s\n", p.getId(), p.getName(), nvl(p.getStatus(), "no status")));
        return sb.toString().trim();
    }

    private String handleProject(String args) {
        Long id = parseLong(args);
        if (id == null) return "Usage: /project <id>";
        Optional<Project> opt = projectService.findById(id);
        if (opt.isEmpty()) return "Project #" + id + " not found.";
        Project p = opt.get();
        int memberCount = projectMemberService.findByProjectId(id).size();
        int taskCount   = tasksService.findByProjectId(id).size();
        return String.format(
                "Project #%d\nName:    %s\nStatus:  %s\nStart:   %s\nEnd:     %s\nMembers: %d\nTasks:   %d",
                p.getId(), p.getName(), nvl(p.getStatus(), "—"),
                fmt(p.getStartDate()), fmt(p.getEndDate()), memberCount, taskCount);
    }

    private String handleTasks(String args) {
        Long projectId = parseLong(args);
        if (projectId == null) return "Usage: /tasks <projectId>";
        if (projectService.findById(projectId).isEmpty()) return "Project #" + projectId + " not found.";
        List<Tasks> tasks = tasksService.findByProjectId(projectId);
        if (tasks.isEmpty()) return "No tasks found for project #" + projectId + ".";
        StringBuilder sb = new StringBuilder(
                "Tasks for project #" + projectId + " (" + tasks.size() + ")\n───────────────────────\n");
        for (Tasks t : tasks)
            sb.append(String.format("[%d] %s — %s | %s | %s h\n",
                    t.getId(), t.getTitle(),
                    nvl(t.getPriority(), "?"), nvl(t.getStatus(), "?"),
                    t.getEstimatedHours() != null ? t.getEstimatedHours() : "—"));
        return sb.toString().trim();
    }

    private String handleTask(String args) {
        Long id = parseLong(args);
        if (id == null) return "Usage: /task <id>";
        Optional<Tasks> opt = tasksService.findById(id);
        if (opt.isEmpty()) return "Task #" + id + " not found.";
        Tasks t = opt.get();
        Project proj = t.getProject();
        return String.format(
                "Task #%d\n" +
                "Title:       %s\n" +
                "Project:     #%d %s\n" +
                "Priority:    %s\n" +
                "Status:      %s\n" +
                "Est. hours:  %s\n" +
                "Act. hours:  %s\n" +
                "Start:       %s\n" +
                "Due:         %s",
                t.getId(), t.getTitle(),
                proj.getId(), proj.getName(),
                nvl(t.getPriority(), "—"), nvl(t.getStatus(), "—"),
                t.getEstimatedHours() != null ? t.getEstimatedHours() : "—",
                t.getActualHours() != null ? t.getActualHours() : "—",
                fmt(t.getStartDate()), fmt(t.getDueDate()));
    }

    private String handleMembers(String args) {
        Long projectId = parseLong(args);
        if (projectId == null) return "Usage: /members <projectId>";
        if (projectService.findById(projectId).isEmpty()) return "Project #" + projectId + " not found.";
        List<ProjectMember> members = projectMemberService.findByProjectId(projectId);
        if (members.isEmpty()) return "No members found for project #" + projectId + ".";
        StringBuilder sb = new StringBuilder(
                "Members of project #" + projectId + " (" + members.size() + ")\n───────────────────────\n");
        for (ProjectMember m : members)
            sb.append(String.format("@%s — %s\n", m.getUser().getUsername(), nvl(m.getRole(), "member")));
        return sb.toString().trim();
    }

    private String handleSprints(String args) {
        Long projectId = parseLong(args);
        if (projectId == null) return "Usage: /sprints <projectId>";
        if (projectService.findById(projectId).isEmpty()) return "Project #" + projectId + " not found.";
        List<Sprint> sprints = sprintService.findByProjectId(projectId);
        if (sprints.isEmpty()) return "No sprints found for project #" + projectId + ".";
        StringBuilder sb = new StringBuilder(
                "Sprints for project #" + projectId + " (" + sprints.size() + ")\n───────────────────────\n");
        for (Sprint s : sprints)
            sb.append(String.format("[%d] %s — %s | %s → %s\n",
                    s.getId(), s.getName(), nvl(s.getStatus(), "?"),
                    fmt(s.getStartDate()), fmt(s.getDueDate())));
        return sb.toString().trim();
    }

    /**
     * /sprinttasks <projectId>
     * Displays the task board for the last 2 sprints, grouped by sprint.
     */
    private String handleSprintTasks(String args) {
        Long projectId = parseLong(args);
        if (projectId == null) return "Usage: /sprinttasks <projectId>";
        if (projectService.findById(projectId).isEmpty()) return "Project #" + projectId + " not found.";

        List<SprintTaskRow> rows = sprintStoryAssignmentService.findSprintBoard(projectId);
        if (rows.isEmpty()) return "No tasks found in recent sprints for project #" + projectId + ".";

        StringBuilder sb = new StringBuilder();
        sb.append("Sprint Board — Project #").append(projectId).append("\n");
        sb.append("════════════════════════════════\n\n");

        String currentSprint = null;
        for (SprintTaskRow r : rows) {
            // sprint header whenever sprint changes
            if (!r.getSprintName().equals(currentSprint)) {
                if (currentSprint != null) sb.append("\n");
                currentSprint = r.getSprintName();
                sb.append("▸ ").append(currentSprint).append("\n");
                sb.append("────────────────────────────────\n");
            }

            // status badge
            String badge = switch (nvl(r.getStatus(), "")) {
                case "in_progress" -> "IN_PROG";
                case "review"      -> "REVIEW ";
                case "done"        -> "DONE   ";
                default            -> "TODO   ";
            };

            String assignees = (r.getAssignees() != null && !r.getAssignees().isBlank())
                    ? "@" + r.getAssignees().replace(", ", ", @")
                    : "—";

            String est = r.getEstimatedHours() != null ? r.getEstimatedHours() + "h est" : "";
            String act = r.getActualHours()    != null ? " / " + r.getActualHours() + "h act" : "";

            sb.append(String.format("[%d] %s\n", r.getId(), r.getTitle()));
            sb.append(String.format("    %s | %s | %s%s | %s\n",
                    badge,
                    nvl(r.getPriority(), "—"),
                    est, act,
                    assignees));
        }

        return sb.toString().trim();
    }

    private String handleUsers() {
        List<User> users = userService.findAll();
        if (users.isEmpty()) return "No users found.";
        StringBuilder sb = new StringBuilder("Users (" + users.size() + ")\n───────────────────────\n");
        for (User u : users)
            sb.append(String.format("[%d] @%s — %s\n", u.getId(), u.getUsername(), u.getName()));
        return sb.toString().trim();
    }

    private String handleUser(String args) {
        Long id = parseLong(args);
        if (id == null) return "Usage: /user <id>";
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) return "User #" + id + " not found.";
        User u = opt.get();
        return String.format(
                "User #%d\nName:     %s\nUsername: @%s\nEmail:    %s\nPhone:    %s",
                u.getId(), u.getName(), u.getUsername(), u.getEmail(), nvl(u.getPhone(), "—"));
    }

    private String handleCapabilities(String args) {
        Long projectId = parseLong(args);
        if (projectId == null) return "Usage: /capabilities <projectId>";
        if (projectService.findById(projectId).isEmpty()) return "Project #" + projectId + " not found.";
        List<Capability> caps = capabilityService.findByProjectId(projectId);
        if (caps.isEmpty()) return "No capabilities found for project #" + projectId + ".";
        StringBuilder sb = new StringBuilder(
                "Capabilities for project #" + projectId + " (" + caps.size() + ")\n───────────────────────\n");
        for (Capability c : caps)
            sb.append(String.format("[%d] %s — %s\n", c.getId(), c.getName(), nvl(c.getStatus(), "?")));
        return sb.toString().trim();
    }

    private String handleCapability(String args) {
        Long id = parseLong(args);
        if (id == null) return "Usage: /capability <id>";
        Capability cap = capabilityService.findById(id).orElse(null);
        if (cap == null) return "Capability #" + id + " not found.";
        int featureCount = featureService.findByCapabilityId(id).size();
        return String.format(
                "Capability #%d\nName:     %s\nProject:  #%d %s\nStatus:   %s\nFeatures: %d",
                cap.getId(), cap.getName(),
                cap.getProject().getId(), cap.getProject().getName(),
                nvl(cap.getStatus(), "—"), featureCount);
    }

    private String handleFeatures(String args) {
        Long capabilityId = parseLong(args);
        if (capabilityId == null) return "Usage: /features <capabilityId>";
        Capability cap = capabilityService.findById(capabilityId).orElse(null);
        if (cap == null) return "Capability #" + capabilityId + " not found.";
        List<Feature> features = featureService.findByCapabilityId(capabilityId);
        if (features.isEmpty()) return "No features found for capability #" + capabilityId + ".";
        StringBuilder sb = new StringBuilder(
                "Features for capability #" + capabilityId + " — " + cap.getName() +
                " (" + features.size() + ")\n───────────────────────\n");
        for (Feature f : features)
            sb.append(String.format("[%d] %s — %s\n", f.getId(), f.getName(), nvl(f.getStatus(), "?")));
        return sb.toString().trim();
    }

    private String handleFeature(String args) {
        Long id = parseLong(args);
        if (id == null) return "Usage: /feature <id>";
        Feature feature = featureService.findById(id).orElse(null);
        if (feature == null) return "Feature #" + id + " not found.";
        int storyCount = userStoryService.findByFeatureId(id).size();
        Capability cap = feature.getCapability();
        return String.format(
                "Feature #%d\nName:         %s\nCapability:   #%d %s\nProject:      #%d %s\nStatus:       %s\nUser Stories: %d",
                feature.getId(), feature.getName(),
                cap.getId(), cap.getName(),
                cap.getProject().getId(), cap.getProject().getName(),
                nvl(feature.getStatus(), "—"), storyCount);
    }

    private String handleProjectStories(String args) {
        Long projectId = parseLong(args);
        if (projectId == null) return "Usage: /projectstories <projectId>";
        if (projectService.findById(projectId).isEmpty()) return "Project #" + projectId + " not found.";
        List<UserStory> stories = userStoryService.findByProjectId(projectId);
        if (stories.isEmpty()) return "No user stories found for project #" + projectId + ".";
        StringBuilder sb = new StringBuilder(
                "User Stories for project #" + projectId + " (" + stories.size() + ")\n───────────────────────\n");
        for (UserStory s : stories)
            sb.append(String.format("[%d] %s — %s | %s | %s pts\n",
                    s.getId(), s.getTitle(),
                    nvl(s.getPriority(), "?"), nvl(s.getStatus(), "?"),
                    s.getStoryPoints() != null ? s.getStoryPoints() : "—"));
        return sb.toString().trim();
    }

    private String handleUserStories(String args) {
        Long featureId = parseLong(args);
        if (featureId == null) return "Usage: /userstories <featureId>";
        Feature feature = featureService.findById(featureId).orElse(null);
        if (feature == null) return "Feature #" + featureId + " not found.";
        List<UserStory> stories = userStoryService.findByFeatureId(featureId);
        if (stories.isEmpty()) return "No user stories found for feature #" + featureId + ".";
        StringBuilder sb = new StringBuilder(
                "User Stories for feature #" + featureId + " — " + feature.getName() +
                " (" + stories.size() + ")\n───────────────────────\n");
        for (UserStory s : stories)
            sb.append(String.format("[%d] %s — %s | %s | %s pts\n",
                    s.getId(), s.getTitle(),
                    nvl(s.getPriority(), "?"), nvl(s.getStatus(), "?"),
                    s.getStoryPoints() != null ? s.getStoryPoints() : "—"));
        return sb.toString().trim();
    }

    private String handleUserStory(String args) {
        Long id = parseLong(args);
        if (id == null) return "Usage: /userstory <id>";
        UserStory s = userStoryService.findById(id).orElse(null);
        if (s == null) return "User story #" + id + " not found.";
        Feature feature = s.getFeature();
        String owner = s.getOwner() != null ? "@" + s.getOwner().getUsername() : "—";
        return String.format(
                "User Story #%d\nTitle:     %s\nFeature:   #%d %s\nPriority:  %s\nStatus:    %s\nPoints:    %s\nOwner:     %s",
                s.getId(), s.getTitle(),
                feature.getId(), feature.getName(),
                nvl(s.getPriority(), "—"), nvl(s.getStatus(), "—"),
                s.getStoryPoints() != null ? s.getStoryPoints() : "—",
                owner);
    }

    // ── write commands ────────────────────────────────────────────────────────

    /**
     * /newproject <name> [| description] [| status]
     */
    private String handleNewProject(String args, Long telegramUserId) {
        Optional<User> actor = linkedUser(telegramUserId);
        if (actor.isEmpty()) return NOT_LINKED;
        if (args.isBlank()) return "Usage: /newproject <name> [| description] [| status]";

        String[] pipes = splitPipes(args, 3);
        String name   = pipes[0];
        String desc   = pipes.length > 1 ? pipes[1] : null;
        String status = pipes.length > 2 ? pipes[2] : "active";

        if (name.isEmpty()) return "Project name cannot be empty.";
        if (!VALID_PROJECT_STATUSES.contains(status))
            return "Invalid status '" + status + "'. Valid: active, paused, completed, cancelled";

        Project project = new Project();
        project.setName(name);
        project.setDescription(desc);
        project.setStatus(status);
        Project saved = projectService.save(project);

        return String.format("Project created!\nID:     %d\nName:   %s\nStatus: %s",
                saved.getId(), saved.getName(), saved.getStatus());
    }

    /**
     * /newsprint <projectId> | <name> [| goal] [| status] [| startDate] [| dueDate]
     *
     *
     */
    private String handleNewSprint(String args, Long telegramUserId) {
        if (linkedUser(telegramUserId).isEmpty()) return NOT_LINKED;
        if (args.isBlank())
            return "Usage: /newsprint <projectId> | <name> [| goal] [| status] [| startDate] [| dueDate]";

        String[] pipes = splitPipes(args, 6);
        if (pipes.length < 2)
            return "Usage: /newsprint <projectId> | <name> [| goal] [| status] [| startDate] [| dueDate]";

        Long projectId = parseLong(pipes[0]);
        if (projectId == null)
            return "Usage: /newsprint <projectId> | <name> [| goal] [| status] [| startDate] [| dueDate]";

        String name = pipes[1];
        if (name.isEmpty()) return "Sprint name cannot be empty.";

        String goal = pipes.length > 2 && !pipes[2].isEmpty() ? pipes[2] : null;
        String status = pipes.length > 3 && !pipes[3].isEmpty() ? pipes[3].toLowerCase() : "planned";
        if (!VALID_SPRINT_STATUSES.contains(status))
            return "Invalid status '" + status + "'. Valid: planned, active, completed";

        LocalDateTime startDate = null;
        if (pipes.length > 4 && !pipes[4].isEmpty()) {
            startDate = parseDateInput(pipes[4]);
            if (startDate == null)
                return "Invalid startDate '" + pipes[4] + "'. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss";
        }

        LocalDateTime dueDate = null;
        if (pipes.length > 5 && !pipes[5].isEmpty()) {
            dueDate = parseDateInput(pipes[5]);
            if (dueDate == null)
                return "Invalid dueDate '" + pipes[5] + "'. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss";
        }

        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) return "Project #" + projectId + " not found.";

        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName(name);
        sprint.setGoal(goal);
        sprint.setStatus(status);
        sprint.setStartDate(startDate);
        sprint.setDueDate(dueDate);
        Sprint saved = sprintService.save(sprint);

        return String.format(
                "Sprint created!\nID:      %d\nName:    %s\nProject: #%d %s\nStatus:  %s\nStart:   %s\nDue:     %s",
                saved.getId(), saved.getName(),
                project.getId(), project.getName(),
                saved.getStatus(),
                fmt(saved.getStartDate()), fmt(saved.getDueDate()));
    }

    /**
     * /newtask <projectId> | <title> | <estimatedHours> [| priority]
     *
     * estimatedHours must be > 0 and <= 4.0. If > 4, the bot rejects and
     * suggests how many subtasks to split into.
     */
    private String handleNewTask(String args, Long telegramUserId) {
        Optional<User> actor = linkedUser(telegramUserId);
        if (actor.isEmpty()) return NOT_LINKED;
        if (args.isBlank())
            return "Usage: /newtask <projectId> | <title> | <estimatedHours> [| priority]";

        String[] pipes = splitPipes(args, 4);
        if (pipes.length < 3)
            return "Usage: /newtask <projectId> | <title> | <estimatedHours> [| priority]";

        Long projectId = parseLong(pipes[0]);
        if (projectId == null)
            return "Usage: /newtask <projectId> | <title> | <estimatedHours> [| priority]";

        String title = pipes[1];
        if (title.isEmpty()) return "Task title cannot be empty.";

        BigDecimal estimatedHours = parseBigDecimal(pipes[2]);
        if (estimatedHours == null || estimatedHours.compareTo(BigDecimal.ZERO) <= 0)
            return "estimatedHours must be a positive number (e.g. 2 or 1.5).";

        // 4-hour rule
        if (estimatedHours.compareTo(MAX_TASK_HOURS) > 0) {
            int subtasks = (int) Math.ceil(estimatedHours.doubleValue() / 4.0);
            return String.format(
                    "This task is estimated at %s h, which exceeds the 4 h limit.\n" +
                    "Please split it into %d subtasks of ≤ 4 h each and add them separately.",
                    estimatedHours, subtasks);
        }

        String priority = pipes.length > 3 && !pipes[3].isEmpty() ? pipes[3] : "medium";

        if (!VALID_TASK_PRIORITIES.contains(priority))
            return "Invalid priority '" + priority + "'. Valid: low, medium, high, critical";

        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) return "Project #" + projectId + " not found.";

        Tasks task = new Tasks();
        task.setProject(project);
        task.setTitle(title);
        task.setPriority(priority);
        task.setStatus("todo");
        task.setEstimatedHours(estimatedHours);
        Tasks saved = tasksService.save(task);

        return String.format(
                "Task created!\nID:          %d\nTitle:       %s\nProject:     #%d %s\n" +
                "Priority:    %s\nEst. hours:  %s h",
                saved.getId(), saved.getTitle(),
                project.getId(), project.getName(),
                saved.getPriority(), saved.getEstimatedHours());
    }

    /**
     * /assigntask <sprintId> <taskId>
     *
     * Links the task to the sprint, marks it in_progress, sets start date,
     * and auto-assigns the calling developer to the task.
     */
    private String handleAssignTask(String args, Long telegramUserId) {
        Optional<User> actor = linkedUser(telegramUserId);
        if (actor.isEmpty()) return NOT_LINKED;

        String[] tokens = args.split("\\s+", 2);
        if (tokens.length < 2) return "Usage: /assigntask <sprintId> <taskId>";

        Long sprintId = parseLong(tokens[0]);
        Long taskId   = parseLong(tokens[1]);
        if (sprintId == null || taskId == null) return "Usage: /assigntask <sprintId> <taskId>";

        Sprint sprint = sprintService.findById(sprintId).orElse(null);
        if (sprint == null) return "Sprint #" + sprintId + " not found.";

        Tasks task = tasksService.findById(taskId).orElse(null);
        if (task == null) return "Task #" + taskId + " not found.";

        if (!sprint.getProject().getId().equals(task.getProject().getId()))
            return "Sprint #" + sprintId + " and task #" + taskId + " belong to different projects.";

        if (task.getUserStoryId() == null)
            return "Task #" + taskId + " is not linked to any user story.";

        if (sprintStoryAssignmentService.isStoryActiveInSprint(sprintId, task.getUserStoryId()))
            return "Task #" + taskId + " is already in sprint #" + sprintId + ".";

        // link task → sprint
        SprintStoryAssignment st = new SprintStoryAssignment();
        st.setSprint(sprint);
        st.setUserStoryId(task.getUserStoryId());
        st.setAddedAt(LocalDateTime.now());
        st.setIsActive(1);
        sprintStoryAssignmentService.save(st);

        // mark task as started
        task.setStatus("in_progress");
        if (task.getStartDate() == null) task.setStartDate(LocalDateTime.now());
        tasksService.save(task);

        // auto-assign developer if not already assigned
        User dev = actor.get();
        if (taskAssignmentService.findByTaskIdAndUserId(taskId, dev.getId()).isEmpty()) {
            TaskAssignment ta = new TaskAssignment();
            ta.setTask(task);
            ta.setUser(dev);
            taskAssignmentService.save(ta);
        }

        return String.format(
                "Task assigned to sprint!\nTask:   #%d %s\nSprint: #%d %s\nStatus: todo → in_progress\nDev:    @%s",
                task.getId(), task.getTitle(),
                sprint.getId(), sprint.getName(),
                dev.getUsername());
    }

    /**
     * /completetask <taskId> <actualHours>
     *
     * Marks the task as done, records actual hours, and sets actual_end timestamp.
     */
    private String handleCompleteTask(String args, Long telegramUserId) {
        Optional<User> actor = linkedUser(telegramUserId);
        if (actor.isEmpty()) return NOT_LINKED;

        String[] tokens = args.split("\\s+", 2);
        if (tokens.length < 2) return "Usage: /completetask <taskId> <actualHours>";

        Long taskId = parseLong(tokens[0]);
        BigDecimal actualHours = parseBigDecimal(tokens[1]);

        if (taskId == null || actualHours == null || actualHours.compareTo(BigDecimal.ZERO) <= 0)
            return "Usage: /completetask <taskId> <actualHours>  (actualHours must be > 0)";

        Tasks task = tasksService.findById(taskId).orElse(null);
        if (task == null) return "Task #" + taskId + " not found.";

        String previousStatus = task.getStatus();
        task.setStatus("done");
        task.setActualEnd(LocalDateTime.now());
        task.setActualHours(actualHours);
        tasksService.save(task);

        String estVsAct = "";
        if (task.getEstimatedHours() != null) {
            double diff = actualHours.doubleValue() - task.getEstimatedHours().doubleValue();
            String sign = diff >= 0 ? "+" : "";
            estVsAct = String.format("\nEst. hours:  %s h\nAct. hours:  %s h  (%s%.1f h)",
                    task.getEstimatedHours(), actualHours, sign, diff);
        } else {
            estVsAct = "\nAct. hours:  " + actualHours + " h";
        }

        return String.format("Task completed!\nID:     %d\nTitle:  %s\nStatus: %s → done%s",
                task.getId(), task.getTitle(), nvl(previousStatus, "?"), estVsAct);
    }

    /**
     * /taskstatus <taskId> <status>
     */
    private String handleTaskStatus(String args, Long telegramUserId) {
        if (linkedUser(telegramUserId).isEmpty()) return NOT_LINKED;

        String[] tokens = args.split("\\s+", 2);
        if (tokens.length < 2) return "Usage: /taskstatus <taskId> <status>";

        Long taskId = parseLong(tokens[0]);
        String status = tokens[1].trim().toLowerCase();
        if (taskId == null) return "Usage: /taskstatus <taskId> <status>";
        if (!VALID_TASK_STATUSES.contains(status))
            return "Invalid status '" + status + "'. Valid: todo, in_progress, review, done";

        Tasks task = tasksService.findById(taskId).orElse(null);
        if (task == null) return "Task #" + taskId + " not found.";

        String previous = task.getStatus();
        task.setStatus(status);
        tasksService.save(task);

        return String.format("Task #%d updated.\nTitle:  %s\nStatus: %s → %s",
                task.getId(), task.getTitle(), nvl(previous, "?"), status);
    }

    /**
     * /taskpriority <taskId> <priority>
     */
    private String handleTaskPriority(String args, Long telegramUserId) {
        if (linkedUser(telegramUserId).isEmpty()) return NOT_LINKED;

        String[] tokens = args.split("\\s+", 2);
        if (tokens.length < 2) return "Usage: /taskpriority <taskId> <priority>";

        Long taskId = parseLong(tokens[0]);
        String priority = tokens[1].trim().toLowerCase();
        if (taskId == null) return "Usage: /taskpriority <taskId> <priority>";
        if (!VALID_TASK_PRIORITIES.contains(priority))
            return "Invalid priority '" + priority + "'. Valid: low, medium, high, critical";

        Tasks task = tasksService.findById(taskId).orElse(null);
        if (task == null) return "Task #" + taskId + " not found.";

        String previous = task.getPriority();
        task.setPriority(priority);
        tasksService.save(task);

        return String.format("Task #%d updated.\nTitle:    %s\nPriority: %s → %s",
                task.getId(), task.getTitle(), nvl(previous, "?"), priority);
    }

    /**
     * /projectstatus <projectId> <status>
     */
    private String handleProjectStatus(String args, Long telegramUserId) {
        if (linkedUser(telegramUserId).isEmpty()) return NOT_LINKED;

        String[] tokens = args.split("\\s+", 2);
        if (tokens.length < 2) return "Usage: /projectstatus <projectId> <status>";

        Long projectId = parseLong(tokens[0]);
        String status  = tokens[1].trim().toLowerCase();
        if (projectId == null) return "Usage: /projectstatus <projectId> <status>";
        if (!VALID_PROJECT_STATUSES.contains(status))
            return "Invalid status '" + status + "'. Valid: active, paused, completed, cancelled";

        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) return "Project #" + projectId + " not found.";

        String previous = project.getStatus();
        project.setStatus(status);
        projectService.save(project);

        return String.format("Project #%d updated.\nName:   %s\nStatus: %s → %s",
                project.getId(), project.getName(), nvl(previous, "?"), status);
    }

    /**
     * /addmember <projectId> <userId> [role]
     */
    private String handleAddMember(String args, Long telegramUserId) {
        if (linkedUser(telegramUserId).isEmpty()) return NOT_LINKED;

        String[] tokens = args.split("\\s+", 3);
        if (tokens.length < 2) return "Usage: /addmember <projectId> <userId> [role]";

        Long projectId = parseLong(tokens[0]);
        Long userId    = parseLong(tokens[1]);
        String role    = tokens.length > 2 ? tokens[2].trim().toLowerCase() : "member";

        if (projectId == null || userId == null) return "Usage: /addmember <projectId> <userId> [role]";
        if (!VALID_MEMBER_ROLES.contains(role))
            return "Invalid role '" + role + "'. Valid: owner, manager, member";

        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) return "Project #" + projectId + " not found.";

        User user = userService.findById(userId).orElse(null);
        if (user == null) return "User #" + userId + " not found.";

        if (projectMemberService.existsByProjectIdAndUserId(projectId, userId))
            return "@" + user.getUsername() + " is already a member of project #" + projectId + ".";

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        ProjectMember saved = projectMemberService.save(member);

        return String.format("Member added!\nProject: #%d %s\nUser:    @%s\nRole:    %s",
                project.getId(), project.getName(), user.getUsername(), saved.getRole());
    }

    // ── AI commands ───────────────────────────────────────────────────────────

    /**
     * /pingai
     * Tests AI backend reachability and reports round-trip latency.
     */
    private String handlePingAi() {
        if (!aiService.isEnabled())
            return "AI is disabled (agent.ai.enabled=false).";

        long ms = aiService.ping();
        if (ms < 0)
            return "AI backend is not responding. Check the API key and base URL configuration.";

        return String.format(
                "AI Backend — Online\nModel:   %s\nLatency: %d ms\n\n" +
                "Try AI commands:\n" +
                "/suggest <projectId> — improvement suggestions\n" +
                "/analyze <projectId> — project health analysis\n" +
                "/fix <taskId>        — task resolution guidance\n\n" +
                "Or ask in plain English — I'll figure it out.\n" +
                "Debug: try mentioning guacamole in any message.",
                aiService.getModel(), ms);
    }

    /**
     * /suggest <projectId>
     * Fetches project data and asks the AI for actionable improvement suggestions.
     */
    private String handleSuggest(String args) {
        if (!aiService.isEnabled()) return "AI is currently disabled.";
        Long projectId = parseLong(args);
        if (projectId == null) return "Usage: /suggest <projectId>";

        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) return "Project #" + projectId + " not found.";

        List<Tasks> tasks            = tasksService.findByProjectId(projectId);
        List<Sprint> sprints         = sprintService.findByProjectId(projectId);
        List<ProjectMember> members  = projectMemberService.findByProjectId(projectId);
        List<UserStory> stories      = userStoryService.findByProjectId(projectId);

        long todo         = tasks.stream().filter(t -> "todo".equals(t.getStatus())).count();
        long inProgress   = tasks.stream().filter(t -> "in_progress".equals(t.getStatus())).count();
        long review       = tasks.stream().filter(t -> "review".equals(t.getStatus())).count();
        long done         = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        long critical     = tasks.stream().filter(t -> "critical".equals(t.getPriority())).count();
        long activeSp     = sprints.stream().filter(s -> "active".equals(s.getStatus())).count();
        long storiesDone  = stories.stream().filter(s -> "done".equals(s.getStatus())).count();
        long storiesInPrg = stories.stream().filter(s -> "in_progress".equals(s.getStatus())).count();

        StringBuilder ctx = new StringBuilder();
        ctx.append("Project: ").append(project.getName())
           .append(" (ID: ").append(projectId).append(", status: ")
           .append(nvl(project.getStatus(), "unknown")).append(")\n");
        ctx.append("User Stories: ").append(stories.size()).append(" total — ")
           .append("done=").append(storiesDone).append(", in_progress=").append(storiesInPrg).append("\n");
        ctx.append("Tasks: ").append(tasks.size()).append(" total — ")
           .append("todo=").append(todo).append(", in_progress=").append(inProgress)
           .append(", review=").append(review).append(", done=").append(done).append("\n");
        ctx.append("Critical priority tasks: ").append(critical).append("\n");
        ctx.append("Sprints: ").append(sprints.size()).append(" (").append(activeSp).append(" active)\n");
        ctx.append("Members: ").append(members.size()).append("\n");
        if (!stories.isEmpty()) {
            ctx.append("Recent user stories:\n");
            stories.stream().limit(4).forEach(s ->
                ctx.append("  [").append(nvl(s.getStatus(), "?")).append("] ")
                   .append(s.getTitle()).append(" (").append(nvl(s.getPriority(), "?")).append(")\n"));
        }
        if (!tasks.isEmpty()) {
            ctx.append("Recent tasks:\n");
            tasks.stream().limit(4).forEach(t ->
                ctx.append("  [").append(nvl(t.getStatus(), "?")).append("] ")
                   .append(t.getTitle()).append(" (").append(nvl(t.getPriority(), "?")).append(")\n"));
        }

        String answer = aiService.chat(
                "You are a project management expert for the Andromeda system. " +
                "Analyze the project data and give 3-5 concise, actionable suggestions to improve project health. " +
                "Format as a numbered list. Be specific and practical.",
                ctx.toString());

        return answer != null
                ? "AI Suggestions — " + project.getName() + ":\n\n" + answer
                : "AI did not respond. Try again later.";
    }

    /**
     * /analyze <projectId>
     * Fetches project data and asks the AI for a health analysis with risk identification.
     */
    private String handleAnalyze(String args) {
        if (!aiService.isEnabled()) return "AI is currently disabled.";
        Long projectId = parseLong(args);
        if (projectId == null) return "Usage: /analyze <projectId>";

        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) return "Project #" + projectId + " not found.";

        List<Tasks> tasks           = tasksService.findByProjectId(projectId);
        List<Sprint> sprints        = sprintService.findByProjectId(projectId);
        List<ProjectMember> members = projectMemberService.findByProjectId(projectId);
        List<UserStory> stories     = userStoryService.findByProjectId(projectId);

        long notDone      = tasks.stream().filter(t -> !"done".equals(t.getStatus())).count();
        long overdueCount = tasks.stream().filter(t ->
                t.getDueDate() != null &&
                t.getDueDate().isBefore(LocalDateTime.now()) &&
                !"done".equals(t.getStatus())).count();
        long critical     = tasks.stream().filter(t -> "critical".equals(t.getPriority())).count();
        long highPri      = tasks.stream().filter(t -> "high".equals(t.getPriority())).count();
        long activeSp     = sprints.stream().filter(s -> "active".equals(s.getStatus())).count();
        long completedSp  = sprints.stream().filter(s -> "completed".equals(s.getStatus())).count();
        long storiesDone  = stories.stream().filter(s -> "done".equals(s.getStatus())).count();

        double taskCompletion  = tasks.isEmpty()   ? 0.0
                : (double) tasks.stream().filter(t -> "done".equals(t.getStatus())).count() / tasks.size() * 100.0;
        double storyCompletion = stories.isEmpty() ? 0.0
                : (double) storiesDone / stories.size() * 100.0;

        StringBuilder ctx = new StringBuilder();
        ctx.append("Project: ").append(project.getName())
           .append(" | Status: ").append(nvl(project.getStatus(), "unknown")).append("\n");
        ctx.append("User Stories: ").append(stories.size())
           .append(" | Completed: ").append(String.format("%.0f%%", storyCompletion)).append("\n");
        ctx.append("Task completion rate: ").append(String.format("%.0f%%", taskCompletion)).append("\n");
        ctx.append("Open tasks: ").append(notDone).append(" | Overdue: ").append(overdueCount).append("\n");
        ctx.append("Critical: ").append(critical).append(" | High priority: ").append(highPri).append("\n");
        ctx.append("Sprints: active=").append(activeSp).append(", completed=").append(completedSp).append("\n");
        ctx.append("Team size: ").append(members.size()).append("\n");
        ctx.append("Start: ").append(fmt(project.getStartDate()))
           .append(" | End: ").append(fmt(project.getEndDate())).append("\n");

        String answer = aiService.chat(
                "You are a project health analyst for the Andromeda system. " +
                "Given the project metrics, provide: 1) an overall health score (0-10), " +
                "2) top 2-3 risks identified, 3) one key recommendation. " +
                "Be concise — maximum 200 words.",
                ctx.toString());

        return answer != null
                ? "AI Health Analysis — " + project.getName() + ":\n\n" + answer
                : "AI did not respond. Try again later.";
    }

    /**
     * /fix <taskId>
     * Fetches task details and asks the AI for practical resolution guidance.
     */
    private String handleFix(String args) {
        if (!aiService.isEnabled()) return "AI is currently disabled.";
        Long taskId = parseLong(args);
        if (taskId == null) return "Usage: /fix <taskId>";

        Tasks task = tasksService.findById(taskId).orElse(null);
        if (task == null) return "Task #" + taskId + " not found.";

        StringBuilder ctx = new StringBuilder();
        ctx.append("Task: ").append(task.getTitle()).append("\n");
        ctx.append("Project: ").append(task.getProject().getName()).append("\n");
        ctx.append("Status: ").append(nvl(task.getStatus(), "unknown")).append("\n");
        ctx.append("Priority: ").append(nvl(task.getPriority(), "unknown")).append("\n");
        if (task.getDescription() != null && !task.getDescription().isBlank())
            ctx.append("Description: ").append(task.getDescription()).append("\n");
        if (task.getUserStoryId() != null) {
            userStoryService.findById(task.getUserStoryId()).ifPresent(us -> {
                ctx.append("User Story: ").append(us.getTitle()).append("\n");
                if (us.getAcceptanceCriteria() != null && !us.getAcceptanceCriteria().isBlank())
                    ctx.append("Acceptance Criteria: ").append(us.getAcceptanceCriteria()).append("\n");
            });
        }
        if (task.getEstimatedHours() != null)
            ctx.append("Estimated: ").append(task.getEstimatedHours()).append("h\n");
        if (task.getDueDate() != null)
            ctx.append("Due: ").append(fmt(task.getDueDate())).append("\n");

        String answer = aiService.chat(
                "You are a technical project management assistant for the Andromeda system. " +
                "Given a task description, provide practical, step-by-step guidance on how to approach, " +
                "implement, or resolve it. Be specific and actionable. Maximum 150 words.",
                ctx.toString());

        return answer != null
                ? "AI Guidance — Task #" + task.getId() + " " + task.getTitle() + ":\n\n" + answer
                : "AI did not respond. Try again later.";
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String[] splitPipes(String s, int limit) {
        String[] raw = s.split("\\s*\\|\\s*", limit);
        String[] trimmed = new String[raw.length];
        for (int i = 0; i < raw.length; i++) trimmed[i] = raw[i].trim();
        return trimmed;
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private Integer parseInteger(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private LocalDateTime parseDateInput(String s) {
        if (s == null || s.isBlank()) return null;
        String value = s.trim();
        try {
            if (value.length() == 10) {
                return java.time.LocalDate.parse(value).atStartOfDay();
            }
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "—";
    }

    private String nvl(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
