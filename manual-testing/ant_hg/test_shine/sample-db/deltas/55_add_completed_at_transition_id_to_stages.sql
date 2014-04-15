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

ALTER TABLE stages ADD COLUMN completedByTransitionId BIGINT;

UPDATE stages SET completedByTransitionId = -1 WHERE id IN
    (SELECT DISTINCT b.stageid FROM builds b WHERE state <> 'Completed' and state <> 'Rescheduled');

CREATE INDEX idx_stages_completedByTransitionId ON stages (completedByTransitionId DESC);

UPDATE stages SET completedByTransitionId =
    (SELECT MAX(bst.id) FROM buildstatetransitions bst WHERE bst.stageid = stages.id) WHERE completedByTransitionId IS null;

UPDATE stages SET completedByTransitionId = null WHERE completedByTransitionId = -1;

--//@UNDO

DROP INDEX IF EXISTS idx_stages_completedByTransitionId;

ALTER TABLE stages DROP COLUMN completedByTransitionId;