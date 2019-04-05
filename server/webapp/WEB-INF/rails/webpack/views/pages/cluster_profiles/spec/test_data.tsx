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

export function DockerClusterProfile() {
  return {
    _links: {
      self: {
        href: "https://localhost:8154/go/api/admin/plugin_settings/cd.go.contrib.elasticagent.kubernetes"
      },
      doc: {
        href: "https://api.gocd.org/19.3.0/#plugin-settings"
      },
      find: {
        href: "https://localhost:8154/go/api/admin/plugin_settings/:plugin_id"
      }
    },
    id: "cluster_3",
    plugin_id: "cd.go.contrib.elastic-agent.docker",
    properties: [
      {
        key: "go_server_url",
        value: "https://localhost:8154/go"
      },
      {
        key: "max_docker_containers",
        value: "30"
      },
      {
        key: "auto_register_timeout",
        value: "10"
      },
      {
        key: "docker_uri",
        value: "unix:///var/docker.sock"
      }
    ]
  };
}

export function K8SClusterProfile() {
  return {
    _links: {
      self: {
        href: "https://localhost:8154/go/api/admin/plugin_settings/cd.go.contrib.elasticagent.kubernetes"
      },
      doc: {
        href: "https://api.gocd.org/19.3.0/#plugin-settings"
      },
      find: {
        href: "https://localhost:8154/go/api/admin/plugin_settings/:plugin_id"
      }
    },
    id: "cluster_1",
    plugin_id: "cd.go.contrib.elasticagent.kubernetes",
    properties: [
      {
        key: "security_token",
        encrypted_value: "some-encrypted-value"
      },
      {
        key: "go_server_url",
        value: "https://localhost:8154/go"
      },
      {
        key: "kubernetes_cluster_url",
        value: "https://clusterurl.amazonaws.com"
      },
      {
        key: "namespace"
      },
      {
        key: "kubernetes_cluster_ca_cert",
        encrypted_value: "some-encrypted-value"
      },
      {
        key: "pending_pods_count"
      },
      {
        key: "auto_register_timeout",
        value: "3"
      }
    ]
  };
}

export function DockerPluginJSON() {
  // noinspection TsLint
  return {
    _links: {
      self: {
        href: "http://localhost:8153/go/api/admin/plugin_info/cd.go.contrib.elastic-agent.docker"
      },
      doc: {
        href: "https://api.gocd.org/#plugin-info"
      },
      find: {
        href: "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
      },
      image: {
        href: "http://localhost:8153/go/api/plugin_images/cd.go.contrib.elastic-agent.docker/e1ce6a7746cdab85ec2229424463c48cc15b761459dd85202acbc12693787aff"
      }
    },
    id: "cd.go.contrib.elastic-agent.docker",
    status: {
      state: "active"
    },
    plugin_file_location: "/path/plugins/external/docker-elastic-agents-2.0.0-160.jar",
    bundled_plugin: false,
    about: {
      name: "Docker Elastic Agent Plugin",
      version: "2.0.0-160",
      target_go_version: "18.10.0",
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
        supports_cluster_profiles: true,
        cluster_profile_settings: {
          configurations: [
            {
              key: "go_server_url",
              metadata: {
                secure: false,
                required: true
              }
            },
            {
              key: "environment_variables",
              metadata: {
                secure: false,
                required: false
              }
            },
            {
              key: "max_docker_containers",
              metadata: {
                secure: false,
                required: true
              }
            },
            {
              key: "docker_uri",
              metadata: {
                secure: false,
                required: true
              }
            },
            {
              key: "auto_register_timeout",
              metadata: {
                secure: false,
                required: true
              }
            },
            {
              key: "docker_ca_cert",
              metadata: {
                secure: true,
                required: false
              }
            },
            {
              key: "docker_client_key",
              metadata: {
                secure: true,
                required: false
              }
            },
            {
              key: "docker_client_cert",
              metadata: {
                secure: true,
                required: false
              }
            },
            {
              key: "enable_private_registry_authentication",
              metadata: {
                secure: false,
                required: true
              }
            },
            {
              key: "private_registry_server",
              metadata: {
                secure: false,
                required: false
              }
            },
            {
              key: "private_registry_username",
              metadata: {
                secure: false,
                required: false
              }
            },
            {
              key: "private_registry_password",
              metadata: {
                secure: true,
                required: false
              }
            },
            {
              key: "pull_on_container_create",
              metadata: {
                secure: false,
                required: true
              }
            }
          ],
          view: {
            template: "<!--\n  ~ Copyright 2018 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n\n<div data-plugin-style-id=\"ea-plugin\">\n\t<style>\n\t\t[data-plugin-style-id=\"ea-plugin\"] fieldset {\n\t\t\tpadding:          20px;\n\t\t\tbackground-color: transparent;\n\t\t\tmargin-bottom:    20px;\n\t\t\tborder:           1px solid #ddd;\n\t\t}\n\n\t\t[data-plugin-style-id=\"ea-plugin\"] fieldset legend {\n\t\t\tfont-size:        0.875rem;\n\t\t\tbackground-color: #fff;\n\t\t\tpadding:          5px;\n\t\t}\n\t</style>\n\n\t<div class=\"form_item_block\">\n\t\t<label>Go Server URL (this is passed to the agents, so don't use <code>localhost</code>):<span\n\t\t\tclass=\"asterix\">*</span></label>\n\t\t<input type=\"text\" ng-model=\"go_server_url\" ng-required=\"true\" placeholder=\"https://ipaddress:8154/go\"/>\n\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[go_server_url].$error.server\">{{GOINPUTNAME[go_server_url].$error.server}}</span>\n\t</div>\n\n\t<fieldset>\n\t\t<legend>Container configuration</legend>\n\t\t<div class=\"form_item_block\">\n\t\t\t<label>Environment Variables\n\t\t\t\t<small>(Enter one variable per line)</small>\n\t\t\t</label>\n\t\t\t<textarea type=\"text\" ng-model=\"environment_variables\" ng-required=\"true\" rows=\"7\"\n\t\t\t\t\t  placeholder=\"JAVA_HOME=/opt/java&#x000A;MAVEN_HOME=/opt/maven\"></textarea>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[environment_variables].$error.server\">{{GOINPUTNAME[environment_variables].$error.server}}</span>\n\t\t</div>\n\n\t\t<div class=\"form_item_block\">\n\t\t\t<label>Agent auto-register Timeout (in minutes)<span class=\"asterix\">*</span></label>\n\t\t\t<input type=\"text\" ng-model=\"auto_register_timeout\" ng-required=\"true\"/>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[auto_register_timeout].$error.server\">{{GOINPUTNAME[auto_register_timeout].$error.server}}</span>\n\t\t</div>\n\n\t\t<div class=\"form_item_block\">\n\t\t\t<label>Maximum docker containers to run at any given point in time:<span class=\"asterix\">*</span></label>\n\t\t\t<input type=\"text\" ng-model=\"max_docker_containers\" ng-required=\"true\"/>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[max_docker_containers].$error.server\">{{GOINPUTNAME[max_docker_containers].$error.server}}</span>\n\t\t</div>\n\n\t\t<div class=\"form_item_block\">\n\t\t\t<input type=\"checkbox\" ng-model=\"pull_on_container_create\" id=\"pull_on_container_create\" ng-true-value=\"true\" ng-false-value=\"false\"/>\n\t\t\t<label for=\"pull_on_container_create\">Pull image before creating the container</label>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[pull_on_container_create].$error.server\">{{GOINPUTNAME[pull_on_container_create].$error.server}}</span>\n\t\t</div>\n\t</fieldset>\n\t<fieldset>\n\t\t<legend>Docker client configuration</legend>\n\t\t<div class=\"form_item_block\">\n\t\t\t<label>Docker URI:<span class=\"asterix\">*</span></label>\n\t\t\t<input type=\"text\" ng-model=\"docker_uri\" ng-required=\"true\"/>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[docker_uri].$error.server\">{{GOINPUTNAME[docker_uri].$error.server}}</span>\n\t\t</div>\n\n\t\t<div class=\"form_item_block\">\n\t\t\t<label>Docker CA Certificate:</label>\n\t\t\t<textarea type=\"text\" ng-model=\"docker_ca_cert\" rows=\"7\"></textarea>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[docker_ca_cert].$error.server\">{{GOINPUTNAME[docker_ca_cert].$error.server}}</span>\n\t\t</div>\n\n\t\t<div class=\"form_item_block\">\n\t\t\t<label>Docker Client Key:</label>\n\t\t\t<textarea type=\"text\" ng-model=\"docker_client_key\" rows=\"7\"></textarea>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[docker_client_key].$error.server\">{{GOINPUTNAME[docker_client_key].$error.server}}</span>\n\t\t</div>\n\n\t\t<div class=\"form_item_block\">\n\t\t\t<label>Docker Client Certificate:</label>\n\t\t\t<textarea type=\"text\" ng-model=\"docker_client_cert\" rows=\"7\"></textarea>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[docker_client_cert].$error.server\">{{GOINPUTNAME[docker_client_cert].$error.server}}</span>\n\t\t</div>\n\t</fieldset>\n\n\t<fieldset>\n\t\t<legend>Docker registry settings</legend>\n\t\t<div class=\"form_item_block\">\n\t\t\t<input type=\"checkbox\" ng-model=\"enable_private_registry_authentication\" id=\"enable_private_registry_authentication\" ng-true-value=\"true\" ng-false-value=\"false\"/>\n\t\t\t<label for=\"enable_private_registry_authentication\">Use Private Registry</label>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[enable_private_registry_authentication].$error.server\">{{GOINPUTNAME[enable_private_registry_authentication].$error.server}}</span>\n\t\t</div>\n\n\t\t<div ng-show=\"enable_private_registry_authentication\">\n\t\t\t<div class=\"form_item_block\">\n\t\t\t\t<label>Private Registry Server:</label>\n\t\t\t\t<input type=\"text\" ng-model=\"private_registry_server\" ng-required=\"true\"/>\n\t\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[private_registry_server].$error.server\">{{GOINPUTNAME[private_registry_server].$error.server}}</span>\n\t\t\t</div>\n\t\t\t<div class=\"form_item_block\">\n\t\t\t\t<label>Private Registry Username:</label>\n\t\t\t\t<input type=\"text\" ng-model=\"private_registry_username\" ng-required=\"true\"/>\n\t\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[private_registry_username].$error.server\">{{GOINPUTNAME[private_registry_username].$error.server}}</span>\n\t\t\t</div>\n\t\t\t<div class=\"form_item_block\">\n\t\t\t\t<label>Private Registry Password:</label>\n\t\t\t\t<input type=\"password\" ng-model=\"private_registry_password\" ng-required=\"true\"/>\n\t\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[private_registry_password].$error.server\">{{GOINPUTNAME[private_registry_password].$error.server}}</span>\n\t\t\t</div>\n\t\t</div>\n\t</fieldset>\n</div>\n"
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
