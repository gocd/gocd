/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {EnvironmentVariable, EnvironmentVariables} from "models/environment_variables/types";
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

  it("validate name format", () => {
    const job = new Job("my awesome job that has a terrible name", [new ExecTask("ls", ["-lA"])]);
    expect(job.isValid()).toBe(false);
    expect(job.errors().count()).toBe(1);
    expect(job.errors().keys()).toEqual(["name"]);
    expect(job.errors().errorsForDisplay("name")).toBe("Invalid name. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
  });

  it("adopts errors in server response", () => {
    const job = new Job("scooby", [
        new ExecTask("whoami", []),
        new ExecTask("id", ["apache"])
      ],
      new EnvironmentVariables(new EnvironmentVariable("FOO", "OOF"), new EnvironmentVariable("BAR", "RAB"))
    );

    const unmatched = job.consumeErrorsResponse({
      errors: {name: ["ruh-roh!"]},
      tasks: [{errors: {command: ["who are you?"], not_exist: ["well, ain't that a doozy"]}}, {}],
      environment_variables: [{}, {errors: {name: ["BAR? yes please!"]}}]
    });

    expect(unmatched.hasErrors()).toBe(true);
    expect(unmatched.errorsForDisplay("job.tasks[0].notExist")).toBe("well, ain't that a doozy.");

    expect(job.errors().errorsForDisplay("name")).toBe("ruh-roh!.");

    const tasks = job.tasks();
    expect(tasks[0].attributes().errors().errorsForDisplay("command")).toBe("who are you?.");
    expect(tasks[1].attributes().errors().hasErrors()).toBe(false);

    const envs = job.environmentVariables();
    expect(envs[0].errors().hasErrors()).toBe(false);
    expect(envs[1].errors().errorsForDisplay("name")).toBe("BAR? yes please!.");
  });

  it("should serialize correctly", () => {
    const job = validJob();
    expect(job.toApiPayload()).toEqual({
      name: "name",
      environment_variables: [],
      resources: [],
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
