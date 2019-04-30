/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {Job} from "models/pipeline_configs/job";
import {ExecTask} from "models/pipeline_configs/task";

describe("Job model", () => {
  function validJob() {
    return new Job("name", [new ExecTask("ls", ["-lA"])]);
  }

  it("should include a name", () => {
    let job = validJob();
    expect(job.isValid()).toBe(true);
    expect(job.errors().count()).toBe(0);

    job = new Job("", [new ExecTask("ls", [])]);
    expect(job.isValid()).toBe(false);
    expect(job.errors().count()).toBe(1);
  });

  it("should serialize correctly", () => {
    const job = validJob();
    expect(job.toApiPayload()).toEqual({
      name: "name",
      tasks: [{
        type: "exec",
        attributes: {
          command: "ls",
          arguments: ["-lA"],
          run_if: []
        }
      }]
    });
  });

});
