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

CREATE INDEX idx_pipeline_name ON pipelines (name);
CREATE INDEX idx_stage_name ON stages (name);
CREATE INDEX idx_properties_key ON properties (key);
CREATE INDEX idx_state_transition ON buildStateTransitions (currentState);
CREATE INDEX idx_build_state ON builds (state);
CREATE INDEX idx_build_name ON builds (name);
CREATE INDEX idx_build_result ON builds (result);
CREATE INDEX idx_build_agent ON builds (agentUuid);

--//@UNDO

DROP INDEX idx_build_agent
DROP INDEX idx_build_result
DROP INDEX idx_build_name
DROP INDEX idx_build_state
DROP INDEX idx_state_transition
DROP INDEX idx_properties_key
DROP INDEX idx_stage_name
DROP INDEX idx_pipeline_name