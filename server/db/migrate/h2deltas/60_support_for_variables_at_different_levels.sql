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


ALTER TABLE environmentVariables ADD entityType VARCHAR(100);
UPDATE environmentVariables SET entityType='Job';
ALTER TABLE environmentVariables ALTER COLUMN entityType SET NOT NULL;
ALTER TABLE environmentVariables ALTER COLUMN jobId RENAME TO entityId;

--//@UNDO

DELETE FROM environmentVariables WHERE entityType != 'Job';
ALTER TABLE environmentVariables DROP COLUMN entityType;
ALTER TABLE environmentVariables ALTER COLUMN entityId RENAME TO jobId;