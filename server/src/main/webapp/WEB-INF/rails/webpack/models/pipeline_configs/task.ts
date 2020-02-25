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
import {
  AntTaskAttributesJSON,
  ExecTaskAttributesJSON,
  NAntTaskAttributesJSON,
  RakeTaskAttributesJSON,
  TaskJSON
} from "models/admin_templates/templates";
import {ErrorsConsumer} from "models/mixins/errors_consumer";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

type ValidTypes = "exec" | "ant" | "nant" | "rake" | "fetchArtifact";
export type RunIfCondition = "passed" | "failed" | "any";

interface TaskOpts {
  onCancel: Task | undefined;
  runIf: RunIfCondition[];
}

interface ExecOpts extends TaskOpts {
  workingDirectory: string;
}

interface BuildOpts extends TaskOpts {
  buildFile: string;
  target: string;
  workingDirectory: string;
}

// tslint:disable-next-line:no-empty-interface
interface AntOpts extends BuildOpts {
}

interface NantOpts extends BuildOpts {
  nantPath: string;
}

// tslint:disable-next-line:no-empty-interface
interface RakeOpts extends BuildOpts {
}

type Partial<T extends TaskOpts> = { [P in keyof T]?: T[P] };

export interface Task extends ValidatableMixin {
  type: ValidTypes;
  attributes: Stream<TaskAttributes>;

  hasErrors(): boolean;
}

export interface TaskAttributes extends ValidatableMixin {
  runIf: Stream<RunIfCondition[]>;
  properties: () => Map<string, string>;
  toApiPayload: () => any;
  onCancel: Stream<Task>;
}

export abstract class AbstractTask extends ValidatableMixin implements Task {
  abstract type: ValidTypes;
  attributes: Stream<TaskAttributes> = Stream();

  constructor() {
    super();
    this.validateAssociated("attributes");
  }

  static fromJSONArray(json: TaskJSON[]) {
    return json.map(this.fromJSON);
  }

  //TODO: Implement me and remove ts ignore
  //@ts-ignore
  static fromJSON(json: TaskJSON): Task {
    switch (json.type) {
      case "pluggable_task":
        break;
      case "fetch":
        break;
      case "ant":
        return AntTask.from(json.attributes as AntTaskAttributesJSON);
      case "nant":
        return NantTask.from(json.attributes as NAntTaskAttributesJSON);
      case "exec":
        return ExecTask.from(json.attributes as ExecTaskAttributesJSON);
      case "rake":
        return RakeTask.from(json.attributes as ExecTaskAttributesJSON);
    }
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
  readonly runIf: Stream<RunIfCondition[]> = Stream([] as RunIfCondition[]);
  readonly onCancel: Stream<Task>          = Stream();

  constructor(opts: TaskOpts) {
    super();
    this.setRunIf(opts);
    this.setOnCancel(opts);
  }

  abstract properties(): Map<string, string>;

  abstract toApiPayload(): any;

  protected setRunIf(opts: TaskOpts) {
    if (opts.runIf) {
      this.runIf(opts.runIf);
    }

    if (opts.onCancel) {
      this.onCancel(opts.onCancel);
    }
  }

  protected setOnCancel(opts: TaskOpts) {
    if (opts.onCancel) {
      this.onCancel(opts.onCancel);
    }
  }
}

export abstract class BuildTaskAttributes extends AbstractTaskAttributes {
  buildFile: Stream<string>        = Stream();
  target: Stream<string>           = Stream();
  workingDirectory: Stream<string> = Stream();

  constructor(opts: Partial<AntOpts>) {
    super(opts as TaskOpts);

    this.buildFile = Stream(opts.buildFile || "");
    this.target    = Stream(opts.target || "");

    if (opts.workingDirectory) {
      this.workingDirectory(opts.workingDirectory);
    }
  }

  properties(): Map<string, string> {
    const map: Map<string, string> = new Map();
    map.set("Build File", this.buildFile());
    map.set("Target", this.target());
    map.set("Working Directory", this.workingDirectory());

    return map;
  }

  toApiPayload(): any {
    return JsonUtils.toSnakeCasedObject(this);
  }
}

export class AntTask extends AbstractTask {
  readonly type: ValidTypes = "ant";

  constructor(opts: Partial<AntOpts> = {}) {
    super();
    this.attributes(new AntTaskAttributes(opts));
  }

  static from(attributes: AntTaskAttributesJSON) {
    return new AntTask({
                         buildFile: attributes.build_file,
                         target: attributes.target,
                         onCancel: attributes.on_cancel ? AbstractTask.fromJSON(attributes.on_cancel) : undefined,
                         runIf: attributes.run_if,
                         workingDirectory: attributes.working_directory
                       });
  }
}

export class AntTaskAttributes extends BuildTaskAttributes {
}

export class NantTask extends AbstractTask {
  readonly type: ValidTypes = "nant";

  constructor(opts: Partial<NantOpts> = {}) {
    super();
    this.attributes(new NantTaskAttributes(opts));
  }

  static from(attributes: NAntTaskAttributesJSON) {
    return new NantTask({
                          nantPath: attributes.nant_path || "",
                          buildFile: attributes.build_file,
                          target: attributes.target,
                          onCancel: attributes.on_cancel ? AbstractTask.fromJSON(attributes.on_cancel) : undefined,
                          runIf: attributes.run_if,
                          workingDirectory: attributes.working_directory
                        });
  }
}

export class NantTaskAttributes extends BuildTaskAttributes {
  readonly nantPath: Stream<string> = Stream();

  constructor(opts: Partial<NantOpts>) {
    super(opts);
    this.nantPath = Stream(opts.nantPath || "");
  }

  properties(): Map<string, string> {
    const properties = super.properties();
    properties.set("Nant Path", this.nantPath());

    return properties;
  }

  toApiPayload(): any {
    const json: any   = super.toApiPayload();
    json.nant_path = this.nantPath();
    return json;
  }
}

export class RakeTask extends AbstractTask {
  readonly type: ValidTypes = "rake";

  constructor(opts: Partial<RakeOpts> = {}) {
    super();
    this.attributes(new RakeTaskAttributes(opts));
  }

  static from(attributes: RakeTaskAttributesJSON) {
    return new RakeTask({
                          buildFile: attributes.build_file,
                          target: attributes.target,
                          onCancel: attributes.on_cancel ? AbstractTask.fromJSON(attributes.on_cancel) : undefined,
                          runIf: attributes.run_if,
                          workingDirectory: attributes.working_directory
                        });
  }
}

export class RakeTaskAttributes extends BuildTaskAttributes {
}

export class ExecTask extends AbstractTask {
  readonly type: ValidTypes = "exec";

  constructor(cmd: string, args: string[], opts: Partial<ExecOpts> = {}) {
    super();
    this.attributes(new ExecTaskAttributes(cmd, args, opts));
  }

  static from(attributes: ExecTaskAttributesJSON) {
    return new ExecTask(attributes.command, attributes.arguments!, {
      onCancel: attributes.on_cancel ? AbstractTask.fromJSON(attributes.on_cancel) : undefined,
      runIf: attributes.run_if,
      workingDirectory: attributes.working_directory
    });
  }
}

export class ExecTaskAttributes extends AbstractTaskAttributes {
  // validators expect streams for attrs
  command: Stream<string>;
  arguments: Stream<string[]>;
  workingDirectory: Stream<string> = Stream();

  constructor(cmd: string, args: string[], opts: Partial<ExecOpts>) {
    super(opts as TaskOpts);
    this.command = Stream(cmd);
    this.validatePresenceOf("command");

    this.arguments = Stream(args || []);

    if (opts.workingDirectory) {
      this.workingDirectory(opts.workingDirectory);
    }
  }

  properties(): Map<string, string> {
    const map: Map<string, string> = new Map();
    map.set("Command", this.command());
    map.set("Arguments", this.arguments().join(" "));
    map.set("Working Directory", this.workingDirectory());

    return map;
  }

  toApiPayload(): any {
    return JsonUtils.toSnakeCasedObject(this);
  }
}
