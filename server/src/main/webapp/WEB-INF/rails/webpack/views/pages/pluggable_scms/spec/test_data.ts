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

import {ScmJSON} from "models/materials/pluggable_scm";
import {PluginInfoJSON} from "models/shared/plugin_infos_new/serialization";

export function getPluggableScm() {
  return {
    id:              "scm-id",
    name:            "pluggable.scm.material.name",
    origin:          {
      type: "gocd"
    },
    plugin_metadata: {
      id:      "scm-plugin-id",
      version: "1"
    },
    auto_update:     true,
    configuration:   [
      {
        key:   "url",
        value: "https://github.com/sample/example.git"
      }
    ]
  } as ScmJSON;
}

export function getScmPlugin() {
  return {
    _links:               {
      self: {
        href: "http://test-server:8153/go/api/admin/plugin_info/github.pr"
      },
      doc:  {
        href: "https://api.gocd.org/#plugin-info"
      },
      find: {
        href: "http://test-server:8153/go/api/admin/plugin_info/:id"
      }
    },
    id:                   "scm-plugin-id",
    status:               {
      state: "active"
    },
    plugin_file_location: "/tmp/abc.jar",
    bundled_plugin:       false,
    about:                {
      name:                     "SCM Plugin",
      version:                  "1.4.0-RC2",
      target_go_version:        "15.1.0",
      description:              "Plugin that polls a GitHub repository for pull requests and triggers a build for each of them",
      target_operating_systems: [],
      vendor:                   {
        name: "User",
        url:  "https://github.com/user/abc"
      }
    },
    extensions:           [{
      type:         "scm",
      display_name: "Github",
      scm_settings: {
        configurations: [{
          key:      "url",
          metadata: {
            secure:           false,
            required:         true,
            part_of_identity: true
          }
        }, {
          key:      "username",
          metadata: {
            secure:           false,
            required:         false,
            part_of_identity: false
          }
        }, {
          key:      "password",
          metadata: {
            secure:           true,
            required:         false,
            part_of_identity: false
          }
        }, {
          key:      "defaultBranch",
          metadata: {
            secure:           false,
            required:         false,
            part_of_identity: false
          }
        }, {
          key:      "shallowClone",
          metadata: {
            secure:           false,
            required:         false,
            part_of_identity: false
          }
        }],
        view:           {
          template: "<div class=\"form_item_block\">\n    <label>URL:<span class=\"asterisk\">*</span></label>\n    <input type=\"text\" ng-model=\"url\" ng-required=\"true\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[url].$error.server\">{{ GOINPUTNAME[url].$error.server }}</span>\n</div>\n<div class=\"form_item_block\">\n    <label>Username:</label>\n    <input type=\"text\" autocomplete=\"new-password\" ng-model=\"username\" ng-required=\"false\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[username].$error.server\">{{ GOINPUTNAME[username].$error.server }}</span>\n</div>\n<div class=\"form_item_block\">\n    <label>Password:</label>\n    <input type=\"password\" autocomplete=\"new-password\" ng-model=\"password\" ng-required=\"false\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[password].$error.server\">{{ GOINPUTNAME[password].$error.server }}</span>\n</div>\n<div class=\"form_item_block\">\n    <label>Default Branch:</label>\n    <input type=\"text\" ng-model=\"defaultBranch\" ng-required=\"false\"/>\n    <span class=\"form_error\" ng-show=\"GOINPUTNAME[defaultBranch].$error.server\">{{ GOINPUTNAME[defaultBranch].$error.server }}</span>\n</div>\n"
        }
      }
    }]
  } as PluginInfoJSON;
}
