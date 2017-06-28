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

describe('PluginInfos', () => {

  const PluginInfos = require("models/shared/plugin_infos");
  const _           = require("lodash");

  it("should deserialize collection", () => {
    const json = [
      {
        "id":      "github.oauth.login",
        "type":    "authentication",
        "status":  {
          "state": "active"
        },
        "about":   {
          "name":                     "GitHub OAuth Login",
          "version":                  "2.2",
          "target_go_version":        "16.2.1",
          "description":              "Login using GitHub OAuth",
          "target_operating_systems": ['Linux', 'Mac'],
          "vendor":                   {
            "name": "GoCD Contributors",
            "url":  "https://github.com/gocd-contrib/gocd-oauth-login"
          }
        }
      }
    ];

    const pluginInfos = PluginInfos.fromJSON(json);
    expect(pluginInfos.countPluginInfo()).toBe(1);
    expect(pluginInfos.firstPluginInfo().id()).toBe('github.oauth.login');
  });

  describe("Authentication", () => {
    it("should deserialize", () => {
      const json = {
        "id":      "github.oauth.login",
        "type":    "authentication",
        "status":  {
          "state": "active"
        },
        "about":   {
          "name":                     "GitHub OAuth Login",
          "version":                  "2.2",
          "target_go_version":        "16.2.1",
          "description":              "Login using GitHub OAuth",
          "target_operating_systems": ['Linux', 'Mac'],
          "vendor":                   {
            "name": "GoCD Contributors",
            "url":  "https://github.com/gocd-contrib/gocd-oauth-login"
          }
        },
        "extension_info": {
          "plugin_settings": {
            "configurations": [
              {
                "key": "server_base_url",
                "metadata": {
                  "secure": false,
                  "required": true
                }
              }
            ],
            "view": {
              "template": "authentication plugin view"
            }
          }
        }
      };

      const pluginInfo = PluginInfos.PluginInfo.fromJSON(json);
      verifyBasicProperties(pluginInfo, json);

      expect(pluginInfo.pluginSettings().viewTemplate()).toEqual(json.extension_info.plugin_settings.view.template);
      expect(pluginInfo.pluginSettings().configurations().countConfiguration()).toEqual(1);
      expect(pluginInfo.pluginSettings().configurations().collectConfigurationProperty('key')).toEqual(['server_base_url']);
      expect(pluginInfo.pluginSettings().configurations().firstConfiguration().metadata()).toEqual({
        secure:   false,
        required: true
      });

    });
  });

  describe("ElasticAgent", () => {
    it("should deserialize", () => {
      const json = {
        "id":               "cd.go.contrib.elastic-agent.docker",
        "type":             "elastic-agent",
        "status":           {
          "state": "active"
        },
        "about":            {
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
        "extension_info": {
          "plugin_settings": {
            "configurations": [
              {
                "key": "instance_type",
                "metadata": {
                  "secure": false,
                  "required": true
                }
              }
            ],
            "view": {
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
              },
              {
                "key":      "Command",
                "metadata": {
                  "secure":   false,
                  "required": false
                }
              },
              {
                "key":      "Environment",
                "metadata": {
                  "secure":   false,
                  "required": false
                }
              }
            ],
            "view":           {
              "template": '<!--\n  ~ Copyright 2016 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the "License");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an "AS IS" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n\n<div class="form_item_block">\n    <label ng-class="{\'is-invalid-label\': GOINPUTNAME[Image].$error.server}">Docker image:<span class=\'asterix\'>*</span></label>\n    <input ng-class="{\'is-invalid-input\': GOINPUTNAME[Image].$error.server}" type="text" ng-model="Image" ng-required="true" placeholder="alpine:latest"/>\n    <span class="form_error form-error" ng-class="{\'is-visible\': GOINPUTNAME[Image].$error.server}" ng-show="GOINPUTNAME[Image].$error.server">{{GOINPUTNAME[Image].$error.server}}</span>\n</div>\n\n<div class="form_item_block">\n    <label ng-class="{\'is-invalid-label\': GOINPUTNAME[Command].$error.server}">Docker Command: <small>(Enter one parameter per line)</small></label>\n    <textarea ng-class="{\'is-invalid-input\': GOINPUTNAME[Command].$error.server}" type="text" ng-model="Command" ng-required="true" rows="7" placeholder="ls&#x000A;-al&#x000A;/usr/bin"></textarea>\n    <span class="form_error form-error" ng-class="{\'is-visible\': GOINPUTNAME[Command].$error.server}" ng-show="GOINPUTNAME[Command].$error.server">{{GOINPUTNAME[Command].$error.server}}</span>\n</div>\n\n<div class="form_item_block">\n    <label ng-class="{\'is-invalid-label\': GOINPUTNAME[Environment].$error.server}">Environment Variables <small>(Enter one variable per line)</small></label>\n    <textarea ng-class="{\'is-invalid-input\': GOINPUTNAME[Environment].$error.server}" type="text" ng-model="Environment" ng-required="true" rows="7" placeholder="JAVA_HOME=/opt/java&#x000A;MAVEN_HOME=/opt/maven"></textarea>\n    <span class="form_error form-error" ng-class="{\'is-visible\': GOINPUTNAME[Environment].$error.server}" ng-show="GOINPUTNAME[Environment].$error.server">{{GOINPUTNAME[Environment].$error.server}}</span>\n</div>\n'
            }
          },
          "capabilities":     {
            "supports_status_report": true
          }
        }
      };

      const pluginInfo = PluginInfos.PluginInfo.fromJSON(json);
      verifyBasicProperties(pluginInfo, json);

      expect(pluginInfo.profileSettings().viewTemplate()).toEqual(json.extension_info.profile_settings.view.template);
      expect(pluginInfo.profileSettings().configurations().countConfiguration()).toEqual(3);
      expect(pluginInfo.profileSettings().configurations().collectConfigurationProperty('key')).toEqual(['Image', 'Command', 'Environment']);
      expect(pluginInfo.profileSettings().configurations().firstConfiguration().metadata()).toEqual({
        secure:   false,
        required: true
      });
      expect(pluginInfo.capabilities().supportsStatusReport()).toBeTruthy();

      expect(pluginInfo.pluginSettings().viewTemplate()).toEqual(json.extension_info.plugin_settings.view.template);
      expect(pluginInfo.pluginSettings().configurations().countConfiguration()).toEqual(1);
      expect(pluginInfo.pluginSettings().configurations().collectConfigurationProperty('key')).toEqual(['instance_type']);
      expect(pluginInfo.pluginSettings().configurations().firstConfiguration().metadata()).toEqual({
        secure:   false,
        required: true
      });
    });
  });

  describe("Notification", () => {
    it("should deserialize", () => {
      const json = {
        "id":      "github.pr.status",
        "type":    "notification",
        "status":  {
          "state": "active"
        },
        "about":   {
          "name":                     "GitHub Pull Requests status notifier",
          "version":                  "1.2",
          "target_go_version":        "15.1.0",
          "description":              "Updates build status for GitHub pull request",
          "target_operating_systems": [],
          "vendor":                   {
            "name": "Srinivas Upadhya",
            "url":  "https://github.com/srinivasupadhya/gocd-build-status-notifier"
          }
        },
        "extension_info": {
          "plugin_settings": {
            "configurations": [
              {
                "key": "hostname",
                "metadata": {
                  "secure": false,
                  "required": true
                }
              }
            ],
            "view": {
              "template": "notification plugin view"
            }
          }
        }
      };

      const pluginInfo = PluginInfos.PluginInfo.fromJSON(json);
      verifyBasicProperties(pluginInfo, json);

      expect(pluginInfo.pluginSettings().viewTemplate()).toEqual(json.extension_info.plugin_settings.view.template);
      expect(pluginInfo.pluginSettings().configurations().countConfiguration()).toEqual(1);
      expect(pluginInfo.pluginSettings().configurations().collectConfigurationProperty('key')).toEqual(['hostname']);
      expect(pluginInfo.pluginSettings().configurations().firstConfiguration().metadata()).toEqual({
        secure:   false,
        required: true
      });
    });
  });

  describe("PackageRepository", () => {
    it("should deserialize", () => {
      const json = {
        "id":                  "nuget",
        "type":                "package-repository",
        "status":              {
          "state": "active"
        },
        "about":               {
          "name":                     "Nuget plugin",
          "version":                  "1.0.0",
          "target_go_version":        "15.3.0",
          "description":              "Plugin that polls a Nuget Server using the new API",
          "target_operating_systems": [],
          "vendor":                   {
            "name": "ThoughtWorks Go Plugin Team",
            "url":  "www.thoughtworks.com"
          }
        },
        "extension_info": {
          "package_settings":    {
            "configurations": [
              {
                "key":      "PACKAGE_ID",
                "metadata": {
                  "part_of_identity": true,
                  "display_order":    0,
                  "secure":           false,
                  "display_name":     "Package ID",
                  "required":         true
                }
              },
              {
                "key":      "POLL_VERSION_FROM",
                "metadata": {
                  "part_of_identity": false,
                  "display_order":    1,
                  "secure":           false,
                  "display_name":     "Version to poll >=",
                  "required":         false
                }
              },
              {
                "key":      "POLL_VERSION_TO",
                "metadata": {
                  "part_of_identity": false,
                  "display_order":    2,
                  "secure":           false,
                  "display_name":     "Version to poll <",
                  "required":         false
                }
              },
              {
                "key":      "INCLUDE_PRE_RELEASE",
                "metadata": {
                  "part_of_identity": false,
                  "display_order":    3,
                  "secure":           false,
                  "display_name":     "Include Prerelease? (yes/no, defaults to yes)",
                  "required":         false
                }
              }
            ]
          },
          "repository_settings": {
            "configurations": [
              {
                "key":      "REPO_URL",
                "metadata": {
                  "part_of_identity": true,
                  "display_order":    0,
                  "secure":           false,
                  "display_name":     "Repository Url",
                  "required":         true
                }
              },
              {
                "key":      "USERNAME",
                "metadata": {
                  "part_of_identity": false,
                  "display_order":    1,
                  "secure":           false,
                  "display_name":     "Username",
                  "required":         false
                }
              },
              {
                "key":      "PASSWORD",
                "metadata": {
                  "part_of_identity": false,
                  "display_order":    2,
                  "secure":           true,
                  "display_name":     "Password (use only with https)",
                  "required":         false
                }
              }
            ]
          }
        }
      };

      const pluginInfo = PluginInfos.PluginInfo.fromJSON(json);
      verifyBasicProperties(pluginInfo, json);

      expect(pluginInfo.packageSettings().configurations().countConfiguration()).toEqual(4);
      expect(pluginInfo.packageSettings().configurations().collectConfigurationProperty('key')).toEqual(['PACKAGE_ID', 'POLL_VERSION_FROM', 'POLL_VERSION_TO', 'INCLUDE_PRE_RELEASE']);
      expect(pluginInfo.packageSettings().configurations().firstConfiguration().metadata()).toEqual({
        "part_of_identity": true,
        "display_order":    0,
        "secure":           false,
        "display_name":     "Package ID",
        "required":         true
      });

      expect(pluginInfo.repositorySettings().configurations().countConfiguration()).toEqual(3);
      expect(pluginInfo.repositorySettings().configurations().collectConfigurationProperty('key')).toEqual(['REPO_URL', 'USERNAME', 'PASSWORD']);
      expect(pluginInfo.repositorySettings().configurations().firstConfiguration().metadata()).toEqual({
        "part_of_identity": true,
        "display_order":    0,
        "secure":           false,
        "display_name":     "Repository Url",
        "required":         true
      });
    });
  });

  describe("Task", () => {
    it("should deserialize", () => {
      const json = {
        "id":            "docker-task",
        "type":          "task",
        "status":        {
          "state": "active"
        },
        "about":         {
          "name":                     "Docker Task",
          "version":                  "0.1.27",
          "target_go_version":        "14.4.0",
          "description":              "Docker task to build, run with and push images",
          "target_operating_systems": [
            "Linux",
            "Mac OS X"
          ],
          "vendor":                   {
            "name": "manojlds",
            "url":  "www.stacktoheap.com"
          }
        },
        "extension_info": {
          "display_name":  "Docker Task",
          "task_settings": {
            "configurations": [
              {
                "key":      "DockerFile",
                "metadata": {
                  "secure":   false,
                  "required": false
                }
              },
              {
                "key":      "DockerRunArguments",
                "metadata": {
                  "secure":   false,
                  "required": false
                }
              },
              {
                "key":      "IsDockerPush",
                "metadata": {
                  "secure":   false,
                  "required": true
                }
              },
              {
                "key":      "DockerBuildTag",
                "metadata": {
                  "secure":   false,
                  "required": false
                }
              }
            ],
            "view":           {
              "template": "<div class=\"form_item_block\">\n    <div class=\"checkbox_row\">\n        <input id=\"IsDockerBuild2\" type=\"checkbox\" ng-model=\"IsDockerBuild2\" ng-init=\"IsDockerBuild2 = IsDockerBuild\" ng-change=\"IsDockerBuild = IsDockerBuild2\" ng-true-value=\"true\" ng-false-value=\"false\">\n        <input id=\"IsDockerBuild\" type=\"hidden\" ng-model=\"IsDockerBuild\" value=\"{{IsDockerBuild}}\">\n        <label for=\"IsDockerBuild\">Build docker image</label>\n    </div>\n</div>\n<div class=\"form_item_block\">\n    <div class=\"docker_build\" ng-show=\"IsDockerBuild == 'true'\">\n        <div class=\"form_item_block\" >\n            <label>Dockerfile path:<span class=\"asterisk\">*</span></label>\n            <input type=\"text\" ng-model=\"DockerFile\">\n            <span class=\"form_error\" ng-show=\"GOINPUTNAME[DockerFile].$error.server\">{{GOINPUTNAME[DockerFile].$error.server}}</span>\n        </div>\n        <div class=\"form_item_block\" >\n            <label>Tag name:</label>\n            <input type=\"text\" ng-model=\"DockerBuildTag\">\n        </div>\n        <div class=\"checkbox_row\" ng-show=\"DockerBuildTag\">\n            <input id=\"TagWithPipelineLabel2\" type=\"checkbox\" ng-model=\"TagWithPipelineLabel2\" ng-init=\"TagWithPipelineLabel2 = TagWithPipelineLabel\" ng-change=\"TagWithPipelineLabel = TagWithPipelineLabel2\" ng-true-value=\"true\" ng-false-value=\"false\">\n            <input id=\"TagWithPipelineLabel\" type=\"hidden\" ng-model=\"TagWithPipelineLabel\" value=\"{{TagWithPipelineLabel}}\">\n            <label for=\"TagWithPipelineLabel\">Include pipeline label (tag_name:GO_PIPELINE_LABEL)</label>\n        </div>\n    </div>\n</div>\n<div class=\"form_item_block\">\n    <div class=\"checkbox_row\">\n        <input id=\"IsDockerRun2\" type=\"checkbox\" ng-model=\"IsDockerRun2\" ng-init=\"IsDockerRun2 = IsDockerRun\" ng-change=\"IsDockerRun = IsDockerRun2\" ng-true-value=\"true\" ng-false-value=\"false\">\n        <input id=\"IsDockerRun\" type=\"hidden\" ng-model=\"IsDockerRun\" value=\"{{IsDockerRun}}\">\n        <label for=\"IsDockerRun\">Run command in docker image</label>\n    </div>\n</div>\n<div class=\"form_item_block\">\n    <div class=\"docker_run\" ng-show=\"IsDockerRun == 'true'\">\n        <div class=\"form_item_block\" >\n            <label>Script path:<span class=\"asterisk\">*</span></label>\n            <input type=\"text\" ng-model=\"DockerRunScript\">\n            <span class=\"form_error\" ng-show=\"GOINPUTNAME[DockerRunScript].$error.server\">{{GOINPUTNAME[DockerRunScript].$error.server}}</span>\n        </div>\n        <div class=\"form_item_block\" >\n            <label>Arguments (one per line):</label>\n            <textarea ng-model=\"DockerRunArguments\" wrap=\"off\" style=\"width: 250px; resize: both;\" cols=\"58\" autocomplete=\"off\"></textarea>\n        </div>\n    </div>\n</div>\n<div class=\"form_item_block\">\n    <div class=\"checkbox_row\">\n        <input id=\"IsDockerPush2\" type=\"checkbox\" ng-model=\"IsDockerPush2\" ng-init=\"IsDockerPush2 = IsDockerPush\" ng-change=\"IsDockerPush = IsDockerPush2\" ng-true-value=\"true\" ng-false-value=\"false\">\n        <input id=\"IsDockerPush\" type=\"hidden\" ng-model=\"IsDockerPush\" value=\"{{IsDockerPush}}\">\n        <label for=\"IsDockerPush\">Push image to registry</label>\n    </div>\n</div>\n<div class=\"form_item_block\">\n    <div class=\"docker_push\" ng-show=\"IsDockerPush == 'true'\">\n        <div class=\"form_item_block\" >\n            <label>Docker hub user:<span class=\"asterisk\">*</span></label>\n            <input type=\"text\" ng-model=\"DockerPushUser\">\n            <span class=\"form_error\" ng-show=\"GOINPUTNAME[DockerPushUser].$error.server\">{{GOINPUTNAME[DockerPushUser].$error.server}}</span>\n        </div>\n        <div class=\"checkbox_row\" ng-show=\"DockerBuildTag\">\n            <input id=\"RemoveAfterPush2\" type=\"checkbox\" ng-model=\"RemoveAfterPush2\" ng-init=\"RemoveAfterPush2 = RemoveAfterPush\" ng-change=\"RemoveAfterPush = RemoveAfterPush2\" ng-true-value=\"true\" ng-false-value=\"false\">\n            <input id=\"RemoveAfterPush\" type=\"hidden\" ng-model=\"RemoveAfterPush\" value=\"{{RemoveAfterPush}}\">\n            <label for=\"RemoveAfterPush\">Remove local image after push</label>\n        </div>\n    </div>\n</div>\n"
            }
          }
        }
      };

      const pluginInfo = PluginInfos.PluginInfo.fromJSON(json);
      verifyBasicProperties(pluginInfo, json);

      expect(pluginInfo.taskSettings().viewTemplate()).toEqual(json.extension_info.task_settings.view.template);
      expect(pluginInfo.taskSettings().configurations().countConfiguration()).toEqual(4);
      expect(pluginInfo.taskSettings().configurations().collectConfigurationProperty('key')).toEqual(['DockerFile', 'DockerRunArguments', 'IsDockerPush', 'DockerBuildTag']);
      expect(pluginInfo.taskSettings().configurations().firstConfiguration().metadata()).toEqual({
        secure:   false,
        required: false
      });
    });
  });

  describe("SCM", () => {
    it("should deserialize", () => {
      const json = {
        "id":           "github.pr",
        "type":         "scm",
        "status":       {
          "state": "active"
        },
        "about":        {
          "name":                     "Github Pull Requests Builder",
          "version":                  "1.3.0-RC2",
          "target_go_version":        "15.1.0",
          "description":              "Plugin that polls a GitHub repository for pull requests and triggers a build for each of them",
          "target_operating_systems": [],
          "vendor":                   {
            "name": "Ashwanth Kumar",
            "url":  "https://github.com/ashwanthkumar/gocd-build-github-pull-requests"
          }
        },
        "extension_info": {
          "display_name": "GitHub",
          "scm_settings": {
            "configurations": [
              {
                "key":      "url",
                "metadata": {
                  "part_of_identity": true,
                  "secure":           false,
                  "required":         true
                }
              },
              {
                "key":      "username",
                "metadata": {
                  "part_of_identity": false,
                  "secure":           false,
                  "required":         false
                }
              },
              {
                "key":      "password",
                "metadata": {
                  "part_of_identity": false,
                  "secure":           true,
                  "required":         false
                }
              }
            ],
            "view":           {
              "template": "<div class=\"form_item_block\">\n    <label>URL:<span class=\"asterisk\">*</span></label>\n    <input type=\"text\" ng-model=\"url\" ng-required=\"true\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[url].$error.server\">{{ GOINPUTNAME[url].$error.server }}</span>\n</div>\n<div class=\"form_item_block\">\n    <label>Username:</label>\n    <input type=\"text\" ng-model=\"username\" ng-required=\"false\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[username].$error.server\">{{ GOINPUTNAME[username].$error.server }}</span>\n</div>\n<div class=\"form_item_block\">\n    <label>Password:</label>\n    <input type=\"password\" ng-model=\"password\" ng-required=\"false\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[password].$error.server\">{{ GOINPUTNAME[password].$error.server }}</span>\n</div>"
            }
          }
        }
      };

      const pluginInfo = PluginInfos.PluginInfo.fromJSON(json);
      verifyBasicProperties(pluginInfo, json);

      expect(pluginInfo.scmSettings().viewTemplate()).toEqual(json.extension_info.scm_settings.view.template);
      expect(pluginInfo.scmSettings().configurations().countConfiguration()).toEqual(3);
      expect(pluginInfo.scmSettings().configurations().collectConfigurationProperty('key')).toEqual(['url', 'username', 'password']);
      expect(pluginInfo.scmSettings().configurations().firstConfiguration().metadata()).toEqual({
        "part_of_identity": true,
        "secure":           false,
        "required":         true
      });
    });
  });

  describe("Authorization", () => {
    it("should deserialize", () => {
      const json = {
        "id":                   "cd.go.authorization.ldap",
        "type":                 "authorization",
        "status":               {
          "state": "active"
        },
        "about":                {
          "name":                     "LDAP Authorization Plugin for GoCD",
          "version":                  "0.0.1",
          "target_go_version":        "16.12.0",
          "description":              "LDAP Authorization Plugin for GoCD",
          "target_operating_systems": [],
          "vendor":                   {
            "name": "ThoughtWorks, Inc. & GoCD Contributors",
            "url":  "https://github.com/gocd/gocd-ldap-authorization-plugin"
          }
        },
        "extension_info": {
          "auth_config_settings": {
            "configurations": [
              {
                "key":      "Url",
                "metadata": {
                  "secure":   false,
                  "required": true
                }
              },
              {
                "key":      "SearchBases",
                "metadata": {
                  "secure":   false,
                  "required": true
                }
              },
              {
                "key":      "ManagerDN",
                "metadata": {
                  "secure":   false,
                  "required": true
                }
              },
              {
                "key":      "Password",
                "metadata": {
                  "secure":   true,
                  "required": true
                }
              },
            ],
            "view":           {
              "template": "<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Url].$error.server}\">URI:<span class='asterix'>*</span></label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Url].$error.server}\" type=\"text\" ng-model=\"Url\" ng-required=\"true\" placeholder=\"ldap://your.first.uri:port\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Url].$error.server}\" ng-show=\"GOINPUTNAME[Url].$error.server\">{{GOINPUTNAME[Url].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[SearchBases].$error.server}\">Search Base:<span class='asterix'>*</span></label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[SearchBases].$error.server}\" type=\"text\" ng-model=\"SearchBases\" ng-required=\"true\" rows=\"3\" placeholder=\"\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[SearchBases].$error.server}\" ng-show=\"GOINPUTNAME[SearchBases].$error.server\">{{GOINPUTNAME[SearchBases].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[ManagerDN].$error.server}\">Manager DN:<span class='asterix'>*</span></label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[ManagerDN].$error.server}\" type=\"text\" ng-model=\"ManagerDN\" ng-required=\"true\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[ManagerDN].$error.server}\" ng-show=\"GOINPUTNAME[ManagerDN].$error.server\">{{GOINPUTNAME[ManagerDN].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Password].$error.server}\">Password:<span class='asterix'>*</span></label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Password].$error.server}\" type=\"password\" ng-model=\"Password\" ng-required=\"true\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Password].$error.server}\" ng-show=\"GOINPUTNAME[Password].$error.server\">{{GOINPUTNAME[Password].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n  <label ng-class=\"{'is-invalid-label': GOINPUTNAME[LoginAttribute].$error.server}\">Search Filter:<span class='asterix'>*</span></label>\n  <input ng-class=\"{'is-invalid-input': GOINPUTNAME[LoginAttribute].$error.server}\" type=\"text\" ng-model=\"LoginAttribute\" ng-required=\"true\"/>\n  <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[LoginAttribute].$error.server}\" ng-show=\"GOINPUTNAME[LoginAttribute].$error.server\">{{GOINPUTNAME[LoginAttribute].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[SearchAttributes].$error.server}\">Search Attributes:</label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[SearchAttributes].$error.server}\" type=\"text\" ng-model=\"SearchAttributes\" ng-required=\"true\" placeholder=\"uid,cn,mail\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[SearchAttributes].$error.server}\" ng-show=\"GOINPUTNAME[SearchAttributes].$error.server\">{{GOINPUTNAME[SearchAttributes].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n  <label ng-class=\"{'is-invalid-label': GOINPUTNAME[DisplayNameAttribute].$error.server}\">Display Name Attribute:<span class='asterix'>*</span></label>\n  <input ng-class=\"{'is-invalid-input': GOINPUTNAME[DisplayNameAttribute].$error.server}\" type=\"text\" ng-model=\"DisplayNameAttribute\" ng-required=\"true\"/>\n  <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[DisplayNameAttribute].$error.server}\" ng-show=\"GOINPUTNAME[DisplayNameAttribute].$error.server\">{{GOINPUTNAME[DisplayNameAttribute].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n  <label ng-class=\"{'is-invalid-label': GOINPUTNAME[EmailAttribute].$error.server}\">Email Attribute:<span class='asterix'>*</span></label>\n  <input ng-class=\"{'is-invalid-input': GOINPUTNAME[EmailAttribute].$error.server}\" type=\"text\" ng-model=\"EmailAttribute\" ng-required=\"true\"/>\n  <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[EmailAttribute].$error.server}\" ng-show=\"GOINPUTNAME[EmailAttribute].$error.server\">{{GOINPUTNAME[EmailAttribute].$error.server}}</span>\n</div>\n\n\n\n"
            }
          },
          "role_settings":        {
            "configurations": [
              {
                "key":      "AttributeName",
                "metadata": {
                  "secure":   false,
                  "required": false
                }
              },
              {
                "key":      "AttributeValue",
                "metadata": {
                  "secure":   false,
                  "required": false
                }
              },
              {
                "key":      "GroupMembershipFilter",
                "metadata": {
                  "secure":   false,
                  "required": false
                }
              },
              {
                "key":      "GroupMembershipSearchBase",
                "metadata": {
                  "secure":   false,
                  "required": false
                }
              }
            ],
            "view":           {
              "template": "<div class=\"row\">\n  <h4>Query user attribute</h4>\n  <div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[AttributeName].$error.server}\">Attribute name</label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[AttributeName].$error.server}\" type=\"text\" ng-model=\"AttributeName\" ng-required=\"true\" placeholder=\"OU=foo,dc=example,dc=com\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[AttributeName].$error.server}\" ng-show=\"GOINPUTNAME[AttributeName].$error.server\">{{GOINPUTNAME[AttributeName].$error.server}}</span>\n  </div>\n\n  <div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[AttributeValue].$error.server}\">Attribute value</label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[AttributeValue].$error.server}\" type=\"text\" ng-model=\"AttributeValue\" ng-required=\"true\" placeholder=\"OU=foo,dc=example,dc=com\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[AttributeValue].$error.server}\" ng-show=\"GOINPUTNAME[AttributeValue].$error.server\">{{GOINPUTNAME[AttributeValue].$error.server}}</span>\n  </div>\n</div>\n\n<div>\n  <h4>Use group membership filter</h4>\n  <div class=\"row\">\n    <div class=\"form_item_block\">\n      <label ng-class=\"{'is-invalid-label': GOINPUTNAME[GroupMembershipSearchBase].$error.server}\">Group membership search base</label>\n      <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[GroupMembershipSearchBase].$error.server}\" type=\"text\" ng-model=\"GroupMembershipSearchBase\" ng-required=\"true\" placeholder=\"OU=foo,dc=example,dc=com\"></textarea>\n      <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[GroupMembershipSearchBase].$error.server}\" ng-show=\"GOINPUTNAME[GroupMembershipSearchBase].$error.server\">{{GOINPUTNAME[GroupMembershipSearchBase].$error.server}}</span>\n    </div>\n  </div>\n\n  <div class=\"row\">\n    <div class=\"form_item_block\">\n      <label ng-class=\"{'is-invalid-label': GOINPUTNAME[GroupMembershipFilter].$error.server}\">Group membership filter</label>\n      <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[GroupMembershipFilter].$error.server}\" type=\"text\" ng-model=\"GroupMembershipFilter\" ng-required=\"true\" placeholder=\"OU=foo,dc=example,dc=com\"></textarea>\n      <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[GroupMembershipFilter].$error.server}\" ng-show=\"GOINPUTNAME[GroupMembershipFilter].$error.server\">{{GOINPUTNAME[GroupMembershipFilter].$error.server}}</span>\n    </div>\n  </div>\n</div>\n\n\n\n"
            }
          },
          "capabilities":         {
            "can_authorize":       true,
            "can_search":          false,
            "supported_auth_type": 'web'
          }
        }
      };

      const pluginInfo = PluginInfos.PluginInfo.fromJSON(json);
      verifyBasicProperties(pluginInfo, json);

      expect(pluginInfo.authConfigSettings().viewTemplate()).toEqual(json.extension_info.auth_config_settings.view.template);
      expect(pluginInfo.authConfigSettings().configurations().countConfiguration()).toEqual(4);
      expect(pluginInfo.authConfigSettings().configurations().collectConfigurationProperty('key')).toEqual(['Url', 'SearchBases', 'ManagerDN', 'Password']);
      expect(pluginInfo.authConfigSettings().configurations().firstConfiguration().metadata()).toEqual({
        secure:   false,
        required: true
      });

      expect(pluginInfo.roleSettings().viewTemplate()).toEqual(json.extension_info.role_settings.view.template);
      expect(pluginInfo.roleSettings().configurations().countConfiguration()).toEqual(4);
      expect(pluginInfo.roleSettings().configurations().collectConfigurationProperty('key')).toEqual(['AttributeName', 'AttributeValue', 'GroupMembershipFilter', 'GroupMembershipSearchBase']);
      expect(pluginInfo.roleSettings().configurations().firstConfiguration().metadata()).toEqual({
        secure:   false,
        required: false
      });

      expect(pluginInfo.capabilities().canAuthorize()).toBeTruthy();
      expect(pluginInfo.capabilities().canSearch()).toBeFalsy();
      expect(pluginInfo.capabilities().supportedAuthType()).toEqual('web');
    });
  });

  describe("ConfigRepo", () => {
    it("should deserialize", () => {
      const json = {
        "id":      "json.config.plugin",
        "type":    "configrepo",
        "status":  {
          "state": "active"
        },
        "about":   {
          "name":                     "JSON Configuration Plugin",
          "version":                  "0.2",
          "target_go_version":        "16.1.0",
          "description":              "Configuration plugin that supports Go configuration in JSON",
          "target_operating_systems": [],
          "vendor":                   {
            "name": "Tomasz Setkowski",
            "url":  "https://github.com/tomzo/gocd-json-config-plugin"
          }
        },
        "extension_info": {
          "plugin_settings": {
            "configurations": [
              {
                "key": "pipeline_pattern",
                "metadata": {
                  "required": false,
                  "secure": false
                }
              }
            ],
            "view": {
              "template": "config repo plugin view"
            }
          }
        }
      };

      const pluginInfo = PluginInfos.PluginInfo.fromJSON(json);
      verifyBasicProperties(pluginInfo, json);

      expect(pluginInfo.pluginSettings().viewTemplate()).toEqual(json.extension_info.plugin_settings.view.template);
      expect(pluginInfo.pluginSettings().configurations().countConfiguration()).toEqual(1);
      expect(pluginInfo.pluginSettings().configurations().collectConfigurationProperty('key')).toEqual(['pipeline_pattern']);
      expect(pluginInfo.pluginSettings().configurations().firstConfiguration().metadata()).toEqual({
        secure:   false,
        required: false
      });

    });
  });

  describe("Reading images", () => {
    const json = {
      "_links":  {
        "image": {
          "href": "http://localhost:8153/go/api/plugin_images/cd.go.contrib.elastic-agent.ecs/ff36b7db1762e22ea7523980d90ffa5759bc7f08393be910601f15bfea1f4ca6"
        }
      },
      "id":      "github.pr",
      "status":  {
        "state": "active"
      },
      "about":   {
        "name":                     "GitHub Pull Requests Builder",
        "version":                  "1.3.0-RC2",
        "target_go_version":        "15.1.0",
        "description":              "Plugin that polls a GitHub repository for pull requests and triggers a build for each of them",
        "target_operating_systems": [],
        "vendor":                   {
          "name": "Ashwanth Kumar",
          "url":  "https://github.com/ashwanthkumar/gocd-build-github-pull-requests"
        }
      },
    };

    _.each(_.keys(PluginInfos.Types), (pluginType) => {
      it(`should read image for ${pluginType}`, () => {
        const pluginInfoJSON = _.cloneDeep(json);
        pluginInfoJSON.type  = pluginType;
        const pluginInfo     = PluginInfos.PluginInfo.fromJSON(pluginInfoJSON);
        expect(pluginInfo.imageUrl()).toBe(json._links.image.href);
      });
    });
  });

  const verifyBasicProperties = (pluginInfo, {id, type, about, status}) => {
    expect(pluginInfo.id()).toEqual(id);
    expect(pluginInfo.type()).toEqual(type);
    expect(pluginInfo.status().state()).toEqual(status.state);
    expect(pluginInfo.status().messages()).toEqual(status.messages);
    expect(pluginInfo.about().name()).toEqual(about.name);
    expect(pluginInfo.about().version()).toEqual(about.version);
    expect(pluginInfo.about().targetGoVersion()).toEqual(about.target_go_version);
    expect(pluginInfo.about().description()).toEqual(about.description);
    expect(pluginInfo.about().targetOperatingSystems()).toEqual(about.target_operating_systems);
    expect(pluginInfo.about().vendor().name()).toEqual(about.vendor.name);
    expect(pluginInfo.about().vendor().url()).toEqual(about.vendor.url);
  };

});
