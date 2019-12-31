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
import {ApiResult, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {ConfigReposCRUD, configRepoToSnakeCaseJSON} from "models/config_repos/config_repos_crud";
import {ConfigRepo} from "models/config_repos/types";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {Configuration, PlainTextValue} from "models/shared/plugin_infos_new/plugin_settings/plugin_settings";

describe("Config Repo Serialization", () => {
  it("should serialize configuration properties for JSON plugin", () => {
    const configuration1 = new Configuration("pipeline_pattern", new PlainTextValue("test-value-1"));
    const configuration2 = new Configuration("environment_pattern", new PlainTextValue("test-value-2"));
    const configRepo     = new ConfigRepo("test",
                                          ConfigRepo.JSON_PLUGIN_ID,
                                          new Material("git",
                                                       new GitMaterialAttributes("test",
                                                                                 false,
                                                                                 "https://example.com")),
                                          false,
                                          [configuration1, configuration2]);
    const json           = configRepoToSnakeCaseJSON(configRepo);
    const expectedJSON     = [
      {key: "pipeline_pattern", value: "test-value-1"},
      {key: "environment_pattern", value: "test-value-2"}
    ];
    expect(json.configuration).toEqual(expectedJSON);
  });

  it("should serialize configuration properties for YAML plugin", () => {
    const configuration1 = new Configuration("file_pattern", new PlainTextValue("test-value-1"));
    const configRepo     = new ConfigRepo("test",
                                          ConfigRepo.YAML_PLUGIN_ID,
                                          new Material("git",
                                                       new GitMaterialAttributes("test",
                                                                                 false,
                                                                                 "https://example.com")),
                                          false,
                                          [configuration1]);
    const json           = configRepoToSnakeCaseJSON(configRepo);
    expect(json.configuration).toEqual([{key: "file_pattern", value: "test-value-1"}]);
  });

  it("should not serialize configuration properties when not present", () => {
    const configRepo = new ConfigRepo("test",
                                      "test",
                                      new Material("git",
                                                   new GitMaterialAttributes("test",
                                                                             false,
                                                                             "https://example.com")));
    const json       = configRepoToSnakeCaseJSON(configRepo);
    expect(json.configuration).toHaveLength(0);
  });
});

describe('ConfigRepoCRUD', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make get request", (done) => {
    const api = SparkRoutes.apiConfigReposInternalPath();
    jasmine.Ajax.stubRequest(api).andReturn(getAllConfigRepos());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const configRepos  = (responseJSON.body as ConfigRepo[]);
      expect(configRepos).toHaveLength(2);
      expect(configRepos[0].id()).toBe("config-repo-id-1");
      expect(configRepos[1].id()).toBe("config-repo-id-2");
      done();
    });

    ConfigReposCRUD.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(api);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
  });

  it("should make a show request", (done) => {
    const api = SparkRoutes.ApiConfigRepoPath("config-repo-id-1");
    jasmine.Ajax.stubRequest(api).andReturn(getConfigRepo());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const configRepo   = (responseJSON.body as ObjectWithEtag<ConfigRepo>).object;
      expect(configRepo.id()).toBe("config-repo-id-1");
      expect(configRepo.material()!.materialUrl()).toBe("http://foo.com");
      done();
    });

    ConfigReposCRUD.get("config-repo-id-1").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(api);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
  });

  it("should make a create request", (done) => {
    const configRepo = ConfigRepo.fromJSON(JSON.parse(getConfigRepo().responseText));
    const api        = SparkRoutes.ApiConfigReposListPath();
    jasmine.Ajax.stubRequest(api).andReturn(getConfigRepo());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const configRepo   = (responseJSON.body as ObjectWithEtag<ConfigRepo>).object;
      expect(configRepo.id()).toBe("config-repo-id-1");
      expect(configRepo.material()!.materialUrl()).toBe("http://foo.com");
      done();
    });

    ConfigReposCRUD.create(configRepo).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(api);
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(configRepoToSnakeCaseJSON(configRepo));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
  });

  it("should make an update request", (done) => {
    const configRepo = ConfigRepo.fromJSON(JSON.parse(getConfigRepo().responseText));
    const api        = SparkRoutes.ApiConfigRepoPath("config-repo-id-1");
    jasmine.Ajax.stubRequest(api).andReturn(getConfigRepo());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const configRepo   = (responseJSON.body as ObjectWithEtag<ConfigRepo>).object;
      expect(configRepo.id()).toBe("config-repo-id-1");
      expect(configRepo.material()!.materialUrl()).toBe("http://foo.com");
      done();
    });

    ConfigReposCRUD.update({object: configRepo, etag: "some-etag"}).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(api);
    expect(request.method).toEqual("PUT");
    expect(request.data()).toEqual(configRepoToSnakeCaseJSON(configRepo));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
  });

  it("should make an delete request", (done) => {
    const configRepo = ConfigRepo.fromJSON(JSON.parse(getConfigRepo().responseText));
    const api        = SparkRoutes.ApiConfigRepoPath("config-repo-id-1");
    jasmine.Ajax.stubRequest(api).andReturn(deleteConfigRepoResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.message).toBe("Deleted Successfully!");
      done();
    });

    ConfigReposCRUD.delete(configRepo).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(api);
    expect(request.method).toEqual("DELETE");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
  });

  it("should make a trigger update request", () => {
    const api = SparkRoutes.configRepoTriggerUpdatePath("config-repo-id-1");
    jasmine.Ajax.stubRequest(api).andReturn(triggerUpdateResponse());

    ConfigReposCRUD.triggerUpdate("config-repo-id-1");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(api);
    expect(request.method).toEqual("POST");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
  });

  function getJSON() {
    return {
      _embedded: {
        config_repos: [
          {
            id: "config-repo-id-1",
            plugin_id: "yaml.config.plugin",
            material: {
              type: "git",
              attributes: {
                name: null,
                auto_update: true,
                url: "http://foo.com",
                branch: "master"
              }
            },
            can_administer: true,
            configuration: [],
            material_update_in_progress: false,
            parse_info: {
              latest_parsed_modification: {
                username: "username <username@googlegroups.com>",
                email_address: null,
                revision: "b07d423864ec120362b3584635c",
                comment: "some comment",
                modified_time: "2019-12-23T10:25:52Z"
              },
              good_modification: {
                username: "username <username@googlegroups.com>",
                email_address: null,
                revision: "b07d6523f1252ab4ec120362b3584635c",
                comment: "some comment",
                modified_time: "2019-12-23T10:25:52Z"
              },
              error: null
            }
          },
          {
            id: "config-repo-id-2",
            plugin_id: "yaml.config.plugin",
            material: {
              type: "git",
              attributes: {
                name: null,
                auto_update: true,
                url: "https://bar.com",
                branch: "master"
              }
            },
            can_administer: true,
            configuration: [],
            material_update_in_progress: false,
            parse_info: {
              latest_parsed_modification: {
                username: "username1 <27856297+username1@users.noreply.github.com>",
                email_address: null,
                revision: "f1a4bf2f85542d8f8c19ff9823e3b92",
                comment: "some major comment",
                modified_time: "2019-12-27T07:57:57Z"
              },
              good_modification: {
                username: "username1 <27856297+username1@users.noreply.github.com>",
                email_address: null,
                revision: "f1a4bf2f85542d8f8c19ff9823e3b92",
                comment: "some major comment",
                modified_time: "2019-12-27T07:57:57Z"
              },
              error: null
            }
          }]
      }
    };
  }

  function getAllConfigRepos() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify(getJSON())
    };
  }

  function getConfigRepo() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify({
                                     id: "config-repo-id-1",
                                     plugin_id: "yaml.config.plugin",
                                     material: {
                                       type: "git",
                                       attributes: {
                                         name: null,
                                         auto_update: true,
                                         url: "http://foo.com",
                                         branch: "master"
                                       }
                                     },
                                     can_administer: true,
                                     configuration: [],
                                     material_update_in_progress: false,
                                     parse_info: {
                                       latest_parsed_modification: {
                                         username: "username <username@googlegroups.com>",
                                         email_address: null,
                                         revision: "b07d423864ec120362b3584635c",
                                         comment: "some comment",
                                         modified_time: "2019-12-23T10:25:52Z"
                                       },
                                       good_modification: {
                                         username: "username <username@googlegroups.com>",
                                         email_address: null,
                                         revision: "b07d6523f1252ab4ec120362b3584635c",
                                         comment: "some comment",
                                         modified_time: "2019-12-23T10:25:52Z"
                                       },
                                       error: null
                                     }
                                   })
    };
  }

  function deleteConfigRepoResponse() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify({message: "Deleted Successfully!"})
    };
  }

  function triggerUpdateResponse() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify({message: "Updated triggered!"})
    };
  }
});
