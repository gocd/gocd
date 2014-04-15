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

ALTER TABLE pipelineMaterialRevisions ADD materialType VARCHAR(255);

UPDATE pipelineMaterialRevisions pmr SET materialType = (SELECT m.type FROM modifications mods INNER JOIN materials m on mods.materialId = m.id WHERE mods.id = pmr.toRevisionId);

UPDATE pipelineMaterialRevisions SET fromRevisionId = toRevisionId, scheduleTimeFromRevisionId = scheduleTimeToRevisionId WHERE materialType = 'DependencyMaterial';

ALTER TABLE pipelineMaterialRevisions DROP COLUMN materialType;

--//@UNDO