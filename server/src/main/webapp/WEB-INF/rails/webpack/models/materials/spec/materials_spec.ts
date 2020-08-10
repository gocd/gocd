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

import {ApiResult, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {
  GitMaterialAttributes,
  HgMaterialAttributes,
  MaterialAPIs,
  Materials,
  MaterialWithFingerprint,
  MaterialWithModification,
  P4MaterialAttributes,
  PackageMaterialAttributes,
  PluggableScmMaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "../materials";

describe('MaterialsAPISpec', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should get all materials", (done) => {
    const url = SparkRoutes.getAllMaterials();
    jasmine.Ajax.stubRequest(url).andReturn(materialsResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const materials    = (responseJSON.body as Materials);

      expect(materials).toHaveLength(1);

      const material = materials[0];
      expect(material.config.type()).toBe('git');
      expect(material.config.name()).toBe('some-name');
      expect(material.config.fingerprint()).toBe('4879d548d34a4f3ba7ed4a532bc1b02');

      expect(material.config.attributes()).toBeInstanceOf(GitMaterialAttributes);

      expect(material.modification).not.toBeNull();
      expect(material.modification!.modifiedTime).toBe("2019-12-23T10:25:52Z");
      expect(material.modification!.username).toBe("GoCD test user");
      expect(material.modification!.comment).toBe("Dummy commit");
      expect(material.modification!.emailAddress).toBe("gocd@test.com");
      expect(material.modification!.revision).toBe("abcd1234");
      done();
    });

    MaterialAPIs.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  function materialsResponse() {
    const data = {
      materials: [{
        config:       {
          type:        "git",
          fingerprint: "4879d548d34a4f3ba7ed4a532bc1b02",
          attributes:  {
            url:              "test-repo",
            destination:      null,
            filter:           null,
            invert_filter:    false,
            name:             'some-name',
            auto_update:      true,
            branch:           "master",
            submodule_folder: null,
            shallow_clone:    false
          }
        },
        modification: {
          username:      "GoCD test user",
          email_address: "gocd@test.com",
          revision:      "abcd1234",
          comment:       "Dummy commit",
          modified_time: "2019-12-23T10:25:52Z"
        }
      }]
    };
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      },
      responseText:    JSON.stringify(data)
    };
  }
});

describe('MaterialWithFingerPrintSpec', () => {
  describe('MaterialAttrsAsMapSpec', () => {
    it('should convert git attributes into Map', () => {
      const material = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("name", false, "some-url", "master"));

      const attrs = material.attributesAsMap();
      const keys  = Array.from(attrs.keys());

      expect(keys.length).toBe(2);
      expect(keys).toEqual(['URL', 'Branch']);
    });

    it('should convert hg attributes into Map', () => {
      const material = new MaterialWithFingerprint("hg", "fingerprint", new HgMaterialAttributes("name", false, "some-url"));

      const attrs = material.attributesAsMap();
      const keys  = Array.from(attrs.keys());

      expect(keys.length).toBe(2);
      expect(keys).toEqual(['URL', 'Branch']);
    });

    it('should convert svn attributes into Map', () => {
      const material = new MaterialWithFingerprint("svn", "fingerprint", new SvnMaterialAttributes("name", false, "some-url"));

      const attrs = material.attributesAsMap();
      const keys  = Array.from(attrs.keys());

      expect(keys.length).toBe(1);
      expect(keys).toEqual(['URL']);
    });

    it('should convert p4 attributes into Map', () => {
      const material = new MaterialWithFingerprint("p4", "fingerprint", new P4MaterialAttributes("name", false, "some-url", false, "view"));

      const attrs = material.attributesAsMap();
      const keys  = Array.from(attrs.keys());

      expect(keys.length).toBe(2);
      expect(keys).toEqual(['Host and Port', 'View']);
    });

    it('should convert tfs attributes into Map', () => {
      const material = new MaterialWithFingerprint("tfs", "fingerprint", new TfsMaterialAttributes("name", false, "some-url", "domain", "view"));

      const attrs = material.attributesAsMap();
      const keys  = Array.from(attrs.keys());

      expect(keys.length).toBe(3);
      expect(keys).toEqual(['URL', 'Domain', 'Project Path']);
    });
  });
});

describe('MaterialsWithModificationsSpec', () => {
  it('should sort based on type', () => {
    const materials = new Materials();
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("git", "some", new GitMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("hg", "some", new HgMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("svn", "some", new SvnMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("p4", "some", new P4MaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("tfs", "some", new TfsMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("package", "some", new PackageMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("plugin", "some", new PluggableScmMaterialAttributes(undefined, undefined, "", "scm_name")), null));

    materials.sortOnType();

    expect(materials[0].config.type()).toBe('git');
    expect(materials[1].config.type()).toBe('hg');
    expect(materials[2].config.type()).toBe('p4');
    expect(materials[3].config.type()).toBe('package');
    expect(materials[4].config.type()).toBe('plugin');
    expect(materials[5].config.type()).toBe('svn');
    expect(materials[6].config.type()).toBe('tfs');
  });
});
