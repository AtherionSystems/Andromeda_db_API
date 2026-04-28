package com.atherion.andromeda.telegram;

import com.atherion.andromeda.dto.SprintTaskRow;
import com.atherion.andromeda.model.*;
import com.atherion.andromeda.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final BCryptPasswordEncoder passwordEncoder;

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
            case "/projects"      -> handleProjects();
            case "/project"       -> handleProject(args);
            case "/tasks"         -> handleTasks(args);
            case "/task"          -> handleTask(args);
            case "/members"       -> handleMembers(args);
            case "/sprints"       -> handleSprints(args);
            case "/sprinttasks"   -> handleSprintTasks(args);
            case "/users"         -> handleUsers();
            case "/user"          -> handleUser(args);
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

                READ
                /projects                     List all projects
                /project <id>                 Project details
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
