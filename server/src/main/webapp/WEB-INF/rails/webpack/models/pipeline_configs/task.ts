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
import Stream from "mithril/stream";
import {ErrorsConsumer} from "models/mixins/errors_consumer";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

type ValidTypes = "exec" | "fetchArtifact";
type RunIfCondition = "passed" | "failed" | "any";

interface TaskOpts {
  runIf: RunIfCondition[];
}

interface ExecOpts extends TaskOpts {
  workingDirectory: string;
}

type Partial<T extends TaskOpts> = { [P in keyof T]?: T[P] };

export interface Task extends ValidatableMixin {
  type: ValidTypes;
  attributes: Stream<TaskAttributes>;
  hasErrors(): boolean;
}

export interface TaskAttributes extends ValidatableMixin {
  runIf: Stream<RunIfCondition[]>;
  toApiPayload: () => any;
  onCancel: Stream<Task>;
}

abstract class AbstractTask extends ValidatableMixin implements Task {
  abstract type: ValidTypes;
  attributes: Stream<TaskAttributes> = Stream();

  constructor() {
    super();
    this.validateAssociated("attributes");
  }

  hasErrors() {
    return this.errors().hasErrors() || this.attributes().errors().hasErrors();
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
  runIf: Stream<RunIfCondition[]> = Stream([] as RunIfCondition[]);
  onCancel: Stream<Task> = Stream();

  constructor() {
    super();
  }

  abstract toApiPayload(): any;
}

export class ExecTask extends AbstractTask {
  readonly type: ValidTypes = "exec";

  constructor(cmd: string, args: string[], opts: Partial<ExecOpts> = {}) {
    super();
    this.attributes(new ExecTaskAttributes(cmd, args, opts));
  }
}

export class ExecTaskAttributes extends AbstractTaskAttributes {
  // validators expect streams for attrs
  command: Stream<string>;
  arguments: Stream<string[]>;
  workingDirectory: Stream<string> = Stream();

  constructor(cmd: string, args: string[], opts: Partial<ExecOpts>) {
    super();
    this.command = Stream(cmd);
    this.validatePresenceOf("command");

    this.arguments = Stream(args || []);

    if (opts.workingDirectory) {
      this.workingDirectory(opts.workingDirectory);
    }

    if (opts.runIf) {
      this.runIf(opts.runIf);
    }
  }

  toApiPayload(): any {
    return JsonUtils.toSnakeCasedObject(this);
  }
}
