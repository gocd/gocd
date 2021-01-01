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

import {ArtifactStoresJSON} from "models/artifact_stores/artifact_stores";

export class ArtifactStoreTestData {
  static artifactStoreList(...objects: any[]) {
    return {
      _links: {
        self: {
          href: "https://ci.example.com/go/api/admin/artifact_stores"
        },
        doc: {
          href: "https://api.gocd.org/current/#artifact_stores"
        },
        find: {
          href: "https://ci.example.com/go/api/admin/artifact_stores/:id"
        }
      },
      _embedded: {
        artifact_stores: objects
      }
    } as ArtifactStoresJSON;
  }

  static dockerArtifactStore() {
    return {
      id: "hub.docker",
      plugin_id: "cd.go.artifact.docker.registry",
      properties: [{
        key: "RegistryURL",
        value: "https://your_docker_registry_url"
      }, {
        key: "Username",
        value: "admin"
      }, {
        key: "Password",
        encrypted_value: "AES:tdfTtYtIUSAF2JXJP/3YwA==:43Kjidjuh42NHKisCAs/BQ=="
      }]
    };
  }

  static mavenArtifactStore() {
    return {
      id: "maven.central",
      plugin_id: "cd.go.artifact.maven.registry",
      properties: [{
        key: "RegistryURL",
        value: "https://your.maven.registry"
      }, {
        key: "Username",
        value: "bob"
      }, {
        key: "Password",
        encrypted_value: "AES:tdfTtYtIUSAF2JXJP/3YwA==:43Kjidjuh42NHKisCAs/BQ=="
      }]
    };
  }
}
