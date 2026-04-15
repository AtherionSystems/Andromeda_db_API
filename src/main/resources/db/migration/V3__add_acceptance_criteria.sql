-- V3: Add acceptance_criteria to tasks as VARCHAR2(4000)
-- Using VARCHAR2 instead of CLOB to avoid Oracle Autonomous Database
-- restrictions on LOB columns in multi-step or standalone ALTER TABLE ADD.
ALTER TABLE tasks ADD (acceptance_criteria VARCHAR2(4000));
