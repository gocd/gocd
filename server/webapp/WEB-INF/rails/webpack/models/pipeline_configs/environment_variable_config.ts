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

import JsonUtils from "helpers/json_utils";
import * as _ from "lodash";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

export class EnvironmentVariableConfig extends ValidatableMixin {
  name: Stream<string>;
  value: Stream<string>;
  secure: Stream<boolean> = stream(false);

  constructor(secure: boolean, name: string, value: string) {
    super();
    this.name = stream(name);
    this.value = stream(value);
    this.secure = stream(secure);
    ValidatableMixin.call(this);
    this.validatePresenceOf("name");
  }

  toApiPayload() {
    return JsonUtils.toSnakeCasedObject(this);
  }

  modelType(): string {
    return "EnvironmentVariableConfig";
  }
}
