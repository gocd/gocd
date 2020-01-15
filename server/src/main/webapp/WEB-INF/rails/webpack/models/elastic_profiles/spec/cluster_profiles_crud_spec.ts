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
import {ClusterProfile, ClusterProfiles} from "models/elastic_profiles/types";

describe("ClusterProfileCRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());
  const ALL_CLUSTER_PROFILES_PATH = "/go/api/admin/elastic/cluster_profiles";
  const GET_CLUSTER_PROFILE_PATH  = `${ALL_CLUSTER_PROFILES_PATH}/cluster_1`;

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
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
  });

  it("should get a cluster profile", (done) => {
    jasmine.Ajax.stubRequest(GET_CLUSTER_PROFILE_PATH).andReturn(clusterProfileResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.object.id()).toEqual("cluster_1");
      expect(responseJSON.body.object.pluginId()).toEqual("plugin_1");
      expect(response.getEtag()).toEqual("some-etag");
      done();
    });

    ClusterProfilesCRUD.get("cluster_1").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(GET_CLUSTER_PROFILE_PATH);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
  });

  it("should create a cluster profile", (done) => {
    jasmine.Ajax.stubRequest(ALL_CLUSTER_PROFILES_PATH).andReturn(clusterProfileResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.object.id()).toEqual("cluster_1");
      expect(responseJSON.body.object.pluginId()).toEqual("plugin_1");
      expect(response.getEtag()).toEqual("some-etag");
      done();
    });

    ClusterProfilesCRUD.create(ClusterProfile.fromJSON(clusterProfileTestData("cluster_1", "plugin_1")))
                       .then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(ALL_CLUSTER_PROFILES_PATH);
    expect(request.method).toEqual("POST");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
  });

  it("should update a cluster profile", (done) => {
    jasmine.Ajax.stubRequest(GET_CLUSTER_PROFILE_PATH).andReturn(clusterProfileResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.object.id()).toEqual("cluster_1");
      expect(responseJSON.body.object.pluginId()).toEqual("plugin_1");
      expect(response.getEtag()).toEqual("some-etag");
      done();
    });

    ClusterProfilesCRUD.update(ClusterProfile.fromJSON(clusterProfileTestData("cluster_1", "plugin_2")), "old_etag")
                       .then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(GET_CLUSTER_PROFILE_PATH);
    expect(request.method).toEqual("PUT");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
  });

  it("should delete a cluster profile", (done) => {
    jasmine.Ajax.stubRequest(GET_CLUSTER_PROFILE_PATH).andReturn(clusterProfileResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      done();
    });

    ClusterProfilesCRUD.delete("cluster_1").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(GET_CLUSTER_PROFILE_PATH);
    expect(request.method).toEqual("DELETE");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
  });

  function clusterProfilesResponse() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify({
                                     _embedded: {
                                       cluster_profiles: [
                                         clusterProfileTestData("dev2", "cd.go.contrib.elastic-agent.docker")
                                       ]
                                     }
                                   })
    };
  }

  function clusterProfileResponse() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify(clusterProfileTestData("cluster_1", "plugin_1"))
    };
  }

  function clusterProfileTestData(id: string, pluginID: string, canAdminister: boolean = false) {
    return {
      id,
      plugin_id: pluginID,
      can_administer: canAdminister,
      properties: [
        {
          key: "docker_uri",
          value: "unix:///var/run/docker.sock"
        }
      ]
    };
  }
});
