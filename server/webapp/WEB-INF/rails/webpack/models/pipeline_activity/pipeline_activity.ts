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

import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {
  ConfigJSON,
  GroupJSON, HistoryJSON, MaterialRevisionJSON, ModificationJSON,
  PipelineActivityJSON,
  StageConfigJSON, StageJSON
} from "models/pipeline_activity/pipeline_activity_json";

const TimeFormatter = require("helpers/time_formatter");

class StageConfig {
  name: Stream<string>;
  isAutoApproved: Stream<boolean>;


  constructor(name: string, isAutoApproved: boolean) {
    this.name           = stream(name);
    this.isAutoApproved = stream(isAutoApproved);
  }

  static fromJSON(stage: StageConfigJSON) {
    return new StageConfig(stage.name, stage.isAutoApproved);
  }
}

class StageConfigs extends Array<StageConfig> {
  constructor(...stageConfigs: Array<StageConfig>) {
    super(...stageConfigs);
    Object.setPrototypeOf(this, Object.create(StageConfigs.prototype));
  }

  static fromJSON(stages: StageConfigJSON[]) {
    return new StageConfigs(...stages.map(StageConfig.fromJSON));
  }
}

export class Config {
  stages: Stream<StageConfigs>;

  constructor(stages: StageConfigs) {
    this.stages = stream(stages);
  }

  static fromJSON(config: ConfigJSON) {
    return new Config(StageConfigs.fromJSON(config.stages));
  }
}

class Modification {
  user: Stream<string>;
  revision: Stream<string>;
  date: Stream<Date>;
  comment: Stream<string>;
  modifiedFiles: Stream<string[]>;


  constructor(user: string, revision: string, date: Date, comment: string, modifiedFiles: string[]) {
    this.user          = stream(user);
    this.revision      = stream(revision);
    this.date          = stream(date);
    this.comment       = stream(comment);
    this.modifiedFiles = stream(modifiedFiles);
  }

  static fromJSON(modification: ModificationJSON) {
    return new Modification(modification.user,
                            modification.revision,
                            parseDate(modification.date),
                            modification.comment,
                            modification.modifiedFiles);
  }
}

class Modifications extends Array<Modification> {
  constructor(...modifications: Array<Modification>) {
    super(...modifications);
    Object.setPrototypeOf(this, Object.create(Modifications.prototype));
  }

  static fromJSON(modifications: ModificationJSON[]) {
    return new Modifications(...modifications.map(Modification.fromJSON));
  }
}

class MaterialRevision {
  revision: Stream<string>;
  revision_href: Stream<string>;
  user: Stream<string>;
  date: Stream<Date>;
  changed: Stream<boolean>;
  modifications: Stream<Modifications>;
  folder: Stream<string>;
  scmType: Stream<string>;
  location: Stream<string>;
  action: Stream<string>;

  constructor(revision: string,
              revision_href: string,
              user: string,
              date: Date,
              changed: boolean,
              modifications: Modifications,
              folder: string,
              scmType: string,
              location: string,
              action: string) {
    this.revision      = stream(revision);
    this.revision_href = stream(revision_href);
    this.user          = stream(user);
    this.date          = stream(date);
    this.changed       = stream(changed);
    this.modifications = stream(modifications);
    this.folder        = stream(folder);
    this.scmType       = stream(scmType);
    this.location      = stream(location);
    this.action        = stream(action);
  }

  static fromJSON(materialRevision: MaterialRevisionJSON) {
    return new MaterialRevision(materialRevision.revision,
                                materialRevision.revision_href,
                                materialRevision.user,
                                parseDate(materialRevision.date),
                                materialRevision.changed,
                                Modifications.fromJSON(materialRevision.modifications),
                                materialRevision.folder,
                                materialRevision.scmType,
                                materialRevision.location,
                                materialRevision.action);
  }
}

class MaterialRevisions extends Array<MaterialRevision> {
  constructor(...materialRevisions: Array<MaterialRevision>) {
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
  stageCounter: Stream<string>;
  approvedBy: Stream<string>;


  constructor(stageName: string,
              stageId: number,
              stageStatus: string,
              stageLocator: string,
              getCanRun: boolean,
              getCanCancel: boolean,
              scheduled: boolean,
              stageCounter: string,
              approvedBy: string) {
    this.stageName    = stream(stageName);
    this.stageId      = stream(stageId);
    this.stageStatus  = stream(stageStatus);
    this.stageLocator = stream(stageLocator);
    this.getCanRun    = stream(getCanRun);
    this.getCanCancel = stream(getCanCancel);
    this.scheduled    = stream(scheduled);
    this.stageCounter = stream(stageCounter);
    this.approvedBy   = stream(approvedBy);
  }

  static fromJSON(stage: StageJSON) {
    return new Stage(stage.stageName,
                     stage.stageId,
                     stage.stageStatus,
                     stage.stageLocator,
                     stage.getCanRun,
                     stage.getCanCancel,
                     stage.scheduled,
                     stage.stageCounter,
                     stage.approvedBy);
  }
}

class Stages extends Array<Stage> {
  constructor(...stages: Array<Stage>) {
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
  scheduled_date: Stream<Date>;
  scheduled_timestamp: Stream<Date>;
  buildCauseBy: Stream<string>;
  modification_date: Stream<Date>;
  materialRevisions: Stream<MaterialRevisions>;
  stages: Stream<Stages>;
  revision: Stream<string>;
  comment?: Stream<string>;


  constructor(pipelineId: number,
              label: string,
              counterOrLabel: string,
              scheduled_date: Date,
              scheduled_timestamp: Date,
              buildCauseBy: string,
              modification_date: Date,
              materialRevisions: MaterialRevisions,
              stages: Stages,
              revision: string,
              comment?: string) {
    this.pipelineId          = stream(pipelineId);
    this.label               = stream(label);
    this.counterOrLabel      = stream(counterOrLabel);
    this.scheduled_date      = stream(scheduled_date);
    this.scheduled_timestamp = stream(scheduled_timestamp);
    this.buildCauseBy        = stream(buildCauseBy);
    this.modification_date   = stream(modification_date);
    this.materialRevisions   = stream(materialRevisions);
    this.stages              = stream(stages);
    this.revision            = stream(revision);
    this.comment             = stream(comment);
  }

  static fromJSON(pipelineRunInfo: HistoryJSON) {
    return new PipelineRunInfo(pipelineRunInfo.pipelineId,
                               pipelineRunInfo.label,
                               pipelineRunInfo.counterOrLabel,
                               parseDate(pipelineRunInfo.scheduled_date),
                               parseDate(pipelineRunInfo.scheduled_timestamp),
                               pipelineRunInfo.buildCauseBy,
                               parseDate(pipelineRunInfo.modification_date),
                               MaterialRevisions.fromJSON(pipelineRunInfo.materialRevisions),
                               Stages.fromJSON(pipelineRunInfo.stages),
                               pipelineRunInfo.revision,
                               pipelineRunInfo.comment);
  }

}

class PipelineHistory extends Array<PipelineRunInfo> {
  constructor(...pipelineRunInfos: Array<PipelineRunInfo>) {
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
    this.config  = stream(config);
    this.history = stream(history);
  }

  static fromJSON(group: GroupJSON): Group {
    return new Group(Config.fromJSON(group.config), PipelineHistory.fromJSON(group.history));

  }
}

class Groups extends Array<Group> {
  constructor(...groups: Array<Group>) {
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
    this.pipelineName         = stream(pipelineName);
    this.paused               = stream(paused);
    this.pauseCause           = stream(pauseCause);
    this.pauseBy              = stream(pauseBy);
    this.canForce             = stream(canForce);
    this.nextLabel            = stream(nextLabel);
    this.groups               = stream(groups);
    this.forcedBuild          = stream(forcedBuild);
    this.showForceBuildButton = stream(showForceBuildButton);
    this.canPause             = stream(canPause);
    this.count                = stream(count);
    this.start                = stream(start);
    this.perPage              = stream(perPage);
  }

// to be implemented
  static fromJSON(data: PipelineActivityJSON) {
    // to be implemented
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

function parseDate(dateString: string | null) {
  if (dateString) {
    return TimeFormatter.toDate(dateString);
  }
  return undefined;
}
