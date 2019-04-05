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
