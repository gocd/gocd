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
import {UserSearchCRUD} from "models/users/users_crud";

describe("Users search", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make a get request and return desired json response", (done) => {
    const USERS_SEARCH_API = "/go/api/user_search?q=bob";
    jasmine.Ajax.stubRequest(USERS_SEARCH_API).andReturn(usersResponse());
    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body).toEqual(users());
      done();
    });

    UserSearchCRUD.search("bob").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(USERS_SEARCH_API);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });
});

function users() {
  return [
    {
      display_name: "Bob",
      login_name: "bob",
      email: "bob@example.com"
    }
  ];
}

function usersResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify({
                                   _embedded: {
                                     users: users()
                                   }
                                 })
  };
}
