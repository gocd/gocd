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
  readonly name                 = Stream<string>();
  readonly runInstanceCount     = Stream<number | "all" | null>();
  readonly timeout              = Stream<"never" | number | null>();
  readonly elasticProfileId     = Stream<string>();
  readonly environmentVariables = Stream<EnvironmentVariables>();
  readonly resources            = Stream<string[]>();
  readonly tasks                = Stream<Task[]>();
  readonly tabs                 = Stream<Tabs>();
  readonly artifacts            = Stream<Artifacts>();

  constructor(name: string = "", tasks: Task[] = [], envVars = new EnvironmentVariables()) {
    super();

    this.name                 = Stream(name);
    this.tasks                = Stream(tasks);
    this.environmentVariables = Stream(envVars!);
    this.validatePresenceOf("name");
    this.validateIdFormat("name");

    this.validatePresenceOf("tasks");
    this.validateNonEmptyCollection("tasks", {message: "A job must have at least one task"});
    this.validateEach("tasks");
    this.validateEach("environmentVariables");
    this.validateChildAttrIsUnique("environmentVariables", "name", {message: "Environment Variable names must be unique"});
  }

  static fromJSONArray(jobs: JobJSON[]) {
    return jobs.map(this.fromJSON);
  }

  static fromJSON(json: JobJSON) {
    const job = new Job();
    job.name(json.name);
    job.runInstanceCount(json.run_instance_count);
    job.timeout(json.timeout);
    job.elasticProfileId(json.elastic_profile_id!);
    job.environmentVariables(EnvironmentVariables.fromJSON(json.environment_variables));
    job.resources(json.resources);
    job.tasks(AbstractTask.fromJSONArray(json.tasks));
    job.tabs(Tabs.fromJSON(json.tabs));
    job.artifacts(Artifacts.fromJSON(json.artifacts));
    return job;
  }

  toApiPayload() {
    return JsonUtils.toSnakeCasedObject(this);
  }
}
