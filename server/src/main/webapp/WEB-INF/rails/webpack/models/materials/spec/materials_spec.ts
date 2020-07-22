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
import {Filter} from "models/maintenance_mode/material";
import {MaterialAPIs, Materials, MaterialWithFingerprint, MaterialWithModification} from "../materials";
import {
  DependencyMaterialAttributes,
  GitMaterialAttributes,
  HgMaterialAttributes,
  P4MaterialAttributes,
  PackageMaterialAttributes,
  PluggableScmMaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "../types";

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
  it('should convert attributes into Map', () => {
    const material = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("name", false, "some-url", "master"));

    const attrs = material.attributesAsMap();

    expect(attrs.size).toBe(10);
    expect(attrs.has("Fingerprint")).toBeTrue();
    expect(attrs.has("name")).toBeFalse();
  });

  it('should render filters', () => {
    const attrs = new GitMaterialAttributes();
    attrs.filter(new Filter(["abc"]));
    const material = new MaterialWithFingerprint("git", "fingerprint", attrs);

    expect(material.attributesAsMap().get("Filter")).toEqual(['abc']);
  });

  it('should return true if search string matches name, type or display url', () => {
    const material = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("some-name", false, "http://svn.com/gocd/gocd", "master"));

    expect(material.matches("git")).toBeTrue();
    expect(material.matches("name")).toBeTrue();
    expect(material.matches("gocd")).toBeTrue();
    expect(material.matches("mas")).toBeTrue();
    expect(material.matches("abc")).toBeFalse();
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
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("dependency", "some", new DependencyMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("package", "some", new PackageMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("plugin", "some", new PluggableScmMaterialAttributes(undefined, undefined, "", "", new Filter([]))), null));

    materials.sortOnType();

    expect(materials[0].config.type()).toBe('dependency');
    expect(materials[1].config.type()).toBe('git');
    expect(materials[2].config.type()).toBe('hg');
    expect(materials[3].config.type()).toBe('p4');
    expect(materials[4].config.type()).toBe('package');
    expect(materials[5].config.type()).toBe('plugin');
    expect(materials[6].config.type()).toBe('svn');
    expect(materials[7].config.type()).toBe('tfs');
  });
});
