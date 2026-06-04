-- Recreate FK constraints with ON DELETE CASCADE for the project entity hierarchy.
-- Oracle requires DROP + ADD to change FK behavior.

-- sprints → projects
ALTER TABLE sprints DROP CONSTRAINT fk_sprints_project;
ALTER TABLE sprints ADD CONSTRAINT fk_sprints_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

-- project_members → projects
ALTER TABLE project_members DROP CONSTRAINT fk_pm_project;
ALTER TABLE project_members ADD CONSTRAINT fk_pm_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

-- capabilities → projects
ALTER TABLE capabilities DROP CONSTRAINT fk_capabilities_project;
ALTER TABLE capabilities ADD CONSTRAINT fk_capabilities_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

-- tasks → projects
ALTER TABLE tasks DROP CONSTRAINT fk_tasks_project;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

-- features → capabilities
ALTER TABLE features DROP CONSTRAINT fk_features_capability;
ALTER TABLE features ADD CONSTRAINT fk_features_capability
    FOREIGN KEY (capability_id) REFERENCES capabilities(id) ON DELETE CASCADE;

-- user_stories → features
ALTER TABLE user_stories DROP CONSTRAINT fk_us_feature;
ALTER TABLE user_stories ADD CONSTRAINT fk_us_feature
    FOREIGN KEY (feature_id) REFERENCES features(id) ON DELETE CASCADE;

-- tasks → user_stories (nullable; CASCADE so tasks are removed with their story)
ALTER TABLE tasks DROP CONSTRAINT fk_tasks_user_story;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_user_story
    FOREIGN KEY (user_story_id) REFERENCES user_stories(id) ON DELETE CASCADE;

-- task_assignments → tasks
ALTER TABLE task_assignments DROP CONSTRAINT fk_ta_task;
ALTER TABLE task_assignments ADD CONSTRAINT fk_ta_task
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE;

-- sprint_retrospectives → sprints
ALTER TABLE sprint_retrospectives DROP CONSTRAINT fk_retro_sprint;
ALTER TABLE sprint_retrospectives ADD CONSTRAINT fk_retro_sprint
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE;

-- sprint_stories_assignments → sprints
ALTER TABLE sprint_stories_assignments DROP CONSTRAINT fk_ss_sprint;
ALTER TABLE sprint_stories_assignments ADD CONSTRAINT fk_ss_sprint
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE;

-- sprint_stories_assignments → user_stories
ALTER TABLE sprint_stories_assignments DROP CONSTRAINT fk_ss_story;
ALTER TABLE sprint_stories_assignments ADD CONSTRAINT fk_ss_story
    FOREIGN KEY (user_story_id) REFERENCES user_stories(id) ON DELETE CASCADE;

-- sprint_stories_assignments moved_to → sprints (SET NULL: reference becomes invalid but assignment stays)
ALTER TABLE sprint_stories_assignments DROP CONSTRAINT fk_ss_moved;
ALTER TABLE sprint_stories_assignments ADD CONSTRAINT fk_ss_moved
    FOREIGN KEY (moved_to) REFERENCES sprints(id) ON DELETE SET NULL;
