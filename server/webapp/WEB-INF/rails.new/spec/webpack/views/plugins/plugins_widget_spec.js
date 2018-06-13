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
describe("PluginsWidget", () => {
  const $             = require("jquery");
  const m             = require("mithril");
  const Stream        = require("mithril/stream");
  const simulateEvent = require('simulate-event');

  require('jasmine-jquery');
  require('jasmine-ajax');

  const PluginsWidget = require("views/plugins/plugins_widget");
  const Modal         = require('views/shared/new_modal');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const configRepoPluginInfoJSON = {
    "id":                   "json.config.plugin",
    "status":               {
      "state": "active"
    },
    "plugin_file_location": "/server/plugins/external/json.config.plugin-0.2.1.jar",
    "bundled_plugin":       true,
    "about":                {
      "name":                     "JSON Config Plugin",
      "version":                  "0.2",
      "target_go_version":        "16.6.0",
      "description":              "JSON Config Plugin",
      "target_operating_systems": [],
      "vendor":                   {
        "name": "GoCD Contributors",
        "url":  "https://github.com/tomzo/json-config-plugin"
      }
    },
    "extensions": [
      {
        "type": "configrepo",
        "plugin_settings": {
          "configurations": [
            {
              "key":      "pattern",
              "metadata": {
                "secure":   false,
                "required": false
              }
            }
          ],
          "view":           {
            "template": "plugin settings view"
          }
        }
      }
    ]
  };

  const elasticAgentPluginInfoJSON = {
    "id":             "cd.go.contrib.elastic-agent.docker",
    "status":         {
      "state": "active"
    },
    "about":          {
      "name":                     "Docker Elastic Agent Plugin",
      "version":                  "0.6.1",
      "target_go_version":        "16.12.0",
      "description":              "Docker Based Elastic Agent Plugins for GoCD",
      "target_operating_systems": [],
      "vendor":                   {
        "name": "GoCD Contributors",
        "url":  "https://github.com/gocd-contrib/docker-elastic-agents"
      }
    },
    "extensions": [
      {
        "type": "elastic-agent",
        "plugin_settings":  {
          "configurations": [
            {
              "key":      "instance_type",
              "metadata": {
                "secure":   false,
                "required": true
              }
            }
          ],
          "view":           {
            "template": "elastic agent plugin settings view"
          }
        },
        "profile_settings": {
          "configurations": [
            {
              "key":      "Image",
              "metadata": {
                "secure":   false,
                "required": true
              }
            }
          ],
          "view":           {
            "template": 'elastic-profile-view'
          }
        },
        "capabilities":     {
          "supports_status_report": true
        }
      }
    ]
  };
  const pluginInfosUrl             = '/go/api/admin/plugin_info?include_bad=true';

  describe('functionality', () => {
    const pluginSettingJSON = {
      "plugin_id":     "github.oauth.login",
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
      "id":                   "github.oauth.login",
      "status":               {
        "state": "active"
      },
      "plugin_file_location": "/server/plugins/external/github-oauth-login-2.4.jar",
      "bundled_plugin":       false,
      "about":                {
        "name":                     "GitHub OAuth Login",
        "version":                  "2.4",
        "target_go_version":        "16.2.1",
        "description":              "GitHub OAuth Login",
        "target_operating_systems": [],
        "vendor":                   {
          "name": "GoCD Contributors",
          "url":  "https://github.com/gocd-contrib/gocd-oauth-login"
        }
      },
      "extensions": [
        {
          "type": "authorization",
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
            "view":           {
              "template": "plugin settings view"
            }
          }
        }
      ]
    };
    const githubScmPluginInfoJSON  = {
      "id":                   "github.pr",
      "status":               {
        "state": "active"
      },
      "plugin_file_location": "/server/plugins/external/github-pr-poller-1.3.3.jar",
      "bundled_plugin":       false,
      "about":                {
        "name":                     "Github Pull Requests Builder",
        "version":                  "1.3.3",
        "target_go_version":        "15.1.0",
        "description":              "Plugin that polls a GitHub repository for pull requests and triggers a build for each of them",
        "target_operating_systems": [],
        "vendor":                   {
          "name": "Ashwanth Kumar",
          "url":  "https://github.com/ashwanthkumar/gocd-build-github-pull-requests"
        }
      },
      "extensions": [
        {
          "type": "scm",
          "display_name": "GitHub",
          "scm_settings": {
            "configurations": [
              {
                "key":      "url",
                "metadata": {
                  "secure":           false,
                  "required":         true,
                  "part_of_identity": true
                }
              }
            ],
            "view":           {
              "template": "scm settings view"
            }
          }
        }
      ]
    };

    const yumPluginInfoJSON = {
      "id":                   "yum",
      "type":                 null,
      "status":               {
        "state":    "invalid",
        "messages": ["Yum plugin not supported"]
      },
      "plugin_file_location": "/server/plugins/bundled/gocd-yum-repository-poller-plugin.jar",
      "bundled_plugin":       true,
      "about":                {
        "name":                     "Yum Plugin",
        "version":                  "2.0.3",
        "target_go_version":        "15.2.0",
        "description":              "Plugin that polls a yum repository",
        "target_operating_systems": [
          "Linux"
        ],
        "vendor":                   {
          "name": "ThoughtWorks Go Team",
          "url":  "https://www.thoughtworks.com"
        }
      }
    };

    const allPluginInfosJSON = {
      "_embedded": {
        "plugin_info": [githubAuthPluginInfoJSON, githubScmPluginInfoJSON, yumPluginInfoJSON, elasticAgentPluginInfoJSON]
      }
    };
    const isUserAnAdmin      = Stream('true' === 'true');

    beforeEach(() => {
      jasmine.Ajax.install();

      jasmine.Ajax.stubRequest(pluginInfosUrl, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(allPluginInfosJSON),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/json'
        }
      });

      m.mount(root, {
        view() {
          return m(PluginsWidget, {isUserAnAdmin});
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
        expect($root.find('.plugin-actions')).toContainElement('a.edit-plugin');
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

      it("should popup a new modal to allow creating plugin settings", () => {
        jasmine.Ajax.stubRequest(`/go/api/admin/plugin_settings/${pluginSettingJSON.plugin_id}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify({message: 'Not found'}),
          status:          404
        });
        expect($root.find('.reveal:visible')).not.toBeInDOM();

        simulateEvent.simulate($root.find('.edit-plugin').get(0), 'click');
        m.redraw();
        expect($('.reveal:visible')).toBeInDOM();
      });
    });
  });

  describe('for an admin', () => {
    const allPluginInfosJSON = {
      "_embedded": {
        "plugin_info": [configRepoPluginInfoJSON, elasticAgentPluginInfoJSON]
      }
    };
    const isUserAnAdmin      = Stream('true' === 'true');
    beforeEach(() => {
      jasmine.Ajax.install();

      jasmine.Ajax.stubRequest(pluginInfosUrl, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(allPluginInfosJSON),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/json'
        }
      });

      m.mount(root, {
        view() {
          return m(PluginsWidget, {isUserAnAdmin});
        }
      });
      m.redraw(true);
    });

    afterEach(() => {
      m.mount(root, null);
      m.redraw();
    });

    it("should show settings icon for admin", () => {
      expect($root.find('.plugin .plugin-name')).toContainText(configRepoPluginInfoJSON.about.name);
      expect($root.find('.plugin-actions')).toContainElement('a.edit-plugin');
    });

    it("should show status report button for admin", () => {
      const pluginWithStatusReport = $root.find('.plugin .plugin-actions a.status-report-btn').parents('.plugin');
      expect(pluginWithStatusReport.find('.plugin-name')).toContainText(elasticAgentPluginInfoJSON.about.name);
      expect(pluginWithStatusReport.find('.plugin-name')).not.toContainText(configRepoPluginInfoJSON.about.name);
    });
  });

  describe('for a non-admin', () => {
    const allPluginInfosJSON = {
      "_embedded": {
        "plugin_info": [configRepoPluginInfoJSON]
      }
    };
    const isUserAnAdmin      = Stream('false' === 'true');


    beforeEach(() => {
      jasmine.Ajax.install();

      jasmine.Ajax.stubRequest(pluginInfosUrl, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(allPluginInfosJSON),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/json'
        }
      });

      m.mount(root, {
        view() {
          return m(PluginsWidget, {isUserAnAdmin});
        }
      });
      m.redraw(true);
    });

    afterEach(() => {
      m.mount(root, null);
      m.redraw();
    });

    it("should show the disabled settings icon for admin", () => {
      expect($root.find('.plugin .plugin-name')).toContainText(configRepoPluginInfoJSON.about.name);
      expect($root.find('.plugin-actions')).toContainElement('a.edit-plugin.disabled');
    });
  });
});
