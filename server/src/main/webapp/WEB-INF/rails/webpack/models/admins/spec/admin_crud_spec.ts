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

import {ApiResult} from "helpers/api_request_builder";
import {AdminsCRUD} from "models/admins/admin_crud";

describe("admin crud", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  describe("bulkUpdate", () => {
    const API_SYSTEM_ADMINS_PATH = "/go/api/admin/security/system_admins";
    it("should make a patch request", (done) => {
      const bulkUpdateSystemAdmins = {
        operations: {
          users: {
            add: ["bob", "alice"]
          }
        }
      };

      jasmine.Ajax.stubRequest(API_SYSTEM_ADMINS_PATH, JSON.stringify(bulkUpdateSystemAdmins), "PATCH")
             .andReturn({
                          status: 200,
                          responseHeaders: {
                            "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8",
                            "ETag": "some-etag"
                          },
                          responseText: JSON.stringify(
                            bulkUpdateSystemAdmins)
                        });

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
        done();
      });

      AdminsCRUD.bulkUpdate(bulkUpdateSystemAdmins).then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();

      expect(request.url).toEqual(API_SYSTEM_ADMINS_PATH);
      expect(request.method).toEqual("PATCH");
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
      expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
    });
  });
});
