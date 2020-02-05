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

import {MaterialJSON} from "models/materials/serialization";
import {OriginType} from "models/origin";
import {JobJSON} from "models/pipeline_configs/job";
import {LockBehavior, PipelineConfigJSON} from "models/pipeline_configs/pipeline_config";
import {ApprovalType, StageJSON} from "models/pipeline_configs/stage";

export class PipelineConfigTestData {
  static withTwoStages(): PipelineConfigJSON {
    return new Builder()
      .name("Test")
      .origin(OriginType.GoCD)
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
  private readonly _parent: StageBuilder;

  constructor(parent: StageBuilder) {
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
    if (!this._parent.json.jobs) {
      this._parent.json.jobs = [];
    }
    this._parent.json.jobs.push(this.json);
    return this._parent;
  }
}
