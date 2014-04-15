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

UPDATE stages SET orderId = CONVERT(id, INTEGER) WHERE ORDERID is NULL;

ALTER TABLE stages
ALTER COLUMN result
SET NULL;

UPDATE stages
SET result=NULL
WHERE result = 'Unknown';

UPDATE stages
SET result='Cancelled'
WHERE exists
    (select *
    from builds
    where builds.stageId = stages.id
    and builds.result ='Cancelled')
AND stages.result is null;

UPDATE stages
SET result='Failed'
WHERE exists
    (select *
    from builds
    where builds.stageId = stages.id
    and builds.result ='Failed')
AND stages.result is null;

UPDATE stages
SET result='Unknown'
WHERE exists
    (select *
    from builds
    where builds.stageId = stages.id
    and builds.result ='Unknown')
AND stages.result is null;

UPDATE stages
SET result='Passed'
WHERE stages.result is null;

ALTER TABLE stages
ALTER COLUMN result
SET NOT NULL;

ALTER TABLE STAGES ALTER COLUMN ORDERID SET NOT NULL;

--//@UNDO

ALTER TABLE stages ALTER COLUMN orderid SET NULL;
