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

UPDATE pipelinematerialrevisions  SET fromRevisionId = scheduleTimeFromRevisionId;
UPDATE pipelinematerialrevisions SET toRevisionId = scheduleTimeToRevisionId;

ALTER TABLE pipelinematerialrevisions DROP CONSTRAINT fk_pmr_schedule_time_to_revision;
ALTER TABLE pipelinematerialrevisions DROP CONSTRAINT fk_pmr_schedule_time_from_revision;

ALTER TABLE pipelinematerialrevisions DROP COLUMN scheduleTimeToRevisionId;
ALTER TABLE pipelinematerialrevisions DROP COLUMN scheduleTimeFromRevisionId;

--//@UNDO

ALTER TABLE pipelinematerialrevisions ADD scheduleTimeFromRevisionId BIGINT;
ALTER TABLE pipelinematerialrevisions ADD scheduleTimeToRevisionId BIGINT;

UPDATE pipelinematerialrevisions SET scheduleTimeFromRevisionId = fromRevisionId;
UPDATE pipelinematerialrevisions SET scheduleTimeToRevisionId = toRevisionId;

ALTER TABLE pipelineMaterialRevisions ADD CONSTRAINT fk_pmr_schedule_time_from_revision FOREIGN KEY (scheduleTimeFromRevisionId) REFERENCES modifications (id);
ALTER TABLE pipelineMaterialRevisions ADD CONSTRAINT fk_pmr_schedule_time_to_revision FOREIGN KEY (scheduleTimeToRevisionId) REFERENCES modifications (id);

ALTER TABLE pipelineMaterialRevisions ALTER COLUMN scheduleTimeFromRevisionId SET NOT NULL;
ALTER TABLE pipelineMaterialRevisions ALTER COLUMN scheduleTimeToRevisionId SET NOT NULL;
