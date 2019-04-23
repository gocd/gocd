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
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import SparkRoutes from "helpers/spark_routes";
import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";

export class PipelineConfig extends ValidatableMixin {
  group: Stream<string> = stream("defaultGroup");
  name: Stream<string>;

  constructor(name: string) {
    super();
    ValidatableMixin.call(this);
    this.name = stream(name);
    this.validatePresenceOf("name");
  }
}
