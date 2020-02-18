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

import {ApiResult, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {MailServerCrud} from "../server_configuartion_crud";
import {MailServer} from "../server_configuration";

describe('ServerConfigurationCRUDSpecs', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it('should send a test mail', (done) => {
    jasmine.Ajax.stubRequest(SparkRoutes.testMailForMailServerConfigPath()).andReturn(testMailResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.message).toEqual("some message");
      done();
    });

    MailServerCrud.testMail(new MailServer()).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(SparkRoutes.testMailForMailServerConfigPath());
    expect(request.method).toEqual("POST");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });
});

function testMailResponse() {
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
    },
    responseText:    JSON.stringify({message: "some message"})
  };
}
