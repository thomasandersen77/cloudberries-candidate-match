-- liquibase formatted sql

-- changeset tandersen:005-create-normalized-skill-tables
-- comment: Create normalized skill tables to replace enum-based approach

CREATE TABLE skill (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE INDEX idx_skill_name ON skill(name);

CREATE TABLE consultant_skill (
    id BIGSERIAL PRIMARY KEY,
    consultant_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    duration_years INT,
    FOREIGN KEY (consultant_id) REFERENCES consultant(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE
);

CREATE INDEX idx_consultant_skill_consultant ON consultant_skill(consultant_id);
CREATE INDEX idx_consultant_skill_skill ON consultant_skill(skill_id);

-- Migrate existing consultant skills from the old MutableSet<String> approach
-- First, populate the skill table with unique skill names
INSERT INTO skill (name) 
SELECT DISTINCT cs.skill 
FROM consultant_skills cs 
WHERE cs.skill IS NOT NULL AND cs.skill != '';

-- Then migrate the consultant-skill relationships
INSERT INTO consultant_skill (consultant_id, skill_id)
SELECT DISTINCT cs.consultant_id, s.id
FROM consultant_skills cs
JOIN skill s ON cs.skill = s.name
WHERE cs.skill IS NOT NULL AND cs.skill != '';

-- Create new project skill table with proper normalization
CREATE TABLE cv_project_experience_skill_v2 (
    id BIGSERIAL PRIMARY KEY,
    project_experience_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    duration_years INT,
    FOREIGN KEY (project_experience_id) REFERENCES cv_project_experience(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE
);

CREATE INDEX idx_cv_proj_skill_project ON cv_project_experience_skill_v2(project_experience_id);
CREATE INDEX idx_cv_proj_skill_skill ON cv_project_experience_skill_v2(skill_id);

-- Migrate existing project skills from string-based to normalized approach
INSERT INTO cv_project_experience_skill_v2 (project_experience_id, skill_id)
SELECT DISTINCT cps.project_experience_id, s.id
FROM cv_project_experience_skill cps
JOIN skill s ON cps.skill = s.name
WHERE cps.skill IS NOT NULL AND cps.skill != '';

-- Note: The old tables (consultant_skills, cv_project_experience_skill) will be dropped 
-- in a future migration after confirming the new system works correctly

-- rollback DROP TABLE cv_project_experience_skill_v2;
-- rollback DROP TABLE consultant_skill;  
-- rollback DROP TABLE skill;