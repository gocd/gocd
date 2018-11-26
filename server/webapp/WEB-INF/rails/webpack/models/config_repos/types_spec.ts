/*
 * Copyright 2018 ThoughtWorks, Inc.
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
  GitMaterialAttributes, HgMaterialAttributes,
  IGNORED_MATERIAL_ATTRIBUTES,
  Material, P4MaterialAttributes,
  SvnMaterialAttributes, TfsMaterialAttributes
} from "models/config_repos/types";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

describe("Config Repo Types", () => {
  it("should ignore attributes that are in the ValidatableMixin", () => {
    const properties = Object.getOwnPropertyNames(new ValidatableMixin());
    expect(IGNORED_MATERIAL_ATTRIBUTES).toEqual(properties);
  });

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
      expect(configRepo.material().attributes().errors().count()).toBe(2);
      expect(configRepo.material().attributes().errors().keys()).toEqual(["name", "url"]);
    });

    it("should should validate SVN material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("git", new SvnMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material().errors().count()).toBe(0);
      expect(configRepo.material().attributes().errors().count()).toBe(2);
      expect(configRepo.material().attributes().errors().keys()).toEqual(["name", "url"]);
    });

    it("should should validate P4 material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("git", new P4MaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material().errors().count()).toBe(0);
      expect(configRepo.material().attributes().errors().count()).toBe(2);
      expect(configRepo.material().attributes().errors().keys()).toEqual(["name", "view"]);
    });

    it("should should validate Hg material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("git", new HgMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material().errors().count()).toBe(0);
      expect(configRepo.material().attributes().errors().count()).toBe(2);
      expect(configRepo.material().attributes().errors().keys()).toEqual(["name", "url"]);
    });

    it("should should validate TFS material attributes", () => {
      const configRepo = new ConfigRepo("id", "pluginId", new Material("git", new TfsMaterialAttributes()));
      expect(configRepo.isValid()).toBe(false);
      expect(configRepo.errors().count()).toBe(0);
      expect(configRepo.material().errors().count()).toBe(0);
      expect(configRepo.material().attributes().errors().count()).toBe(3);
      expect(configRepo.material().attributes().errors().keys()).toEqual(["name", "url", "projectPath"]);
    });
  });
});
