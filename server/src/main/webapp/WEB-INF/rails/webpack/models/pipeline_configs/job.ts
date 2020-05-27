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

import {JsonUtils} from "helpers/json_utils";
import _ from "lodash";
import Stream from "mithril/stream";
import {TaskJSON} from "models/admin_templates/templates";
import {EnvironmentVariableJSON, EnvironmentVariables} from "models/environment_variables/types";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {ArtifactJSON, Artifacts} from "models/pipeline_configs/artifact";
import {TabJSON, Tabs} from "models/pipeline_configs/tab";
import {AbstractTask, Task} from "models/pipeline_configs/task";

export interface JobJSON {
  name: string;
  run_instance_count: number | "all" | null;
  timeout: "never" | number | null;
  elastic_profile_id?: string;
  environment_variables: EnvironmentVariableJSON[];
  resources: string[];
  tasks: TaskJSON[];
  tabs: TabJSON[];
  artifacts: ArtifactJSON[];
}

export class Job extends ValidatableMixin {
  //view state
  readonly runType              = Stream<"one" | "all" | "number">();
  readonly jobTimeoutType       = Stream<"never" | "default" | "number">();
  readonly name                 = Stream<string>();
  readonly runInstanceCount     = Stream<number | "all" | null>();
  readonly timeout              = Stream<"never" | number | null>();
  readonly elasticProfileId     = Stream<string>();
  readonly environmentVariables = Stream<EnvironmentVariables>();
  readonly resources            = Stream<string>();
  readonly tasks                = Stream<Task[]>();
  readonly tabs                 = Stream<Tabs>();
  readonly artifacts            = Stream<Artifacts>();

  private readonly __name = Stream<string>();

  constructor(name: string = "", tasks: Task[] = [], envVars = new EnvironmentVariables()) {
    super();

    this.__name               = Stream(name);
    this.name                 = Stream(name);
    this.tasks                = Stream(tasks);
    this.environmentVariables = Stream(envVars!);
    this.validatePresenceOf("name");
    this.validateIdFormat("name");

    this.validatePresenceOf("tasks");
    this.validateEach("tasks");
    this.validateEach("tabs");
    this.validateEach("environmentVariables");
    this.validatePresenceOf("timeout", {condition: () => this.jobTimeoutType() === "number"});
    this.validatePresenceOf("runInstanceCount", {condition: () => this.runType() === "number"});
    this.validateChildAttrIsUnique("environmentVariables",
                                   "name",
                                   {message: "Environment Variable names must be unique"});
  }

  static fromJSONArray(jobs: JobJSON[]) {
    return jobs.map(this.fromJSON);
  }

  static fromJSON(json: JobJSON) {
    const job = new Job();
    job.__name(json.name);
    job.name(json.name);
    job.setRunInstanceCount(json.run_instance_count);
    job.setJobTimeout(json.timeout);
    job.elasticProfileId(json.elastic_profile_id!);
    job.environmentVariables(EnvironmentVariables.fromJSON(json.environment_variables || []));
    job.resources(_.join(json.resources, ","));
    job.tasks(AbstractTask.fromJSONArray(json.tasks || []));
    job.tabs(Tabs.fromJSON(json.tabs || []));
    job.artifacts(Artifacts.fromJSON(json.artifacts || []));

    return job;
  }

  setJobTimeout(timeout: "never" | number | null) {
    if (timeout === "never") {
      this.jobTimeoutType("never");
    } else if (timeout === null || timeout === undefined) {
      this.jobTimeoutType("default");
    } else if ("number" === typeof timeout) {
      this.jobTimeoutType("number");
    }

    this.timeout(timeout);
  }

  setRunInstanceCount(runInstanceCount: number | "all" | null) {
    if (runInstanceCount === "all") {
      this.runType("all");
    } else if (runInstanceCount === null || runInstanceCount === undefined) {
      this.runType("one");
    } else if ("number" === typeof runInstanceCount) {
      this.runType("number");
    }

    this.runInstanceCount(runInstanceCount);
  }

  getOriginalName() {
    return this.__name();
  }

  toApiPayload() {
    if (this.tabs()) {
      this.tabs(this.tabs().filter(t => !_.isEmpty(t.name()) || !_.isEmpty(t.path())));
    }

    const json = JsonUtils.toSnakeCasedObject(this);

    json.resources = (json.resources || "")
      .split(",")
      .map((r: string) => r.trim())
      .filter((r: string) => !!r);

    return json;
  }
}
