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

import {AboutJSON, PackageRepoExtensionJSON, PluginInfoJSON} from "models/shared/plugin_infos_new/serialization";
import {activeStatus, someVendor} from "models/shared/plugin_infos_new/spec/test_data";
import {PackageJSON, PackageRepositoryJSON} from "../package_repositories_json";

export function getPackageRepository() {
  return {
    repo_id:         "pkg-repo-id",
    name:            "pkg-repo-name",
    plugin_metadata: {
      id:      "npm",
      version: "1"
    },
    configuration:   [{
      key:   "REPO_URL",
      value: "https://npm.com"
    }],
    _embedded:       {
      packages: [getPackage()]
    }
  } as PackageRepositoryJSON;
}

export function getPackage() {
  return {
    id:            "pkg-id",
    name:          "pkg-name",
    auto_update:   true,
    configuration: [{
      key:   "PACKAGE_ID",
      value: "pkg"
    }],
    package_repo:  {
      id:   "pkg-repo-id",
      name: "pkg-repo-name"
    }
  } as PackageJSON;
}

function about(): AboutJSON {
  return {
    name:                     "NPM plugin for package repo",
    version:                  "0.6.1",
    target_go_version:        "16.12.0",
    description:              "NPM plugin for package repo",
    target_operating_systems: [
      "Linux",
      "Mac OS X"
    ],
    vendor:                   someVendor
  } as AboutJSON;
}

export function pluginInfoWithPackageRepositoryExtension(): PluginInfoJSON {
  return {
    _links:               {},
    id:                   "npm",
    status:               activeStatus,
    plugin_file_location: "/foo/bar.jar",
    bundled_plugin:       false,
    about:                about(),
    extensions:           [
      {
        type:                "package-repository",
        package_settings:    {
          configurations: [
            {
              key:      "PACKAGE_ID",
              metadata: {
                part_of_identity: true,
                display_order:    0,
                secure:           false,
                display_name:     "Package ID",
                required:         true
              }
            },
            {
              key:      "POLL_VERSION_FROM",
              metadata: {
                part_of_identity: false,
                display_order:    1,
                secure:           false,
                display_name:     "Version to poll >=",
                required:         false
              }
            },
            {
              key:      "POLL_VERSION_TO",
              metadata: {
                part_of_identity: false,
                display_order:    2,
                secure:           false,
                display_name:     "Version to poll <",
                required:         false
              }
            },
            {
              key:      "INCLUDE_PRE_RELEASE",
              metadata: {
                part_of_identity: false,
                display_order:    3,
                secure:           false,
                display_name:     "Include Prerelease? (yes/no, defaults to yes)",
                required:         false
              }
            }
          ]
        },
        repository_settings: {
          configurations: [
            {
              key:      "REPO_URL",
              metadata: {
                part_of_identity: true,
                display_order:    0,
                secure:           false,
                display_name:     "Repository Url",
                required:         true
              }
            },
            {
              key:      "USERNAME",
              metadata: {
                part_of_identity: false,
                display_order:    1,
                secure:           false,
                display_name:     "Username",
                required:         false
              }
            },
            {
              key:      "PASSWORD",
              metadata: {
                part_of_identity: false,
                display_order:    2,
                secure:           true,
                display_name:     "Password (use only with https)",
                required:         false
              }
            }
          ]
        },
        plugin_settings:     {
          configurations: [
            {
              key:      "another-property",
              metadata: {
                secure:   false,
                required: true
              }
            }
          ],
          view:           {
            template: "Plugin Settings View for package repository plugin"
          }
        }
      } as PackageRepoExtensionJSON
    ]
  } as PluginInfoJSON;
}
