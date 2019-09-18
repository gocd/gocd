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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import {AbstractObjCache, ObjectCache, rejectAsString} from "models/base/cache";
import {Option} from "views/components/forms/input_fields";

interface Template {
  name: string;
}

export interface TemplateCache<G> extends ObjectCache<Template[]> {
  templates: () => G[];
}

export class TemplatesCache<G> extends AbstractObjCache<Template[]> {
  private toTemplate: (templateName: string) => G;

  constructor(toTemplate: (templateName: string) => G) {
    super();
    this.toTemplate = toTemplate;
  }

  doFetch(resolve: (data: Template[]) => void, reject: (reason: string) => void) {
    ApiRequestBuilder.GET(SparkRoutes.templatesPath(), ApiVersion.v5).then((res) => {
      res.do((s) => {
        resolve(JSON.parse(s.body)._embedded.templates);
      }, (e) => {
        reject(e.message);
      });
    }).catch(rejectAsString(reject));
  }

  templates(): G[] {
    return this.ready() ? _.map(this.contents(), (entry) => this.toTemplate(entry.name)) : [];
  }
}

export class DefaultTemplatesCache extends TemplatesCache<Option> {
  constructor() {
    super((templateName: string) => ({id: templateName, text: templateName} as Option));
  }
}
