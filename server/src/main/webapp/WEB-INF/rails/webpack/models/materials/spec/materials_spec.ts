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
import {MaterialModification} from "models/config_repos/types";
import {GitMaterialAttributes, HgMaterialAttributes, MaterialAPIs, MaterialModifications, Materials, MaterialUsages, MaterialWithFingerprint, MaterialWithModification, P4MaterialAttributes, PackageMaterialAttributes, PluggableScmMaterialAttributes, SvnMaterialAttributes, TfsMaterialAttributes} from "../materials";

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
      expect(material.materialUpdateInProgress).toBe(true);

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
    expect(request.requestHeaders['If-None-Match']).toBeUndefined();
  });

  it('should send etag for fetching all materials', () => {
    const url = SparkRoutes.getAllMaterials();
    jasmine.Ajax.stubRequest(url).andReturn(materialsResponse());

    MaterialAPIs.all("etag-to-send");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders['If-None-Match']).toEqual("etag-to-send");
  });

  it('should get list of modifications', (done) => {
    const url = SparkRoutes.getModifications("fingerprint");
    jasmine.Ajax.stubRequest(url).andReturn(modificationResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON  = response.unwrap() as SuccessResponse<any>;
      const modifications = (responseJSON.body as MaterialModifications);

      expect(modifications).toHaveLength(1);
      expect(modifications.previousLink).toBeUndefined();
      expect(modifications.nextLink).toBe('some-link-for-next-page');

      const mod = modifications[0];

      expect(mod).not.toBeNull();
      expect(mod.modifiedTime).toBe("2019-12-23T10:25:52Z");
      expect(mod.username).toBe("GoCD test user");
      expect(mod.comment).toBe("Dummy commit");
      expect(mod.emailAddress).toBe("gocd@test.com");
      expect(mod.revision).toBe("abcd1234");
      done();
    });

    MaterialAPIs.modifications("fingerprint", "").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it('should get the usages for a given material', (done) => {
    const url = SparkRoutes.getMaterialUsages("fingerprint");
    jasmine.Ajax.stubRequest(url).andReturn(usagesResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON  = response.unwrap() as SuccessResponse<any>;
      const modifications = (responseJSON.body as MaterialUsages);

      expect(modifications).toHaveLength(2);
      expect(modifications).toEqual(['pipeline1', 'pipeline2']);
      done();
    });

    MaterialAPIs.usages("fingerprint").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  function materialsResponse() {
    const data = {
      materials: [{
        config:                      {
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
        material_update_in_progress: true,
        modification:                {
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

  function modificationResponse() {
    const data = {
      _links:        {
        next: {
          href: "some-link-for-next-page"
        }
      },
      modifications: [{
        username:      "GoCD test user",
        email_address: "gocd@test.com",
        revision:      "abcd1234",
        comment:       "Dummy commit",
        modified_time: "2019-12-23T10:25:52Z"
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

  function usagesResponse() {
    const data = {
      usages: ["pipeline1", "pipeline2"]
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

  describe('DisplayNameSpec', () => {
    it('should show name or url as display name for git', () => {
      const material = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("name", false, "some-url", "master"));
      expect(material.displayName()).toBe("name");

      material.attributes().name(undefined);
      expect(material.displayName()).toBe("some-url");
    });

    it('should show name or url as display name for hg', () => {
      const material = new MaterialWithFingerprint("hg", "fingerprint", new HgMaterialAttributes("name", false, "some-url", "branch"));
      expect(material.displayName()).toBe("name");

      material.attributes().name(undefined);
      expect(material.displayName()).toBe("some-url");
    });

    it('should show name or url as display name for svn', () => {
      const material = new MaterialWithFingerprint("svn", "fingerprint", new SvnMaterialAttributes("name", false, "some-url"));
      expect(material.displayName()).toBe("name");

      material.attributes().name(undefined);
      expect(material.displayName()).toBe("some-url");
    });

    it('should show name or port as display name for p4', () => {
      const material = new MaterialWithFingerprint("p4", "fingerprint", new P4MaterialAttributes("name", false, "some-url", false, "view"));
      expect(material.displayName()).toBe("name");

      material.attributes().name(undefined);
      expect(material.displayName()).toBe("some-url");
    });

    it('should show name or url as display name for tfs', () => {
      const material = new MaterialWithFingerprint("tfs", "fingerprint", new TfsMaterialAttributes("name", false, "some-url", "domain", "view"));
      expect(material.displayName()).toBe("name");

      material.attributes().name(undefined);
      expect(material.displayName()).toBe("some-url");
    });

    it('should show repo and package name as display name for package', () => {
      const material = new MaterialWithFingerprint("package", "fingerprint", new PackageMaterialAttributes(undefined, true, "some-ref", "package_name", "pkg-repo-name"));
      expect(material.displayName()).toBe("pkg-repo-name_package_name");
    });

    it('should show scm name as display name for plugin', () => {
      const material = new MaterialWithFingerprint("plugin", "fingerprint", new PluggableScmMaterialAttributes(undefined, true, "some-ref", "scm-name"));
      expect(material.displayName()).toBe("scm-name");
    });
  });

  describe('AttributesAsStringSpec', () => {
    it('should show name or url and branch for attributes as string for git', () => {
      const material = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("", false, "some-url", "master"));
      expect(material.attributesAsString()).toBe("some-url [ master ]");
    });

    it('should show url and branch for attributes as string for hg', () => {
      const material = new MaterialWithFingerprint("hg", "fingerprint", new HgMaterialAttributes("", false, "some-url", "branch"));
      expect(material.attributesAsString()).toBe("some-url [ branch ]");
    });

    it('should show only url for attributes as string for hg when branch is not set', () => {
      const material = new MaterialWithFingerprint("hg", "fingerprint", new HgMaterialAttributes("", false, "some-url"));
      expect(material.attributesAsString()).toBe("some-url");
    });

    it('should show url for attributes as string for svn', () => {
      const material = new MaterialWithFingerprint("svn", "fingerprint", new SvnMaterialAttributes("", false, "some-url"));
      expect(material.attributesAsString()).toBe("some-url");
    });

    it('should show port and view for attributes as string for p4', () => {
      const material = new MaterialWithFingerprint("p4", "fingerprint", new P4MaterialAttributes("", false, "some-url", false, "view"));
      expect(material.attributesAsString()).toBe("some-url [ view ]");
    });

    it('should show url for attributes as string for tfs', () => {
      const material = new MaterialWithFingerprint("tfs", "fingerprint", new TfsMaterialAttributes("", false, "some-url", "domain", "view"));
      expect(material.attributesAsString()).toBe("some-url");
    });

    it('should show repo and package name for attributes as string for package', () => {
      const material = new MaterialWithFingerprint("package", "fingerprint", new PackageMaterialAttributes(undefined, true, "some-ref", "package_name", "pkg-repo-name"));
      expect(material.attributesAsString()).toBe("pkg-repo-name_package_name");
    });

    it('should show scm name for attributes as string for plugin', () => {
      const material = new MaterialWithFingerprint("plugin", "fingerprint", new PluggableScmMaterialAttributes(undefined, true, "some-ref", "scm-name"));
      expect(material.attributesAsString()).toBe("scm-name");
    });
  });
});

describe('MaterialsSpec', () => {
  it('should sort based on type', () => {
    const materials = new Materials();
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("git", "some", new GitMaterialAttributes()), false, null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("hg", "some", new HgMaterialAttributes()), false, null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("svn", "some", new SvnMaterialAttributes()), false, null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("p4", "some", new P4MaterialAttributes()), false, null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("tfs", "some", new TfsMaterialAttributes()), false, null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("package", "some", new PackageMaterialAttributes()), false, null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("plugin", "some", new PluggableScmMaterialAttributes(undefined, undefined, "", "scm_name")), false, null));

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

describe('MaterialsWithModificationSpec', () => {
  it('should return true if search string matches name, type or display url of the config', () => {
    const material = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("some-name", false, "http://svn.com/gocd/gocd", "master"));
    const withMod  = new MaterialWithModification(material, false, null);

    expect(withMod.matches("git")).toBeTrue();
    expect(withMod.matches("name")).toBeTrue();
    expect(withMod.matches("gocd")).toBeTrue();
    expect(withMod.matches("mas")).toBeTrue();
    expect(withMod.matches("abc")).toBeFalse();
  });

  it('should return true if search string matches username, revision or comment for the latest modification', () => {
    const material = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("", false, "some-url", "master"));
    const withMod  = new MaterialWithModification(material, true, new MaterialModification("username", "email_address", "some-revision", "a very very long comment with abc", ""));

    expect(withMod.matches("revision")).toBeTrue();
    expect(withMod.matches("comment")).toBeTrue();
    expect(withMod.matches("name")).toBeTrue();
    expect(withMod.matches("abc")).toBeTrue();
    expect(withMod.matches("123")).toBeFalse();
  });

  it('should return type as config.type', () => {
    const material = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("some-name", false, "http://svn.com/gocd/gocd", "master"));
    const withMod  = new MaterialWithModification(material, true, null);

    expect(withMod.type()).toBe(material.type());
  });
});
