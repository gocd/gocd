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

import Stream from "mithril/stream";

export enum RunIfCondition {
  Passed = "Passed",
  Failed = "Failed",
  Any    = "Any"
}

export enum TaskType {
  Exec,
  Fetch
}

export interface Task {
  represent: () => any;
  getType: () => TaskType;
}

abstract class TaskBase {
  public readonly runIfCondition: Stream<RunIfCondition>;

  constructor(runIfCondition?: RunIfCondition) {
    this.runIfCondition = Stream(runIfCondition ? runIfCondition : RunIfCondition.Passed);
  }
}

export class ExecTask extends TaskBase implements Task {
  public readonly command: Stream<string | undefined>;
  public readonly workingDir: Stream<string | undefined>;

  constructor(command?: string, workingDir?: string, runIfCondition?: RunIfCondition) {
    super(runIfCondition);
    this.command    = Stream(command);
    this.workingDir = Stream(workingDir);
  }

  getType() {
    return TaskType.Exec;
  }

  represent() {
    return this.command();
  }
}

export class FetchArtifactTask extends TaskBase implements Task {
  constructor(runIfCondition?: RunIfCondition) {
    super(runIfCondition);
  }

  getType() {
    return TaskType.Fetch;
  }

  represent() {
    return "fetch task representation";
  }
}
