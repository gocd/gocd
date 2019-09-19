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

import Stream from "mithril/stream";
import {Pipeline, PipelineJSON} from "models/environments/types";
import {Origin, OriginJSON} from "models/new-environments/origin";

export interface EnvironmentPipelineJSON extends PipelineJSON {
  origin: OriginJSON;
}

export class PipelineWithOrigin extends Pipeline {
  readonly origin: Stream<Origin>;

  constructor(name: string, origin: Origin) {
    super(name);
    this.origin = Stream(origin);
  }

  static fromJSON(data: EnvironmentPipelineJSON) {
    return new PipelineWithOrigin(data.name, Origin.fromJSON(data.origin));
  }

  clone() {
    return new PipelineWithOrigin(this.name(), this.origin().clone());
  }
}

export class Pipelines extends Array<PipelineWithOrigin> {
  constructor(...pipelines: PipelineWithOrigin[]) {
    super(...pipelines);
    Object.setPrototypeOf(this, Object.create(Pipelines.prototype));
  }

  static fromJSON(pipelines: EnvironmentPipelineJSON[]) {
    return new Pipelines(...pipelines.map(PipelineWithOrigin.fromJSON));
  }

  containsPipeline(name: string): boolean {
    return this.map((p) => p.name()).indexOf(name) !== -1;
  }

  clone() {
    return new Pipelines(...this.map((p) => p.clone()));
  }
}
