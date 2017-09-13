/*
 * Copyright 2017 ThoughtWorks, Inc.
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
describe("PluginsWidget", () => {
  const $             = require("jquery");
  const m             = require("mithril");
  const Stream        = require("mithril/stream");
  const simulateEvent = require('simulate-event');

  require('jasmine-jquery');
  require('jasmine-ajax');

  const PluginsWidget = require("views/plugins/plugins_widget");
  const PluginInfos   = require('models/shared/plugin_infos');
  const Modal         = require('views/shared/new_modal');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const pluginSettingJSON = {
    "plugin_id":  "github.oauth.login",
    "configuration": [
      {
        "key":   "server_base_url",
        "value": "https://localhost:8154/go"
      },
      {
        "key":   "consumer_key",
        "value": "foo"
      }
    ]
  };

  const githubAuthPluginInfoJSON = {
    "id": "github.oauth.login",
    "type": "authentication",
    "status": {
      "state": "active"
    },
    "plugin_file_location": "/server/plugins/external/github-oauth-login-2.4.jar",
    "bundled_plugin": false,
    "about": {
      "name": "GitHub OAuth Login",
      "version": "2.4",
      "target_go_version": "16.2.1",
      "description": "GitHub OAuth Login",
      "target_operating_systems": [

      ],
      "vendor": {
        "name": "GoCD Contributors",
        "url": "https://github.com/gocd-contrib/gocd-oauth-login"
      }
    },
    "extension_info": {
      "plugin_settings": {
        "configurations": [
          {
            "key":      "server_base_url",
            "metadata": {
              "secure":   false,
              "required": true
            }
          },
          {
            "key":      "consumer_key",
            "metadata": {
              "secure":   false,
              "required": true
            }
          }
        ],
        "view": {
          "template": "plugin settings view"
        }
      }
    }
  };
  const githubScmPluginInfoJSON = {
    "id": "github.pr",
    "type": "scm",
    "status": {
      "state": "active"
    },
    "plugin_file_location": "/server/plugins/external/github-pr-poller-1.3.3.jar",
    "bundled_plugin": false,
    "about": {
      "name": "Github Pull Requests Builder",
      "version": "1.3.3",
      "target_go_version": "15.1.0",
      "description": "Plugin that polls a GitHub repository for pull requests and triggers a build for each of them",
      "target_operating_systems": [

      ],
      "vendor": {
        "name": "Ashwanth Kumar",
        "url": "https://github.com/ashwanthkumar/gocd-build-github-pull-requests"
      }
    },
    "extension_info": {
      "display_name": "GitHub",
      "scm_settings": {
        "configurations": [
          {
            "key": "url",
            "metadata": {
              "secure": false,
              "required": true,
              "part_of_identity": true
            }
          }
        ],
        "view": {
          "template": "scm settings view"
        }
      }
    }
  };

  const yumPluginInfoJSON = {
    "id": "yum",
    "type": null,
    "status": {
      "state": "invalid",
      "messages": ["Yum plugin not supported"]
    },
    "plugin_file_location": "/server/plugins/bundled/gocd-yum-repository-poller-plugin.jar",
    "bundled_plugin": true,
    "about": {
      "name": "Yum Plugin",
      "version": "2.0.3",
      "target_go_version": "15.2.0",
      "description": "Plugin that polls a yum repository",
      "target_operating_systems": [
        "Linux"
      ],
      "vendor": {
        "name": "ThoughtWorks Go Team",
        "url": "https://www.thoughtworks.com"
      }
    }
  };

  const allPluginInfosJSON = [githubAuthPluginInfoJSON, githubScmPluginInfoJSON, yumPluginInfoJSON];
  const allPluginInfos     = Stream(PluginInfos.fromJSON([]));

  beforeEach(() => {
    jasmine.Ajax.install();
    allPluginInfos(PluginInfos.fromJSON(allPluginInfosJSON));

    m.mount(root, {
      view() {
        return m(PluginsWidget, {
          pluginInfos: allPluginInfos
        });
      }
    });
    m.redraw(true);
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();

    m.mount(root, null);
    m.redraw();

    expect($('.new-modal-container .reveal')).not.toExist('Did you forget to close the modal before the test?');
  });

  describe("list all plugins", () => {
    it("should list active and bad plugins alike", () => {
      expect($root.find('.plugin .plugin-name')).toContainText(githubAuthPluginInfoJSON.about.name);
      expect($root.find('.plugin .plugin-name')).toContainText(githubScmPluginInfoJSON.about.name);
      expect($root.find('.plugin .plugin-name')).toContainText(yumPluginInfoJSON.about.name);

      expect($root.find('.plugin .plugin-version .value')).toContainText(githubAuthPluginInfoJSON.about.version);
      expect($root.find('.plugin .plugin-version .value')).toContainText(githubScmPluginInfoJSON.about.version);
      expect($root.find('.plugin .plugin-version .value')).toContainText(yumPluginInfoJSON.about.version);

      expect($root.find('.plugin .plugin-id')).toContainText(githubAuthPluginInfoJSON.id);
      expect($root.find('.plugin .plugin-id')).toContainText(githubScmPluginInfoJSON.id);
      expect($root.find('.plugin .plugin-id')).toContainText(yumPluginInfoJSON.id);
    });

    it("should show the additional information about loaded plugin", () => {
      expect($root.find('.plugin .plugin-config-read-only')).toContainText(githubAuthPluginInfoJSON.about.description);
      expect($root.find('.plugin .plugin-config-read-only')).toContainText(githubAuthPluginInfoJSON.about.vendor.name);
      expect($root.find('.plugin .plugin-config-read-only')).toContainText(githubAuthPluginInfoJSON.about.target_operating_systems);
      expect($root.find('.plugin .plugin-config-read-only')).toContainText(githubAuthPluginInfoJSON.plugin_file_location);
      expect($root.find('.plugin .plugin-config-read-only')).toContainText(githubAuthPluginInfoJSON.about.target_go_version);
    });

    it("should show the reason for why a plugin is not loaded", () => {
      expect($root.find('.plugin')).toContainText("Yum plugin not supported");
    });

  });

  describe("add/edit plugin settings", () => {
    afterEach(Modal.destroyAll);
    it("should popup a new modal to allow editing plugin settings", () => {
      jasmine.Ajax.stubRequest(`/go/api/admin/plugin_settings/${pluginSettingJSON.plugin_id}`, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(pluginSettingJSON),
        responseHeaders: {
          'ETag': '"foo"'
        },
        status:          200
      });
      expect($root.find('.reveal:visible')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.edit-plugin').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible')).toBeInDOM();
    });
  });
});
