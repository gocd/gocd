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
import {SparkRoutes} from "helpers/spark_routes";
import {ConfigReposCRUD as ConfigReposCrud} from "models/config_repos/config_repos_crud";

describe("Config Repo CRUD model", () => {
  it("all() should cache etag", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.apiConfigReposInternalPath(), undefined, "GET").andReturn({
        responseText:    JSON.stringify({
          "_embedded": {
            "config_repos": []
          },
          "auto_completion": []
        }),
        responseHeaders: {
          "Content-Type": "application/vnd.go.cd.v1+json"
        },
        status:          200
      });

      ConfigReposCrud.all().then(() => {
        done();
      }, () => done.fail("request should be successful"));
    });
  });
});
