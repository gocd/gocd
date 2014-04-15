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

ALTER TABLE fetchArtifactPlans ALTER COLUMN jobId SET NOT NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN pipelineLabel SET NOT NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN pipeline SET NOT NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN stage SET NOT NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN job SET NOT NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN path SET NOT NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN dest SET NOT NULL;

--//@UNDO
ALTER TABLE fetchArtifactPlans ALTER COLUMN jobId SET NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN pipelineLabel SET NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN pipeline SET NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN stage SET NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN job SET NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN path SET NULL;
ALTER TABLE fetchArtifactPlans ALTER COLUMN dest SET NULL;

