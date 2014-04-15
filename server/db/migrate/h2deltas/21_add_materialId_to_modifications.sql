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

ALTER TABLE modifications
ADD COLUMN materialId INTEGER;

-- associate modifications with materials

update modifications
SET materialId=
    (SELECT materials.id
    FROM materials
    JOIN pipelines
    ON materials.pipelineId=pipelines.id
    AND pipelines.id=modifications.pipelineId
    LIMIT 1);

CREATE INDEX idx_modifications_materialId ON modifications(materialId);
CREATE INDEX idx_modifications_modifiedtime ON modifications(modifiedtime);

-- copy modifications onto new materials for svn repos with multiple materials
-- this is for some number of historical pipelines that were created when
-- multiple materials support was not complete

INSERT INTO modifications(TYPE, USERNAME, COMMENT, EMAILADDRESS, REVISION, MODIFIEDTIME, PIPELINEID, FROMEXTERNAL, MATERIALID )
SELECT m1.TYPE, m1.USERNAME, m1.COMMENT, m1.EMAILADDRESS, m1.REVISION, m1.MODIFIEDTIME, m1.PIPELINEID, m1.FROMEXTERNAL, materials.ID
FROM modifications m1
JOIN materials on  m1.pipelineid = materials.pipelineid
WHERE not exists(select 1 from modifications where modifications.materialId = materials.id);

ALTER TABLE modifications
ALTER COLUMN materialId SET NOT NULL;

ALTER TABLE modifications
ADD CONSTRAINT fk_modifications_materials
FOREIGN KEY (materialId)
REFERENCES materials(id) ON DELETE CASCADE;

--//@UNDO

ALTER TABLE modifications DROP COLUMN materialId;



