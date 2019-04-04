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

import {ApiResult, SuccessResponse} from "helpers/api_request_builder";
import {ClusterProfilesCRUD} from "models/elastic_profiles/cluster_profiles_crud";
import {ClusterProfiles} from "models/elastic_profiles/types";

describe("ClusterProfileCRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());
  const ALL_CLUSTER_PROFILES_PATH = "/go/api/admin/elastic/cluster_profiles";

  it("should get all cluster profiles", (done) => {
    jasmine.Ajax.stubRequest(ALL_CLUSTER_PROFILES_PATH).andReturn(clusterProfilesResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON    = response.unwrap() as SuccessResponse<any>;
      const clusterProfiles = (responseJSON.body as ClusterProfiles);
      expect(clusterProfiles.all()).toHaveLength(1);
      done();
    });

    ClusterProfilesCRUD.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(ALL_CLUSTER_PROFILES_PATH);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });

  function clusterProfilesResponse() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify({
                                     _embedded: {
                                       cluster_profiles: [
                                         {
                                           id: "dev1",
                                           plugin_id: "cd.go.contrib.elastic-agent.docker",
                                           properties: [
                                             {
                                               key: "docker_uri",
                                               value: "unix:///var/run/docker.sock"
                                             }
                                           ]
                                         }
                                       ]
                                     }
                                   })
    };
  }
});
