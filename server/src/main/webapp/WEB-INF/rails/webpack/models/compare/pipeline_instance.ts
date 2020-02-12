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

import {timeFormatter} from "helpers/time_formatter";
import _ from "lodash";
import Stream from "mithril/stream";
import {BuildCauseJSON, dateOrUndefined, JobJSON, MaterialJSON, MaterialRevisionJSON, ModificationJSON, PipelineHistoryJSON, PipelineInstanceJSON, StageJSON, stringOrNull, stringOrUndefined} from "./pipeline_instance_json";

export class PipelineHistory {
  pipelineName: string;
  nextLink: stringOrUndefined;
  previousLink: stringOrUndefined;
  pipelineInstances: PipelineInstances;

  constructor(pipelineName: string, pipelineInstances: PipelineInstances) {
    this.pipelineName      = pipelineName;
    this.pipelineInstances = pipelineInstances;
  }

  static fromJSON(pipelineName: string, data: PipelineHistoryJSON): PipelineHistory {
    const pipelineHistory = new PipelineHistory(pipelineName, PipelineInstances.fromJSON(data.pipelines));
    if (data._links) {
      pipelineHistory.nextLink     = data._links.next === undefined ? undefined : data._links.next.href;
      pipelineHistory.previousLink = data._links.previous === undefined ? undefined : data._links.previous.href;
    }
    return pipelineHistory;
  }
}

export class PipelineInstances extends Array<PipelineInstance> {
  constructor(...pipelineInstances: PipelineInstance[]) {
    super(...pipelineInstances);
    Object.setPrototypeOf(this, Object.create(PipelineInstances.prototype));
  }

  static fromJSON(data: PipelineInstanceJSON[]): PipelineInstances {
    return new PipelineInstances(...data.map((instance) => PipelineInstance.fromJSON(instance)));
  }
}

export class PipelineInstance {
  id: Stream<number>;
  name: Stream<string>;
  counter: Stream<number>;
  label: Stream<string>;
  naturalOrder: Stream<number>;
  canRun: Stream<boolean>;
  preparingToSchedule: Stream<boolean>;
  comment: Stream<stringOrNull>;
  scheduledDate: Stream<dateOrUndefined>;
  buildCause: Stream<BuildCause>;
  stages: Stream<Stages>;

  constructor(id: number, name: string, counter: number, label: string, naturalOrder: number, canRun: boolean, preparingToSchedule: boolean, comment: stringOrNull, scheduledDate: number, buildCause: BuildCause, stages: Stages) {
    this.id                  = Stream(id);
    this.name                = Stream(name);
    this.counter             = Stream(counter);
    this.label               = Stream(label);
    this.naturalOrder        = Stream(naturalOrder);
    this.canRun              = Stream(canRun);
    this.preparingToSchedule = Stream(preparingToSchedule);
    this.comment             = Stream(comment);
    this.scheduledDate       = Stream(parseDate(scheduledDate));
    this.buildCause          = Stream(buildCause);
    this.stages              = Stream(stages);
  }

  static fromJSON(data: PipelineInstanceJSON): PipelineInstance {
    return new PipelineInstance(data.id, data.name, data.counter, data.label, data.natural_order, data.can_run, data.preparing_to_schedule, data.comment, data.scheduled_date, BuildCause.fromJSON(data.build_cause), Stages.fromJSON(data.stages));
  }

  isBisect(): boolean {
    return !!(this.naturalOrder() - (_.toInteger(this.naturalOrder())));
  }
}

export class BuildCause {
  triggerMessage: Stream<string>;
  triggerForced: Stream<boolean>;
  approver: Stream<string>;
  materialRevisions: Stream<MaterialRevisions>;

  constructor(triggerMessage: string, triggerForced: boolean, approver: string, materialRevisions: MaterialRevisions) {
    this.triggerMessage    = Stream(triggerMessage);
    this.triggerForced     = Stream(triggerForced);
    this.approver          = Stream(approver);
    this.materialRevisions = Stream(materialRevisions);
  }

  static fromJSON(data: BuildCauseJSON): BuildCause {
    return new BuildCause(data.trigger_message, data.trigger_forced, data.approver, MaterialRevisions.fromJSON(data.material_revisions));
  }

  getApprover() {
    if (this.approver() === "") {
      return "GoCD";
    }
    return this.approver();
  }
}

export class MaterialRevision {
  changed: Stream<boolean>;
  material: Stream<Material>;
  modifications: Stream<Modifications>;

  constructor(changed: boolean, material: Material, modifications: Modifications) {
    this.changed       = Stream(changed);
    this.material      = Stream(material);
    this.modifications = Stream(modifications);
  }

  static fromJSON(data: MaterialRevisionJSON): MaterialRevision {
    return new MaterialRevision(data.changed, Material.fromJSON(data.material), Modifications.fromJSON(data.modifications));
  }
}

export class MaterialRevisions extends Array<MaterialRevision> {
  constructor(...revisions: MaterialRevision[]) {
    super(...revisions);
    Object.setPrototypeOf(this, Object.create(MaterialRevisions.prototype));
  }

  static fromJSON(data: MaterialRevisionJSON[]): MaterialRevisions {
    return new MaterialRevisions(...data.map((rev) => MaterialRevision.fromJSON(rev)));
  }

  filterRevision(filterText: string): MaterialRevisions {
    if (_.isEmpty(filterText)) {
      return this;
    }

    return new MaterialRevisions(...this.filter((revision) => {
      return revision.modifications().some((modification) => this.searchPredicate(modification, filterText));
    }));
  }

  private searchPredicate(modification: Modification, filterText: string) {
    const lowercaseFilterText = filterText.toLowerCase();
    return _.includes(this.getOrEmpty(modification.comment()), lowercaseFilterText) ||
           _.includes(this.getOrEmpty(modification.userName()), lowercaseFilterText) ||
           _.includes(this.getOrEmpty(modification.emailAddress()), lowercaseFilterText) ||
           _.includes(this.getOrEmpty(modification.pipelineLabel()), lowercaseFilterText) ||
           _.includes(this.getOrEmpty(modification.revision()), lowercaseFilterText);
  }

  private getOrEmpty(str: string | number | null) {
    return str ? str.toString().toLowerCase() : "";
  }
}

class Material {
  id: Stream<number>;
  name: Stream<string>;
  type: Stream<string>;
  fingerprint: Stream<string>;
  description: Stream<string>;

  constructor(id: number, name: string, type: string, fingerprint: string, description: string) {
    this.id          = Stream(id);
    this.name        = Stream(name);
    this.type        = Stream(type);
    this.fingerprint = Stream(fingerprint);
    this.description = Stream(description);
  }

  static fromJSON(data: MaterialJSON): Material {
    return new Material(data.id, data.name, data.type, data.fingerprint, data.description);
  }
}

class Modification {
  id: Stream<number>;
  revision: Stream<string>;
  modifiedTime: Stream<Date | undefined>;
  userName: Stream<string>;
  comment: Stream<stringOrNull>;
  emailAddress: Stream<stringOrNull>;
  pipelineLabel: Stream<stringOrNull>;

  constructor(id: number, revision: string, modifiedTime: string, userName: string, comment: stringOrNull, emailAddress: stringOrNull, pipelineLabel: stringOrNull) {
    this.id            = Stream(id);
    this.revision      = Stream(revision);
    this.modifiedTime  = Stream(parseDate(modifiedTime));
    this.userName      = Stream(userName);
    this.comment       = Stream(comment);
    this.emailAddress  = Stream(emailAddress);
    this.pipelineLabel = Stream(pipelineLabel);
  }

  static fromJSON(data: ModificationJSON): Modification {
    return new Modification(data.id, data.revision, data.modified_time, data.user_name, data.comment, data.email_address, data.pipeline_label);
  }
}

class Modifications extends Array<Modification> {
  constructor(...modifications: Modification[]) {
    super(...modifications);
    Object.setPrototypeOf(this, Object.create(Modifications.prototype));
  }

  static fromJSON(modifications: ModificationJSON[]) {
    return new Modifications(...modifications.map(Modification.fromJSON));
  }
}

export class Stage {
  id: Stream<number>;
  name: Stream<string>;
  counter: Stream<string>;
  scheduled: Stream<boolean>;
  result: Stream<string>;
  status: Stream<string>;
  approvalType: Stream<string>;
  approvedBy: Stream<string>;
  operatePermission: Stream<boolean>;
  canRun: Stream<boolean>;
  jobs: Stream<Jobs>;

  constructor(id: number, name: string, counter: string, scheduled: boolean, result: string, status: string, approvalType: string, approvedBy: string, operatePermission: boolean, canRun: boolean, jobs: Jobs) {
    this.result            = Stream(result);
    this.status            = Stream(status);
    this.id                = Stream(id);
    this.name              = Stream(name);
    this.counter           = Stream(counter);
    this.scheduled         = Stream(scheduled);
    this.approvalType      = Stream(approvalType);
    this.approvedBy        = Stream(approvedBy);
    this.operatePermission = Stream(operatePermission);
    this.canRun            = Stream(canRun);
    this.jobs              = Stream(jobs);
  }

  static fromJSON(data: StageJSON): Stage {
    return new Stage(data.id, data.name, data.counter, data.scheduled, data.result, data.status, data.approval_type, data.approved_by, data.operate_permission, data.can_run, Jobs.fromJSON(data.jobs));
  }
}

export class Stages extends Array<Stage> {

  constructor(...stages: Stage[]) {
    super(...stages);
    Object.setPrototypeOf(this, Object.create(Stages.prototype));
  }

  static fromJSON(data: StageJSON[]): Stages {
    return new Stages(...data.map((stage) => Stage.fromJSON(stage)));
  }
}

export class Job {
  id: Stream<number>;
  name: Stream<string>;
  scheduledDate: Stream<Date | undefined>;
  state: Stream<string>;
  result: Stream<string>;

  constructor(id: number, name: string, scheduled_date: number, state: string, result: string) {
    this.id            = Stream(id);
    this.name          = Stream(name);
    this.scheduledDate = Stream(parseDate(scheduled_date));
    this.state         = Stream(state);
    this.result        = Stream(result);
  }

  static fromJSON(data: JobJSON): Job {
    return new Job(data.id, data.name, data.scheduled_date, data.state, data.result);
  }
}

export class Jobs extends Array<Job> {
  constructor(...jobs: Job[]) {
    super(...jobs);
    Object.setPrototypeOf(this, Object.create(Jobs.prototype));
  }

  static fromJSON(data: JobJSON[]): Jobs {
    if (!data) {
      return new Jobs();
    }
    return new Jobs(...data.map((job) => Job.fromJSON(job)));
  }
}

export function parseDate(dateAsStringOrNumber: string | number | null) {
  if (!dateAsStringOrNumber) {
    return undefined;
  }

  if (_.isNumber(dateAsStringOrNumber)) {
    return new Date(dateAsStringOrNumber);
  }

  return timeFormatter.toDate(dateAsStringOrNumber);
}
