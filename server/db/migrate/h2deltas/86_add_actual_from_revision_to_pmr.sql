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

-- This index helps speed up the pipeline graph compare queries, and is needed for quickly finding the nextModificationId in the Java migration below   
CREATE INDEX idx_modifications_materialid_id ON modifications(materialId,id);


ALTER TABLE pipelineMaterialRevisions ADD COLUMN actualFromRevisionId BIGINT;
ALTER TABLE pipelineMaterialRevisions ADD CONSTRAINT fk_pmr_actualFromRevisionId FOREIGN KEY (actualFromRevisionId) REFERENCES modifications;

CREATE TABLE DUMMY_TABLE_FOR_MIGRATION_86 (id int);

CREATE TRIGGER TRIGGER_86_FOR_STORY_4643 BEFORE SELECT ON DUMMY_TABLE_FOR_MIGRATION_86 CALL "com.thoughtworks.go.server.sqlmigration.Migration_86";

SELECT * FROM DUMMY_TABLE_FOR_MIGRATION_86;

DROP TRIGGER TRIGGER_86_FOR_STORY_4643;

DROP TABLE DUMMY_TABLE_FOR_MIGRATION_86;

ALTER TABLE pipelineMaterialRevisions ALTER COLUMN actualFromRevisionId BIGINT NOT NULL;

--//@UNDO

ALTER TABLE pipelineMaterialRevisions DROP CONSTRAINT fk_pmr_actualFromRevisionId;
ALTER TABLE pipelineMaterialRevisions DROP COLUMN actualFromRevisionId;

DROP INDEX idx_modifications_materialid_id;