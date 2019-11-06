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
import {EnvironmentPipelineJSON, Pipelines} from "models/new-environments/environment_pipelines";

interface PipelineGroupJSON {
  name: string;
  pipelines: EnvironmentPipelineJSON[];
}

export interface PipelineGroupsJSON {
  groups: PipelineGroupJSON[];
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

  static fromJSON(data: PipelineGroupsJSON) {
    return new PipelineGroups(...data.groups.map(PipelineGroup.fromJSON));
  }
}
