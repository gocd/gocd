/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {ApiRequestBuilder, ApiResult, ApiVersion, ObjectWithEtag} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {Template, TemplateAuthorization, TemplateAuthorizationJSON, TemplateSummary} from "models/admin_templates/templates";
import TemplateSummaryRootObject = TemplateSummary.TemplateSummaryRootObject;

export class TemplatesCRUD {

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.templatesPath(), ApiVersion.latest)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              return (JSON.parse(body) as TemplateSummaryRootObject)._embedded.templates;
                            }));
  }

  static get(name: string) {
    return ApiRequestBuilder.GET(SparkRoutes.templatesPath(name), ApiVersion.latest)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              return (JSON.parse(body)) as Template;
                            }));

  }

  static getAuthorization(name: string) {
    return ApiRequestBuilder.GET(SparkRoutes.templateAuthorizationPath(name), ApiVersion.latest)
                            .then(this.extractObjectWithEtag);

  }

  static updateAuthorization(templateName: string, updatedTemplateAuthorization: TemplateAuthorization, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.templateAuthorizationPath(templateName),
                                 ApiVersion.latest,
                                 {
                                   payload: updatedTemplateAuthorization, etag
                                 })
                            .then(this.extractObjectWithEtag);
  }

  static createEmptyTemplate(newName: string) {
    return ApiRequestBuilder.POST(SparkRoutes.templatesPath(), ApiVersion.latest, {
      payload: {
        name:   newName,
        stages: [
          {
            name: "defaultStage",
            jobs: [
                {
                  name: "defaultJob",
                  tasks: [
                    {
                      type : "exec",
                      attributes : {
                        run_if : [ "passed" ],
                        command : "echo",
                        args : ""
                      }
                    }
                  ]
                }
            ]
          }
        ]
      }
    });
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const json = JSON.parse(body) as TemplateAuthorizationJSON;
      return {
        object: TemplateAuthorization.fromJSON(json),
        etag:   result.getEtag()
      } as ObjectWithEtag<TemplateAuthorization>;
    });
  }
}
