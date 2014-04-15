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

ALTER TABLE modifications ADD counterBasedRevision VARCHAR(1024);
ALTER TABLE modifications ADD materialType VARCHAR(255);
UPDATE modifications m1 SET materialType = (SELECT materials.type FROM modifications m2 INNER JOIN materials on m2.materialid = materials.id WHERE m1.id = m2.id);
UPDATE modifications m1 SET counterBasedRevision = revision WHERE materialType = 'DependencyMaterial' AND revision REGEXP '.*\/\d+\[.*\]\/.*\/.*';
UPDATE modifications SET counterBasedRevision = REGEXP_REPLACE(REGEXP_REPLACE(REGEXP_REPLACE(counterBasedRevision, '^.*?\/', ''), '\/.*', ''), '\[.*', '') WHERE counterBasedRevision IS NOT NULL;
UPDATE modifications SET counterBasedRevision = REGEXP_REPLACE(revision, '\d+\[.*?\]', counterBasedRevision) WHERE counterBasedRevision IS NOT NULL;

ALTER TABLE modifications ADD pipelineName VARCHAR(255);
ALTER TABLE modifications ADD pipelineLabel VARCHAR(255);
ALTER TABLE modifications ADD stageName VARCHAR(255);
ALTER TABLE modifications ADD stageCounter VARCHAR(255);
ALTER TABLE modifications ADD pipelineCounter VARCHAR(255);

UPDATE modifications SET
    pipelineName = REGEXP_REPLACE(revision, '\/.*', ''),
    pipelineLabel = REGEXP_REPLACE(REGEXP_REPLACE(revision, '^.*?\/', ''), '\/.*', ''),
    stageName = REGEXP_REPLACE(REGEXP_REPLACE(revision, '^.*?\/.*?\/', ''), '\/.*', ''),
    stageCounter = REGEXP_REPLACE(revision, '^.*?\/.*?\/.*?\/', '')
        WHERE counterBasedRevision IS NULL AND materialType = 'DependencyMaterial';

CREATE TABLE pipelineStages AS (SELECT p.name AS pipelineName, p.counter AS pipelineCounter, p.label AS pipelineLabel, s.name AS stageName, s.counter AS stageCounter FROM stages s INNER JOIN pipelines p ON s.pipelineId = p.id);

CREATE INDEX TMP_IDX_PIPELINE_STAGES_LABEL ON pipelineStages(pipelineName, pipelineLabel, stageName, stageCounter);
CREATE INDEX TMP_IDX_PIPELINE_STAGES_COUNTER ON pipelineStages(pipelineName, pipelineCounter, stageName, stageCounter);

UPDATE modifications m SET pipelineCounter = (SELECT MIN(ps.pipelineCounter) FROM pipelineStages ps WHERE ps.pipelineName = m.pipelineName AND ps.pipelineLabel = m.pipelineLabel AND ps.stageName = m.stageName AND ps.stageCounter = m.stageCounter) where materialType = 'DependencyMaterial';

UPDATE modifications SET counterBasedRevision = CONCAT(pipelineName, '/', pipelineCounter, '/', stageName, '/', stageCounter) WHERE pipelineCounter IS NOT NULL;

UPDATE modifications SET counterBasedRevision = revision WHERE counterBasedRevision IS NULL AND materialType = 'DependencyMaterial';

ALTER TABLE modifications ADD uniqueModificationId bigint(19);
CREATE INDEX TMP_IDX_UNIQUE_MODIFICATION_ID ON modifications(counterBasedRevision);
UPDATE modifications m1 SET uniqueModificationId = (SELECT MIN(id) FROM modifications m2 WHERE m2.counterBasedRevision = m1.counterBasedRevision AND m2.materialType = 'DependencyMaterial');
UPDATE modifications SET uniqueModificationId = id WHERE uniqueModificationId IS NULL;
UPDATE pipelineMaterialRevisions pmr SET toRevisionId = (SELECT uniqueModificationId FROM modifications m WHERE pmr.toRevisionId = m.id);
UPDATE pipelineMaterialRevisions pmr SET fromRevisionId = (SELECT uniqueModificationId FROM modifications m WHERE pmr.fromRevisionId = m.id);

DELETE FROM modifications WHERE id <> uniqueModificationId;

UPDATE modifications SET revision = counterBasedRevision WHERE materialType = 'DependencyMaterial';

UPDATE modifications m SET pipelineLabel = (SELECT ps.pipelineLabel FROM pipelineStages ps WHERE ps.pipelineName = m.pipelineName AND ps.pipelineCounter = m.pipelineCounter AND ps.stageName = m.stageName AND ps.stageCounter = m.stageCounter) where materialType = 'DependencyMaterial';

DROP INDEX TMP_IDX_UNIQUE_MODIFICATION_ID;
DROP INDEX TMP_IDX_PIPELINE_STAGES_LABEL;
DROP INDEX TMP_IDX_PIPELINE_STAGES_COUNTER;
DROP TABLE pipelineStages;
ALTER TABLE modifications DROP COLUMN counterBasedRevision;
ALTER TABLE modifications DROP COLUMN uniqueModificationId;
ALTER TABLE modifications DROP COLUMN materialType;
ALTER TABLE modifications DROP COLUMN pipelineName;
ALTER TABLE modifications DROP COLUMN pipelineCounter;
ALTER TABLE modifications DROP COLUMN stageName;
ALTER TABLE modifications DROP COLUMN stageCounter;

--//@UNDO
