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

import {PluginConfiguration} from "models/admin_templates/templates";
import {MaterialJSON} from "models/materials/serialization";
import {OriginType} from "models/origin";
import {JobJSON} from "models/pipeline_configs/job";
import {LockBehavior, PipelineConfigJSON} from "models/pipeline_configs/pipeline_config";
import {ApprovalType, StageJSON} from "models/pipeline_configs/stage";
import {AntTask, ExecTask, FetchArtifactTask, NantTask, PluggableTask, RakeTask} from "models/pipeline_configs/task";
import {Configurations, PropertyJSON} from "models/shared/configuration";

export class PipelineConfigTestData {
  static withTwoStages(): PipelineConfigJSON {
    return new Builder()
      .name("Test")
      .origin(OriginType.GoCD)
      .materials(this.git())
      .stageWithJobs("StageOne", "JobOne")
      .stageWithJobs("StageTwo", "JobOne", "JobTwo", "JobThree")
      .build();
  }

  static withGitMaterial() {
    return new Builder()
      .name("Test")
      .origin(OriginType.GoCD)
      .materials(this.git())
      .stageWithJobs("StageOne", "JobOne")
      .stageWithJobs("StageTwo", "JobOne", "JobTwo", "JobThree")
      .build();
  }

  static withTemplate() {
    return new Builder()
      .name("pipeline-from-template")
      .template("template")
      .origin(OriginType.GoCD)
      .materials(this.git())
      .build();
  }

  static stage(stageName: string, ...jobs: string[]) {
    return new Builder().stageWithJobs(stageName, ...jobs).build().stages[0];
  }

  private static git() {
    return {
      attributes: {
        name: "GM",
        shallow_clone: true,
        url: "test-repo",
        username: "abc",
        auto_update: true,
        branch: "master",
        destination: "gm",
        encrypted_password: "AES:EiGwOWJC9SLR70J/xv2Vzg==:EIJoCae8FB4vaInvh4WrCQ==",
        invert_filter: true,
        filter: {
          ignore: [
            "abc"
          ]
        }
      },
      type: "git"
    } as MaterialJSON;
  }
}

export class JobTestData {
  static with(name: string) {
    return new JobBuilder()
      .name(name)
      .build();
  }
}

export class TaskTestData {
  static exec(cmd: string = "ls", args: [] = []): ExecTask {
    return new ExecTask(cmd, args, undefined, "/tmp", ["passed"]);
  }

  static ant(buildFile: string = "ant-build-file"): AntTask {
    return new AntTask(buildFile,
                       "target",
                       "/tmp",
                       ["any"],
                       TaskTestData.exec());
  }

  static nant(buildFile: string = "nant-build-file", nantPath: string = "path-to-nant-exec"): NantTask {
    return new NantTask(buildFile, "target",
                        nantPath,
                        "/tmp",
                        ["any"],
                        TaskTestData.exec());

  }

  static rake(buildFile: string = "rake-build-file"): RakeTask {
    return new RakeTask(buildFile,
                        "target",
                        "/tmp",
                        ["any"],
                        TaskTestData.exec());
  }

  static fetchGoCDTask(pipelineName: string = "pipeline", stageName: string = "stage", jobName: string = "job") {
    return new FetchArtifactTask("gocd",
                                 pipelineName,
                                 stageName,
                                 jobName,
                                 true,
                                 "source-file",
                                 "destination-file",
                                 undefined,
                                 new Configurations([]),
                                 ["any"],
                                 TaskTestData.exec());
  }

  static fetchExternalTask(pipelineName: string = "pipeline", stageName: string = "stage", jobName: string = "job") {
    return new FetchArtifactTask("external",
                                 pipelineName,
                                 stageName,
                                 jobName,
                                 false,
                                 undefined,
                                 undefined,
                                 "artifact-id",
                                 new Configurations([]),
                                 ["any"],
                                 TaskTestData.exec());
  }

  static pluggableTask() {
    const pluginConfiguration: PluginConfiguration = {
      id: "script-executor",
      version: "1"
    };

    const property: PropertyJSON = {
      key: "username",
      value: "bob"
    };

    const encryptedProperty: PropertyJSON = {
      key: "password",
      encrypted_value: "AES:someencryptedvalue"
    };

    return new PluggableTask(pluginConfiguration,
                             Configurations.fromJSON([property, encryptedProperty]),
                             ["any"],
                             TaskTestData.exec());
  }
}

class Builder {
  readonly json = {} as PipelineConfigJSON;

  labelTemplate(labelTemplate: string) {
    this.json.label_template = labelTemplate;
    return this;
  }

  lockBehavior(lockBehavior: LockBehavior) {
    this.json.lock_behavior = lockBehavior;
    return this;
  }

  name(name: string) {
    this.json.name = name;
    return this;
  }

  template(name: string) {
    this.json.template = name;
    return this;
  }

  group(group: string) {
    this.json.group = group;
    return this;
  }

  origin(type: OriginType) {
    this.json.origin = {type};
    return this;
  }

  param(name: string, value: string) {
    if (!this.json.parameters) {
      this.json.parameters = [];
    }

    this.json.parameters.push({name, value});
    return this;
  }

  environmentVariable(name: string, value: string, secure: boolean = false) {
    if (!this.json.environment_variables) {
      this.json.environment_variables = [];
    }

    this.json.environment_variables.push({name, value, secure});
    return this;
  }

  trackingTool(urlPattern: string, regex: string) {
    this.json.tracking_tool = {
      type: "generic",
      attributes: {url_pattern: urlPattern, regex}
    };
    return this;
  }

  timer(spec: string, onlyOnChanges: boolean) {
    this.json.timer = {
      only_on_changes: onlyOnChanges,
      spec
    };
    return this;
  }

  materials(...materials: MaterialJSON[]) {
    this.json.materials = materials;
    return this;
  }

  stages(...stages: StageJSON[]) {
    this.json.stages = stages;
    return this;
  }

  stage() {
    return new StageBuilder(this);
  }

  stageWithJobs(name: string, ...jobs: string[]) {
    const stageBuilder = new StageBuilder(this).name(name)
                                               .approval("manual", false);
    jobs.forEach((job) => {
      stageBuilder.job().name(job).done();
    });
    return stageBuilder.done();
  }

  build(): PipelineConfigJSON {
    return this.json;
  }
}

class StageBuilder {
  readonly json = {} as StageJSON;
  private readonly _parent: Builder;

  constructor(parent: Builder) {
    this._parent = parent;
  }

  name(name: string) {
    this.json.name = name;
    return this;
  }

  fetchMaterials(fetchMaterials: boolean) {
    this.json.fetch_materials = fetchMaterials;
    return this;
  }

  cleanWorkingDirectory(cleanWorkingDirectory: boolean) {
    this.json.clean_working_directory = cleanWorkingDirectory;
    return this;
  }

  neverCleanupArtifacts(neverCleanupArtifacts: boolean) {
    this.json.never_cleanup_artifacts = neverCleanupArtifacts;
    return this;
  }

  approval(type: ApprovalType, allowOnlyOnSuccess: boolean, roles: string[] = [], users: string[] = []) {
    this.json.approval = {
      type, allow_only_on_success: allowOnlyOnSuccess, authorization: {roles, users}
    };
    return this;
  }

  environmentVariable(name: string, value: string, secure: boolean = false) {
    if (!this.json.environment_variables) {
      this.json.environment_variables = [];
    }

    this.json.environment_variables.push({name, value, secure});
    return this;
  }

  job() {
    return new JobBuilder(this);
  }

  done() {
    if (!this._parent.json.stages) {
      this._parent.json.stages = [];
    }
    this._parent.json.stages.push(this.json);
    return this._parent;
  }
}

class JobBuilder {
  readonly json = {} as JobJSON;
  private readonly _parent?: StageBuilder;

  constructor(parent?: StageBuilder) {
    this._parent = parent;
  }

  name(name: string) {
    this.json.name = name;
    return this;
  }

  environmentVariable(name: string, value: string, secure: boolean = false) {
    if (!this.json.environment_variables) {
      this.json.environment_variables = [];
    }

    this.json.environment_variables.push({name, value, secure});
    return this;
  }

  done() {
    if (!this._parent!.json.jobs) {
      this._parent!.json.jobs = [];
    }
    this._parent!.json.jobs.push(this.json);
    return this._parent;
  }

  build(): JobJSON {
    return this.json;
  }
}
