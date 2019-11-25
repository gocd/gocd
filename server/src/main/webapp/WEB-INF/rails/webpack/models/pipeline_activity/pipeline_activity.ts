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
import {
  ConfigJSON,
  GroupJSON,
  HistoryJSON,
  MaterialRevisionJSON,
  ModificationJSON,
  PipelineActivityJSON,
  StageConfigJSON,
  StageJSON
} from "models/pipeline_activity/pipeline_activity_json";

const TimeFormatter = require("helpers/time_formatter");

function toBool(str: string | boolean) {
  if (typeof str === "undefined") {
    return false;
  }

  if (typeof str === "boolean") {
    return str;
  }

  return str.trim().toLowerCase() === "true";

}

class StageConfig {
  name: Stream<string>;
  isAutoApproved: Stream<boolean>;

  constructor(name: string, isAutoApproved: boolean) {
    this.name           = Stream(name);
    this.isAutoApproved = Stream(isAutoApproved);
  }

  static fromJSON(stage: StageConfigJSON) {
    return new StageConfig(stage.name, toBool(stage.isAutoApproved));
  }
}

export class StageConfigs extends Array<StageConfig> {
  constructor(...stageConfigs: StageConfig[]) {
    super(...stageConfigs);
    Object.setPrototypeOf(this, Object.create(StageConfigs.prototype));
  }

  isAutoApproved(name: string): boolean {
    return this.find((stage: StageConfig) => stage.name() === name)!.isAutoApproved();
  }


  static fromJSON(stages: StageConfigJSON[]) {
    return new StageConfigs(...stages.map(StageConfig.fromJSON));
  }
}

export class Config {
  stages: Stream<StageConfigs>;

  constructor(stages: StageConfigs) {
    this.stages = Stream(stages);
  }

  static fromJSON(config: ConfigJSON) {
    return new Config(StageConfigs.fromJSON(config.stages));
  }
}

export class Modification {
  user: Stream<string>;
  revision: Stream<string>;
  date: Stream<string>;
  comment: Stream<string>;
  modifiedFiles: Stream<string[]>;

  constructor(user: string, revision: string, date: string, comment: string, modifiedFiles: string[]) {
    this.user          = Stream(user);
    this.revision      = Stream(revision);
    this.date          = Stream(date);
    this.comment       = Stream(comment);
    this.modifiedFiles = Stream(modifiedFiles);
  }

  static fromJSON(modification: ModificationJSON) {
    return new Modification(modification.user,
      modification.revision,
      modification.date,
      modification.comment,
      modification.modifiedFiles);
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

export class MaterialRevision {
  revision: Stream<string>;
  revisionHref: Stream<string>;
  user: Stream<string>;
  date: Stream<string>;
  changed: Stream<boolean>;
  modifications: Stream<Modifications>;
  folder: Stream<string>;
  scmType: Stream<string>;
  location: Stream<string>;
  action: Stream<string>;

  constructor(revision: string,
              revision_href: string,
              user: string,
              date: string,
              changed: boolean,
              modifications: Modifications,
              folder: string,
              scmType: string,
              location: string,
              action: string) {
    this.revision      = Stream(revision);
    this.revisionHref  = Stream(revision_href);
    this.user          = Stream(user);
    this.date          = Stream(date);
    this.changed       = Stream(changed);
    this.modifications = Stream(modifications);
    this.folder        = Stream(folder);
    this.scmType       = Stream(scmType);
    this.location      = Stream(location);
    this.action        = Stream(action);
  }

  static fromJSON(materialRevision: MaterialRevisionJSON) {
    return new MaterialRevision(materialRevision.revision,
      materialRevision.revision_href,
      materialRevision.user,
      materialRevision.date,
      materialRevision.changed,
      Modifications.fromJSON(materialRevision.modifications),
      materialRevision.folder,
      materialRevision.scmType,
      materialRevision.location,
      materialRevision.action);
  }
}

class MaterialRevisions extends Array<MaterialRevision> {
  constructor(...materialRevisions: MaterialRevision[]) {
    super(...materialRevisions);
    Object.setPrototypeOf(this, Object.create(MaterialRevisions.prototype));
  }

  static fromJSON(materialRevisions: MaterialRevisionJSON[]) {
    return new MaterialRevisions(...materialRevisions.map(MaterialRevision.fromJSON));
  }
}

class Stage {
  stageName: Stream<string>;
  stageId: Stream<number>;
  stageStatus: Stream<string>;
  stageLocator: Stream<string>;
  getCanRun: Stream<boolean>;
  getCanCancel: Stream<boolean>;
  scheduled: Stream<boolean>;
  stageCounter: Stream<number>;
  approvedBy: Stream<string | undefined>;

  constructor(stageName: string,
              stageId: number,
              stageStatus: string,
              stageLocator: string,
              getCanRun: boolean,
              getCanCancel: boolean,
              scheduled: boolean,
              stageCounter: number,
              approvedBy?: string) {
    this.stageName    = Stream(stageName);
    this.stageId      = Stream(stageId);
    this.stageStatus  = Stream(stageStatus);
    this.stageLocator = Stream(stageLocator);
    this.getCanRun    = Stream(getCanRun);
    this.getCanCancel = Stream(getCanCancel);
    this.scheduled    = Stream(scheduled);
    this.stageCounter = Stream(stageCounter);
    this.approvedBy   = Stream(approvedBy);
  }

  static fromJSON(stage: StageJSON) {
    return new Stage(stage.stageName,
      stage.stageId,
      stage.stageStatus,
      stage.stageLocator,
      toBool(stage.getCanRun),
      toBool(stage.getCanCancel),
      stage.scheduled,
      stage.stageCounter,
      stage.approvedBy);
  }
}

class Stages extends Array<Stage> {
  constructor(...stages: Stage[]) {
    super(...stages);
    Object.setPrototypeOf(this, Object.create(Stages.prototype));
  }

  static fromJSON(stages: StageJSON[]) {
    return new Stages(...stages.map(Stage.fromJSON));
  }
}

export class PipelineRunInfo {
  pipelineId: Stream<number>;
  label: Stream<string>;
  counterOrLabel: Stream<string>;
  scheduledDate: Stream<string>;
  scheduledTimestamp: Stream<Date>;
  buildCauseBy: Stream<string>;
  modificationDate: Stream<string>;
  materialRevisions: Stream<MaterialRevisions>;
  stages: Stream<Stages>;
  revision: Stream<string>;
  comment: Stream<string | null>;

  constructor(pipelineId: number,
              label: string,
              counterOrLabel: string,
              scheduled_date: string,
              scheduled_timestamp: Date,
              buildCauseBy: string,
              modification_date: string,
              materialRevisions: MaterialRevisions,
              stages: Stages,
              revision: string,
              comment: string | null) {
    this.pipelineId         = Stream(pipelineId);
    this.label              = Stream(label);
    this.counterOrLabel     = Stream(counterOrLabel);
    this.scheduledDate      = Stream(scheduled_date);
    this.scheduledTimestamp = Stream(scheduled_timestamp);
    this.buildCauseBy       = Stream(buildCauseBy);
    this.modificationDate   = Stream(modification_date);
    this.materialRevisions  = Stream(materialRevisions);
    this.stages             = Stream(stages);
    this.revision           = Stream(revision);
    this.comment            = Stream(comment);
  }

  static fromJSON(pipelineRunInfo: HistoryJSON) {
    return new PipelineRunInfo(pipelineRunInfo.pipelineId,
      pipelineRunInfo.label,
      pipelineRunInfo.counterOrLabel,
      pipelineRunInfo.scheduled_date,
      parseDate(pipelineRunInfo.scheduled_timestamp),
      pipelineRunInfo.buildCauseBy,
      pipelineRunInfo.modification_date,
      MaterialRevisions.fromJSON(pipelineRunInfo.materialRevisions),
      Stages.fromJSON(pipelineRunInfo.stages),
      pipelineRunInfo.revision,
      pipelineRunInfo.comment);
  }

}

class PipelineHistory extends Array<PipelineRunInfo> {
  constructor(...pipelineRunInfos: PipelineRunInfo[]) {
    super(...pipelineRunInfos);
    Object.setPrototypeOf(this, Object.create(PipelineHistory.prototype));
  }

  static fromJSON(history: HistoryJSON[]) {
    return new PipelineHistory(...history.map(PipelineRunInfo.fromJSON));
  }
}

export class Group {
  config: Stream<Config>;
  history: Stream<PipelineHistory>;

  constructor(config: Config, history: PipelineHistory) {
    this.config  = Stream(config);
    this.history = Stream(history);
  }

  static fromJSON(group: GroupJSON): Group {
    return new Group(Config.fromJSON(group.config), PipelineHistory.fromJSON(group.history));

  }
}

class Groups extends Array<Group> {
  constructor(...groups: Group[]) {
    super(...groups);
    Object.setPrototypeOf(this, Object.create(Groups.prototype));
  }

  static fromJSON(groups: GroupJSON[]) {
    return new Groups(...groups.map(Group.fromJSON));
  }
}

export class PipelineActivity {
  pipelineName: Stream<string>;
  paused: Stream<boolean>;
  pauseCause: Stream<string>;
  pauseBy: Stream<string>;
  canForce: Stream<boolean>;
  nextLabel: Stream<string>;
  groups: Stream<Groups>;
  forcedBuild: Stream<boolean>;
  showForceBuildButton: Stream<boolean>;
  canPause: Stream<boolean>;
  count: Stream<number>;
  start: Stream<number>;
  perPage: Stream<number>;

  constructor(pipelineName: string,
              nextLabel: string,
              canPause: boolean,
              paused: boolean,
              pauseCause: string,
              pauseBy: string,
              canForce: boolean,
              forcedBuild: boolean,
              showForceBuildButton: boolean,
              count: number,
              start: number,
              perPage: number,
              groups: Groups) {
    this.pipelineName         = Stream(pipelineName);
    this.paused               = Stream(paused);
    this.pauseCause           = Stream(pauseCause);
    this.pauseBy              = Stream(pauseBy);
    this.canForce             = Stream(canForce);
    this.nextLabel            = Stream(nextLabel);
    this.groups               = Stream(groups);
    this.forcedBuild          = Stream(forcedBuild);
    this.showForceBuildButton = Stream(showForceBuildButton);
    this.canPause             = Stream(canPause);
    this.count                = Stream(count);
    this.start                = Stream(start);
    this.perPage              = Stream(perPage);
  }

  static fromJSON(data: PipelineActivityJSON) {
    return new PipelineActivity(data.pipelineName,
      data.nextLabel,
      data.canPause,
      data.paused,
      data.pauseCause,
      data.pauseBy,
      data.canForce,
      data.forcedBuild,
      data.showForceBuildButton,
      data.count,
      data.start,
      data.perPage,
      Groups.fromJSON(data.groups));
  }
}

function parseDate(dateAsStringOrNumber: string | number | null) {
  if (!dateAsStringOrNumber) {
    return undefined;
  }

  if (_.isNumber(dateAsStringOrNumber)) {
    return new Date(dateAsStringOrNumber);
  }

  return TimeFormatter.toDate(dateAsStringOrNumber);
}
