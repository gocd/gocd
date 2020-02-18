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
export class TestData {
  public static kubernetesPluginJSON(): any {
    // noinspection TsLint
    return {
      _links: {
        self: {
          href: "http://localhost:8153/go/api/admin/plugin_info/cd.go.contrib.elasticagent.kubernetes"
        },
        doc: {
          href: "https://api.gocd.org/#plugin-info"
        },
        find: {
          href: "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
        },
        image: {
          href: "http://localhost:8153/go/api/plugin_images/cd.go.contrib.elasticagent.kubernetes/48e902f41fd4c1f6844a9c178d1cb8ec54ab44f28ebedfbb759d2d71e4ce0aaa"
        }
      },
      id: "cd.go.contrib.elasticagent.kubernetes",
      status: {
        state: "active"
      },
      plugin_file_location: "/Users/vishaldevgire/projects/gocd/server/plugins/external/kubernetes-elastic-agent-2.0.0-113.jar",
      bundled_plugin: false,
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
          plugin_settings: {
            configurations: [
              {
                key: "go_server_url",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "auto_register_timeout",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "pending_pods_count",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "kubernetes_cluster_url",
                metadata: {
                  secure: false,
                  required: true
                }
              },
              {
                key: "namespace",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "security_token",
                metadata: {
                  secure: true,
                  required: true
                }
              },
              {
                key: "kubernetes_cluster_ca_cert",
                metadata: {
                  secure: true,
                  required: false
                }
              }
            ],
            view: {
              template: "<!--\n  ~ Copyright 2018 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n\n<div data-plugin-style-id=\"kubernetes-plugin\">\n\t<style>\n\t\t[data-plugin-style-id=\"kubernetes-plugin\"] .row {\n\t\t\twidth:  100%;\n\t\t\tmargin: auto;\n\t\t}\n\n\t\t[data-plugin-style-id=\"kubernetes-plugin\"] .no-padding {\n\t\t\tpadding: 0 !important;\n\t\t}\n\n\t\t[data-plugin-style-id=\"kubernetes-plugin\"] fieldset {\n\t\t\tborder:           1px solid #ddd;\n\t\t\tpadding:          20px;\n\t\t\tmargin-bottom:    20px;\n\t\t\tborder-radius:    3px;\n\t\t\tbackground-color: transparent;\n\t\t}\n\n\t\t[data-plugin-style-id=\"kubernetes-plugin\"] fieldset legend {\n\t\t\tpadding:          5px;\n\t\t\tfont-size:        0.875rem;\n\t\t\tmargin-bottom:    0px;\n\t\t\tbackground-color: #fff;\n\t\t}\n\n\t\t[data-plugin-style-id=\"kubernetes-plugin\"] .form-help-content {\n\t\t\tcolor:         #666;\n\t\t\tclear:         both;\n\t\t\tfont-size:     0.82rem;\n\t\t\tfont-style:    italic;\n\t\t\tmargin-top:    -15px;\n\t\t\tpadding-left:  2px;\n\t\t\tmargin-bottom: 10px;\n\t\t}\n\n\t\t[data-plugin-style-id=\"kubernetes-plugin\"] .form-help-content code {\n\t\t\tborder:           0px;\n\t\t\tpadding:          2px 5px;\n\t\t\tborder-radius:    3px;\n\t\t\tbackground-color: #eee;\n\t\t}\n\n\t\t[data-plugin-style-id=\"kubernetes-plugin\"] .form-help-content .code {\n\t\t\tpadding:          16px;\n\t\t\toverflow:         auto;\n\t\t\tfont-size:        85%;\n\t\t\tline-height:      1.45;\n\t\t\tborder-radius:    3px;\n\t\t\tbackground-color: #f6f8fa;\n\t\t}\n\t</style>\n\n\t<div class=\"row\">\n\t\t<label>Go Server URL</label>\n\t\t<input type=\"text\" ng-model=\"go_server_url\" ng-required=\"false\"/>\n\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[go_server_url].$error.server\">{{GOINPUTNAME[go_server_url].$error.server}}</span>\n\t\t<label class=\"form-help-content\">\n\t\t\tServer hostname must resolve in your container. Don't use <code>localhost</code> or <code>127.0.0.1</code>.\n\t\t\tDefaults to GoCD Secure site URL if not specified.\n\t\t</label>\n\t</div>\n\n\t<div class=\"row\">\n\t\t<label>Agent auto-register timeout (in minutes)</label>\n\t\t<input type=\"text\" ng-model=\"auto_register_timeout\" ng-required=\"true\"/>\n\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[auto_register_timeout].$error.server\">{{GOINPUTNAME[auto_register_timeout].$error.server}}</span>\n\t\t<label class=\"form-help-content\">Defaults to <code>10 minutes</code>.</label>\n\t</div>\n\n\t<div class=\"row\">\n\t\t<label>Maximum pending pods</label>\n\t\t<input type=\"text\" ng-model=\"pending_pods_count\" ng-required=\"true\"/>\n\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[pending_pods_count].$error.server\">{{GOINPUTNAME[pending_pods_count].$error.server}}</span>\n\t\t<label class=\"form-help-content\">Defaults to <code>10 pods</code>.</label>\n\t</div>\n\n\t<fieldset>\n\t\t<legend>Cluster Information</legend>\n\t\t<div class=\"row\">\n\t\t\t<div class=\"columns large-5\">\n\t\t\t\t<label>Cluster URL<span class=\"asterix\">*</span></label>\n\t\t\t\t<input type=\"text\" ng-model=\"kubernetes_cluster_url\" ng-required=\"true\"/>\n\t\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[kubernetes_cluster_url].$error.server\">{{GOINPUTNAME[kubernetes_cluster_url].$error.server}}</span>\n\t\t\t\t<label class=\"form-help-content\">\n\t\t\t\t\tKubernetes Cluster URL. Can be obtained by running <code>kubectl cluster-info</code>\n\t\t\t\t</label>\n\t\t\t</div>\n\t\t\t<div class=\"columns large-5 end\">\n\t\t\t\t<label>Namespace</label>\n\t\t\t\t<input type=\"text\" ng-model=\"namespace\" ng-required=\"true\"/>\n\t\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[namespace].$error.server\">{{GOINPUTNAME[namespace].$error.server}}</span>\n\t\t\t\t<label class=\"form-help-content\">\n\t\t\t\t\tNamespace in which plugin will create the agent pods. defaults to <code>default</code> namespace.\n\t\t\t\t</label>\n\t\t\t</div>\n\t\t</div>\n\n\t\t<div class=\"row\">\n\t\t\t<label>Security token\n\t\t\t\t<span class=\"asterix\">*</span>\n\t\t\t</label>\n\t\t\t<textarea rows=\"5\" ng-model=\"security_token\"></textarea>\n\t\t\t<span class=\"form_error form-error\" ng-show=\"GOINPUTNAME[security_token].$error.server\">{{GOINPUTNAME[security_token].$error.server}}</span>\n\t\t\t<label class=\"form-help-content\">\n\t\t\t\tGet the service account token by running following command <code>kubectl describe secret\n\t\t\t\tTOKEN_NAME</code> and copy the value of token here.\n\t\t\t</label>\n\t\t</div>\n\n\t\t<div class=\"row\">\n\t\t\t<label>Cluster ca certificate data</label>\n\t\t\t<textarea ng-model=\"kubernetes_cluster_ca_cert\" rows=\"7\"></textarea>\n\t\t\t<span class=\"form_error\" ng-show=\"GOINPUTNAME[kubernetes_cluster_ca_cert].$error.server\">{{GOINPUTNAME[kubernetes_cluster_ca_cert].$error.server}}</span>\n\t\t\t<label class=\"form-help-content\">\n\t\t\t\tKubernetes cluster ca certificate data. Do not provide <code> -----BEGIN * </code> and <code> -----END\n\t\t\t\t* </code> in your certificate data.\n\t\t\t</label>\n\t\t</div>\n\t</fieldset>\n</div>\n"
            }
          },
          elastic_agent_profile_settings: {
            configurations: [
              {
                key: "Image",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "MaxMemory",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "MaxCPU",
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
              },
              {
                key: "PodConfiguration",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "SpecifiedUsingPodConfiguration",
                metadata: {
                  secure: false,
                  required: true
                }
              },
              {
                key: "Privileged",
                metadata: {
                  secure: false,
                  required: false
                }
              }
            ],
            view: {
              template: "<!--\n  ~ Copyright 2018 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n\n<div data-plugin-style-id=\"kubernetes-plugin\">\n\n    <style>\n        [data-plugin-style-id=\"kubernetes-plugin\"] .tooltip-info {\n            position: relative;\n            display: inline-block;\n            cursor: pointer;\n        }\n\n        [data-plugin-style-id=\"kubernetes-plugin\"] .tooltip-info .tooltip-content {\n            font-family: \"Open Sans\", \"Helvetica Neue\", Helvetica, Roboto, Arial, sans-serif;\n            cursor: auto;\n            font-size: 0.78rem;\n            text-transform: none;\n            background-color: #efefef;\n            border: 1px solid #cacaca;\n            border-radius: 3px;\n            display: block;\n            padding: 1rem;\n            position: absolute;\n            visibility: hidden;\n            width: 500px;\n            z-index: 10;\n            top: 100%;\n            color: #000;\n        }\n\n        [data-plugin-style-id=\"kubernetes-plugin\"] .tooltip-info:after {\n            font-family: 'FontAwesome';\n            content: \"\\f05a\";\n            font-weight: normal;\n            font-style: normal;\n            display: inline-block;\n            text-decoration: inherit;\n            line-height: 1.8;\n            font-size: 0.875rem;\n            color: #0a0a0a;\n            -webkit-font-smoothing: antialiased;\n            margin: 0 10px;\n        }\n\n        [data-plugin-style-id=\"kubernetes-plugin\"] .tooltip-info:hover .tooltip-content {\n            visibility: visible;\n        }\n\n        [data-plugin-style-id=\"kubernetes-plugin\"] .tooltip-info .tooltip-content-right {\n            right: 0;\n        }\n\n        [data-plugin-style-id=\"kubernetes-plugin\"] code {\n            border: none;\n            background: #ddd;\n            border-radius: 3px;\n            color: inherit;\n        }\n\n        [data-plugin-style-id=\"kubernetes-plugin\"] textarea {\n            font-family: \"SFMono-Regular\", Consolas, \"Liberation Mono\", Menlo, Courier, monospace;\n        }\n\n        [data-plugin-style-id=\"kubernetes-plugin\"] .highlight {\n            background: #f0f0f0;\n        }\n\n        [data-plugin-style-id=\"kubernetes-plugin\"] .code {\n            font-family: Consolas, \"Liberation Mono\", Courier, monospace;\n            padding: 16px;\n            overflow: auto;\n            font-size: 0.8125rem;\n            line-height: 1.45;\n            background-color: #e6e6e6;\n            border-radius: 3px;\n        }\n\n        [data-plugin-style-id=\"kubernetes-plugin\"] .form-help-content {\n            color: #666;\n            font-style: italic;\n            clear: both;\n            font-size: 0.82rem;\n        }\n\n    </style>\n\n    <div class=\"row collapse\">\n        <label>Specify Elastic Agent Pod Configuration using</label>\n        <div class=\"form_item_block row\" style=\"padding-top: 10px\">\n            <div class=\"columns small-9 medium-10 larger-10\"\n                 ng-init=\"SpecifiedUsingPodConfiguration = SpecifiedUsingPodConfiguration || 'false'\">\n                <input type=\"radio\" ng-model=\"SpecifiedUsingPodConfiguration\" value=\"false\"\n                       id=\"login-using-github\"/>\n                <label for=\"login-using-github\">Config Properties</label>\n\n                <input type=\"radio\" ng-model=\"SpecifiedUsingPodConfiguration\" value=\"true\"\n                       id=\"login-using-github-enterprise\"/>\n                <label for=\"login-using-github-enterprise\">Pod Yaml</label>\n            </div>\n        </div>\n    </div>\n\n    <div ng-show=\"SpecifiedUsingPodConfiguration == 'false'\">\n        <div class=\"row\">\n            <div class=\"columns medium-6 large-5\">\n                <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Image].$error.server}\">Image:\n                    <span class=\"asterix\">*</span>\n                </label>\n                <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Image].$error.server}\" type=\"text\" ng-model=\"Image\"\n                       ng-required=\"true\" placeholder=\"alpine:latest\"/>\n                <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Image].$error.server}\"\n                      ng-show=\"GOINPUTNAME[Image].$error.server\">{{GOINPUTNAME[Image].$error.server}}</span>\n            </div>\n\n            <div class=\"columns medium-4 large-3 end\">\n                <label ng-class=\"{'is-invalid-label': GOINPUTNAME[MaxMemory].$error.server}\">Maximum Memory limit:\n                    <div class=\"tooltip-info\">\n                  <span class=\"tooltip-content tooltip-content-right\">\n                    The maximum amount of memory the container is allowed to use. This field take a positive integer,\n                    followed by a suffix of B, K, M, G and T to indicate bytes, kilobytes, megabytes, gigabytes or terabytes.<br/>\n                    <a href=\"https://docs.docker.com/engine/admin/resource_constraints/\"\n                       target=\"_blank\">Read more about memory</a>\n                  </span>\n                    </div>\n                </label>\n                <input ng-class=\"{'is-invalid-input': GOINPUTNAME[MaxMemory].$error.server}\" type=\"text\"\n                       ng-model=\"MaxMemory\" ng-required=\"false\"/>\n                <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[MaxMemory].$error.server}\"\n                      ng-show=\"GOINPUTNAME[MaxMemory].$error.server\">{{GOINPUTNAME[MaxMemory].$error.server}}</span>\n            </div>\n\n            <div class=\"columns medium-4 large-3 end\">\n                <label ng-class=\"{'is-invalid-label': GOINPUTNAME[MaxCPU].$error.server}\">Maximum CPU limit:\n                    <div class=\"tooltip-info\">\n                  <span class=\"tooltip-content tooltip-content-right\">\n                    The maximum amount of cpu units the container is allowed to use. This field take a positive integer.<br/>\n                    <a href=\"https://docs.docker.com/engine/admin/resource_constraints/#cpu/\"\n                       target=\"_blank\">Read more about memory</a>\n                  </span>\n                    </div>\n                </label>\n                <input ng-class=\"{'is-invalid-input': GOINPUTNAME[MaxCPU].$error.server}\" type=\"text\" ng-model=\"MaxCPU\"\n                       ng-required=\"false\"/>\n                <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[MaxCPU].$error.server}\"\n                      ng-show=\"GOINPUTNAME[MaxCPU].$error.server\">{{GOINPUTNAME[MaxCPU].$error.server}}</span>\n            </div>\n        </div>\n\n        <div class=\"form_item_block\">\n            <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Privileged].$error.server}\" type=\"checkbox\" ng-model=\"Privileged\" ng-required=\"true\" ng-true-value=\"true\" ng-false-value=\"false\" id=\"Privileged\"/>\n            <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Privileged].$error.server}\" for=\"Privileged\">Privileged</label>\n            <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Privileged].$error.server}\" ng-show=\"GOINPUTNAME[Privileged].$error.server\">{{GOINPUTNAME[Privileged].$error.server}}</span>\n            <span class=\"form-help-content\">\n                <strong>Note:</strong> When privileged mode is enabled, the container is given elevated privileges on the host container instance.\n            </span>\n        </div>\n\n        <div class=\"form_item_block\">\n            <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Environment].$error.server}\">Environment Variables\n                <small>(Enter one variable per line)</small>\n                <div class=\"tooltip-info\">\n          <span class=\"tooltip-content\">\n            Specify the environment variables. This allows you to override the <code>ENV</code> that is specified in\n            the <code>Dockerfile</code>, or provide new environment variables in case the <code>Dockerfile</code> does not\n            contain any <code>ENV</code>.\n            <br/>\n            <div class=\"code\">\n              JAVA_HOME=/opt/java<br/>\n              ANT_HOME=/opt/ant\n            </div>\n            <a href=\"https://docs.docker.com/engine/reference/builder/#env\"\n               target=\"_blank\">Read more about <code>ENV</code></a>\n          </span>\n                </div>\n            </label>\n            <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Environment].$error.server}\" ng-model=\"Environment\"\n                      ng-required=\"false\" rows=\"7\"></textarea>\n            <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Environment].$error.server}\"\n                  ng-show=\"GOINPUTNAME[Environment].$error.server\">{{GOINPUTNAME[Environment].$error.server}}</span>\n        </div>\n    </div>\n\n    <div ng-show=\"SpecifiedUsingPodConfiguration == 'true'\">\n        <div class=\"form_item_block\">\n            <label ng-class=\"{'is-invalid-label': GOINPUTNAME[PodConfiguration].$error.server}\">Specify Elastic Agent\n                <code>pod.yaml</code> here\n                <div class=\"tooltip-info\">\n                  <span class=\"tooltip-content\">\n                    Specify the pod.yaml configuration. This allows you to specify advance options for elastic agent pod such as\n                      <code>command</code>, <code>args</code>, <code>volumes</code>, <code>secrets</code>, <code>configMaps</code> etc.\n                    <br/>\n                    <a href=\"https://kubernetes-v1-4.github.io/docs/user-guide/pods/multi-container/#pod-configuration-file\"\n                       target=\"_blank\">Read more about <code>Pod Yaml</code></a>\n                  </span>\n                </div>\n            </label>\n            <textarea class=\"highlight\" ng-class=\"{'is-invalid-input': GOINPUTNAME[PodConfiguration].$error.server}\"\n                      ng-model=\"PodConfiguration\"\n                      ng-required=\"false\" rows=\"9\" columns=\"15\" ng-init=\"PodConfiguration = (PodConfiguration || 'apiVersion: v1\nkind: Pod\nmetadata:\n  name: pod-name-prefix-{{ POD_POSTFIX }}\n  labels:\n    app: web\nspec:\n  containers:\n    - name: gocd-agent-container-{{ CONTAINER_POSTFIX }}\n      image: {{ GOCD_AGENT_IMAGE }}:{{ LATEST_VERSION }}\n      securityContext:\n        privileged: true')\">\n            </textarea>\n            <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[PodConfiguration].$error.server}\"\n                  ng-show=\"GOINPUTNAME[PodConfiguration].$error.server\">{{GOINPUTNAME[PodConfiguration].$error.server}}</span>\n        </div>\n    </div>\n\n</div>\n"
            }
          },
          capabilities: {
            supports_plugin_status_report: true,
            supports_cluster_status_report: false,
            supports_agent_status_report: true
          }
        }
      ]
    };
  }

  public static dockerSwarmPluginJSON(): any {
    // noinspection TsLint
    return {
      _links: {
        self: {
          href: "http://localhost:8153/go/api/admin/plugin_info/cd.go.contrib.elastic-agent.docker-swarm"
        },
        doc: {
          href: "https://api.gocd.org/#plugin-info"
        },
        find: {
          href: "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
        },
        image: {
          href: "http://localhost:8153/go/api/plugin_images/cd.go.contrib.elastic-agent.docker-swarm/176f84f4804cd55e556170df3ecb88dacf40d733077e5251b4ea6048a2257d5c"
        }
      },
      id: "cd.go.contrib.elastic-agent.docker-swarm",
      status: {
        state: "active"
      },
      plugin_file_location: "/Users/vishaldevgire/projects/gocd/server/plugins/external/docker-swarm-elastic-agents-4.0.0-126.jar",
      bundled_plugin: false,
      about: {
        name: "GoCD Docker Swarm Elastic Agents",
        version: "4.0.0-126",
        target_go_version: "18.10.0",
        description: "Docker Swarm Based Elastic Agent Plugins for GoCD",
        target_operating_systems: [],
        vendor: {
          name: "GoCD Contributors",
          url: "https://github.com/gocd-contrib/docker-swarm-elastic-agents"
        }
      },
      extensions: [
        {
          type: "elastic-agent",
          plugin_settings: {
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
              }
            ],
            view: {
              template: "<!--\n  ~ Copyright 2018 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n\n<div data-plugin-style-id=\"docker-swarm-plugin\">\n  <style>\n    [data-plugin-style-id=\"docker-swarm-plugin\"] fieldset {\n      padding:          20px;\n      background-color: transparent;\n      margin-bottom:    20px;\n      border:           1px solid #ddd;\n    }\n\n    [data-plugin-style-id=\"docker-swarm-plugin\"] fieldset legend {\n      font-size:        0.875rem;\n      background-color: #fff;\n      padding:          5px;\n    }\n\n    [data-plugin-style-id=\"docker-swarm-plugin\"] .form-help-content {\n      color:      #666;\n      font-style: italic;\n      clear:      both;\n      font-size:  0.82rem;\n    }\n\n    [data-plugin-style-id=\"docker-swarm-plugin\"] .form-help-content code {\n      background-color: #eee;\n      padding: 2px 5px;\n      border-radius: 3px;\n    }\n\n    [data-plugin-style-id=\"docker-swarm-plugin\"] .form-help-content .code {\n      padding:          16px;\n      overflow:         auto;\n      font-size:        85%;\n      line-height:      1.45;\n      background-color: #f6f8fa;\n      border-radius:    3px;\n    }\n  </style>\n\n  <div class=\"form_item_block\">\n    <label>Go Server URL:<span class=\"asterix\">*</span></label>\n    <input type=\"text\" ng-model=\"go_server_url\" ng-required=\"true\" placeholder=\"https://ipaddress:8154/go\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[go_server_url].$error.server\">{{GOINPUTNAME[go_server_url].$error.server}}</span>\n    <label class=\"form-help-content\">\n      Server hostname must resolve in your container. Don't use <code>localhost</code> or <code>127.0.0.1</code>.\n    </label>\n  </div>\n\n  <fieldset>\n    <legend>Docker container configuration</legend>\n    <div class=\"form_item_block\">\n      <label>Environment Variables <small>(Enter one variable per line)</small></label>\n      <textarea type=\"text\" ng-model=\"environment_variables\" ng-required=\"true\" rows=\"7\" placeholder=\"JAVA_HOME=/opt/java&#x000A;MAVEN_HOME=/opt/maven\"></textarea>\n      <span class=\"form_error\" ng-show=\"GOINPUTNAME[environment_variables].$error.server\">{{GOINPUTNAME[environment_variables].$error.server}}</span>\n    </div>\n\n    <div class=\"form_item_block\">\n      <label>Agent auto-register Timeout (in minutes)<span class=\"asterix\">*</span></label>\n      <input type=\"text\" ng-model=\"auto_register_timeout\" ng-required=\"true\"/>\n      <span class=\"form_error\" ng-show=\"GOINPUTNAME[auto_register_timeout].$error.server\">{{GOINPUTNAME[auto_register_timeout].$error.server}}</span>\n    </div>\n\n    <div class=\"form_item_block\">\n      <label>Maximum docker containers to run at any given point in time:<span class=\"asterix\">*</span></label>\n      <input type=\"text\" ng-model=\"max_docker_containers\" ng-required=\"true\"/>\n      <span class=\"form_error\" ng-show=\"GOINPUTNAME[max_docker_containers].$error.server\">{{GOINPUTNAME[max_docker_containers].$error.server}}</span>\n    </div>\n  </fieldset>\n\n  <fieldset>\n    <legend>Docker configuration</legend>\n    <div class=\"form_item_block\">\n      <label>Docker URI:<span class=\"asterix\">*</span></label>\n      <input type=\"text\" ng-model=\"docker_uri\" ng-required=\"true\"/>\n      <span class=\"form_error\" ng-show=\"GOINPUTNAME[docker_uri].$error.server\">{{GOINPUTNAME[docker_uri].$error.server}}</span>\n    </div>\n\n    <div class=\"form_item_block\">\n      <label>Docker CA Certificate:</label>\n      <textarea type=\"text\" ng-model=\"docker_ca_cert\" rows=\"7\"></textarea>\n      <span class=\"form_error\" ng-show=\"GOINPUTNAME[docker_ca_cert].$error.server\">{{GOINPUTNAME[docker_ca_cert].$error.server}}</span>\n    </div>\n\n    <div class=\"form_item_block\">\n      <label>Docker Client Key:</label>\n      <textarea type=\"text\" ng-model=\"docker_client_key\" rows=\"7\"></textarea>\n      <span class=\"form_error\" ng-show=\"GOINPUTNAME[docker_client_key].$error.server\">{{GOINPUTNAME[docker_client_key].$error.server}}</span>\n    </div>\n\n    <div class=\"form_item_block\">\n      <label>Docker Client Certificate:</label>\n      <textarea type=\"text\" ng-model=\"docker_client_cert\" rows=\"7\"></textarea>\n      <span class=\"form_error\" ng-show=\"GOINPUTNAME[docker_client_cert].$error.server\">{{GOINPUTNAME[docker_client_cert].$error.server}}</span>\n    </div>\n  </fieldset>\n\n  <fieldset>\n    <legend>Private Docker Registry</legend>\n    <div class=\"form_item_block\" ng-init=\"enable_private_registry_authentication = (enable_private_registry_authentication || 'false')\">\n      <input type=\"radio\" ng-model=\"enable_private_registry_authentication\" value=\"false\" id=\"use-default-docker-registry\"/>\n      <label for=\"use-default-docker-registry\">Default</label>\n      <input type=\"radio\" ng-model=\"enable_private_registry_authentication\" value=\"true\" id=\"use-private-docker-registry\"/>\n      <label for=\"use-private-docker-registry\">Use Private Registry</label>\n      <span class=\"form_error\" ng-show=\"GOINPUTNAME[enable_private_registry_authentication].$error.server\">{{GOINPUTNAME[enable_private_registry_authentication].$error.server}}</span>\n    </div>\n    <div ng-show=\"enable_private_registry_authentication\">\n      <div class=\"form_item_block\">\n        <label>Private Registry Server:<span class=\"asterix\">*</span></label>\n        <input type=\"text\" ng-model=\"private_registry_server\" ng-required=\"true\"/>\n        <span class=\"form_error\" ng-show=\"GOINPUTNAME[private_registry_server].$error.server\">{{GOINPUTNAME[private_registry_server].$error.server}}</span>\n      </div>\n      <div class=\"form_item_block\">\n        <label>Private Registry Username:<span class=\"asterix\">*</span></label>\n        <input type=\"text\" ng-model=\"private_registry_username\" ng-required=\"true\"/>\n        <span class=\"form_error\" ng-show=\"GOINPUTNAME[private_registry_username].$error.server\">{{GOINPUTNAME[private_registry_username].$error.server}}</span>\n      </div>\n      <div class=\"form_item_block\">\n        <label>Private Registry Password:<span class=\"asterix\">*</span></label>\n        <input type=\"password\" ng-model=\"private_registry_password\" ng-required=\"true\"/>\n        <span class=\"form_error\" ng-show=\"GOINPUTNAME[private_registry_password].$error.server\">{{GOINPUTNAME[private_registry_password].$error.server}}</span>\n      </div>\n    </div>\n    <label class=\"form-help-content\">\n      This allows you to pull images from private docker registry, either to\n      your own account or within an organization or team.\n    </label>\n  </fieldset>\n</div>\n"
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
              },
              {
                key: "MaxMemory",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "ReservedMemory",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "Secrets",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "Networks",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "Mounts",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "Hosts",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "Constraints",
                metadata: {
                  secure: false,
                  required: false
                }
              }
            ],
            view: {
              template: "<!--\n  ~ Copyright 2018 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n<div data-plugin-style-id=\"docker-swarm-plugin\">\n\n\t<style>\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .tooltip-info {\n\t\t\tposition: relative;\n\t\t\tdisplay:  inline-block;\n\t\t\tcursor:   pointer;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .tooltip-info .tooltip-content {\n\t\t\tfont-family:      \"Open Sans\", \"Helvetica Neue\", Helvetica, Roboto, Arial, sans-serif;\n\t\t\tcursor:           auto;\n\t\t\tfont-size:        0.78rem;\n\t\t\ttext-transform:   none;\n\t\t\tbackground-color: #efefef;\n\t\t\tborder:           1px solid #cacaca;\n\t\t\tborder-radius:    3px;\n\t\t\tdisplay:          block;\n\t\t\tpadding:          1rem;\n\t\t\tposition:         absolute;\n\t\t\tvisibility:       hidden;\n\t\t\twidth:            500px;\n\t\t\tz-index:          10;\n\t\t\ttop:              100%;\n\t\t\tcolor:            #000;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .tooltip-info .tooltip-content-right {\n\t\t\tright: 0;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .tooltip-info .tooltip-content-top {\n\t\t\tbottom: 100%;\n\t\t\ttop:    unset;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .tooltip-info:after {\n\t\t\tfont-family:            'FontAwesome';\n\t\t\tcontent:                \"\\f05a\";\n\t\t\tfont-weight:            normal;\n\t\t\tfont-style:             normal;\n\t\t\tdisplay:                inline-block;\n\t\t\ttext-decoration:        inherit;\n\t\t\tline-height:            1.8;\n\t\t\tfont-size:              0.875rem;\n\t\t\tcolor:                  #0a0a0a;\n\t\t\t-webkit-font-smoothing: antialiased;\n\t\t\tmargin:                 0 10px;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .tooltip-info:hover .tooltip-content {\n\t\t\tvisibility: visible;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] code {\n\t\t\tborder:        none;\n\t\t\tbackground:    #ddd;\n\t\t\tborder-radius: 3px;\n\t\t\tcolor:         inherit;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] textarea {\n\t\t\tfont-family: \"SFMono-Regular\", Consolas, \"Liberation Mono\", Menlo, Courier, monospace;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .code {\n\t\t\tfont-family:      Consolas, \"Liberation Mono\", Courier, monospace;\n\t\t\tpadding:          16px;\n\t\t\toverflow:         auto;\n\t\t\tfont-size:        0.8125rem;\n\t\t\tline-height:      1.45;\n\t\t\tbackground-color: #e6e6e6;\n\t\t\tborder-radius:    3px;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] fieldset {\n\t\t\tpadding:       10px 20px;\n\t\t\tborder-radius: 3px;\n\t\t\tborder:        1px solid #ddd;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .icon {\n\t\t\twidth: 0px;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .icon:after {\n\t\t\tfont-family:            'FontAwesome';\n\t\t\tfont-weight:            normal;\n\t\t\tfont-style:             normal;\n\t\t\tdisplay:                inline-block;\n\t\t\ttext-decoration:        inherit;\n\t\t\tline-height:            1.8;\n\t\t\tfont-size:              0.875rem;\n\t\t\t-webkit-font-smoothing: antialiased;\n\t\t\tmargin:                 0 10px;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .icon.delete:after {\n\t\t\tcontent:   \"\\f00d\";\n\t\t\tcolor:     darkred;\n\t\t\tcursor:    pointer;\n\t\t\tfont-size: 1.2em;\n\t\t}\n\n\t\t[data-plugin-style-id=\"docker-swarm-plugin\"] .btn-add {\n\t\t\tdisplay: inline-block;\n\t\t\twidth:   auto;\n\t\t}\n\n\t</style>\n\n\t<div class=\"row\">\n\t\t<div class=\"columns medium-4 large-3\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[Image].$error.server}\">Docker image:<span class=\"asterix\">*</span></label>\n\t\t\t<input ng-class=\"{'is-invalid-input': GOINPUTNAME[Image].$error.server}\" type=\"text\" ng-model=\"Image\"\n\t\t\t\t   ng-required=\"true\" placeholder=\"alpine:latest\"/>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Image].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[Image].$error.server\">{{GOINPUTNAME[Image].$error.server}}</span>\n\t\t</div>\n\n\t\t<div class=\"columns medium-4 large-3\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[ReservedMemory].$error.server}\">Memory soft limit:\n\t\t\t\t<div class=\"tooltip-info\">\n          <span class=\"tooltip-content\">\n            The docker container will start with this amount of memory. This field take a positive integer,\n            followed by a suffix of B, K, M, G and T to indicate bytes, kilobytes, megabytes, gigabytes or terabytes.\n            <a href=\"https://docs.docker.com/engine/admin/resource_constraints/\"\n\t\t\t   target=\"_blank\">Read more about memory</a>\n          </span>\n\t\t\t\t</div>\n\t\t\t</label>\n\t\t\t<input ng-class=\"{'is-invalid-input': GOINPUTNAME[ReservedMemory].$error.server}\" type=\"text\"\n\t\t\t\t   ng-model=\"ReservedMemory\" ng-required=\"true\" rows=\"7\"/>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[ReservedMemory].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[ReservedMemory].$error.server\">{{GOINPUTNAME[ReservedMemory].$error.server}}</span>\n\t\t</div>\n\n\t\t<div class=\"columns medium-4 large-3 end\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[MaxMemory].$error.server}\">Maximum hard limit:\n\t\t\t\t<div class=\"tooltip-info\">\n          <span class=\"tooltip-content tooltip-content-right\">\n            The maximum amount of memory the container is allowed to use. This field take a positive integer,\n            followed by a suffix of B, K, M, G and T to indicate bytes, kilobytes, megabytes, gigabytes or terabytes.<br/>\n            <a href=\"https://docs.docker.com/engine/admin/resource_constraints/\"\n\t\t\t   target=\"_blank\">Read more about memory</a>\n          </span>\n\t\t\t\t</div>\n\t\t\t</label>\n\t\t\t<input ng-class=\"{'is-invalid-input': GOINPUTNAME[MaxMemory].$error.server}\" type=\"text\"\n\t\t\t\t   ng-model=\"MaxMemory\" ng-required=\"true\" rows=\"7\"/>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[MaxMemory].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[MaxMemory].$error.server\">{{GOINPUTNAME[MaxMemory].$error.server}}</span>\n\t\t</div>\n\t</div>\n\n\t<div class=\"row\">\n\t\t<div class=\"columns end\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[Command].$error.server}\">Docker Command\n\t\t\t\t<small>(Enter one parameter per line)</small>\n\t\t\t\t<div class=\"tooltip-info\">\n          <span class=\"tooltip-content\">\n            Specify the command to run in the container. This allows you to override the <code>CMD</code> that is specified in\n            the <code>Dockerfile</code>, or provide one in case the <code>Dockerfile</code> does not contain a <code>CMD</code>.\n          <br/>\n          <div class=\"code\">\n            ls<br/>\n            al<br/>\n            /usr/bin\n          </div>\n          <a href=\"https://docs.docker.com/engine/reference/builder/#cmd\"\n\t\t\t target=\"_blank\">Read more about <code>CMD</code></a>\n          </span>\n\t\t\t\t</div>\n\t\t\t</label>\n\t\t\t<textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Command].$error.server}\" type=\"text\" ng-model=\"Command\"\n\t\t\t\t\t  ng-required=\"true\" rows=\"7\"></textarea>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Command].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[Command].$error.server\">{{GOINPUTNAME[Command].$error.server}}</span>\n\t\t</div>\n\t</div>\n\n\t<div class=\"row\">\n\t\t<div class=\"columns end\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[Environment].$error.server}\">Environment Variables\n\t\t\t\t<small>(Enter one variable per line)</small>\n\t\t\t\t<div class=\"tooltip-info\">\n          <span class=\"tooltip-content\">\n            Specify the environment variables. This allows you to override the <code>ENV</code> that is specified in\n            the <code>Dockerfile</code>, or provide new environment variables in case the <code>Dockerfile</code> does not\n            contain any <code>ENV</code>.\n            <br/>\n            <div class=\"code\">\n              JAVA_HOME=/opt/java<br/>\n              ANT_HOME=/opt/ant\n            </div>\n            <a href=\"https://docs.docker.com/engine/reference/builder/#env\"\n\t\t\t   target=\"_blank\">Read more about <code>ENV</code></a>\n          </span>\n\t\t\t\t</div>\n\t\t\t</label>\n\t\t\t<textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Environment].$error.server}\" type=\"text\"\n\t\t\t\t\t  ng-model=\"Environment\" ng-required=\"true\" rows=\"5\"></textarea>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Environment].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[Environment].$error.server\">{{GOINPUTNAME[Environment].$error.server}}</span>\n\t\t</div>\n\t</div>\n\n\t<div class=\"row\">\n\t\t<div class=\"columns end\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[Secrets].$error.server}\">Secrets\n\t\t\t\t<small>(Enter one secret per line)</small>\n\t\t\t\t<div class=\"tooltip-info\">\n              <span class=\"tooltip-content\">\n                  This allows users bind a secret with container. Enter each secret per line as mentioned in the following example:\n                  <div class=\"code\">\n                    src=AWS_PRIVATE_KEY, uid=1001, gid=10, mode=0640<br/>\n                    src=AWS_SECRET_KEY, target=/tmp/AWS_SECRET_KEY\n                  </div><br/>\n              </span>\n\t\t\t\t</div>\n\t\t\t</label>\n\t\t\t<textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Secrets].$error.server}\" ng-model=\"Secrets\"\n\t\t\t\t\t  rows=\"3\"></textarea>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Secrets].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[Secrets].$error.server\">{{GOINPUTNAME[Secrets].$error.server}}</span>\n\t\t</div>\n\t</div>\n\n\t<div class=\"row\">\n\t\t<div class=\"columns end\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[Networks].$error.server}\">Network attachments\n\t\t\t\t<small>(Enter one network name per line)</small>\n\t\t\t\t<div class=\"tooltip-info\">\n          <span class=\"tooltip-content\">\n            Attach a service to an existing network.\n            <div>Enter each network per line:\n              <div class=\"code\">\n                frontend<br/>\n                backend<br/>\n              </div><br/>\n            </div>\n          </span>\n\t\t\t\t</div>\n\t\t\t</label>\n\t\t\t<textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Networks].$error.server}\" ng-model=\"Networks\"\n\t\t\t\t\t  rows=\"3\"></textarea>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Networks].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[Networks].$error.server\">{{GOINPUTNAME[Networks].$error.server}}</span>\n\t\t</div>\n\t</div>\n\n\t<div class=\"row\">\n\t\t<div class=\"columns end\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[Mounts].$error.server}\">Volume Mounts\n\t\t\t\t<small>(Enter one mount per line)</small>\n\t\t\t\t<div class=\"tooltip-info\">\n          <span class=\"tooltip-content\">\n            This allows users to add mounts (\"bind\" and \"volume\") to a service.\n            <div>Enter each mount configuration per line and in the following format:\n              <div class=\"code\">\n                source=service-configuration, target=/etc/service, readonly<br/>\n                type=bind, source=/var/run/docker.sock, target=/var/run/docker.sock<br/>\n              </div><br/>\n              <em>Note: Requires docker version 17.06.x or higher.</em>\n            </div>\n          </span>\n\t\t\t\t</div>\n\t\t\t</label>\n\t\t\t<textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Mounts].$error.server}\" ng-model=\"Mounts\"\n\t\t\t\t\t  rows=\"3\"></textarea>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Mounts].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[Mounts].$error.server\">{{GOINPUTNAME[Mounts].$error.server}}</span>\n\t\t</div>\n\t</div>\n\n\t<div class=\"row\">\n\t\t<div class=\"columns end\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[Hosts].$error.server}\">Host entries\n\t\t\t\t<small>(Enter one host entry per line)</small>\n\t\t\t\t<div class=\"tooltip-info\">\n          <span class=\"tooltip-content\">\n            This allows users to add host entries in <code>/etc/hosts</code>.\n            <div>Enter each host entry per line and in the following format:\n              <div class=\"code\">\n                IP-ADDRESS HOSTNAME-1 HOSTNAME-2...<br/>\n                172.10.0.1 host-x<br/>\n                172.10.0.2 host-y host-z\n              </div><br/>\n              <em>Note: Requires docker version 17.04.x or higher.</em>\n            </div>\n          </span>\n\t\t\t\t</div>\n\t\t\t</label>\n\t\t\t<textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Hosts].$error.server}\" ng-model=\"Hosts\"\n\t\t\t\t\t  rows=\"3\"></textarea>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Hosts].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[Hosts].$error.server\">{{GOINPUTNAME[Hosts].$error.server}}</span>\n\t\t</div>\n\t</div>\n\n\t<div class=\"row\">\n\t\t<div class=\"columns end\">\n\t\t\t<label ng-class=\"{'is-invalid-label': GOINPUTNAME[Constraints].$error.server}\">Constraints\n\t\t\t\t<small>(Enter one constraint per line)</small>\n\t\t\t\t<div class=\"tooltip-info\">\n          <span class=\"tooltip-content tooltip-content-top\">\n            You can limit the set of nodes where a task can be scheduled by defining constraint expressions. Multiple constraints find nodes that satisfy every expression (AND match)\n            <div>Enter each constraint per line and in the following format:\n              <div class=\"code\">\n                node.labels.type == queue<br/>\n                node.labels.os == windows\n              </div><br/>\n              <a href=\"https://docs.docker.com/engine/reference/commandline/service_create/#specify-service-constraints-constraint\"\n\t\t\t\t target=\"_blank\">Read more about constraints</a>\n            </div>\n          </span>\n\t\t\t\t</div>\n\t\t\t</label>\n\t\t\t<textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Constraints].$error.server}\" ng-model=\"Constraints\"\n\t\t\t\t\t  rows=\"3\"></textarea>\n\t\t\t<span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Constraints].$error.server}\"\n\t\t\t\t  ng-show=\"GOINPUTNAME[Constraints].$error.server\">{{GOINPUTNAME[Constraints].$error.server}}</span>\n\t\t</div>\n\t</div>\n</div>"
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

  public static dockerPluginJSON(): any {
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
      plugin_file_location: "/Users/vishaldevgire/projects/gocd/server/plugins/external/docker-elastic-agents-2.0.0-160.jar",
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
          plugin_settings: {
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
              },
              {
                key: "Hosts",
                metadata: {
                  secure: false,
                  required: false
                }
              }
            ],
            view: {
              template: "<!--\n  ~ Copyright 2018 ThoughtWorks, Inc.\n  ~\n  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n  ~ you may not use this file except in compliance with the License.\n  ~ You may obtain a copy of the License at\n  ~\n  ~     http://www.apache.org/licenses/LICENSE-2.0\n  ~\n  ~ Unless required by applicable law or agreed to in writing, software\n  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n  ~ See the License for the specific language governing permissions and\n  ~ limitations under the License.\n  -->\n\n<div data-plugin-style-id=\"docker-elastic-plugin\">\n\n    <style>\n        [data-plugin-style-id=\"docker-elastic-plugin\"] .form-help-content {\n            color: #666;\n            font-style: italic;\n            clear: both;\n            font-size: 0.82rem;\n            margin-top: -15px;\n            margin-bottom: 10px;\n        }\n\n        [data-plugin-style-id=\"docker-elastic-plugin\"] .form-help-content .code {\n            padding: 16px;\n            overflow: auto;\n            font-size: 85%;\n            line-height: 1.45;\n            background-color: #f6f8fa;\n            border-radius: 3px;\n        }\n\n    </style>\n\n    <div class=\"form_item_block\">\n        <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Image].$error.server}\">Docker image:<span class=\"asterix\">*</span></label>\n        <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Image].$error.server}\" type=\"text\" ng-model=\"Image\" ng-required=\"true\" placeholder=\"alpine:latest\"/>\n        <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Image].$error.server}\" ng-show=\"GOINPUTNAME[Image].$error.server\">{{GOINPUTNAME[Image].$error.server}}</span>\n    </div>\n\n    <div class=\"form_item_block\">\n        <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Command].$error.server}\">Docker Command: <small>(Enter one parameter per line)</small></label>\n        <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Command].$error.server}\" type=\"text\" ng-model=\"Command\" ng-required=\"true\" rows=\"7\" placeholder=\"ls&#x000A;-al&#x000A;/usr/bin\"></textarea>\n        <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Command].$error.server}\" ng-show=\"GOINPUTNAME[Command].$error.server\">{{GOINPUTNAME[Command].$error.server}}</span>\n    </div>\n\n    <div class=\"form_item_block\">\n        <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Environment].$error.server}\">Environment Variables <small>(Enter one variable per line)</small></label>\n        <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Environment].$error.server}\" type=\"text\" ng-model=\"Environment\" ng-required=\"true\" rows=\"7\" placeholder=\"JAVA_HOME=/opt/java&#x000A;MAVEN_HOME=/opt/maven\"></textarea>\n        <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Environment].$error.server}\" ng-show=\"GOINPUTNAME[Environment].$error.server\">{{GOINPUTNAME[Environment].$error.server}}</span>\n    </div>\n\n    <div class=\"row\">\n        <div class=\"columns end\">\n            <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Hosts].$error.server}\">Host entries\n                <small>(Enter one host entry per line)</small>\n            </label>\n            <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[Hosts].$error.server}\" type=\"text\" ng-model=\"Hosts\" ng-required=\"true\" rows=\"7\"></textarea>\n            <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Hosts].$error.server}\" ng-show=\"GOINPUTNAME[Hosts].$error.server\">{{GOINPUTNAME[Hosts].$error.server}}</span>\n            <label class=\"form-help-content\">\n                This allows users to add host entries in <code>/etc/hosts</code>. Enter one host entry per line and in below mentioned format:\n                <div class=\"code\">\n                    IP-ADDRESS HOSTNAME-1 HOSTNAME-2...<br/>\n                    172.10.0.1 host-x<br/>\n                    172.10.0.2 host-y host-z\n                </div>\n            </label>\n        </div>\n    </div>\n</div>"
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

  public static dockerElasticProfile(): any {
    // noinspection TsLint
    return {
      _links: {
        self: {
          href: "http://localhost:8153/go/api/elastic/profiles/Profile2"
        },
        doc: {
          href: "https://api.gocd.org/current/#elastic-agent-profiles"
        },
        find: {
          href: "http://localhost:8153/go/api/elastic/profiles/:profile_id"
        }
      },
      id: "Profile2",
      plugin_id: "cd.go.contrib.elastic-agent.docker",
      can_administer: true,
      properties: [{
        key: "Image",
        value: "docker-image122345"
      }, {
        key: "Command",
        value: "ls\n-alh"
      }, {
        key: "Environment",
        value: "JAVA_HOME=/bin/java"
      }, {
        key: "Hosts"
      }]
    };
  }

  public static dockerSwarmElasticProfile(): any {
    // noinspection TsLint
    return {
      _links: {
        self: {
          href: "http://localhost:8153/go/api/elastic/profiles/Swarm1"
        },
        doc: {
          href: "https://api.gocd.org/current/#elastic-agent-profiles"
        },
        find: {
          href: "http://localhost:8153/go/api/elastic/profiles/:profile_id"
        }
      },
      id: "Swarm1",
      plugin_id: "cd.go.contrib.elastic-agent.docker-swarm",
      can_administer: true,
      properties: [{
        key: "Image",
        value: "Image1"
      }, {
        key: "Command"
      }, {
        key: "Environment"
      }, {
        key: "MaxMemory"
      }, {
        key: "ReservedMemory"
      }, {
        key: "Secrets"
      }, {
        key: "Networks"
      }, {
        key: "Mounts"
      }, {
        key: "Hosts"
      }, {
        key: "Constraints"
      }]
    };
  }

  public static kubernetesElasticProfile(): any {
    // noinspection TsLint
    return {
      _links: {
        self: {
          href: "http://localhost:8153/go/api/elastic/profiles/Kuber1"
        },
        doc: {
          href: "https://api.gocd.org/current/#elastic-agent-profiles"
        },
        find: {
          href: "http://localhost:8153/go/api/elastic/profiles/:profile_id"
        }
      },
      id: "Kuber1",
      plugin_id: "cd.go.contrib.elasticagent.kubernetes",
      can_administer: true,
      properties: [{
        key: "Image",
        value: "Image1"
      }, {
        key: "MaxMemory"
      }, {
        key: "MaxCPU"
      }, {
        key: "Environment"
      }, {
        key: "PodConfiguration",
        value: "apiVersion: v1\nkind: Pod\nmetadata:\n  name: pod-name-prefix-{{ POD_POSTFIX }}\n  labels:\n    app: web\nspec:\n  containers:\n    - name: gocd-agent-container-{{ CONTAINER_POSTFIX }}\n      image: {{ GOCD_AGENT_IMAGE }}:{{ LATEST_VERSION }}\n      securityContext:\n        privileged: true"
      }, {
        key: "SpecifiedUsingPodConfiguration",
        value: "false"
      }, {
        key: "Privileged"
      }]
    };
  }

  public static dockerClusterProfile(): any {
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
      can_administer: true,
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

  public static kubernetesClusterProfile(): any {
    return {
      _links: {
        self: {
          href: "http://localhost:8153/go/api/admin/plugin_settings/cd.go.contrib.elasticagent.kubernetes"
        },
        doc: {
          href: "https://api.gocd.org/19.3.0/#plugin-settings"
        },
        find: {
          href: "http://localhost:8153/go/api/admin/plugin_settings/:plugin_id"
        }
      },
      id: "cluster_1",
      plugin_id: "cd.go.contrib.elasticagent.kubernetes",
      can_administer: true,
      properties: [
        {
          key: "security_token",
          encrypted_value: "some-token"
        },
        {
          key: "go_server_url",
          value: "http://localhost:8153:8154/go"
        },
        {
          key: "kubernetes_cluster_url",
          value: "https://cluster_url.us-east-1.eks.amazonaws.com"
        },
        {
          key: "namespace"
        },
        {
          key: "kubernetes_cluster_ca_cert",
          encrypted_value: "encrypted_value"
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
}
