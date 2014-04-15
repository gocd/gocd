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

CREATE INDEX IF NOT EXISTS idx_properties_buildId ON properties(buildId);
CREATE INDEX IF NOT EXISTS idx_materials_pipelinename ON materials(pipelinename);
CREATE INDEX IF NOT EXISTS idx_artifactpropertiesgenerator_jobid ON artifactpropertiesgenerator(jobid);
CREATE INDEX IF NOT EXISTS idx_environmentvariables_entitytype ON environmentvariables(entitytype);
CREATE INDEX IF NOT EXISTS idx_stageartifactcleanupprohibited_pipelinename ON stageartifactcleanupprohibited(pipelinename);
CREATE INDEX IF NOT EXISTS idx_pipelineLabelCounts_pipelinename ON pipelineLabelCounts(pipelinename);


--//@UNDO

DROP INDEX idx_properties_buildId IF EXISTS;
DROP INDEX idx_materials_pipelinename IF EXISTS;
DROP INDEX idx_artifactpropertiesgenerator_jobid IF EXISTS;
DROP INDEX idx_environmentvariables_entitytype IF EXISTS;
DROP INDEX idx_stageartifactcleanupprohibited_pipelinename IF EXISTS;
DROP INDEX idx_pipelineLabelCounts_pipelinename IF EXISTS;

