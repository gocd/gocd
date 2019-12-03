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

import {timeFormatter} from "helpers/time_formatter";
import _ from "lodash";
import {
  BuildCauseJSON,
  JobJSON,
  MaterialJSON,
  MaterialRevisionJSON,
  ModificationJSON,
  PipelineInstanceJSON,
  StageJSON,
  stringOrNull
} from "./pipeline_instance_json";

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
  id: number;
  name: string;
  counter: number;
  label: string;
  naturalOrder: number;
  canRun: boolean;
  preparingToSchedule: boolean;
  comment: stringOrNull;
  buildCause: BuildCause;
  stages: Stages;

  constructor(id: number, name: string, counter: number, label: string, naturalOrder: number, canRun: boolean, preparingToSchedule: boolean, comment: stringOrNull, buildCause: BuildCause, stages: Stages) {
    this.id                  = id;
    this.name                = name;
    this.counter             = counter;
    this.label               = label;
    this.naturalOrder        = naturalOrder;
    this.canRun              = canRun;
    this.preparingToSchedule = preparingToSchedule;
    this.comment             = comment;
    this.buildCause          = buildCause;
    this.stages              = stages;
  }

  static fromJSON(data: PipelineInstanceJSON): PipelineInstance {
    return new PipelineInstance(data.id, data.name, data.counter, data.label, data.natural_order, data.can_run, data.preparing_to_schedule, data.comment, BuildCause.fromJSON(data.build_cause), Stages.fromJSON(data.stages));
  }
}

class BuildCause {
  triggerMessage: string;
  triggerForced: boolean;
  approver: string;
  materialRevisions: MaterialRevisions;

  constructor(triggerMessage: string, triggerForced: boolean, approver: string, materialRevisions: MaterialRevisions) {
    this.triggerMessage    = triggerMessage;
    this.triggerForced     = triggerForced;
    this.approver          = approver;
    this.materialRevisions = materialRevisions;
  }

  static fromJSON(data: BuildCauseJSON): BuildCause {
    return new BuildCause(data.trigger_message, data.trigger_forced, data.approver, MaterialRevisions.fromJSON(data.material_revisions));
  }
}

class MaterialRevision {
  changed: boolean;
  material: Material;
  modifications: Modifications;

  constructor(changed: boolean, material: Material, modifications: Modifications) {
    this.changed       = changed;
    this.material      = material;
    this.modifications = modifications;
  }

  static fromJSON(data: MaterialRevisionJSON): MaterialRevision {
    return new MaterialRevision(data.changed, Material.fromJSON(data.material), Modifications.fromJSON(data.modifications));
  }
}

class MaterialRevisions extends Array<MaterialRevision> {
  constructor(...revisions: MaterialRevision[]) {
    super(...revisions);
    Object.setPrototypeOf(this, Object.create(MaterialRevisions.prototype));
  }

  static fromJSON(data: MaterialRevisionJSON[]): MaterialRevisions {
    return new MaterialRevisions(...data.map((rev) => MaterialRevision.fromJSON(rev)));
  }
}

class Material {
  id: number;
  name: string;
  type: string;
  fingerprint: string;
  description: string;

  constructor(id: number, name: string, type: string, fingerprint: string, description: string) {
    this.id          = id;
    this.name        = name;
    this.type        = type;
    this.fingerprint = fingerprint;
    this.description = description;
  }

  static fromJSON(data: MaterialJSON): Material {
    return new Material(data.id, data.name, data.type, data.fingerprint, data.description);
  }
}

class Modification {
  id: number;
  revision: string;
  modifiedTime: Date | undefined;
  userName: string;
  comment: stringOrNull;
  emailAddress: stringOrNull;

  constructor(id: number, revision: string, modifiedTime: string, userName: string, comment: stringOrNull, emailAddress: stringOrNull) {
    this.id           = id;
    this.revision     = revision;
    this.modifiedTime = parseDate(modifiedTime);
    this.userName     = userName;
    this.comment      = comment;
    this.emailAddress = emailAddress;
  }

  static fromJSON(data: ModificationJSON): Modification {
    return new Modification(data.id, data.revision, data.modified_time, data.user_name, data.comment, data.email_address);
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

class Stage {
  id: number;
  name: string;
  counter: string;
  scheduled: boolean;
  result: string;
  approvalType: string;
  approvedBy: string;
  operatePermission: boolean;
  canRun: boolean;
  jobs: Jobs;

  constructor(id: number, name: string, counter: string, scheduled: boolean, result: string, approvalType: string, approvedBy: string, operatePermission: boolean, canRun: boolean, jobs: Jobs) {
    this.result            = result;
    this.id                = id;
    this.name              = name;
    this.counter           = counter;
    this.scheduled         = scheduled;
    this.approvalType      = approvalType;
    this.approvedBy        = approvedBy;
    this.operatePermission = operatePermission;
    this.canRun            = canRun;
    this.jobs              = jobs;
  }

  static fromJSON(data: StageJSON): Stage {
    return new Stage(data.id, data.name, data.counter, data.scheduled, data.result, data.approval_type, data.approved_by, data.operate_permission, data.can_run, Jobs.fromJSON(data.jobs));
  }
}

class Stages extends Array<Stage> {

  constructor(...stages: Stage[]) {
    super(...stages);
    Object.setPrototypeOf(this, Object.create(Stages.prototype));
  }

  static fromJSON(data: StageJSON[]): Stages {
    return new Stages(...data.map((stage) => Stage.fromJSON(stage)));
  }
}

class Job {
  id: number;
  name: string;
  scheduledDate: Date | undefined;
  state: string;
  result: string;

  constructor(id: number, name: string, scheduled_date: string, state: string, result: string) {
    this.id            = id;
    this.name          = name;
    this.scheduledDate = parseDate(scheduled_date);
    this.state         = state;
    this.result        = result;
  }

  static fromJSON(data: JobJSON): Job {
    return new Job(data.id, data.name, data.scheduled_date, data.state, data.result);
  }
}

class Jobs extends Array<Job> {
  constructor(...jobs: Job[]) {
    super(...jobs);
    Object.setPrototypeOf(this, Object.create(Jobs.prototype));
  }

  static fromJSON(data: JobJSON[]): Jobs {
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
