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

ALTER TABLE stages DROP CONSTRAINT FK_STAGES_PIPELINES IF EXISTS;
ALTER TABLE materials DROP CONSTRAINT FK_MATERIALS_PIPELINES IF EXISTS;
ALTER TABLE builds DROP CONSTRAINT FK_BUILDS_STAGES IF EXISTS;
ALTER TABLE properties DROP CONSTRAINT FK_PROPERTIES_BUILDS IF EXISTS;
ALTER TABLE artifactPlans DROP CONSTRAINT FK_ARTIFACTPLANS_BUILDS IF EXISTS;
ALTER TABLE resources DROP CONSTRAINT FK_RESOURCES_BUILDS IF EXISTS;
ALTER TABLE modifiedFiles DROP CONSTRAINT FK_MODIFIEDFILES_MODIFICATIONS IF EXISTS;
ALTER TABLE buildstatetransitions DROP CONSTRAINT FK_BUILDSTATETRANSITIONS_BUILDS IF EXISTS;

ALTER TABLE stages ADD CONSTRAINT fk_stages_pipelines FOREIGN KEY (pipelineId) REFERENCES pipelines(id) ON DELETE CASCADE;
ALTER TABLE materials ADD CONSTRAINT fk_materials_pipelines FOREIGN KEY (pipelineId) REFERENCES pipelines(id) ON DELETE CASCADE;
ALTER TABLE builds ADD CONSTRAINT fk_builds_stages FOREIGN KEY (stageId) REFERENCES stages(id) ON DELETE CASCADE;
ALTER TABLE properties ADD CONSTRAINT fk_properties_builds FOREIGN KEY (buildId) REFERENCES builds(id) ON DELETE CASCADE;
ALTER TABLE artifactPlans ADD CONSTRAINT fk_artifactplans_builds FOREIGN KEY (buildId) REFERENCES builds(id) ON DELETE CASCADE;
ALTER TABLE resources ADD CONSTRAINT fk_resources_builds FOREIGN KEY (buildId) REFERENCES builds(id) ON DELETE CASCADE;
ALTER TABLE modifiedFiles ADD CONSTRAINT fk_modifiedFiles_modifications FOREIGN KEY (modificationId) REFERENCES modifications(id) ON DELETE CASCADE;
ALTER TABLE buildstatetransitions ADD CONSTRAINT fk_buildstatetransitions_builds FOREIGN KEY (buildId) REFERENCES builds(id) ON DELETE CASCADE;

DELETE FROM pipelines WHERE buildCauseType='ErrorBuildCause';

--//@UNDO

ALTER TABLE stages DROP CONSTRAINT FK_STAGES_PIPELINES IF EXISTS;
ALTER TABLE materials DROP CONSTRAINT FK_MATERIALS_PIPELINES IF EXISTS;
ALTER TABLE builds DROP CONSTRAINT FK_BUILDS_STAGES IF EXISTS;
ALTER TABLE properties DROP CONSTRAINT FK_PROPERTIES_BUILDS IF EXISTS;
ALTER TABLE artifactplans DROP CONSTRAINT FK_ARTIFACTPLANS_BUILDS IF EXISTS;
ALTER TABLE resources DROP CONSTRAINT FK_RESOURCES_BUILDS IF EXISTS;
ALTER TABLE modifiedFiles DROP CONSTRAINT FK_MODIFIEDFILES_MODIFICATIONS IF EXISTS;
ALTER TABLE buildstatetransitions DROP CONSTRAINT FK_BUILDSTATETRANSITIONS_BUILDS IF EXISTS;

ALTER TABLE stages ADD CONSTRAINT fk_stages_pipelines FOREIGN KEY (pipelineId) REFERENCES pipelines(id);
ALTER TABLE materials ADD CONSTRAINT fk_materials_pipelines FOREIGN KEY (pipelineId) REFERENCES pipelines(id);
ALTER TABLE builds ADD CONSTRAINT fk_builds_stages FOREIGN KEY (stageId) REFERENCES stages(id);
ALTER TABLE properties ADD CONSTRAINT fk_properties_builds FOREIGN KEY (buildId) REFERENCES builds(id);
ALTER TABLE artifactplans ADD CONSTRAINT fk_artifactplans_builds FOREIGN KEY (buildId) REFERENCES builds(id);
ALTER TABLE resources ADD CONSTRAINT fk_resources_builds FOREIGN KEY (buildId) REFERENCES builds(id);
ALTER TABLE modifiedFiles ADD CONSTRAINT fk_modifiedFiles_modifications FOREIGN KEY (modificationId) REFERENCES modifications(id);
ALTER TABLE buildstatetransitions ADD CONSTRAINT fk_buildstatetransitions_builds FOREIGN KEY (buildId) REFERENCES builds(id);
