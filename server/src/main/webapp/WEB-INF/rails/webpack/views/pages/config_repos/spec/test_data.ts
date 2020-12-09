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

import {ObjectCache} from "models/base/cache";
import {DefinedStructures} from "models/config_repos/defined_structures";
import {ConfigRepo} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {LinksJSON, PluginInfoJSON} from "models/shared/plugin_infos_new/serialization";
import { v4 as uuid } from 'uuid';

class MockCache implements ObjectCache<DefinedStructures> {
  ready: () => boolean;
  contents: () => DefinedStructures;
  failureReason: () => string | undefined;
  prime: (onSuccess: () => void, onError?: () => void) => void = jasmine.createSpy("cache.prime");
  invalidate: () => void                                       = jasmine.createSpy("cache.invalidate");

  constructor(options: MockedResultsData) {
    this.failureReason = () => options.failureReason;
    this.contents      = () => ("content" in options) ? options.content! : emptyTree();
    this.ready         = () => ("ready" in options) ? !!options.ready : true;
  }

  failed(): boolean {
    return !!this.failureReason();
  }
}

export interface MockedResultsData {
  content?: DefinedStructures;
  failureReason?: string;
  ready?: boolean;
}

export function emptyTree() {
  return new DefinedStructures([], []);
}

export function mockResultsCache(options: MockedResultsData) {
  return new MockCache(options);
}

export function createConfigRepoParsedWithError(overrides?: any): ConfigRepo {
  const parameters = {
    id: uuid(),
    repoId: uuid(),
    material_update_in_progress: false,
    latestCommitMessage: "Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d344fbbb9b1.",
    latestCommitUsername: "Mahesh <mahesh@gmail.com>",
    latestCommitRevision: "5432",
    rules: [],
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
                                   auto_update: ("auto_update" in parameters) ? parameters.auto_update : true,
                                   branch: "master",
                                   destination: ""
                                 }
                               },
                               can_administer: true,
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
                               material_update_in_progress: parameters.material_update_in_progress,
                               rules: parameters.rules
                             });
}

export function createConfigRepoParsed(overrides?: any): ConfigRepo {
  const parameters = {
    id: uuid(),
    repoId: uuid(),
    material_update_in_progress: false,
    latestCommitMessage: "Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d344fbbb9b1.",
    latestCommitUsername: "Mahesh <mahesh@gmail.com>",
    latestCommitRevision: "5432",
    rules: [],
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
                                   auto_update: ("auto_update" in parameters) ? parameters.auto_update : true,
                                   branch: "master",
                                   destination: ""
                                 }
                               },
                               can_administer: false,
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
                               material_update_in_progress: parameters.material_update_in_progress,
                               rules: parameters.rules
                             });
}

export function createConfigRepoWithError(id?: string, repoId?: string): ConfigRepo {
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
                               can_administer: false,
                               configuration: [{
                                 key: "file_pattern",
                                 value: "*.json"
                               }],
                               parse_info: {
                                 error: "blah!"
                               },
                               id,
                               plugin_id: "json.config.plugin",
                               material_update_in_progress: false,
                               rules: []
                             });
}

export function configRepoPluginInfo() {
  const links: LinksJSON = {
    image: {
      href: "http://localhost:8153/go/api/plugin_images/json.config.plugin/f787"
    }
  };

  const pluginInfoWithConfigRepoExtension: PluginInfoJSON = {
    _links: links,
    id: "json.config.plugin",
    status: {
      state: "active"
    },
    bundled_plugin: false,
    plugin_file_location: '/tmp/foo.jar',
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
        capabilities: {
          supports_parse_content: true,
          supports_pipeline_export: true,
          supports_list_config_files: false,
          supports_user_defined_properties: true,
        },
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

  return PluginInfo.fromJSON(pluginInfoWithConfigRepoExtension);
}
