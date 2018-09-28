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

const ConfigRepos = require("models/config_repos/config_repos");
const Routes      = require("gen/js-routes");

describe("Config Repo CRUD model", () => {
  it("all() should cache etag", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(Routes.apiv1AdminConfigReposPath(), undefined, "GET").andReturn({
        responseText:    JSON.stringify({
          "_embedded": {
            "config_repos": []
          },
        }),
        responseHeaders: {
          ETag:           `W/"05548388f7ef5042cd39f7fe42e85735--gzip"`,
          "Content-Type": "application/vnd.go.cd.v1+json"
        },
        status:          200
      });

      const configRepos = new ConfigRepos();
      configRepos.all().then(() => {
        expect(configRepos.etag()).toEqual(`W/"05548388f7ef5042cd39f7fe42e85735"`);
        done();
      }, () => done.fail("request should be successful"));
    });
  });
});
