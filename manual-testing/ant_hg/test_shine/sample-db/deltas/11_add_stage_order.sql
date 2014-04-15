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

ALTER TABLE stages ADD COLUMN orderId INTEGER;
CREATE INDEX idx_stages_orderId ON stages(orderId);
UPDATE stages SET orderId = CONVERT(id, INTEGER);

--//@UNDO

DROP INDEX idx_stages_orderId IF EXISTS;
ALTER TABLE stages DROP COLUMN orderId;