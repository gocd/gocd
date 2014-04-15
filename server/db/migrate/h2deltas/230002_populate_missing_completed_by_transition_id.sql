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

--- populate completedByTransitionId for stages that are unpopulated because migration#55 was incomplete. This is a subset of migration 90 for the bug introduced on go02 and go03

CREATE TABLE latest_bst AS (SELECT MAX(bst.id) id, bst.buildId, bst.stageId FROM buildStateTransitions bst GROUP BY bst.buildId);

ALTER TABLE latest_bst ADD COLUMN state VARCHAR(255);
ALTER TABLE latest_bst ADD COLUMN non_complete_state INT DEFAULT 0;

UPDATE latest_bst SET state = (SELECT currentState FROM buildStateTransitions bst WHERE bst.id = latest_bst.id);

UPDATE latest_bst SET non_complete_state = 1 WHERE state <> 'Completed' AND state <> 'Rescheduled' AND state <> 'Discontinued';

CREATE TABLE latest_stage_bst AS (SELECT MAX(id) id, SUM(non_complete_state) total_incomplete, stageId FROM latest_bst GROUP BY stageId);

DELETE FROM latest_stage_bst WHERE total_incomplete > 0;

MERGE INTO stages (id, completedByTransitionId) key(id) select stageId id, id completedByTransitionId from latest_stage_bst;

DROP TABLE latest_bst;

DROP TABLE latest_stage_bst;

--//@UNDO

--ignore