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
import {Stage} from "models/pipeline_configs/stage";
import {ExecTask} from "models/pipeline_configs/task";
import {EnvironmentVariable, EnvironmentVariables} from "models/environment_variables/types";

describe("Stage model", () => {
  function validJob() {
    return new Job("name", [new ExecTask("ls", ["-lA"])]);
  }

  it("should include a name", () => {
    let stage = new Stage("foo", [validJob()]);
    expect(stage.isValid()).toBe(true);
    expect(stage.errors().count()).toBe(0);

    stage = new Stage("", [validJob()]);
    expect(stage.isValid()).toBe(false);
    expect(stage.errors().count()).toBe(1);
    expect(stage.errors().keys()).toEqual(["name"]);
  });

  it("validates name format", () => {
    const stage = new Stage("my awesome stage that has a terrible name", [validJob()]);
    expect(stage.isValid()).toBe(false);
    expect(stage.errors().count()).toBe(1);
    expect(stage.errors().keys()).toEqual(["name"]);
    expect(stage.errors().errorsForDisplay("name")).toBe("Invalid name. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
  });

  it("should include a job", () => {
    const stage = new Stage("foo", []);
    expect(stage.isValid()).toBe(false);
    expect(stage.errors().count()).toBe(1);
    expect(stage.errors().keys()).toEqual(["jobs"]);
    expect(stage.errors().errorsForDisplay("jobs")).toBe("A stage must have at least one job.");
  });

  it("approval state allows toggling between automatic and manual approval types", () => {
    const stage = new Stage("foo", [validJob()]);
    expect(stage.toApiPayload().approval.type).toBe("success"); // default setting

    stage.approval().state(false);
    expect(stage.toApiPayload().approval.type).toBe("manual");

    stage.approval().state(true);
    expect(stage.toApiPayload().approval.type).toBe("success");
  });

  it("adopts errors in server response", () => {
    const stage = new Stage("meow", [
      new Job("scooby", [new ExecTask("whoami", [])], new EnvironmentVariables(new EnvironmentVariable("FOO", "OOF"))),
      new Job("doo", [new ExecTask("id", ["apache"])], new EnvironmentVariables(new EnvironmentVariable("BAR", "RAB")))
    ]);

    const unmatched = stage.consumeErrorsResponse({
      errors: {name: ["yay"]},
      jobs: [
        {errors: {name: ["ruh-roh!"]}, tasks: [{errors: {command: ["who are you?"]}}], environment_variables: [{}]},
        {
          tasks: [{}],
          environment_variables: [{errors: {name: ["BAR? yes please!"], not_exist: ["well, ain't that a doozy"]}}]
        }
      ]
    });

    expect(unmatched.hasErrors()).toBe(true);
    expect(unmatched.errorsForDisplay("stage.jobs[1].environmentVariables[0].notExist")).toBe("well, ain't that a doozy.");

    expect(stage.errors().errorsForDisplay("name")).toBe("yay.");

    const [job1, job2] = Array.from(stage.jobs());
    expect(job1.errors().errorsForDisplay("name")).toBe("ruh-roh!.");
    expect(job2.errors().hasErrors()).toBe(false);

    expect(job1.tasks()[0].attributes().errors().errorsForDisplay("command")).toBe("who are you?.");
    expect(job1.environmentVariables()[0].errors().hasErrors()).toBe(false);

    expect(job2.tasks()[0].attributes().errors().hasErrors()).toBe(false);
    expect(job2.environmentVariables()[0].errors().errorsForDisplay("name")).toBe("BAR? yes please!.");
  });

  it("should serialize correctly", () => {
    const stage = new Stage("foo", [validJob()]);
    expect(stage.toApiPayload()).toEqual({
      name: "foo",
      approval: {
        type: "success",
        authorization: {}
      },
      jobs: [
        {
          name: "name",
          environment_variables: [],
          tasks: [{
            type: "exec",
            attributes: {
              command: "ls",
              arguments: ["-lA"],
              run_if: []
            }
          }]
        }
      ]
    });
  });

});
