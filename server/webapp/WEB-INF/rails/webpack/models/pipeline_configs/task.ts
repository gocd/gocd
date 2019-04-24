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

import JsonUtils from "helpers/json_utils";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

type ValidTypes = "exec" | "fetchArtifact";
export interface Task extends ValidatableMixin {
  type: ValidTypes;
  toApiPayload: () => any;
}

export class ExecTask extends ValidatableMixin implements Task {
  type: ValidTypes =  "exec";
  command: string;
  arguments: string[];

  constructor(cmd: string, args: string[]) {
    super();
    ValidatableMixin.call(this);
    this.command = cmd;
    this.arguments = args;
  }

  toApiPayload() {
    return JsonUtils.toSnakeCasedObject(this);
  }

  modelType() {
    return "Task";
  }
}
