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

import _ from "lodash";
import Stream = require("mithril/stream");
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

export interface TabJSON {
  name: string;
  path: string;
}

export class Tabs extends Array<Tab> {
  constructor(...tabs: Tab[]) {
    super(...tabs);
    Object.setPrototypeOf(this, Object.create(Tabs.prototype));
  }

  static fromJSON(json: TabJSON[]) {
    return new Tabs(...json.map(Tab.fromJSON));
  }
}

export class Tab extends ValidatableMixin {
  readonly name = Stream<string>();
  readonly path = Stream<string>();

  constructor(name: string, path: string) {
    super();
    this.name(name);
    this.path(path);

    this.validatePresenceOf("path", {condition: () => !_.isEmpty(this.name())});
    this.validatePresenceOf("name", {condition: () => !_.isEmpty(this.path())});
  }

  static fromJSON(json: TabJSON) {
    return new Tab(json.name, json.path);
  }
}
