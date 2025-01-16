/*
 * Copyright 2022 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {JobResult} from "../shared/job_result";
import {JobState} from "../shared/job_state";
import {JobStateTransitionJSON} from "../shared/job_state_transition";

export interface JobInstanceJSON {
  name: string;
  state: JobState;
  result: JobResult;
  original_job_id?: any;
  scheduled_date: number;
  rerun: boolean;
  agent_uuid: string;
  pipeline_name: string;
  pipeline_counter: number;
  stage_name: string;
  stage_counter: string;
  job_state_transitions: JobStateTransitionJSON[];
}

export function findTransition(jobInstance: JobInstanceJSON, state: JobState): JobStateTransitionJSON | undefined {
  return jobInstance.job_state_transitions.find(value => value.state === state);
}
