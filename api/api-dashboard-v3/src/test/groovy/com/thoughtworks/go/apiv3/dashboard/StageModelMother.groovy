/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv3.dashboard

import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.domain.StageIdentifier
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel

class StageModelMother {
  static StageInstanceModel stage_model(String name, Object counter, JobState job_state = JobState.Completed, JobResult job_result = JobResult.Passed) {
    def job_1 = new JobHistory()
    job_1.addJob("job-1", job_state, job_result, new Date())
    return new StageInstanceModel(name, counter.toString(), job_1, new StageIdentifier("cruise", 10, name, counter.toString()))
  }
}
