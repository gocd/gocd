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

import {ConfigRepo} from "models/config_repos/types";
import {allAttributes, resolveHumanReadableAttributes} from "../config_repo_attribute_helper";

describe("ConfigRepo attribute util functions", () => {
  it("resolveHumanReadableAttributes() emits a new attribute map with human-friendly keys", () => {
    const map = resolveHumanReadableAttributes({
      autoUpdate: true,
      branch: true,
      checkExternals: true,
      destination: true,
      domain: true,
      encryptedPassword: true,
      name: true,
      projectPath: true,
      url: true,
      username: true,
      password: true,
      port: true,
      useTickets: true,
      view: true,
      emailAddress: true,
      revision: true,
      comment: true,
      modifiedTime: true,
      file_pattern: true,
      this_key_is_not_modified_at_all: true,
    });

    expect(Array.from(map.keys()).sort()).toEqual([
      "Alternate Checkout Path",
      "Auto-update",
      "Branch",
      "Check Externals",
      "Comment",
      "Domain",
      "Email",
      "File Pattern",
      "Host and Port",
      "Material Name",
      "Modified Time",
      "Password",
      "Project Path",
      "Revision",
      "URL",
      "Use Tickets",
      "Username",
      "View",
      "this_key_is_not_modified_at_all",
    ].sort());
  });

  it("allAttributes() extracts material and custom configuration attributes from a ConfigRepo", () => {
    const repo = ConfigRepo.fromJSON({
      material: {
        type: "git",
        attributes: {
          url: "https://example.com/git/my-repo",
          name: "foo",
          username: "bob",
          encrypted_password: "AES:foo:bar",
          auto_update: true,
          branch: "master",
          destination: ""
        }
      },
      can_administer: false,
      configuration: [{
        key: "file_pattern",
        value: "*.json"
      }],
      parse_info: {},
      id: "my-repo",
      plugin_id: "json.config.plugin",
      material_update_in_progress: false,
      rules: []
    });

    const map = allAttributes(repo);

    const expectations: { [key: string]: string } = {
      "Type": "git",
      "Username": "bob",
      "Password": "********************************",
      "URL": "https://example.com/git/my-repo",
      "Branch": "master",
      "File Pattern": "*.json",
    };

    const keys = Object.keys(expectations);

    for (const k of keys) {
      expect(map.has(k)).toBe(true, `missing key ${k}`);
      expect(map.get(k)).toBe(expectations[k], `wrong value at key ${k}`);
    }
  });
});
