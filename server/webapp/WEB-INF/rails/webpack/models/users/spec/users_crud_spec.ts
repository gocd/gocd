/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import {UsersCRUD} from "models/users/users_crud";

describe("Users CRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  describe("All", () => {
    it("should make a get request", (done) => {
      const ALL_USERS_API = "/go/api/users";
      jasmine.Ajax.stubRequest(ALL_USERS_API).andReturn(usersResponse());

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
        const responseJSON = response.unwrap() as SuccessResponse<any>;
        expect(responseJSON.body).toEqual(users());
        done();
      });

      UsersCRUD.all().then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(ALL_USERS_API);
      expect(request.method).toEqual("GET");
      expect(request.requestHeaders).toEqual({Accept: "application/vnd.go.cd.v2+json"});
    });
  });

  describe("Create", () => {
    it("should make a post request", (done) => {
      const userJSON = users()[0];
      const ALL_USERS_API = "/go/api/users";
      jasmine.Ajax.stubRequest(ALL_USERS_API, JSON.stringify(userJSON), 'POST').andReturn({
                                                                                            status: 200,
                                                                                            responseHeaders: {
                                                                                              "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8",
                                                                                              "ETag": "some-etag"
                                                                                            },
                                                                                            responseText: JSON.stringify(userJSON)
                                                                                          });

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
        const responseJSON = response.unwrap() as SuccessResponse<any>;
        expect(responseJSON.body).toEqual(userJSON);
        done();
      });

      UsersCRUD.create(userJSON).then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();

      expect(request.url).toEqual(ALL_USERS_API);
      expect(request.method).toEqual("POST");
      expect(request.requestHeaders).toEqual({"Accept": "application/vnd.go.cd.v2+json", "Content-Type": "application/json; charset=utf-8"});
    });
  });
});

function users() {
  return [
    {
      display_name: "Bob",
      login_name: "bob",
      email: "bob@example.com"
    },
    {
      display_name: "Alice",
      login_name: "alice",
      email: "alice@example.com"
    }
  ];
}

function usersResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify({
                                   _embedded: {
                                     users: users()
                                   }
                                 })
  };
}
