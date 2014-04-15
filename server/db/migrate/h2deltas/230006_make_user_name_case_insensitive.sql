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


ALTER TABLE users ADD COLUMN preffered_id BIGINT;
ALTER TABLE users ADD COLUMN in_name varchar_ignorecase(255);
UPDATE users SET in_name = name;

CREATE TABLE preffered AS SELECT MIN(id) id, in_name FROM users WHERE enabled = true GROUP BY in_name;
UPDATE users SET preffered_id = (SELECT preffered.id FROM preffered WHERE users.in_name = preffered.in_name);
DELETE FROM users WHERE preffered_id IS NOT NULL AND id != preffered_id;

DROP TABLE preffered;
CREATE TABLE preffered AS SELECT MIN(id) id, in_name FROM users WHERE preffered_id IS NULL GROUP BY in_name;
UPDATE users SET preffered_id = (SELECT preffered.id FROM preffered WHERE users.in_name = preffered.in_name) WHERE preffered_id IS NULL;
DELETE FROM users WHERE id != preffered_id;

ALTER TABLE users DROP COLUMN preffered_id;
ALTER TABLE users DROP COLUMN in_name;

ALTER TABLE users ALTER COLUMN name varchar_ignorecase(255) NOT NULL;

--//@UNDO

ALTER TABLE users ALTER COLUMN name VARCHAR(255) NOT NULL;
