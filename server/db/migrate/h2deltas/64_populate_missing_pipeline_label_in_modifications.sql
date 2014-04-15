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

ALTER TABLE modifications ADD materialType VARCHAR(255);
ALTER TABLE modifications ADD pipelineName VARCHAR(255);
ALTER TABLE modifications ADD pipelineCounter VARCHAR(255);

UPDATE modifications m1 SET materialType = (SELECT materials.type FROM modifications m2 INNER JOIN materials on m2.materialid = materials.id WHERE m1.id = m2.id);

UPDATE modifications SET
       pipelineName = REGEXP_REPLACE(revision, '\/.*', ''),
       pipelineCounter = REGEXP_REPLACE(REGEXP_REPLACE(revision, '^.*?\/', ''), '\/.*', '')
            WHERE materialType = 'DependencyMaterial' AND pipelineLabel IS NULL;

UPDATE modifications m SET m.pipelineLabel = (SELECT p.label FROM pipelines p WHERE p.name = m.pipelineName AND p.counter = m.pipelineCounter) WHERE pipelineCounter REGEXP('\d+') AND pipelineLabel IS NULL;

ALTER TABLE modifications DROP COLUMN materialType;
ALTER TABLE modifications DROP COLUMN pipelineName;
ALTER TABLE modifications DROP COLUMN pipelineCounter;

--//@UNDO