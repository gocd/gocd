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

import m from "mithril";
import {PackageRepository} from "models/package_repositories/package_repositories";
import {getPackageRepository} from "models/package_repositories/spec/test_data";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PackageRepoExtensionJSON, PluginInfoJSON} from "models/shared/plugin_infos_new/serialization";
import {about, activeStatus} from "models/shared/plugin_infos_new/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {PackageRepositoryWidget} from "../package_repository_widget";

describe('PackageRepositoryWidgetSpec', () => {
  const helper = new TestHelper();
  let packageRepository: PackageRepository;
  let pluginInfo: PluginInfo;

  const pluginInfoWithPackageRepositoryExtension: PluginInfoJSON = {
    _links:               {},
    id:                   "nuget",
    status:               activeStatus,
    plugin_file_location: "/foo/bar.jar",
    bundled_plugin:       false,
    about,
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
  };

  beforeEach(() => {
    packageRepository = PackageRepository.fromJSON(getPackageRepository());
    pluginInfo        = PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension);
  });
  afterEach((done) => helper.unmount(done));

  function mount() {
    helper.mount(() => <PackageRepositoryWidget packageRepository={packageRepository} pluginInfo={pluginInfo}/>);
  }

  it('should render package repo details', () => {
    mount();
  });

});
