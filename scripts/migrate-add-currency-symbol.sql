-- MySQL 8 one-time migration for existing ModularJobs installations.
-- Back up the database, stop all ModularJobs instances, then run:
--   mysql --host=host --port=3306 --user=user --password modularjobs \
--     < scripts/migrate-add-currency-symbol.sql
--
-- Existing rows retain NULL because their original symbols are unrecoverable.
-- ModularJobs renders their currency identifier as the documented fallback.

ALTER TABLE job_task_payables
    ADD COLUMN currency_symbol VARCHAR(191) NULL AFTER currency_identifier;
