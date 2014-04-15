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

ALTER TABLE pipelines ADD COLUMN label VARCHAR(255);
CREATE INDEX idx_pipeline_label ON pipelines(label);

// Copy the values from a temp table to the new column
CREATE TABLE temp (label VARCHAR(255));
INSERT INTO temp (label) (SELECT id FROM pipelines);
UPDATE pipelines SET label = (SELECT label FROM temp WHERE temp.label = pipelines.id);
DROP TABLE temp;


--//@UNDO

DROP INDEX idx_pipeline_label IF EXISTS;
ALTER TABLE pipelines DROP COLUMN label;