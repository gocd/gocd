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
  GitMaterialAttributes,
  HgMaterialAttributes,
  Material,
  P4MaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "models/materials/types";

describe("Material Types", () => {

  describe("Validation", () => {
    it("should should validate Git material attributes", () => {
      const material = new Material("git", new GitMaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["url"]);
    });

    it("should should validate SVN material attributes", () => {
      const material = new Material("svn", new SvnMaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["url"]);
    });

    it("should should validate P4 material attributes", () => {
      const material = new Material("p4", new P4MaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes().errors().count()).toBe(2);
      expect(material.attributes().errors().keys()).toEqual(["view", "port"]);
      expect(material.attributes().errors().errorsForDisplay("port"))
        .toEqual("Host and port must be present.");
    });

    it("should should validate Hg material attributes", () => {
      const material = new Material("hg", new HgMaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["url"]);
    });

    it("should should validate TFS material attributes", () => {
      const material = new Material("tfs", new TfsMaterialAttributes());
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes().errors().count()).toBe(4);
      expect(material.attributes().errors().keys())
        .toEqual(["url", "projectPath", "username", "password"]);
    });

    it("should should validate Git URL credentials", () => {
      const material = new Material("git", new GitMaterialAttributes(undefined, true, "http://user:pass@host", "master", "user", "pass"));
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["url"]);
      expect(material.attributes().errors().errorsForDisplay("url")).toBe("URL credentials must be set in either the URL or the username+password fields, but not both.");
    });

    it("should should validate Hg URL credentials", () => {
      const material = new Material("hg", new HgMaterialAttributes(undefined, true, "http://user:pass@host", "user", "pass"));
      expect(material.isValid()).toBe(false);
      expect(material.errors().count()).toBe(0);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["url"]);
      expect(material.attributes().errors().errorsForDisplay("url")).toBe("URL credentials must be set in either the URL or the username+password fields, but not both.");
    });
  });
});
