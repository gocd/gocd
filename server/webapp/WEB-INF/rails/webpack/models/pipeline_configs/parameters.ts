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

import {JsonUtils} from "helpers/json_utils";
import _ from "lodash";
import Stream from "mithril/stream";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

export class PipelineParameters extends ValidatableMixin {
  name: Stream<string> = Stream();
  value: Stream<string> = Stream();

  constructor(name: string, value: string) {
    super();
    this.name(name);
    this.value(value);

    this.validatePresenceOf("name");
    this.validateIdFormat("name");
  }

  isEmpty() {
    return "" === this.name() && "" === this.value();
  }

  toApiPayload() {
    return JsonUtils.toSnakeCasedObject(this);
  }
}
