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

import {ApiResult, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {TemplateAuthorization} from "../templates";
import {TemplatesCRUD} from "../templates_crud";
import {templateJSON} from "./templates_spec";

describe('TemplateCRUD', () => {
  const templateName = 'template-name';

  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make a get template authorization request", (done) => {
    const url = SparkRoutes.templateAuthorizationPath(templateName);
    jasmine.Ajax.stubRequest(url).andReturn(templateAuthResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<ObjectWithEtag<TemplateAuthorization>>) => {
      const responseJSON = response.unwrap() as SuccessResponse<ObjectWithEtag<TemplateAuthorization>>;

      const authorization = responseJSON.body.object;
      expect(authorization.allGroupAdminsAreViewUsers()).toBeTrue();
      done();
    });

    TemplatesCRUD.getAuthorization(templateName).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should make a update template authorization request", (done) => {
    const authorization = TemplateAuthorization.fromJSON(templateJSON());
    const url           = SparkRoutes.templateAuthorizationPath(templateName);

    jasmine.Ajax.stubRequest(url).andReturn(templateAuthResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<ObjectWithEtag<TemplateAuthorization>>;
      expect(responseJSON.body.object.allGroupAdminsAreViewUsers()).toBeTrue();
      done();
    });

    TemplatesCRUD.updateAuthorization(templateName, authorization, "some-etag").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();

    expect(request.url).toEqual(url);
    expect(request.method).toEqual("PUT");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  function templateAuthResponse() {
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd+json; charset=utf-8",
        "ETag":         "some-etag"
      },
      responseText:    JSON.stringify(
        templateJSON())
    };
  }
});
