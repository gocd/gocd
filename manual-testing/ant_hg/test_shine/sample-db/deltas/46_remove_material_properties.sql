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

ALTER TABLE materials ADD COLUMN branch TEXT;
UPDATE materials SET branch = (SELECT props.value FROM MaterialProperties props WHERE props.materialId = materials.id AND props.key = 'branch');

ALTER TABLE materials ADD COLUMN submoduleFolder TEXT;
UPDATE materials SET submoduleFolder = (SELECT props.value FROM MaterialProperties props WHERE props.materialId = materials.id AND props.key = 'submoduleFolder');

ALTER TABLE materials ADD COLUMN useTickets VARCHAR(10);
UPDATE materials SET useTickets = (SELECT props.value FROM MaterialProperties props WHERE props.materialId = materials.id AND props.key = 'useTickets');

ALTER TABLE materials ADD COLUMN view TEXT;
UPDATE materials SET view = (SELECT props.value FROM MaterialProperties props WHERE props.materialId = materials.id AND props.key = 'view');

DROP TABLE materialProperties;

--//@UNDO

-- we do not support undo for this migration
