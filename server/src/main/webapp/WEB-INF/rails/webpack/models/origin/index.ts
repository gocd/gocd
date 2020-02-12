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

import Stream from "mithril/stream";

export enum OriginType {
  GoCD       = "gocd",
  ConfigRepo = "config_repo"
}

export interface OriginJSON {
  type: OriginType;
  id?: string;
}

export class Origin {
  readonly type: Stream<OriginType>;
  readonly id: Stream<string | undefined>;

  constructor(type: OriginType, id?: string) {
    this.type = Stream(type);
    this.id   = Stream(id);
  }

  static fromJSON(data: OriginJSON) {
    return new Origin(data.type, data.id);
  }

  isDefinedInConfigRepo(): boolean {
    return this.type() === OriginType.ConfigRepo;
  }

  clone() {
    return new Origin(this.type(), this.id());
  }
}
