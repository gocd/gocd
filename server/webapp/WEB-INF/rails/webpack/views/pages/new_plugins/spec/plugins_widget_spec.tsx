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

import * as m from "mithril";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginsWidget} from "../plugins_widget";

import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import * as headerIconStyles from "views/components/header_icon/index.scss";
import * as keyValuePairStyles from "views/components/key_value_pair/index.scss";

describe("New Plugins Widget", () => {
  const simulateEvent = require("simulate-event");

  const pluginInfos = [PluginInfo.fromJSON(getEAPluginInfo(), getEAPluginInfo()._links),
    PluginInfo.fromJSON(getNotificationPluginInfo(), undefined),
    PluginInfo.fromJSON(getYumPluginInfo(), undefined),
    PluginInfo.fromJSON(getInvalidPluginInfo(), undefined)
  ];

  let $root: any, root: any;
  beforeEach(() => {
    //@ts-ignore
    [$root, root] = window.createDomElementForTest();
    mount(pluginInfos);
  });

  afterEach(unmount);
  //@ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should render all plugin infos", () => {
    expect(find("plugins-list").get(0).children).toHaveLength(4);
  });

  it("should render plugin name and image", () => {
    expect(find("plugins-list").get(0).children).toHaveLength(4);

    expect(find("plugin-name").get(0)).toContainText(getEAPluginInfo().about.name);
    expect($root.find(`.${headerIconStyles.headerIcon} img`).get(0)).toHaveAttr("src", getEAPluginInfo()._links.image.href);
    expect(find("plugin-name").get(1)).toContainText(getNotificationPluginInfo().about.name);
  });

  it("should render plugin version", () => {
    expect(find("plugins-list").get(0).children).toHaveLength(4);

    const EAPluginHeader           = $root.find(`.${keyValuePairStyles.keyValuePair}`).get(0);
    const notificationPluginHeader = $root.find(`.${keyValuePairStyles.keyValuePair}`).get(2);

    expect(EAPluginHeader).toContainText("Version");
    expect(EAPluginHeader).toContainText(getEAPluginInfo().about.version);

    expect(EAPluginHeader).toContainText("Version");
    expect(notificationPluginHeader).toContainText(getNotificationPluginInfo().about.version);
  });

  it("should render all invalid plugin infos expanded", () => {
    expect(find("plugins-list").get(0).children).toHaveLength(4);

    const invalidPluginInfo = $root.find(`.${collapsiblePanelStyles.collapse}`).get(2);
    expect(invalidPluginInfo).toHaveClass(collapsiblePanelStyles.expanded);
  });

  it("should toggle expanded state of plugin infos on click", () => {
    expect(find("plugins-list").get(0).children).toHaveLength(4);

    const EAPluginInfoHeader           = find("collapse-header").get(0);
    const NotificationPluginInfoHeader = find("collapse-header").get(1);

    expect(EAPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
    expect(NotificationPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

    //expand ea plugin info
    simulateEvent.simulate(EAPluginInfoHeader, "click");
    m.redraw();

    expect(EAPluginInfoHeader).toHaveClass(collapsiblePanelStyles.expanded);
    expect(NotificationPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

    //expand notification plugin info
    simulateEvent.simulate(NotificationPluginInfoHeader, "click");
    m.redraw();

    expect(EAPluginInfoHeader).toHaveClass(collapsiblePanelStyles.expanded);
    expect(NotificationPluginInfoHeader).toHaveClass(collapsiblePanelStyles.expanded);

    //collapse both ea and notification plugin info
    simulateEvent.simulate(EAPluginInfoHeader, "click");
    simulateEvent.simulate(NotificationPluginInfoHeader, "click");
    m.redraw();

    expect(EAPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
    expect(NotificationPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
  });

  it("should render plugin infos information in collapsed body", () => {
    expect(find("plugins-list").get(0).children).toHaveLength(4);

    const EAPluginInfoHeader           = find("collapse-header").get(0);
    const NotificationPluginInfoHeader = find("collapse-header").get(1);
    simulateEvent.simulate(EAPluginInfoHeader, "click");
    simulateEvent.simulate(NotificationPluginInfoHeader, "click");

    const EAPluginInfoBody = find("collapse-body").get(0);

    expect(EAPluginInfoBody).toContainText("Id");
    expect(EAPluginInfoBody).toContainText(getEAPluginInfo().id);

    expect(EAPluginInfoBody).toContainText("Description");
    expect(EAPluginInfoBody).toContainText(getEAPluginInfo().about.description);

    expect(EAPluginInfoBody).toContainText("Author");
    expect(EAPluginInfoBody).toContainText(getEAPluginInfo().about.vendor.name);

    expect(EAPluginInfoBody).toContainText("Supported operating systems");
    expect(EAPluginInfoBody).toContainText("No restrictions");

    expect(EAPluginInfoBody).toContainText("Bundled");
    expect(EAPluginInfoBody).toContainText("No");

    expect(EAPluginInfoBody).toContainText("Target GoCD Version");
    expect(EAPluginInfoBody).toContainText("16.12.0");

    const NotificationPluginInfoBody = find("collapse-body").get(1);

    expect(NotificationPluginInfoBody).toContainText("Description");
    expect(NotificationPluginInfoBody).toContainText(getNotificationPluginInfo().about.description);

    expect(NotificationPluginInfoBody).toContainText("Author");
    expect(NotificationPluginInfoBody).toContainText(getNotificationPluginInfo().about.vendor.name);

    expect(NotificationPluginInfoBody).toContainText("Supported operating systems");
    expect(NotificationPluginInfoBody).toContainText("No restrictions");

    expect(NotificationPluginInfoBody).toContainText("Bundled");
    expect(NotificationPluginInfoBody).toContainText("No");

    expect(NotificationPluginInfoBody).toContainText("Target GoCD Version");
    expect(NotificationPluginInfoBody).toContainText("15.1.0");
  });

  it("should render plugin settings icon for plugins supporting settings", () => {
    expect(find("edit-plugin-settings").get(0)).toBeInDOM();
    expect(find("edit-plugin-settings").get(1)).toBeInDOM();
    expect(find("edit-plugin-settings")).toHaveLength(2);
  });

  it("should render status report link for ea plugins supporting status report", () => {
    expect(find("status-report-link").get(0)).toBeInDOM();
    expect(find("status-report-link")).toHaveLength(1);
  });

  it("should render error messages for plugins in invalid/error state", () => {
    const yumPluginInfoBody = find("collapse-body").get(3);
    expect(yumPluginInfoBody).toContainText("There were errors loading the plugin");
    expect(yumPluginInfoBody).toContainText("Plugin with ID (yum) is not valid: Incompatible with current operating system 'Mac OS X'. Valid operating systems are: [Linux].");
    expect($root.find(`.${collapsiblePanelStyles.collapse}`).get(3)).toHaveClass(collapsiblePanelStyles.error);
  });

  function mount(pluginInfos: any, isUserAdmin: boolean = true) {
    m.mount(root, {
      view() {
        return (
          <PluginsWidget isUserAnAdmin={isUserAdmin} pluginInfos={pluginInfos}/>
        );
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  function getEAPluginInfo() {
    return {
      _links: {
        image: {
          href: "some-image-link"
        }
      },
      id: "cd.go.contrib.elastic-agent.docker",
      status: {
        state: "active"
      },
      about: {
        name: "Docker Elastic Agent Plugin",
        version: "0.6.1",
        target_go_version: "16.12.0",
        description: "Docker Based Elastic Agent Plugins for GoCD",
        target_operating_systems: [],
        vendor: {
          name: "GoCD Contributors",
          url: "https://github.com/gocd-contrib/docker-elastic-agents"
        }
      },
      extensions: [
        {
          type: "elastic-agent",
          plugin_settings: {
            configurations: [
              {
                key: "instance_type",
                metadata: {
                  secure: false,
                  required: true
                }
              }
            ],
            view: {
              template: "elastic agent plugin settings view"
            }
          },
          profile_settings: {
            configurations: [
              {
                key: "Image",
                metadata: {
                  secure: false,
                  required: true
                }
              },
              {
                key: "Command",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "Environment",
                metadata: {
                  secure: false,
                  required: false
                }
              }
            ],
            view: {
              template: `<!--\n  ~ Copyright 2016 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Image].$error.server}\">Docker image:<span class="asterix">*</span></label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Image].$error.server}\" type=\"text\" ng-model=\"Image\" ng-required=\"true\" placeholder=\"alpine:latest\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Image].$error.server}\" ng-show=\"GOINPUTNAME[Image].$error.server\">{{GOINPUTNAME[Image].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Command].$error.server}\">Docker Command: <small>(Enter one parameter per line)</small></label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Command].$error.server}\" type=\"text\" ng-model=\"Command\" ng-required=\"true\" rows=\"7\" placeholder=\"ls&#x000A;-al&#x000A;/usr/bin\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Command].$error.server}\" ng-show=\"GOINPUTNAME[Command].$error.server\">{{GOINPUTNAME[Command].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Environment].$error.server}\">Environment Variables <small>(Enter one variable per line)</small></label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Environment].$error.server}\" type=\"text\" ng-model=\"Environment\" ng-required=\"true\" rows=\"7\" placeholder=\"JAVA_HOME=/opt/java&#x000A;MAVEN_HOME=/opt/maven\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Environment].$error.server}\" ng-show=\"GOINPUTNAME[Environment].$error.server\">{{GOINPUTNAME[Environment].$error.server}}</span>\n</div>\n`
            }
          },
          capabilities: {
            supports_status_report: true,
            supports_agent_status_report: true
          }
        }
      ]
    };
  }

  function getNotificationPluginInfo() {
    return {
      id: "github.pr.status",
      status: {
        state: "active"
      },
      about: {
        name: "GitHub Pull Requests status notifier",
        version: "1.2",
        target_go_version: "15.1.0",
        description: "Updates build status for GitHub pull request",
        target_operating_systems: [],
        vendor: {
          name: "Srinivas Upadhya",
          url: "https://github.com/srinivasupadhya/gocd-build-status-notifier"
        }
      },
      extensions: [
        {
          type: "notification",
          plugin_settings: {
            configurations: [
              {
                key: "hostname",
                metadata: {
                  secure: false,
                  required: true
                }
              }
            ],
            view: {
              template: "notification plugin view"
            }
          }
        }
      ]
    };
  }

  function getYumPluginInfo() {
    return {
      id: "yum",
      status: {
        state: "invalid",
        messages: [
          "Plugin with ID (yum) is not valid: Incompatible with current operating system 'Mac OS X'. Valid operating systems are: [Linux].",
        ]
      },
      plugin_file_location: "/Users/akshayd/projects/go/gocd/server/plugins/bundled/gocd-yum-repository-poller-plugin.jar",
      bundled_plugin: true,
      about: {
        name: "Yum Plugin",
        version: "2.0.3",
        target_go_version: "15.2.0",
        description: "Plugin that polls a yum repository",
        target_operating_systems: [
          "Linux"
        ],
        vendor: {
          name: "ThoughtWorks Go Team",
          url: "https://www.thoughtworks.com"
        }
      },
      extensions: []
    };
  }

  function getInvalidPluginInfo() {
    return {
      _links: {
        self: {
          href: "http://localhost:8153/go/api/admin/plugin_info/plugin-common.jar"
        },
        doc: {
          href: "https://api.gocd.org/#plugin-info"
        },
        find: {
          href: "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
        }
      },
      id: "plugin-common.jar",
      status: {
        state: "invalid",
        messages: [
          "No extensions found in this plugin.Please check for @Extension annotations"
        ]
      },
      plugin_file_location: "/Users/ganeshp/projects/gocd/gocd/server/plugins/external/plugin-common.jar",
      bundled_plugin: false,
      extensions: []
    };
  }

});
