/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import m from "mithril";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {LinksJSON, NotificationExtensionJSON, PluginInfoJSON} from "models/shared/plugin_infos_new/serialization";
import {pluginImageLink} from "models/shared/plugin_infos_new/spec/test_data";
import * as simulateEvent from "simulate-event";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import * as headerIconStyles from "views/components/header_icon/index.scss";
import {TestHelper} from "views/pages/spec/test_helper";
import {PluginsWidget} from "../plugins_widget";

describe("New Plugins Widget", () => {

  const pluginInfos = new PluginInfos(PluginInfo.fromJSON(getEAPluginInfo()),
                                      PluginInfo.fromJSON(getNotificationPluginInfo()),
                                      PluginInfo.fromJSON(getYumPluginInfo()),
                                      PluginInfo.fromJSON(getInvalidPluginInfo()),
                                      PluginInfo.fromJSON(getEAPluginInfoSupportingClusterProfile())
  );

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  beforeEach(() => {
    helper.mount(() => <PluginsWidget isUserAnAdmin={true} pluginInfos={pluginInfos}/>);
  });

  it("should render all plugin infos", () => {
    expect(helper.byTestId("plugins-list").children).toHaveLength(5);
  });

  it("should render plugin name and image", () => {
    expect(helper.byTestId("plugins-list").children).toHaveLength(5);

    expect(helper.textByTestId("plugin-name")).toContain(getEAPluginInfo().about!.name);
    expect(helper.q(`.${headerIconStyles.headerIcon} img`)).toHaveAttr("src", getEAPluginInfo()._links.image!.href);
    expect(helper.allByTestId("plugin-name").item(2).textContent).toContain(getNotificationPluginInfo().about!.name);
  });

  it("should render plugin version", () => {
    expect(helper.byTestId("plugins-list").children).toHaveLength(5);

    const EAPluginHeader           = helper.allByTestId("collapse-header").item(0);
    const notificationPluginHeader = helper.allByTestId("collapse-header").item(2);

    expect(helper.textByTestId("key-value-key-version", EAPluginHeader)).toContain("Version");
    expect(helper.textByTestId("key-value-value-version", EAPluginHeader)).toContain(getEAPluginInfo().about!.version);

    expect(helper.textByTestId("key-value-key-version", notificationPluginHeader)).toContain("Version");
    expect(helper.textByTestId("key-value-value-version", notificationPluginHeader)).toContain(getNotificationPluginInfo().about!.version);
  });

  it("should render all invalid plugin infos expanded", () => {
    expect(helper.byTestId("plugins-list").children).toHaveLength(5);

    const invalidPluginInfo = helper.qa(`.${collapsiblePanelStyles.collapse}`).item(3);
    expect(invalidPluginInfo).toHaveClass(collapsiblePanelStyles.expanded);
  });

  it("should toggle expanded state of plugin infos on click", () => {
    expect(helper.byTestId("plugins-list").children).toHaveLength(5);

    const EAPluginInfoHeader           = helper.allByTestId("collapse-header").item(0);
    const NotificationPluginInfoHeader = helper.allByTestId("collapse-header").item(1);

    expect(EAPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
    expect(NotificationPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

    //expand ea plugin info
    simulateEvent.simulate(EAPluginInfoHeader, "click");
    m.redraw.sync();

    expect(EAPluginInfoHeader).toHaveClass(collapsiblePanelStyles.expanded);
    expect(NotificationPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

    //expand notification plugin info
    simulateEvent.simulate(NotificationPluginInfoHeader, "click");
    m.redraw.sync();

    expect(EAPluginInfoHeader).toHaveClass(collapsiblePanelStyles.expanded);
    expect(NotificationPluginInfoHeader).toHaveClass(collapsiblePanelStyles.expanded);

    //collapse both ea and notification plugin info
    simulateEvent.simulate(EAPluginInfoHeader, "click");
    simulateEvent.simulate(NotificationPluginInfoHeader, "click");
    m.redraw.sync();

    expect(EAPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
    expect(NotificationPluginInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
  });

  it("should render plugin infos information in collapsed body", () => {
    expect(helper.byTestId("plugins-list").children).toHaveLength(5);

    const EAPluginInfoHeader           = helper.allByTestId("collapse-header").item(0);
    const NotificationPluginInfoHeader = helper.allByTestId("collapse-header").item(2);
    simulateEvent.simulate(EAPluginInfoHeader, "click");
    simulateEvent.simulate(NotificationPluginInfoHeader, "click");

    const EAPluginInfoBody = helper.byTestId("collapse-body");

    expect(EAPluginInfoBody).toContainText("Id");
    expect(EAPluginInfoBody).toContainText(getEAPluginInfo().id);

    expect(EAPluginInfoBody).toContainText("Description");
    expect(EAPluginInfoBody).toContainText(getEAPluginInfo().about!.description);

    expect(EAPluginInfoBody).toContainText("Author");
    expect(EAPluginInfoBody).toContainText(getEAPluginInfo().about!.vendor.name);

    expect(EAPluginInfoBody).toContainText("Supported operating systems");
    expect(EAPluginInfoBody).toContainText("No restrictions");

    expect(EAPluginInfoBody).toContainText("Bundled");
    expect(EAPluginInfoBody).toContainText("No");

    expect(EAPluginInfoBody).toContainText("Target GoCD Version");
    expect(EAPluginInfoBody).toContainText("16.12.0");

    const NotificationPluginInfoBody = helper.allByTestId("collapse-body").item(2);

    expect(NotificationPluginInfoBody).toContainText("Description");
    expect(NotificationPluginInfoBody).toContainText(getNotificationPluginInfo().about!.description);

    expect(NotificationPluginInfoBody).toContainText("Author");
    expect(NotificationPluginInfoBody).toContainText(getNotificationPluginInfo().about!.vendor.name);

    expect(NotificationPluginInfoBody).toContainText("Supported operating systems");
    expect(NotificationPluginInfoBody).toContainText("No restrictions");

    expect(NotificationPluginInfoBody).toContainText("Bundled");
    expect(NotificationPluginInfoBody).toContainText("No");

    expect(NotificationPluginInfoBody).toContainText("Target GoCD Version");
    expect(NotificationPluginInfoBody).toContainText("15.1.0");
  });

  it("should render plugin settings icon for plugins supporting settings", () => {
    expect(helper.allByTestId("edit-plugin-settings")).toHaveLength(2);
    expect(helper.allByTestId("edit-plugin-settings").item(0)).toBeInDOM();
    expect(helper.allByTestId("edit-plugin-settings").item(1)).toBeInDOM();
  });

  it("should render status report link for ea plugins supporting status report", () => {
    expect(helper.allByTestId("status-report-link")).toHaveLength(2);
    expect(helper.byTestId("status-report-link")).toBeInDOM();
  });

  it("should render error messages for plugins in invalid/error state", () => {
    const yumPluginInfoBody = helper.allByTestId("collapse-body").item(4);
    expect(yumPluginInfoBody.textContent).toContain("There were errors loading the plugin");
    expect(yumPluginInfoBody.textContent).toContain("Plugin with ID (yum) is not valid: Incompatible with current operating system 'Mac OS X'. Valid operating systems are: [Linux].");
    expect(helper.qa(`.${collapsiblePanelStyles.collapse}`).item(3)).toHaveClass(collapsiblePanelStyles.error);
  });

  it("should display deprecation message for elastic agent plugins not supporting cluster profile", () => {
    expect(helper.textByTestId("collapse-header")).toContain(getEAPluginInfo().about!.name);
    expect(helper.byTestId("deprecation-warning-icon", helper.byTestId("collapse-header"))).toBeInDOM();
    expect(helper.byTestId("collapse-header")).toHaveClass(collapsiblePanelStyles.warning);
    expect(helper.textByTestId("deprecation-warning-tooltip-content", helper.byTestId("collapse-header")))
      .toBe("Version 0.6.1 of plugin is deprecated as it does not support ClusterProfiles. This version of plugin will stop working in upcoming release of GoCD, update to latest version of the plugin.");
  });

  it("should not display deprecation message for elastic agent plugins supporting cluster profile", () => {
    expect(helper.allByTestId("collapse-header").item(1).textContent).toContain(getEAPluginInfoSupportingClusterProfile().about!.name);
    expect(helper.byTestId("deprecation-warning-icon", helper.allByTestId("collapse-header").item(1))).toBeFalsy();
    expect(helper.byTestId("deprecation-warning-tooltip-content", helper.allByTestId("collapse-header").item(1))).toBeFalsy();
  });

  it("should not display deprecation message for plugins not using elastic agent extension", () => {
    expect(helper.allByTestId("collapse-header").item(4).textContent).toContain(getYumPluginInfo().about!.name);
    expect(helper.byTestId("deprecation-warning-icon", helper.allByTestId("collapse-header").item(4))).toBeFalsy();
    expect(helper.byTestId("deprecation-warning-tooltip-content", helper.allByTestId("collapse-header").item(4))).toBeFalsy();
  });

  function getEAPluginInfo(): PluginInfoJSON {
    return {
      _links: {
        image: {
          href: "some-image-link"
        }
      },
      id: "cd.go.contrib.elastic-agent.docker",
      plugin_file_location: '/tmp/foo.jar',
      bundled_plugin: false,
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
          elastic_agent_profile_settings: {
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
              template: `<!--\n  ~ Copyright 2021 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Image].$error.server}\">Docker image:<span class="asterix">*</span></label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Image].$error.server}\" type=\"text\" ng-model=\"Image\" ng-required=\"true\" placeholder=\"alpine:latest\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Image].$error.server}\" ng-show=\"GOINPUTNAME[Image].$error.server\">{{GOINPUTNAME[Image].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Command].$error.server}\">Docker Command: <small>(Enter one parameter per line)</small></label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Command].$error.server}\" type=\"text\" ng-model=\"Command\" ng-required=\"true\" rows=\"7\" placeholder=\"ls&#x000A;-al&#x000A;/usr/bin\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Command].$error.server}\" ng-show=\"GOINPUTNAME[Command].$error.server\">{{GOINPUTNAME[Command].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Environment].$error.server}\">Environment Variables <small>(Enter one variable per line)</small></label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Environment].$error.server}\" type=\"text\" ng-model=\"Environment\" ng-required=\"true\" rows=\"7\" placeholder=\"JAVA_HOME=/opt/java&#x000A;MAVEN_HOME=/opt/maven\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Environment].$error.server}\" ng-show=\"GOINPUTNAME[Environment].$error.server\">{{GOINPUTNAME[Environment].$error.server}}</span>\n</div>\n`
            }
          },
          capabilities: {
            supports_plugin_status_report: true,
            supports_cluster_status_report: true,
            supports_agent_status_report: true
          }
        }
      ]
    };
  }

  function getNotificationPluginInfo(): PluginInfoJSON {
    const extension: NotificationExtensionJSON = {
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
    };

    return {
      _links: pluginImageLink(),
      id: "github.pr.status",
      status: {
        state: "active"
      },
      plugin_file_location: '/tmp/foo.jar',
      bundled_plugin: false,
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
        extension
      ]
    };
  }

  function getYumPluginInfo(): PluginInfoJSON {
    return {
      _links: pluginImageLink(),
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

  function getInvalidPluginInfo(): PluginInfoJSON {
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
      } as LinksJSON,
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
    } as PluginInfoJSON;
  }

  function getEAPluginInfoSupportingClusterProfile(): PluginInfoJSON {
    return {
      _links: {
        image: {
          href: "some-image-link"
        }
      },
      plugin_file_location: '/tmp/foo.jar',
      bundled_plugin: false,
      id: "cd.go.contrib.elasticagent.kubernetes",
      status: {
        state: "active"
      },
      about: {
        name: "Kubernetes Elastic Agent Plugin",
        version: "2.0.0-113",
        target_go_version: "18.10.0",
        description: "Kubernetes Based Elastic Agent Plugins for GoCD",
        target_operating_systems: [],
        vendor: {
          name: "GoCD Contributors",
          url: "https://github.com/gocd-contrib/kubernetes-elastic-agent"
        }
      },
      extensions: [
        {
          type: "elastic-agent",
          supports_cluster_profiles: true,
          cluster_profile_settings: {
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
          elastic_agent_profile_settings: {
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
              template: `<!--\n  ~ Copyright 2021 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Image].$error.server}\">Docker image:<span class="asterix">*</span></label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Image].$error.server}\" type=\"text\" ng-model=\"Image\" ng-required=\"true\" placeholder=\"alpine:latest\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Image].$error.server}\" ng-show=\"GOINPUTNAME[Image].$error.server\">{{GOINPUTNAME[Image].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Command].$error.server}\">Docker Command: <small>(Enter one parameter per line)</small></label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Command].$error.server}\" type=\"text\" ng-model=\"Command\" ng-required=\"true\" rows=\"7\" placeholder=\"ls&#x000A;-al&#x000A;/usr/bin\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Command].$error.server}\" ng-show=\"GOINPUTNAME[Command].$error.server\">{{GOINPUTNAME[Command].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Environment].$error.server}\">Environment Variables <small>(Enter one variable per line)</small></label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Environment].$error.server}\" type=\"text\" ng-model=\"Environment\" ng-required=\"true\" rows=\"7\" placeholder=\"JAVA_HOME=/opt/java&#x000A;MAVEN_HOME=/opt/maven\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Environment].$error.server}\" ng-show=\"GOINPUTNAME[Environment].$error.server\">{{GOINPUTNAME[Environment].$error.server}}</span>\n</div>\n`
            }
          },
          capabilities: {
            supports_plugin_status_report: true,
            supports_cluster_status_report: true,
            supports_agent_status_report: true
          }
        }
      ]
    };
  }
});
