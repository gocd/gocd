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

import * as _ from "lodash";
import * as stream from "mithril/stream";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as simulateEvent from "simulate-event";
import {ModalManager} from "views/components/modal/modal_manager";
import * as styles from "views/pages/config_repos/index.scss";
import {NewConfigRepoModal} from "views/pages/config_repos/modals";

describe("ConfigReposModal", () => {
  let originalTimeout: number;

  beforeEach(() => {
    originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 5000;
  });

  afterEach(() => {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
    ModalManager.closeAll();
  });

  it("should render error message and exclamation icon when connection is not successful", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {message: "Error while parsing material URL"};

      jasmine.Ajax.stubRequest(TEST_CONNECTION_URL, undefined, "POST")
             .andReturn({
                          responseText: JSON.stringify(response),
                          status: 422,
                          responseHeaders: {
                            "Content-Type": "application/vnd.go.cd.v2+json",
                            "ETag": "ETag"
                          }
                        });

      const modal = new NewConfigRepoModal(_.noop, _.noop, stream(PLUGIN_INFOS));
      modal.render();

      expect(find('test-connection-button')).toBeVisible();
      simulateEvent.simulate(find("test-connection-button") as Element, "click");
      expect(jasmine.Ajax.requests.count()).toEqual(1);

      setTimeout(() => {
        expect(find("test-connection-icon")).toHaveClass(styles.testConnectionFailure);
        expect(find("flash-message-alert")!.querySelector("pre")).toContainText("Error while parsing material URL");
        done();
      }, 2000);
    });
  });

  it("should render success icon when connection is successful", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {message: "Connection OK."};

      jasmine.Ajax.stubRequest(TEST_CONNECTION_URL, undefined, "POST")
             .andReturn({
                          responseText: JSON.stringify(response),
                          status: 200,
                          responseHeaders: {
                            "Content-Type": "application/vnd.go.cd.v2+json",
                            "ETag": "ETag"
                          }
                        });

      const modal = new NewConfigRepoModal(_.noop, _.noop, stream(PLUGIN_INFOS));
      modal.render();

      expect(find('test-connection-button')).toBeVisible();
      simulateEvent.simulate(find("test-connection-button") as Element, "click");
      expect(jasmine.Ajax.requests.count()).toEqual(1);

      setTimeout(() => {
        expect(find("test-connection-icon")).toHaveClass(styles.testConnectionSuccess);
        done();
      }, 2000);
    });
  });

  function find(id: string) {
    return document.querySelector(`[data-test-id='${id}']`);
  }

  const TEST_CONNECTION_URL = "/go/api/admin/internal/material_test";
  const pluginInfo = {
    id: "json.config.plugin",
    status: {
      state: "active"
    },
    plugin_file_location: "/Users/projects/gocd/server/plugins/bundled/gocd-json-config-plugin.jar",
    bundled_plugin: true,
    about: {
      name: "JSON Configuration Plugin",
      version: "0.3.6",
      target_go_version: "18.12.0",
      description: "Configuration plugin that supports GoCD configuration in JSON",
      target_operating_systems: [],
      vendor: {
        name: "Test User",
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
                secure: false,
                required: false
              }
            },
            {
              key: "environment_pattern",
              metadata: {
                secure: false,
                required: false
              }
            }
          ],
          view: {
            template: "<div class=\"form_item_block\">\n    <label>GoCD pipeline files global pattern:</label>\n    <input type=\"text\" ng-model=\"pipeline_pattern\" ng-required=\"false\" placeholder=\"**/*.gopipeline.json\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[pipeline_pattern].$error.server\">{{ GOINPUTNAME[pipeline_pattern].$error.server }}</span>\n</div>\n<div class=\"form_item_block\">\n    <label>GoCD environment files global pattern:</label>\n    <input type=\"text\" ng-model=\"environment_pattern\" ng-required=\"false\" placeholder=\"**/*.goenvironment.json\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[environment_pattern].$error.server\">{{ GOINPUTNAME[environment_pattern].$error.server }}</span>\n</div>\n"
          }
        },
        capabilities: {
          supports_pipeline_export: true,
          supports_parse_content: true
        }
      }
    ]
  };
  const PLUGIN_INFOS = [PluginInfo.fromJSON(pluginInfo)];
});
