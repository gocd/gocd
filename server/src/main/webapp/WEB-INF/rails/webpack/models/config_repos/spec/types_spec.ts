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

import _ from "lodash";
import {ConfigRepo, MaterialModification, ParseInfo} from "models/config_repos/types";
import {GitMaterialAttributes, HgMaterialAttributes, Material, P4MaterialAttributes, SvnMaterialAttributes, TfsMaterialAttributes} from "models/materials/types";
import {ErrorIndex} from "models/shared/configuration";
import {ConfigRepoJSON} from "../serialization";

describe("Config Repo Types", () => {

  describe("Validation", () => {
    it("should validate ConfigRepo", () => {
      const configRepo = new ConfigRepo("", "");
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(2);
      expect(configRepo.errors().keys()).toEqual(["id", "pluginId"]);
    });

    it("should should validate Git material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("git", new GitMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material().errors().count()).toBe(0);
      expect(configRepo.material().attributes()!.errors().count()).toBe(1);
      expect(configRepo.material().attributes()!.errors().keys()).toEqual(["url"]);
    });

    it("should should validate SVN material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("svn", new SvnMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material().errors().count()).toBe(0);
      expect(configRepo.material().attributes()!.errors().count()).toBe(1);
      expect(configRepo.material().attributes()!.errors().keys()).toEqual(["url"]);
    });

    it("should should validate P4 material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("p4", new P4MaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material().errors().count()).toBe(0);
      expect(configRepo.material().attributes()!.errors().count()).toBe(2);
      expect(configRepo.material().attributes()!.errors().keys()).toEqual(["view", "port"]);
      expect(configRepo.material().attributes()!.errors().errorsForDisplay("port"))
        .toEqual("Host and port must be present.");
    });

    it("should should validate Hg material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("hg", new HgMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material().errors().count()).toBe(0);
      expect(configRepo.material().attributes()!.errors().count()).toBe(1);
      expect(configRepo.material().attributes()!.errors().keys()).toEqual(["url"]);
    });

    it("should should validate TFS material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("tfs", new TfsMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material().errors().count()).toBe(0);
      expect(configRepo.material().attributes()!.errors().count()).toBe(3);
      expect(configRepo.material().attributes()!.errors().keys()).toEqual(["url", "projectPath", "username"]);
    });

    it("validates configuration properties", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material(
        "hg", new HgMaterialAttributes(void 0, true, "repo.com"))
      );

      expect(configRepo.isValid()).toBe(true);

      configRepo.configuration([
        { key: "foo", value: "a" },
        { key: "bar", value: "?" },
        { key: "foo", value: "b" },
        { key: "", value: "blah" },
      ]);

      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().errorsForDisplay("configuration")).toBe("One or more properties is invalid.");

      const errors = configRepo.propertyErrors();

      expect(errors[0].key).toBe("foo");
      expect(hasErrors(errors[0])).toBe(true);
      expect(errorMsgs(errors[0])).toBe("Names must be unique.");

      expect(errors[1].key).toBe("bar");
      expect(hasErrors(errors[1])).toBe(false);
      expect(errorMsgs(errors[1])).toBeUndefined();

      expect(errors[2].key).toBe("foo");
      expect(hasErrors(errors[2])).toBe(true);
      expect(errorMsgs(errors[2])).toBe("Names must be unique.");

      expect(errors[3].key).toBe("");
      expect(hasErrors(errors[3])).toBe(true);
      expect(errorMsgs(errors[3])).toBe("Name is required.");

      function hasErrors(v: ErrorIndex) {
        return !!(v.errors && Object.keys(v.errors).length);
      }

      function errorMsgs(v: ErrorIndex) {
        if (v.errors) {
          return _.reduce(v.errors, (all, strs) => all.concat(strs!), [] as string[]).join(". ") + ".";
        }
        return;
      }
    });
  });

  describe("Should match against search text", () => {
    it("should match id", () => {
      const configRepo = createConfigRepo();
      expect(configRepo.matches("All_Test")).toBe(true);
    });

    it("should match good revision hash", () => {
      const configRepo = createConfigRepo();
      expect(configRepo.matches("2cda5f702c57757")).toBe(true);
      expect(configRepo.matches("2cda5f702c5775714c060124ff03957d")).toBe(true);
    });

    it("should match latest revision hash", () => {
      const configRepo = createConfigRepo();
      expect(configRepo.matches("4926940143a238f")).toBe(true);
      expect(configRepo.matches("4926940143a238fefb7566141ba24a96")).toBe(true);
    });

    it("should match material url", () => {
      const configRepo = createConfigRepo();
      expect(configRepo.matches("gocd")).toBe(true);
      expect(configRepo.matches("https://github")).toBe(true);
    });

    it("should not match invalid search text", () => {
      const configRepo = createConfigRepo();
      expect(configRepo.matches("random-string")).toBe(false);
    });

    it("should match if search text is null or empty", () => {
      const configRepo = createConfigRepo();
      expect(configRepo.matches("")).toBe(true);
    });

    function createConfigRepo() {
      const attributes         = new GitMaterialAttributes("SomeRepo", false, "https://github.com/gocd", "master");
      const material           = new Material("git", attributes);
      const goodModification   = new MaterialModification("developer", "dev@github.com", "2cda5f702c5775714c060124ff03957d", "Not my best work", "19:30");
      const latestModification = new MaterialModification("jrDev", "jrDev@github.com", "4926940143a238fefb7566141ba24a96", "My first commit", "19:30");
      const lastParse          = new ParseInfo(latestModification, goodModification, void 0);

      return new ConfigRepo("All_Test_Pipelines", "PluginId", material, false, [], lastParse);
    }
  });

  it('should deserialize json into config repo object', () => {
    const inputJson = {
      id:                          "config-repo-id-1",
      plugin_id:                   "yaml.config.plugin",
      material:                    {
        type:       "git",
        attributes: {
          name:        "name",
          auto_update: true,
          url:         "http://foo.com",
          branch:      "master"
        }
      },
      can_administer:              true,
      configuration:               [],
      material_update_in_progress: false,
      parse_info:                  {
        latest_parsed_modification: {
          username:      "username <username@googlegroups.com>",
          email_address: null,
          revision:      "b07d423864ec120362b3584635c",
          comment:       "some comment",
          modified_time: "2019-12-23T10:25:52Z"
        },
        good_modification:          {
          username:      "username <username@googlegroups.com>",
          email_address: null,
          revision:      "b07d6523f1252ab4ec120362b3584635c",
          comment:       "some comment",
          modified_time: "2019-12-23T10:25:52Z"
        }
      },
      rules:                       [
        {
          directive: "allow",
          action:    "refer",
          type:      "environment",
          resource:  "test-env"
        }
      ]
    } as ConfigRepoJSON;

    const configRepo = ConfigRepo.fromJSON(inputJson);

    expect(configRepo.id()).toBe(inputJson.id);
    expect(configRepo.pluginId()).toBe(inputJson.plugin_id);
    expect(configRepo.material().type()).toEqual(inputJson.material.type);
    expect(configRepo.material().name()).toEqual(inputJson.material.attributes.name);
    expect(configRepo.canAdminister()).toEqual(inputJson.can_administer);
    expect(configRepo.configuration()).not.toBeUndefined();
    expect(configRepo.lastParse()).not.toBeUndefined();
    expect(configRepo.materialUpdateInProgress()).toEqual(inputJson.material_update_in_progress);
    expect(configRepo.rules()).not.toBeUndefined();
  });
});
