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
import {
  ConfigRepo,
  MaterialModification, ParseInfo, Permissions,
} from "models/config_repos/types";

import {
  GitMaterialAttributes,
  HgMaterialAttributes,
  Material,
  P4MaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "models/materials/types";

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
      expect(configRepo.material()!.errors().count()).toBe(0);
      expect(configRepo.material()!.attributes()!.errors().count()).toBe(1);
      expect(configRepo.material()!.attributes()!.errors().keys()).toEqual(["url"]);
    });

    it("should should validate SVN material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("svn", new SvnMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material()!.errors().count()).toBe(0);
      expect(configRepo.material()!.attributes()!.errors().count()).toBe(1);
      expect(configRepo.material()!.attributes()!.errors().keys()).toEqual(["url"]);
    });

    it("should should validate P4 material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("p4", new P4MaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material()!.errors().count()).toBe(0);
      expect(configRepo.material()!.attributes()!.errors().count()).toBe(2);
      expect(configRepo.material()!.attributes()!.errors().keys()).toEqual(["view", "port"]);
      expect(configRepo.material()!.attributes()!.errors().errorsForDisplay("port"))
        .toEqual("Host and port must be present.");
    });

    it("should should validate Hg material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("hg", new HgMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material()!.errors().count()).toBe(0);
      expect(configRepo.material()!.attributes()!.errors().count()).toBe(1);
      expect(configRepo.material()!.attributes()!.errors().keys()).toEqual(["url"]);
    });

    it("should should validate TFS material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("tfs", new TfsMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material()!.errors().count()).toBe(0);
      expect(configRepo.material()!.attributes()!.errors().count()).toBe(4);
      expect(configRepo.material()!.attributes()!.errors().keys())
        .toEqual(["url", "projectPath", "username", "password"]);
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
        expect(configRepo.matches("https://githib")).toBe(true);
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
        const attributes = new GitMaterialAttributes("SomeRepo", false, "https://githib.com/gocd", "master");
        const material = new Material("git", attributes);
        const goodModification = new MaterialModification("developer", "dev@github.com", "2cda5f702c5775714c060124ff03957d", "Not my best work", "19:30");
        const latestModification = new MaterialModification("jrDev", "jrDev@github.com", "4926940143a238fefb7566141ba24a96", "My first commit", "19:30");
        const lastParse = new ParseInfo(latestModification, goodModification, null);

        return new ConfigRepo("All_Test_Pipelines", "PluginId", material, new Permissions(false, false), [], lastParse);
      }
    });
  });
});
