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

import {JsonUtils} from "helpers/json_utils";
import {Stream} from "mithril/stream";
import stream from "mithril/stream";
import {ErrorsConsumer} from "models/mixins/errors_consumer";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

type ValidTypes = "exec" | "fetchArtifact";
type RunIfCondition = "passed" | "failed" | "any";

export interface Task extends ValidatableMixin {
  type: ValidTypes;
  attributes: Stream<TaskAttributes>;
}

export interface TaskAttributes extends ValidatableMixin {
  runIf: Stream<RunIfCondition[]>;
  toApiPayload: () => any;
  onCancel: Stream<Task>;
}

abstract class AbstractTask extends ValidatableMixin implements Task {
  abstract type: ValidTypes;
  attributes: Stream<TaskAttributes> = stream();

  constructor() {
    super();
    this.validateAssociated("attributes");
  }

  errorContainerFor(subkey: string): ErrorsConsumer {
    return "type" === subkey ? this : this.attributes();
  }

  toJSON(): any {
    return {
      type: this.type,
      attributes: this.attributes().toApiPayload()
    };
  }
}

abstract class AbstractTaskAttributes extends ValidatableMixin implements TaskAttributes {
  runIf: Stream<RunIfCondition[]> = stream([] as RunIfCondition[]);
  onCancel: Stream<Task> = stream();

  constructor() {
    super();
  }

  abstract toApiPayload(): any;
}

export class ExecTask extends AbstractTask {
  readonly type: ValidTypes = "exec";
  workingDirectory: Stream<string> = stream();

  constructor(cmd: string, args: string[]) {
    super();
    this.attributes(new ExecTaskAttributes(cmd, args));
  }
}

export class ExecTaskAttributes extends AbstractTaskAttributes {
  // validators expect streams for attrs
  command: Stream<string>;
  arguments: Stream<string[]>;

  constructor(cmd: string, args: string[]) {
    super();
    this.command = stream(cmd);
    this.validatePresenceOf("command");

    this.arguments = stream(args || []);
  }

  toApiPayload(): any {
    return JsonUtils.toSnakeCasedObject(this);
  }
}
