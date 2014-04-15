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

-- populate missing buildStateTransitions (for instance, where build is completed but 'completed' transition does not exist)

CREATE TABLE latest_bst AS (SELECT MAX(bst.id) id, bst.buildId, bst.stageId, b.state build_state FROM buildStateTransitions bst inner join builds b on b.id = bst.buildid GROUP BY bst.buildId);

ALTER TABLE latest_bst ADD COLUMN state VARCHAR(255);
ALTER TABLE latest_bst ADD COLUMN transition_time TIMESTAMP(23, 10);

UPDATE latest_bst SET state = (SELECT currentState FROM buildStateTransitions bst WHERE bst.id = latest_bst.id),
                      transition_time = (select statechangetime from buildstatetransitions bst where bst.id = latest_bst.id);

INSERT INTO buildStateTransitions (buildId, currentState, stageId, stateChangeTime)
    SELECT buildId, build_state, stageId, transition_time FROM latest_bst
        WHERE build_state != state AND
              build_state IN ('Completed', 'Discontinued', 'Rescheduled');

DROP TABLE latest_bst;

--- populate completedByTransitionId for stages that are unpopulated because migration#55 was incomplete

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

-- populate completedByTransitionId = -1 for stages that have no builds

CREATE TABLE stages_without_builds AS (SELECT stages.id FROM stages LEFT OUTER JOIN builds ON builds.stageid = stages.id WHERE builds.id IS NULL);

UPDATE stages SET completedByTransitionId = -1 WHERE id IN (SELECT swb.id FROM stages_without_builds swb);

DROP TABLE stages_without_builds;

--//@UNDO

--ignore