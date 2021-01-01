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
import {ApiResult, SuccessResponse} from "helpers/api_request_builder";
import {User, Users} from "models/users/users";
import {UsersCRUD} from "models/users/users_crud";

describe("UsersCRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  describe("All", () => {
    it("should make a get request", (done) => {
      const ALL_USERS_API = "/go/api/users";
      jasmine.Ajax.stubRequest(ALL_USERS_API).andReturn(usersResponse());

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
        const responseJSON = response.unwrap() as SuccessResponse<any>;
        expect((responseJSON.body as Users)).toHaveLength(2);
        done();
      });

      UsersCRUD.all().then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(ALL_USERS_API);
      expect(request.method).toEqual("GET");
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
    });
  });

  describe("Create", () => {
    it("should make a post request", (done) => {
      const userJSON      = users()[0];
      const ALL_USERS_API = "/go/api/users";
      jasmine.Ajax.stubRequest(ALL_USERS_API, JSON.stringify(userJSON), "POST").andReturn({
                                                                                            status: 200,
                                                                                            responseHeaders: {
                                                                                              "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
                                                                                              "ETag": "some-etag"
                                                                                            },
                                                                                            responseText: JSON.stringify(
                                                                                              userJSON)
                                                                                          });

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<User>) => {
        const responseJSON = response.unwrap() as SuccessResponse<User>;
        expect(responseJSON.body.loginName()).toEqual(userJSON.login_name);
        done();
      });

      UsersCRUD.create(userJSON).then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();

      expect(request.url).toEqual(ALL_USERS_API);
      expect(request.method).toEqual("POST");
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
      expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
    });
  });

  describe("bulkUserStateUpdate", () => {
    const BULK_UPDATE_USERS_API = "/go/api/users/operations/state";
    it("should make a patch request", (done) => {
      const bulkUpdateUserJSON = {
        users: ["bob", "alice"],
        operations: {
          enable: true
        }
      };

      jasmine.Ajax.stubRequest(BULK_UPDATE_USERS_API, JSON.stringify(bulkUpdateUserJSON), "PATCH")
             .andReturn({
                          status: 200,
                          responseHeaders: {
                            "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
                            "ETag": "some-etag"
                          },
                          responseText: JSON.stringify(
                            bulkUpdateUserJSON)
                        });

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
        done();
      });

      UsersCRUD.bulkUserStateUpdate(bulkUpdateUserJSON).then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();

      expect(request.url).toEqual(BULK_UPDATE_USERS_API);
      expect(request.method).toEqual("PATCH");
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
      expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
    });
  });

  describe("bulkUserDelete", () => {
    const BULK_DELETE_USERS_API = "/go/api/users";
    it("should make a delete request", (done) => {
      const bulkDeleteUserJSON = {
        users: ["bob", "alice"]
      };

      jasmine.Ajax.stubRequest(BULK_DELETE_USERS_API, JSON.stringify(bulkDeleteUserJSON), "DELETE")
             .andReturn({
                          status: 200,
                          responseHeaders: {
                            "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
                            "ETag": "some-etag"
                          },
                          responseText: JSON.stringify(
                            bulkDeleteUserJSON)
                        });

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<User>) => {
        done();
      });

      UsersCRUD.bulkUserDelete(bulkDeleteUserJSON).then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();

      expect(request.url).toEqual(BULK_DELETE_USERS_API);
      expect(request.method).toEqual("DELETE");
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
      expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
    });
  });

  describe("Get", () => {
    it("should make a get request", (done) => {
      const userJSON     = users()[0];
      const GET_USER_API = "/go/api/users/bob";
      jasmine.Ajax.stubRequest(GET_USER_API).andReturn({
                                                         status: 200,
                                                         responseHeaders: {
                                                           "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
                                                           "ETag": "some-etag"
                                                         },
                                                         responseText: JSON.stringify(
                                                           userJSON)
                                                       });

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<User>) => {
        const responseJSON = response.unwrap() as SuccessResponse<User>;
        expect(responseJSON.body.loginName()).toEqual(userJSON.login_name);
        done();
      });

      UsersCRUD.get("bob").then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(GET_USER_API);
      expect(request.method).toEqual("GET");
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
    });
  });
});

function users() {
  return [
    {
      display_name: "Bob",
      login_name: "bob",
      is_admin: true,
      email: "bob@example.com"
    },
    {
      display_name: "Alice",
      login_name: "alice",
      is_admin: true,
      email: "alice@example.com"
    }
  ];
}

function usersResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify({
                                   _embedded: {
                                     users: users()
                                   }
                                 })
  };
}
