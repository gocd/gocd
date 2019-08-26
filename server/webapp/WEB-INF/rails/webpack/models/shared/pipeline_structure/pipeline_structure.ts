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
import m from "mithril";
import Stream from "mithril/stream";
import {Origin, OriginType} from "models/shared/origin";
import {PipelineStructureJSON} from "models/shared/pipeline_structure/serialization";
import {TriStateCheckbox, TristateState} from "models/tri_state_checkbox";

function setAllChildStates(thing: Clickable) {
  const currentCheckboxState = thing.checkboxState()();
  currentCheckboxState.click();
  if (currentCheckboxState.isChecked()) {
    thing.setCheckboxState(TristateState.on);
  } else if (currentCheckboxState.isIndeterminate()) {
    thing.setCheckboxState(TristateState.indeterminate);
  } else {
    thing.setCheckboxState(TristateState.off);
  }
}

function recomputeParent(parent: HasChildren<PipelineStructureJSON.Nameable & Clickable>) {
  if (isEveryChildChecked(parent)) {
    return TristateState.on;
  } else if (isAnyChildSelected(parent)) {
    return TristateState.indeterminate;
  } else if (isAnyChildIndeterminate(parent)) {
    return TristateState.indeterminate;
  } else {
    return TristateState.off;
  }
}

function isAnyChildIndeterminate(node: HasChildren<PipelineStructureJSON.Nameable & Clickable>) {
  return _.some(node.children, (child) => child.checkboxState()().isIndeterminate());
}

function isAnyChildSelected(node: HasChildren<PipelineStructureJSON.Nameable & Clickable>) {
  return _.some(node.children, (child) => child.checkboxState()().isChecked());
}

function isEveryChildChecked(node: HasChildren<PipelineStructureJSON.Nameable & Clickable>) {
  return _.every(node.children, (child) => child.checkboxState()().isChecked());
}

export interface Clickable {
  checkboxState(): Stream<TriStateCheckbox>;

  wasClicked(): void;

  setCheckboxState(newState: TristateState): void;

  readonly(): boolean;

  readonlyReason(): m.Children;
}

export interface HasChildren<T extends PipelineStructureJSON.Nameable> {
  children: T[];
}

export class Job implements Clickable, PipelineStructureJSON.Nameable {

  // @ts-ignore
  parent: Stage;
  readonly name: string;
  readonly isElastic: boolean;
  private readonly __checkboxState = Stream(new TriStateCheckbox(TristateState.off));

  constructor(name: string, isElastic = false) {
    this.name      = name;
    this.isElastic = isElastic;
  }

  static fromJSON(job: PipelineStructureJSON.Job) {
    return new Job(job.name, job.is_elastic);
  }

  setCheckboxState(newState: TristateState) {
    this.__checkboxState(new TriStateCheckbox(newState));
  }

  wasClicked() {
    // ignore this, no special behavior other than what a checkbox does
  }

  checkboxState(): Stream<TriStateCheckbox> {
    return this.__checkboxState;
  }

  readonly() {
    return this.parent.readonly();
  }

  readonlyReason() {
    return this.parent.readonlyReason();
  }
}

export class Stage implements PipelineStructureJSON.Nameable, Clickable, HasChildren<Job> {
  // @ts-ignore
  parent: Pipeline | Template;
  readonly name: string;
  readonly children: Job[];

  constructor(name: string, jobs: Job[]) {
    this.name     = name;
    this.children = jobs;
    this.children.forEach((job) => job.parent = this);
  }

  static fromJSON(stage: PipelineStructureJSON.Stage) {
    return new Stage(stage.name, stage.jobs.map((job) => Job.fromJSON(job)));
  }

  setCheckboxState(newState: TristateState) {
    this.children.forEach((child) => child.setCheckboxState(newState));
  }

  wasClicked() {
    setAllChildStates(this);
  }

  // we return a new Stream on every invocation, so in theory, updating the value of this stream has no effect!
  // you'd probably want to look at `wasClicked`
  checkboxState(): Stream<TriStateCheckbox> {
    return Stream(new TriStateCheckbox(recomputeParent(this)));
  }

  readonly() {
    return this.parent.readonly();
  }

  readonlyReason() {
    return this.parent.readonlyReason();
  }
}

export class Pipeline implements PipelineStructureJSON.Nameable, Clickable, HasChildren<Stage> {
  // @ts-ignore
  parent: PipelineGroup;
  readonly name: string;
  readonly templateName: string | undefined;
  readonly children: Stage[];
  readonly origin: Origin;

  constructor(name: string, templateName: string | undefined, origin: Origin, stages: Stage[]) {
    this.name         = name;
    this.templateName = templateName;
    this.children     = stages;
    this.origin       = origin;
    this.children.forEach((stage) => stage.parent = this);
  }

  static fromJSON(pipeline: PipelineStructureJSON.Pipeline) {
    return new Pipeline(pipeline.name,
                        pipeline.template_name,
                        pipeline.origin,
                        pipeline.stages.map((stage) => Stage.fromJSON(stage)));
  }

  readonly() {
    return this.hasConfigRepoOrigin() || this.basedOnTemplate();
  }

  basedOnTemplate() {
    return !_.isEmpty(this.templateName);
  }

  setCheckboxState(newState: TristateState) {
    this.children.forEach((child) => child.setCheckboxState(newState));
  }

  hasConfigRepoOrigin(): boolean {
    return this.origin.type === OriginType.CONFIG_REPO;
  }

  wasClicked() {
    setAllChildStates(this);
  }

  // we return a new Stream on every invocation, so in theory, updating the value of this stream has no effect!
  // you'd probably want to look at `wasClicked`
  checkboxState(): Stream<TriStateCheckbox> {
    return Stream(new TriStateCheckbox(recomputeParent(this)));
  }

  readonlyReason() {
    if (this.hasConfigRepoOrigin()) {
      return `You can't associate agent to pipeline - ${this.name} as specified in config repo`;
    }
    if (this.basedOnTemplate()) {
      return `You can't associate agent to pipeline - ${this.name} as specified in template`;
    }
  }
}

export class PipelineGroup implements PipelineStructureJSON.Nameable, Clickable, HasChildren<Pipeline> {
  readonly name: string;
  readonly children: Pipeline[];

  constructor(name: string, pipelines: Pipeline[]) {
    this.name     = name;
    this.children = pipelines;
    this.children.forEach((pipeline) => pipeline.parent = this);
  }

  static fromJSON(data: PipelineStructureJSON.PipelineGroup) {
    return new PipelineGroup(data.name,
                             data.pipelines.map((value) => Pipeline.fromJSON(value))
    );
  }

  wasClicked() {
    setAllChildStates(this);
  }

  setCheckboxState(newState: TristateState) {
    this.children.forEach((child) => child.setCheckboxState(newState));
  }

  // we return a new Stream on every invocation, so in theory, updating the value of this stream has no effect!
  // you'd probably want to look at `wasClicked`
  checkboxState(): Stream<TriStateCheckbox> {
    return Stream(new TriStateCheckbox(recomputeParent(this)));
  }

  readonly() {
    return _.every(this.children, (pipeline) => {
      return pipeline.readonly();
    });
  }

  readonlyReason() {
    if (this.areAllConfigRepoPipelines()) {
      return `You can't associate agent to pipeline group - ${this.name} as specified in config repo`;
    }
    if (this.areAllPipelinesDefinedUsingTemplate()) {
      return `You can't associate agent to pipeline group - ${this.name} as specified in template`;
    }
  }

  private areAllPipelinesDefinedUsingTemplate() {
    return _.every(this.children, (pipeline) => {
      return pipeline.basedOnTemplate();
    });
  }

  private areAllConfigRepoPipelines() {
    return _.every(this.children, (pipeline) => {
      return pipeline.hasConfigRepoOrigin();
    });
  }
}

export class Template implements PipelineStructureJSON.Nameable, Clickable, HasChildren<Stage> {
  readonly name: string;
  readonly children: Stage[];

  constructor(name: string, children: Stage[]) {
    this.name     = name;
    this.children = children;
    this.children.forEach((stage) => stage.parent = this);
  }

  static fromJSON(value: PipelineStructureJSON.Template) {
    return new Template(value.name, value.stages.map((eachStage) => Stage.fromJSON(eachStage)));
  }

  // we return a new Stream on every invocation, so in theory, updating the value of this stream has no effect!
  // you'd probably want to look at `wasClicked`
  checkboxState(): Stream<TriStateCheckbox> {
    return Stream(new TriStateCheckbox(recomputeParent(this)));
  }

  setCheckboxState(newState: TristateState) {
    this.children.forEach((child) => child.setCheckboxState(newState));
  }

  wasClicked() {
    setAllChildStates(this);
  }

  readonly() {
    return false;
  }

  readonlyReason() {
    return undefined;
  }
}

export class PipelineStructure {
  readonly groups: PipelineGroup[];
  readonly templates: Template[];

  constructor(groups: PipelineGroup[], templates: Template[]) {
    this.groups    = groups;
    this.templates = templates;
  }

  static fromJSON(data: PipelineStructureJSON.PipelineStructure) {
    // return data.map((value) => PipelineGroup.fromJSON(value));
    const pipelineGroups = data.groups.map(((value) => PipelineGroup.fromJSON(value)));
    const templates      = data.templates.map(((value) => Template.fromJSON(value)));
    return new PipelineStructure(pipelineGroups, templates);
  }
}
