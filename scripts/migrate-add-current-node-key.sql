-- MySQL 8 idempotent migration for existing ModularJobs installations.
-- Back up the database, stop all ModularJobs instances, then run:
--   mysql --host=host --port=3306 --user=user --password modularjobs \
--     < scripts/migrate-add-current-node-key.sql
--
-- Existing rows start at their previous job key. If jobs.yml makes that key a child
-- node, remap job_key to the owning root after resolving any duplicate player/root
-- rows; current_node_key must remain the selected active child node.

SET @mj_current_node_ddl = IF(
    (SELECT COUNT(*)
     FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'job_progression'
       AND column_name = 'current_node_key') = 0,
    'ALTER TABLE job_progression ADD COLUMN current_node_key VARCHAR(191) NULL AFTER job_key',
    'SELECT 1'
);
PREPARE mj_current_node_statement FROM @mj_current_node_ddl;
EXECUTE mj_current_node_statement;
DEALLOCATE PREPARE mj_current_node_statement;
UPDATE job_progression
SET current_node_key = job_key
WHERE current_node_key IS NULL;
ALTER TABLE job_progression
    MODIFY COLUMN current_node_key VARCHAR(191) NOT NULL;

SET @mj_current_node_ddl = IF(
    (SELECT COUNT(*)
     FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'archive_job_progression'
       AND column_name = 'current_node_key') = 0,
    'ALTER TABLE archive_job_progression ADD COLUMN current_node_key VARCHAR(191) NULL AFTER job_key',
    'SELECT 1'
);
PREPARE mj_current_node_statement FROM @mj_current_node_ddl;
EXECUTE mj_current_node_statement;
DEALLOCATE PREPARE mj_current_node_statement;
UPDATE archive_job_progression
SET current_node_key = job_key
WHERE current_node_key IS NULL;
ALTER TABLE archive_job_progression
    MODIFY COLUMN current_node_key VARCHAR(191) NOT NULL;
