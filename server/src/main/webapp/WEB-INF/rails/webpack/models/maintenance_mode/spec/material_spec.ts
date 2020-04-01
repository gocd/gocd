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
import {timeFormatter} from "helpers/time_formatter";
import {
  Filter,
  Material,
  MaterialJSON,
  P4MaterialAttributesJSON,
  ScmAttributesJSON,
  ScmMaterialAttributes,
  TfsMaterialAttributesJSON
} from "models/maintenance_mode/material";

describe("Material specs", () => {

  it("should deserialize to git material", () => {
    const materialJSON       = TestData.git();
    const scmAttributesJSON  = materialJSON.attributes as ScmAttributesJSON;
    const material           = Material.fromJSON(materialJSON);
    const materialAttributes = material.attributes() as ScmMaterialAttributes;

    expect(material.type()).toEqual("git");
    expect(material.mduStartTime()).toEqual(timeFormatter.toDate(materialJSON.mdu_start_time));
    expect(materialAttributes.name()).toEqual(scmAttributesJSON.name);
    expect(materialAttributes.url()).toEqual(scmAttributesJSON.url);
    expect(materialAttributes.autoUpdate()).toEqual(scmAttributesJSON.auto_update);
    expect(materialAttributes.destination()).toEqual(scmAttributesJSON.destination);
    expect(materialAttributes.filter().ignore()).toEqual(Filter.fromJSON(scmAttributesJSON.filter).ignore());
    expect(materialAttributes.invertFilter()).toEqual(scmAttributesJSON.invert_filter);
  });

  it("should deserialize to svn material", () => {
    const materialJSON       = TestData.svn();
    const attributesJSON     = materialJSON.attributes as ScmAttributesJSON;
    const material           = Material.fromJSON(materialJSON);
    const materialAttributes = material.attributes() as ScmMaterialAttributes;

    expect(material.type()).toEqual("svn");
    expect(material.mduStartTime()).toEqual(timeFormatter.toDate(materialJSON.mdu_start_time));
    expect(materialAttributes.name()).toEqual(attributesJSON.name);
    expect(materialAttributes.url()).toEqual(attributesJSON.url);
    expect(materialAttributes.autoUpdate()).toEqual(attributesJSON.auto_update);
    expect(materialAttributes.destination()).toEqual(attributesJSON.destination);
    expect(materialAttributes.filter().ignore()).toEqual(Filter.fromJSON(attributesJSON.filter).ignore());
    expect(materialAttributes.invertFilter()).toEqual(attributesJSON.invert_filter);
  });

  it("should deserialize to hg material", () => {
    const materialJSON       = TestData.hg();
    const attributesJSON     = materialJSON.attributes as ScmAttributesJSON;
    const material           = Material.fromJSON(materialJSON);
    const materialAttributes = material.attributes() as ScmMaterialAttributes;

    expect(material.type()).toEqual("hg");
    expect(material.mduStartTime()).toEqual(timeFormatter.toDate(materialJSON.mdu_start_time));

    expect(materialAttributes.name()).toEqual(attributesJSON.name);
    expect(materialAttributes.url()).toEqual(attributesJSON.url);
    expect(materialAttributes.autoUpdate()).toEqual(attributesJSON.auto_update);
    expect(materialAttributes.destination()).toEqual(attributesJSON.destination);
    expect(materialAttributes.filter().ignore()).toEqual(Filter.fromJSON(attributesJSON.filter).ignore());
    expect(materialAttributes.invertFilter()).toEqual(attributesJSON.invert_filter);
  });

  it("should validate URL presence", () => {
    const materialJSON   = TestData.git();
    const attributesJSON = materialJSON.attributes as ScmAttributesJSON;

    delete attributesJSON.url;
    const material = Material.fromJSON(materialJSON);

    expect(material.attributes().isValid()).toBe(false);
    expect(material.attributes().errors().count()).toBe(1);
    expect(material.attributes().errors().keys()).toEqual(["url"]);
  });

  describe("P4 test", () => {
    it("should validate presence of view", () => {
      const materialJSON   = TestData.p4();
      const attributesJSON = materialJSON.attributes as P4MaterialAttributesJSON;

      delete attributesJSON.view;
      const material = Material.fromJSON(materialJSON);

      expect(material.attributes().isValid()).toBe(false);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["view"]);
    });

    it("should validate presence of port", () => {
      const materialJSON   = TestData.p4();
      const attributesJSON = materialJSON.attributes as P4MaterialAttributesJSON;

      delete attributesJSON.port;
      const material = Material.fromJSON(materialJSON);

      expect(material.attributes().isValid()).toBe(false);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["port"]);
    });
  });

  describe("Tfs test", () => {
    it("should validate presence of projectPath", () => {
      const materialJSON   = TestData.tfs();
      const attributesJSON = materialJSON.attributes as TfsMaterialAttributesJSON;

      delete attributesJSON.project_path;
      const material = Material.fromJSON(materialJSON);

      expect(material.attributes().isValid()).toBe(false);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["projectPath"]);
    });

    it("should validate presence of username", () => {
      const materialJSON   = TestData.tfs();
      const attributesJSON = materialJSON.attributes as TfsMaterialAttributesJSON;

      delete attributesJSON.username;
      const material = Material.fromJSON(materialJSON);

      expect(material.attributes().isValid()).toBe(false);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["username"]);
    });

    it("should validate presence of password", () => {
      const materialJSON   = TestData.tfs();
      const attributesJSON = materialJSON.attributes as TfsMaterialAttributesJSON;

      delete attributesJSON.password;
      const material = Material.fromJSON(materialJSON);

      expect(material.attributes().isValid()).toBe(false);
      expect(material.attributes().errors().count()).toBe(1);
      expect(material.attributes().errors().keys()).toEqual(["password"]);
    });
  });

  it('should convert filter into json correctly', () => {
    const filter = new Filter([]);

    expect(filter.toJSON()).toBeNull();

    filter.ignore(['abc', 'xyz']);

    expect(filter.toJSON()).toEqual({ignore: ['abc', 'xyz']});
  });
});

class TestData {
  static p4(): MaterialJSON {
    return {
      type: "p4",
      attributes: {
        url: "foo/bar",
        destination: "bar",
        filter: {
          ignore: []
        },
        invert_filter: false,
        name: "Dummy git",
        auto_update: true,
        view: "some-view",
        port: "some-port"
      },
      mdu_start_time: "1970-01-01T02:46:40Z"
    } as MaterialJSON;
  }

  static tfs(): MaterialJSON {
    return {
      type: "tfs",
      attributes: {
        url: "foo/bar",
        destination: "bar",
        filter: {
          ignore: []
        },
        invert_filter: false,
        name: "Dummy git",
        auto_update: true,
        domain: "foo.com",
        project_path: "/var/project",
        username: "bob",
        password: "badger"
      },
      mdu_start_time: "1970-01-01T02:46:40Z"
    } as MaterialJSON;
  }

  static git(): MaterialJSON {
    return {
      type: "git",
      attributes: {
        url: "foo/bar",
        destination: "bar",
        filter: {
          ignore: []
        },
        invert_filter: false,
        name: "Dummy git",
        auto_update: true,
        branch: "master",
        submodule_folder: "/var/run",
        shallow_clone: false
      },
      mdu_start_time: "1970-01-01T02:46:40Z"
    } as MaterialJSON;
  }

  static svn() {
    return {
      type: "svn",
      attributes: {
        url: "url",
        destination: "svnDir",
        filter: {
          ignore: [
            "*.doc"
          ]
        },
        invert_filter: false,
        name: "SvnMaterial",
        auto_update: true,
        check_externals: true,
        username: "user",
        encrypted_password: "AES:lzcCuNSe4vUx+CsWgN11Uw==:3RLqvnnHqhiSx+iq7ak41A=="
      },
      mdu_start_time: "1970-01-01T08:20:00Z"
    } as MaterialJSON;
  }

  static hg() {
    return JSON.parse(
      JSON.stringify({
                       type: "hg",
                       attributes: {
                         url: "hg-url",
                         destination: "foo_bar",
                         filter: {
                           ignore: []
                         },
                         invert_filter: false,
                         name: null,
                         auto_update: true
                       },
                       mdu_start_time: "1970-01-01T05:33:20Z"
                     }
      )) as MaterialJSON;
  }
}
