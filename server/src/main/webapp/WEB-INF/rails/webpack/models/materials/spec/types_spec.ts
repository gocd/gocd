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

import {Filter} from "models/maintenance_mode/material";
import {Hg} from "models/materials/spec/material_test_data";
import {
  DependencyMaterialAttributes,
  GitMaterialAttributes,
  HgMaterialAttributes,
  Material,
  P4MaterialAttributes,
  PackageMaterialAttributes,
  PluggableScmMaterialAttributes,
  ScmMaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "models/materials/types";
import {Configurations} from "../../shared/configuration";
import {PluginMetadata, Scm} from "../pluggable_scm";
import {DependencyMaterialAttributesJSON} from "../serialization";

describe("Material Types", () => {
  describe("Deserialize", () => {
    it("should deserialize hg material with branch", () => {
      const hgJson               = Hg.withBranch();
      const hgMaterialAttributes = HgMaterialAttributes.fromJSON(hgJson);

      expect(hgMaterialAttributes.branch()).toBe(hgJson.branch);
    });

    it('should deserialize dependency material', () => {
      const json = {
        name:                  "dependencyMaterial",
        pipeline:              "upstream",
        stage:                 "stage",
        ignore_for_scheduling: false
      } as DependencyMaterialAttributesJSON;

      const materialAttrs = DependencyMaterialAttributes.fromJSON(json);

      expect(materialAttrs.name()).toBe(json.name);
      expect(materialAttrs.pipeline()).toBe(json.pipeline);
      expect(materialAttrs.stage()).toBe(json.stage);
      expect(materialAttrs.ignoreForScheduling()).toBeFalse();
    });
  });

  describe("Validation", () => {
    it("should should validate Git material attributes", () => {
      const material = new Material("git", new GitMaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(1);
      expect(material.attributes()!.errors().keys()).toEqual(["url"]);
    });

    it("should should validate SVN material attributes", () => {
      const material = new Material("svn", new SvnMaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(1);
      expect(material.attributes()!.errors().keys()).toEqual(["url"]);
    });

    it("should should validate P4 material attributes", () => {
      const material = new Material("p4", new P4MaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(2);
      expect(material.attributes()!.errors().keys()).toEqual(["view", "port"]);
      expect(material.attributes()!.errors().errorsForDisplay("port"))
        .toEqual("Host and port must be present.");
    });

    it("should should validate Hg material attributes", () => {
      const material = new Material("hg", new HgMaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(1);
      expect(material.attributes()!.errors().keys()).toEqual(["url"]);
    });

    it("should should validate TFS material attributes", () => {
      const material = new Material("tfs", new TfsMaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(4);
      expect(material.attributes()!.errors().keys())
        .toEqual(["url", "projectPath", "username", "password"]);
    });

    it("should validate name if provided", () => {
      const material = new Material("git", new GitMaterialAttributes(undefined, true, "http://host"));
      expect(material.isValid()).toBe(true);

      material.attributes()!.name("foo bar baz");
      expect(material.name()).toBe("foo bar baz");

      expect(material.isValid()).toBe(false);
      expect(material.attributes()!.errors().count()).toBe(1);
      expect(material.attributes()!.errors().keys()).toEqual(["name"]);
      expect(material.attributes()!.errors().errorsForDisplay("name"))
        .toBe(
          "Invalid name. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    });

    it("should validate destination directory if provided", () => {
      const attrs    = new GitMaterialAttributes(undefined, true, "http://host");
      const material = new Material("git", attrs);

      assertValidPaths(material, attrs, [
        "",
        "foo/bar",
        "./somepath",
        "here",
        "here or there"
      ]);

      assertInvalidPaths(material, attrs, [
        "..",
        "../up",
        ". ",
        " .",
        "/root"
      ]);

      function assertValidPaths(material: Material, attrs: ScmMaterialAttributes, paths: string[]) {
        for (const path of paths) {
          attrs.destination(path);
          expect(material.isValid()).toBe(true, `${path} should be valid`);
          expect(attrs.errors().hasErrors("destination")).toBe(false, `${path} should yield no errors`);
        }
      }

      function assertInvalidPaths(material: Material, attrs: ScmMaterialAttributes, paths: string[]) {
        for (const path of paths) {
          attrs.destination(path);
          expect(material.isValid()).toBe(false, `${path} should be invalid`);
          expect(attrs.errors().hasErrors("destination")).toBe(true, `${path} should yield errors`);
          expect(attrs.errors().errorsForDisplay("destination"))
            .toBe("Must be a relative path within the pipeline's working directory.");
        }
      }
    });

    it("should should allow Git SCP-style URLs", () => {
      const material = new Material("git", new GitMaterialAttributes(undefined, true, "git@host:repo.git"));
      expect(material.isValid()).toBe(true);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.isValid()).toBe(true);
      expect(material.attributes()!.errors().count()).toBe(0);
    });

    it("should should allow SSH URLs", () => {
      const material = new Material("git", new GitMaterialAttributes(undefined, true, "ssh://git@host/repo.git"));
      expect(material.isValid()).toBe(true);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.isValid()).toBe(true);
      expect(material.attributes()!.errors().count()).toBe(0);
    });

    it("should should validate Git URL credentials", () => {
      const material = new Material("git",
                                    new GitMaterialAttributes(undefined,
                                                              true,
                                                              "http://user:pass@host",
                                                              "master",
                                                              "user",
                                                              "pass"));
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(1);
      expect(material.attributes()!.errors().keys()).toEqual(["url"]);
      expect(material.attributes()!.errors().errorsForDisplay("url"))
        .toBe("URL credentials must be set in either the URL or the username+password fields, but not both.");
    });

    it("should should validate Hg URL credentials", () => {
      const material = new Material("hg",
                                    new HgMaterialAttributes(undefined, true, "http://user:pass@host", "user", "pass"));
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(1);
      expect(material.attributes()!.errors().keys()).toEqual(["url"]);
      expect(material.attributes()!.errors().errorsForDisplay("url"))
        .toBe("URL credentials must be set in either the URL or the username+password fields, but not both.");
    });

    it('should validate Scm material', () => {
      const material = new Scm("", "", false, new PluginMetadata("", ""), new Configurations([]));

      const isValid = material.isValid();

      expect(isValid).toBeFalse();

      expect(material.errors().count()).toBe(1);
      expect(material.errors().errorsForDisplay('name')).toBe('Name must be present.');

      expect(material.pluginMetadata().errors().count()).toBe(1);
      expect(material.pluginMetadata().errors().errorsForDisplay('id')).toBe('Id must be present.');
    });

    it("should should validate not validate tfs password", () => {
      const material = new Material("tfs", new TfsMaterialAttributes("", false, undefined, undefined, undefined, undefined, undefined, undefined, false));
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(3);
      expect(material.attributes()!.errors().keys()).toEqual(["url", "projectPath", "username"]);
    });

    it('should validate Package material attributes', () => {
      const attrs    = new PackageMaterialAttributes();
      const material = new Material("package", attrs);

      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(1);
      expect(material.attributes()!.errors().keys()).toEqual(["ref"]);
      expect(material.attributes()!.errors().allErrorsForDisplay()).toEqual(['A package reference must be present.']);
    });

    it('should validate Plugin material attributes', () => {
      const attrs    = new PluggableScmMaterialAttributes("", false, "", "", new Filter([]));
      const material = new Material("plugin", attrs);

      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes()!.errors().count()).toBe(1);
      expect(material.attributes()!.errors().keys()).toEqual(["ref"]);
    });
  });

  describe('Serialization', () => {
    it('should serialize dependency materials', () => {
      const dependencyAttrs = new DependencyMaterialAttributes("name", false, "pipeline", "stage", false);

      expect(Object.keys(dependencyAttrs.toJSON())).not.toContain('destination');
    });

    it('should serialize git materials', () => {
      const gitAttrs = new GitMaterialAttributes("name", false, "some-url");

      expect(Object.keys(gitAttrs.toJSON())).not.toContain('destination');

      gitAttrs.destination("some-dest");
      expect(Object.keys(gitAttrs.toJSON())).toContain('destination');
    });
  });

  it('should reset password if it is present and has been updated', () => {
    const material = new Material("git", new GitMaterialAttributes("name", true, "some-url", "master", "username", "password"));
    const attrs    = (material.attributes() as GitMaterialAttributes);
    attrs.password().edit();

    expect(attrs.password().value()).toBe('');

    material.resetPasswordIfAny();

    expect(attrs.password().value()).toBe('password');
  });

  describe('Clone', () => {
    it('should clone only the type is attrs are not present', () => {
      const material = new Material("git");
      const clone    = material.clone();

      expect(clone.type()).toBe(material.type());
      expect(clone.attributes()).toBeUndefined();
    });

    it('should clone the attrs as well', () => {
      const gitAttrs = new GitMaterialAttributes("name", true, "some-url", "master", "username", "password");
      gitAttrs.destination("some-destination");
      const material = new Material("git", gitAttrs);
      const clone    = material.clone();

      expect(clone.type()).toBe(material.type());
      expect(clone.attributes()!.name()).toEqual(material.attributes()!.name());
      expect(clone.attributes()!.autoUpdate()).toEqual(material.attributes()!.autoUpdate());
      expect((clone.attributes()! as GitMaterialAttributes).url()).toEqual((material.attributes()! as GitMaterialAttributes).url());
      expect((clone.attributes()! as GitMaterialAttributes).branch()).toEqual((material.attributes()! as GitMaterialAttributes).branch());
      expect((clone.attributes()! as GitMaterialAttributes).username()).toEqual((material.attributes()! as GitMaterialAttributes).username());
      expect((clone.attributes()! as GitMaterialAttributes).password().valueForDisplay()).toEqual((material.attributes()! as GitMaterialAttributes).password().valueForDisplay());
      expect((clone.attributes()! as GitMaterialAttributes).destination()).toEqual((material.attributes()! as GitMaterialAttributes).destination());
    });
  });

});
