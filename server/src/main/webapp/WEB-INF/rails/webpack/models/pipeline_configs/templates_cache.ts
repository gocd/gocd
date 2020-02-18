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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {AbstractObjCache, rejectAsString} from "models/base/cache";

export interface Template {
  name: string;
  parameters: string[];
}

export class TemplateCache extends AbstractObjCache<Template[]> {
  constructor() {
    super();
  }

  doFetch(resolve: (data: Template[]) => void, reject: (reason: string) => void) {
    ApiRequestBuilder
      .GET(SparkRoutes.apiAdminInternalPipelinesListPath("administer", "view"), ApiVersion.latest)
      .then((res) => {
        res.do((s) => {
          resolve(JSON.parse(s.body).templates);
        }, (e) => {
          reject(e.message);
        });
      })
      .catch(rejectAsString(reject));
  }
}
