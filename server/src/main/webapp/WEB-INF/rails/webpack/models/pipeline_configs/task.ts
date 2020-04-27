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
  ArtifactOrigin,
  ExecTaskAttributesJSON,
  FetchTaskAttributesJSON,
  NAntTaskAttributesJSON,
  PluginConfiguration,
  PluginTaskAttributesJSON,
  RakeTaskAttributesJSON,
  TaskJSON,
  TaskType
} from "models/admin_templates/templates";
import {ErrorsConsumer} from "models/mixins/errors_consumer";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Configurations} from "models/shared/configuration";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";

export type RunIfCondition = "passed" | "failed" | "any";

export interface Task extends ValidatableMixin {
  type: TaskType;
  attributes: Stream<TaskAttributes>;
  description(pluginInfos: PluginInfos): string;
  hasErrors(): boolean;
  toJSON(): any;
}

export interface TaskAttributes extends ValidatableMixin {
  runIf: Stream<RunIfCondition[]>;
  properties: () => Map<string, string>;
  toApiPayload: () => any;
  onCancel: Stream<Task | undefined>;
}

export abstract class AbstractTask extends ValidatableMixin implements Task {
  abstract type: TaskType;
  attributes: Stream<TaskAttributes> = Stream();

  constructor() {
    super();
    this.validateAssociated("attributes");
  }

  static fromJSONArray(json: TaskJSON[]) {
    return json.map(this.fromJSON);
  }

  static fromJSON(json: TaskJSON): Task {
    switch (json.type) {
      case "pluggable_task":
        return PluggableTask.from(json.attributes as PluginTaskAttributesJSON);
      case "fetch":
        return FetchArtifactTask.from(json.attributes as FetchTaskAttributesJSON);
      case "ant":
        return AntTask.from(json.attributes as AntTaskAttributesJSON);
      case "nant":
        return NantTask.from(json.attributes as NAntTaskAttributesJSON);
      case "exec":
        return ExecTask.from(json.attributes as ExecTaskAttributesJSON);
      case "rake":
        return RakeTask.from(json.attributes as RakeTaskAttributesJSON);
      default:
        throw new Error(`Invalid task type ${json.type}.`);
    }
  }

  abstract description(pluginInfos: PluginInfos): string;

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
  readonly runIf: Stream<RunIfCondition[]>    = Stream([] as RunIfCondition[]);
  readonly onCancel: Stream<Task | undefined> = Stream();

  constructor(runIf?: RunIfCondition[], onCancel?: Task) {
    super();
    this.runIf(runIf || []);
    this.onCancel(onCancel);
  }

  abstract properties(): Map<string, string>;

  abstract toApiPayload(): any;
}

export abstract class BuildTaskAttributes extends AbstractTaskAttributes {
  buildFile: Stream<string | undefined>        = Stream();
  target: Stream<string | undefined>           = Stream();
  workingDirectory: Stream<string | undefined> = Stream();

  constructor(buildFile: string | undefined,
              target: string | undefined,
              workingDir: string | undefined,
              runIf: RunIfCondition[],
              onCancel?: Task) {

    super(runIf, onCancel);

    this.buildFile(buildFile);
    this.target(target);
    this.workingDirectory(workingDir);
  }

  properties(): Map<string, any> {
    const map: Map<string, string | undefined> = new Map();
    map.set("Build File", this.buildFile());
    map.set("Target", this.target());
    map.set("Working Directory", this.workingDirectory());

    return map;
  }

  toApiPayload(): any {
    return JsonUtils.toSnakeCasedObject(this);
  }
}

export class AntTaskAttributes extends BuildTaskAttributes {
}

export class AntTask extends AbstractTask {
  readonly type: TaskType = "ant";

  constructor(buildFile: string | undefined,
              target: string | undefined,
              workingDir: string | undefined,
              runIf: RunIfCondition[],
              onCancel?: Task) {
    super();
    this.attributes(new AntTaskAttributes(buildFile, target, workingDir, runIf, onCancel));
  }

  static from(attributes: AntTaskAttributesJSON) {
    return new AntTask(attributes.build_file,
                       attributes.target,
                       attributes.working_directory,
                       attributes.run_if,
                       attributes.on_cancel ? AbstractTask.fromJSON(attributes.on_cancel) : undefined);
  }

  description(pluginInfos: PluginInfos): string {
    return "Ant";
  }
}

export class NantTaskAttributes extends BuildTaskAttributes {
  readonly nantPath: Stream<string | undefined> = Stream();

  constructor(buildFile: string | undefined,
              target: string | undefined,
              nantPath: string | undefined,
              workingDir: string | undefined,
              runIf: RunIfCondition[],
              onCancel?: Task) {
    super(buildFile, target, workingDir, runIf, onCancel);
    this.nantPath(nantPath);
  }

  properties(): Map<string, string> {
    const properties = super.properties();
    properties.set("Nant Path", this.nantPath());

    return properties;
  }

  toApiPayload(): any {
    const json: any = super.toApiPayload();
    json.nant_path  = this.nantPath();
    return json;
  }
}

export class RakeTaskAttributes extends BuildTaskAttributes {
}

export class RakeTask extends AbstractTask {
  readonly type: TaskType = "rake";

  constructor(buildFile: string | undefined,
              target: string | undefined,
              workingDir: string | undefined,
              runIf: RunIfCondition[],
              onCancel?: Task) {
    super();
    this.attributes(new RakeTaskAttributes(buildFile, target, workingDir, runIf, onCancel));
  }

  static from(attributes: RakeTaskAttributesJSON) {
    return new RakeTask(attributes.build_file,
                        attributes.target,
                        attributes.working_directory,
                        attributes.run_if,
                        attributes.on_cancel ? AbstractTask.fromJSON(attributes.on_cancel) : undefined);
  }

  description(pluginInfos: PluginInfos): string {
    return "Rake";
  }
}

export class NantTask extends AbstractTask {
  readonly type: TaskType = "nant";

  constructor(buildFile: string | undefined,
              target: string | undefined,
              nantPath: string | undefined,
              workingDir: string | undefined,
              runIf: RunIfCondition[],
              onCancel?: Task) {
    super();
    this.attributes(new NantTaskAttributes(buildFile, target, nantPath, workingDir, runIf, onCancel));
  }

  static from(attributes: NAntTaskAttributesJSON) {
    return new NantTask(attributes.build_file,
                        attributes.target,
                        attributes.nant_path,
                        attributes.working_directory,
                        attributes.run_if,
                        attributes.on_cancel ? AbstractTask.fromJSON(attributes.on_cancel) : undefined);
  }

  description(pluginInfos: PluginInfos): string {
    return "NAnt";
  }
}

export class ExecTask extends AbstractTask {
  readonly type: TaskType = "exec";

  constructor(cmd: string, args: string[], workingDir?: string, runIf?: RunIfCondition[], onCancel?: Task) {
    super();
    this.attributes(new ExecTaskAttributes(cmd, args, workingDir, runIf, onCancel));
  }

  static from(attributes: ExecTaskAttributesJSON) {
    return new ExecTask(attributes.command,
                        attributes.arguments || [],
                        attributes.working_directory,
                        attributes.run_if,
                        attributes.on_cancel ? AbstractTask.fromJSON(attributes.on_cancel) : undefined);
  }

  description(pluginInfos: PluginInfos): string {
    return "Custom Command";
  }
}

export class ExecTaskAttributes extends AbstractTaskAttributes {
  command: Stream<string>                      = Stream();
  arguments: Stream<string[]>                  = Stream();
  workingDirectory: Stream<string | undefined> = Stream();

  constructor(cmd: string,
              args: string[],
              workingDir?: string | undefined,
              runIf?: RunIfCondition[],
              onCancel?: Task) {
    super(runIf, onCancel);

    this.command(cmd);
    this.validatePresenceOf("command");

    this.arguments(args);

    this.workingDirectory(workingDir);
  }

  properties(): Map<string, any> {
    const map: Map<string, any> = new Map();
    map.set("Command", this.command());
    map.set("Arguments", this.arguments().join(" "));
    map.set("Working Directory", this.workingDirectory());

    return map;
  }

  toApiPayload(): any {
    return JsonUtils.toSnakeCasedObject(this);
  }
}

export class FetchArtifactTask extends AbstractTask {
  type: TaskType = "fetch";

  constructor(artifactOrigin: ArtifactOrigin,
              pipelineName: string | undefined,
              stageName: string,
              jobName: string,
              isSourceAFile: boolean,
              source: string | undefined,
              destination: string | undefined,
              artifactId: string | undefined,
              configuration: Configurations,
              runIf: RunIfCondition[],
              onCancel?: Task) {
    super();
    this.attributes(new FetchTaskAttributes(artifactOrigin,
                                            pipelineName,
                                            stageName,
                                            jobName,
                                            isSourceAFile,
                                            source,
                                            destination,
                                            artifactId,
                                            configuration,
                                            runIf,
                                            onCancel));
  }

  static from(json: FetchTaskAttributesJSON): FetchArtifactTask {
    return new FetchArtifactTask(json.artifact_origin,
                                 json.pipeline,
                                 json.stage,
                                 json.job,
                                 json.is_source_a_file || false,
                                 json.source,
                                 json.destination,
                                 json.artifact_id,
                                 Configurations.fromJSON(json.configuration || []),
                                 json.run_if,
                                 json.on_cancel ? AbstractTask.fromJSON(json.on_cancel) : undefined);
  }

  description(pluginInfos: PluginInfos): string {
    return "Fetch Artifact";
  }
}

export class FetchTaskAttributes extends AbstractTaskAttributes {
  readonly artifactOrigin: Stream<ArtifactOrigin> = Stream();
  readonly pipeline: Stream<string | undefined>   = Stream();
  readonly stage: Stream<string>                  = Stream();
  readonly job: Stream<string>                    = Stream();

  readonly isSourceAFile: Stream<boolean>          = Stream();
  readonly source: Stream<string | undefined>      = Stream();
  readonly destination: Stream<string | undefined> = Stream();

  readonly artifactId: Stream<string | undefined> = Stream();
  readonly configuration: Stream<Configurations>  = Stream();

  constructor(artifactOrigin: ArtifactOrigin,
              pipelineName: string | undefined,
              stageName: string,
              jobName: string,
              isSourceAFile: boolean,
              source: string | undefined,
              destination: string | undefined,
              artifactId: string | undefined,
              configuration: Configurations,
              runIf: RunIfCondition[],
              onCancel: Task | undefined) {
    super(runIf, onCancel);

    this.artifactOrigin(artifactOrigin);
    this.pipeline(pipelineName);
    this.stage(stageName);
    this.job(jobName);

    this.isSourceAFile(isSourceAFile);
    this.source(source);
    this.destination(destination);

    this.artifactId(artifactId);
    this.configuration(configuration);
  }

  isBuiltInArtifact(): boolean {
    return this.artifactOrigin() === "gocd";
  }

  properties(): Map<string, any> {
    const map: Map<string, any> = new Map();

    map.set("Pipeline Name", this.pipeline());
    map.set("Stage Name", this.stage());
    map.set("Job Name", this.job());

    if (this.isBuiltInArtifact()) {
      map.set("Source", this.source());
      map.set("Destination", this.destination());
    }

    return map;
  }

  toApiPayload(): any {
    return JsonUtils.toSnakeCasedObject(this);
  }
}

export class PluggableTask extends AbstractTask {
  readonly type: TaskType = "pluggable_task";

  constructor(pluginConfiguration: PluginConfiguration,
              configuration: Configurations,
              runIf: RunIfCondition[],
              onCancel?: Task) {
    super();
    this.attributes(new PluggableTaskAttributes(pluginConfiguration, configuration, runIf, onCancel));
  }

  static from(json: PluginTaskAttributesJSON) {
    return new PluggableTask(json.plugin_configuration,
                             Configurations.fromJSON(json.configuration),
                             json.run_if,
                             json.on_cancel ? AbstractTask.fromJSON(json.on_cancel) : undefined);
  }

  description(pluginInfos: PluginInfos): string {
    const pluginId = (this.attributes() as PluggableTaskAttributes).pluginConfiguration().id;
    return pluginInfos.findByPluginId(pluginId)!.about.name;
  }
}

export class PluggableTaskAttributes extends AbstractTaskAttributes {
  readonly pluginConfiguration: Stream<PluginConfiguration> = Stream();
  readonly configuration: Stream<Configurations>            = Stream();

  constructor(pluginConfiguration: PluginConfiguration,
              configuration: Configurations,
              runIf: RunIfCondition[],
              onCancel?: Task) {
    super(runIf, onCancel);

    this.pluginConfiguration(pluginConfiguration);
    this.configuration(configuration);
  }

  properties(): Map<string, any> {
    return this.configuration().asMap();
  }

  toApiPayload(): any {
    return JsonUtils.toSnakeCasedObject(this);
  }

}
