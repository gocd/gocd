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

import {ConfigRepo} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as uuid from "uuid/v4";

export function createConfigRepoParsedWithError(overrides?: any) {
  const parameters = {
    id: uuid(),
    repoId: uuid(),
    material_update_in_progress: false,
    latestCommitMessage: "Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d344fbbb9b1.",
    latestCommitUsername: "Mahesh <mahesh@gmail.com>",
    latestCommitRevision: "5432",
    ...overrides
  };

  return ConfigRepo.fromJSON({
    material: {
      type: "git",
      attributes: {
        url: "https://example.com/git/" + (parameters.repoId),
        name: "foo",
        username: "bob",
        encrypted_password: "AES:foo:bar",
        auto_update: true,
        branch: "master",
        destination: ""
      }
    },
    configuration: [{
      key: "file_pattern",
      value: "*.json"
    }],
    parse_info: {
      latest_parsed_modification: {
        username: parameters.latestCommitUsername,
        email_address: "mahesh@gmail.com",
        revision: parameters.latestCommitRevision,
        comment: parameters.latestCommitMessage,
        modified_time: "2019-01-14T05:39:40Z"
      },
      good_modification: {
        username: "GaneshSPatil <ganeshpl@gmail.com>",
        email_address: "ganeshpl@gmail.com",
        revision: "1234",
        comment: "Revert \"Delete this\"\n\nThis reverts commit 9b402012ea5c24ce032c8ef4582c0a9ce2d14ade.",
        modified_time: "2019-01-11T11:24:08Z"
      },
      error: "blah!"
    },
    id: parameters.id,
    plugin_id: "json.config.plugin",
    material_update_in_progress: parameters.material_update_in_progress
  });
}

export function createConfigRepoParsed(overrides?: any) {
  const parameters = {
    id: uuid(),
    repoId: uuid(),
    material_update_in_progress: false,
    latestCommitMessage: "Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d344fbbb9b1.",
    latestCommitUsername: "Mahesh <mahesh@gmail.com>",
    latestCommitRevision: "5432",
    ...overrides
  };

  return ConfigRepo.fromJSON({
    material: {
      type: "git",
      attributes: {
        url: "https://example.com/git/" + (parameters.repoId),
        name: "foo",
        username: "bob",
        encrypted_password: "AES:foo:bar",
        auto_update: true,
        branch: "master",
        destination: ""
      }
    },
    configuration: [{
      key: "file_pattern",
      value: "*.json"
    }],
    parse_info: {
      latest_parsed_modification: {
        username: parameters.latestCommitUsername,
        email_address: "mahesh@gmail.com",
        revision: parameters.latestCommitRevision,
        comment: parameters.latestCommitMessage,
        modified_time: "2019-01-14T05:39:40Z"
      },
      good_modification: {
        username: parameters.latestCommitUsername,
        email_address: "mahesh@gmail.com",
        revision: parameters.latestCommitRevision,
        comment: parameters.latestCommitMessage,
        modified_time: "2019-01-14T05:39:40Z"
      }
    },
    id: parameters.id,
    plugin_id: "json.config.plugin",
    material_update_in_progress: parameters.material_update_in_progress
  });
}

export function createConfigRepoWithError(id?: string, repoId?: string) {
  id = id || uuid();
  return ConfigRepo.fromJSON({
                               material: {
                                 type: "git",
                                 attributes: {
                                   url: "https://example.com/git/" + (repoId || uuid()),
                                   name: "foo",
                                   auto_update: true,
                                   branch: "master",
                                   destination: ""
                                 }
                               },
                               configuration: [{
                                 key: "file_pattern",
                                 value: "*.json"
                               }],
                               parse_info: {
                                 error: "blah!"
                               },
                               id,
                               plugin_id: "json.config.plugin",
                               material_update_in_progress: false
                             });
}

export function configRepoPluginInfo() {
  const links                             = {
    self: {
      href: "http://localhost:8153/go/api/admin/plugin_info/json.config.plugin"
    },
    doc: {
      href: "https://api.gocd.org/#plugin-info"
    },
    find: {
      href: "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
    },
    image: {
      href: "http://localhost:8153/go/api/plugin_images/json.config.plugin/f787"
    }
  };
  const pluginInfoWithConfigRepoExtension = {
    id: "json.config.plugin",
    status: {
      state: "active"
    },
    about: {
      name: "JSON Configuration Plugin",
      version: "0.2",
      target_go_version: "16.1.0",
      description: "Configuration plugin that supports Go configuration in JSON",
      target_operating_systems: [],
      vendor: {
        name: "Tomasz Setkowski",
        url: "https://github.com/tomzo/gocd-json-config-plugin"
      }
    },
    extensions: [
      {
        type: "configrepo",
        plugin_settings: {
          configurations: [
            {
              key: "pipeline_pattern",
              metadata: {
                required: false,
                secure: false
              }
            }
          ],
          view: {
            template: "config repo plugin view"
          }
        }
      }
    ]
  };

  return PluginInfo.fromJSON(pluginInfoWithConfigRepoExtension, links);
}
