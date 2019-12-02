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

import _ from "lodash";
import Stream from "mithril/stream";
import {Origin, OriginJSON} from "models/origin";

interface Nameable {
  name: string;
}

export interface PipelineJSON extends Nameable {
  origin: OriginJSON;
  stages: StageJSON[];
  template_name?: string;
}

export interface PipelineGroupJSON extends Nameable {
  pipelines: PipelineJSON[];
}

export interface PipelineGroupsJSON {
  groups: PipelineGroupJSON[];
}

export interface PipelineStructureJSON extends PipelineGroupsJSON {
  groups: PipelineGroupJSON[];
  templates: TemplateJSON[];
}

export interface TemplateJSON extends Nameable {
  stages: StageJSON[];
  parameters: string[];
}

export interface StageJSON extends Nameable {
  jobs: JobJSON[];
}

export interface JobJSON extends Nameable {
  is_elastic: boolean;
}

export class Pipeline {
  name: Stream<string>;

  constructor(name: string) {
    this.name = Stream(name);
  }

  static fromJSON(data: PipelineJSON) {
    return new Pipeline(data.name);
  }
}

export class PipelineWithOrigin extends Pipeline {
  readonly origin: Stream<Origin>;
  readonly templateName: Stream<string | undefined>;
  readonly stages: Stream<Stages>;

  constructor(name: string, templateName: string | undefined, origin: Origin, stages: Stages) {
    super(name);
    this.origin       = Stream(origin);
    this.templateName = Stream(templateName);
    this.stages       = Stream(stages);
  }

  static fromJSON(data: PipelineJSON) {
    return new PipelineWithOrigin(data.name,
                                  data.template_name,
                                  Origin.fromJSON(data.origin),
                                  Stages.fromJSON(data.stages));
  }

  usesTemplate() {
    return !_.isEmpty(this.templateName());
  }

  clone() {
    return new PipelineWithOrigin(this.name(), this.templateName(), this.origin(), this.stages().map((s) => s.clone()));
  }

}

export class Pipelines extends Array<PipelineWithOrigin> {
  constructor(...items: PipelineWithOrigin[]) {
    super(...items);
    Object.setPrototypeOf(this, Object.create(Pipelines.prototype));
  }

  static fromJSON(pipelines: PipelineJSON[] = []) {
    return new Pipelines(...pipelines.map(PipelineWithOrigin.fromJSON));
  }

  containsPipeline(name: string): boolean {
    return this.map((p) => p.name()).indexOf(name) !== -1;
  }

  clone() {
    return new Pipelines(...this.map((p) => p.clone()));
  }
}

export class PipelineGroup {
  readonly name: Stream<string>;
  readonly pipelines: Stream<Pipelines>;

  constructor(name: string, pipelines: Pipelines) {
    this.name      = Stream(name);
    this.pipelines = Stream(pipelines);
  }

  static fromJSON(data: PipelineGroupJSON) {
    return new PipelineGroup(data.name, Pipelines.fromJSON(data.pipelines));
  }

  isEmpty() {
    return _.isEmpty(this.pipelines());
  }

  hasPipelines() {
    return !this.isEmpty();
  }
}

export class PipelineGroups extends Array<PipelineGroup> {
  constructor(...pipelines: PipelineGroup[]) {
    super(...pipelines);
    Object.setPrototypeOf(this, Object.create(PipelineGroups.prototype));
  }

  static fromJSON(data: PipelineGroupJSON[] = []) {
    return new PipelineGroups(...data.map(PipelineGroup.fromJSON));
  }
}

export class Job {
  readonly name: Stream<string>;
  readonly isElastic: Stream<boolean>;

  constructor(name: string, isElastic: boolean) {
    this.name      = Stream(name);
    this.isElastic = Stream(isElastic);

  }

  static fromJSON(data: JobJSON) {
    return new Job(data.name, data.is_elastic);
  }

  clone() {
    return new Job(this.name(), this.isElastic());
  }
}

export class Jobs extends Array<Job> {
  constructor(...items: Job[]) {
    super(...items);
    Object.setPrototypeOf(this, Object.create(Jobs.prototype));
  }

  static fromJSON(jobs: JobJSON[] = []) {
    return new Jobs(...jobs.map(Job.fromJSON));
  }
}

export class Stage {
  readonly name: Stream<string>;
  readonly jobs: Stream<Jobs>;

  constructor(name: string, jobs: Jobs) {
    this.name = Stream(name);
    this.jobs = Stream(jobs);
  }

  static fromJSON(data: StageJSON) {
    return new Stage(data.name, Jobs.fromJSON(data.jobs));
  }

  clone() {
    return new Stage(this.name(), new Jobs(...this.jobs().map((j) => j.clone())));
  }
}

export class Stages extends Array<Stage> {
  constructor(...items: Stage[]) {
    super(...items);
    Object.setPrototypeOf(this, Object.create(Stages.prototype));
  }

  static fromJSON(stages: StageJSON[] = []) {
    return new Stages(...stages.map(Stage.fromJSON));
  }
}

export class Template {
  readonly name: Stream<string>;
  readonly stages: Stream<Stages>;
  readonly parameters: Stream<string[]>;

  constructor(name: string, stages: Stages, parameters: string[]) {
    this.name       = Stream(name);
    this.stages     = Stream(stages);
    this.parameters = Stream(parameters || []);
  }

  static fromJSON(data: TemplateJSON) {
    return new Template(data.name, Stages.fromJSON(data.stages), data.parameters || []);
  }
}

export class Templates extends Array<Template> {
  constructor(...items: Template[]) {
    super(...items);
    Object.setPrototypeOf(this, Object.create(Templates.prototype));
  }

  static fromJSON(templates: TemplateJSON[] = []) {
    return new Templates(...templates.map(Template.fromJSON));
  }
}

export class PipelineStructure {
  readonly groups: Stream<PipelineGroups>;
  readonly templates: Stream<Templates>;

  constructor(groups: PipelineGroups, templates: Templates) {
    this.groups    = Stream(groups);
    this.templates = Stream(templates);
  }

  static fromJSON(data: PipelineStructureJSON) {
    return new PipelineStructure(PipelineGroups.fromJSON(data.groups), Templates.fromJSON(data.templates));
  }

  /**
   * Returns `undefined` if the pipeline is not present, typically when you don't have appropriate access to it.
   */
  findPipeline(name: string) {
    for (const eachGroup of this.groups()) {
      for (const eachPipeline of eachGroup.pipelines()) {
        if (eachPipeline.name().toLowerCase() === name.toLowerCase()) {
          return eachPipeline;
        }
      }
    }
  }

  getAllConfigPipelinesNotUsingTemplates() {
    const result: string[] = [];
    this.groups().forEach((eachGroup) => {
      eachGroup.pipelines().forEach((eachPipeline) => {
        if (_.isEmpty(eachPipeline.templateName()) && !eachPipeline.origin().isDefinedInConfigRepo()) {
          result.push(eachPipeline.name());
        }
      });
    });
    return result;
  }
}
