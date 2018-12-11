/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import {Jsonizable} from "models/jsonizable";

const TimeFormatter = require("helpers/time_formatter");

interface EmbeddedJSON {
  drain: boolean;
  updated_by: string;
  updated_on: string;
}

export interface DrainModeSettingsJSON {
  _embedded: EmbeddedJSON;
}

export class DrainModeSettings extends Jsonizable {
  public readonly drain: Stream<boolean>;
  public readonly updatedOn?: Date;
  public readonly updatedBy?: string;
  private readonly __originalDrainMode: Stream<boolean>;

  constructor(isDrainMode?: boolean, updatedBy?: string, updatedOn?: string) {
    super();
    this.__originalDrainMode = stream(isDrainMode);
    this.drain               = stream(isDrainMode);
    this.updatedBy           = updatedBy;
    this.updatedOn           = TimeFormatter.formatInDate(updatedOn);
  }

  static fromJSON(data: DrainModeSettingsJSON) {
    return new DrainModeSettings(data._embedded.drain, data._embedded.updated_by, data._embedded.updated_on);
  }

  public reset(): void {
    this.drain(this.__originalDrainMode());
  }
}
