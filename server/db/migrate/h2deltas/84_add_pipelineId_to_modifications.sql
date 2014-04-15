--*************************GO-LICENSE-START*********************************
-- Copyright 2014 ThoughtWorks, Inc.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--    http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--*************************GO-LICENSE-END***********************************

ALTER TABLE modifications ADD pipelineId BIGINT;
ALTER TABLE modifications ADD CONSTRAINT fk_modifications_pipelineId FOREIGN KEY (pipelineId) REFERENCES pipelines;

--// Creating temp columns for migration
ALTER TABLE pipelines ADD COLUMN TEMP_CASE_SENSITIVE_NAME VARCHAR_CASESENSITIVE;
UPDATE pipelines SET TEMP_CASE_SENSITIVE_NAME = name;
CREATE INDEX idx_pipelines_temp_case_sensitive_name ON pipelines(TEMP_CASE_SENSITIVE_NAME);

ALTER TABLE modifications ADD COLUMN TEMP_REVISION_FOR_MIGRATION VARCHAR_CASESENSITIVE;
ALTER TABLE modifications ADD COLUMN TEMP_COUNTER_FOR_MIGRATION VARCHAR_CASESENSITIVE;
UPDATE modifications SET TEMP_REVISION_FOR_MIGRATION = REGEXP_REPLACE(revision, '\/.*', ''), TEMP_COUNTER_FOR_MIGRATION = REGEXP_REPLACE(REGEXP_REPLACE(revision, '^.*?\/', ''), '\/.*', '');
CREATE INDEX idx_modifications_temp_revision_for_migration ON modifications(TEMP_REVISION_FOR_MIGRATION, TEMP_COUNTER_FOR_MIGRATION);

--// Actual Migration 
UPDATE modifications
  SET pipelineId = (SELECT id
                    FROM pipelines
                    WHERE TEMP_CASE_SENSITIVE_NAME = TEMP_REVISION_FOR_MIGRATION
                      AND counter = TEMP_COUNTER_FOR_MIGRATION)
  WHERE revision LIKE '%/%/%/%' AND revision REGEXP('.+?/-?\d+/.+');

--// Deleting temp columns for migration
DROP INDEX idx_modifications_temp_revision_for_migration IF EXISTS;
ALTER TABLE modifications DROP COLUMN TEMP_COUNTER_FOR_MIGRATION;
ALTER TABLE modifications DROP COLUMN TEMP_REVISION_FOR_MIGRATION;
DROP INDEX idx_pipelines_temp_case_sensitive_name IF EXISTS;
ALTER TABLE pipelines DROP COLUMN TEMP_CASE_SENSITIVE_NAME;


--//@UNDO
DROP INDEX idx_modifications_temp_revision_for_migration IF EXISTS;
DROP INDEX idx_pipelines_temp_case_sensitive_name IF EXISTS;
ALTER TABLE modifications DROP CONSTRAINT fk_modifications_pipelineId;
ALTER TABLE modifications DROP pipelineId;
