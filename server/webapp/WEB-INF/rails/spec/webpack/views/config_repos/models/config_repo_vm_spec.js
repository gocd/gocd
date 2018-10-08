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

const ConfigRepoVM = require("views/config_repos/models/config_repo_vm");
const Routes       = require("gen/js-routes");

describe("Config Repo View Model", () => {
  it("allowSave() runs ALL validations on local and child fields, even if there are failures (does not short-circuit)", () => {
    const validVm = stubbingValidate(simple());

    expect(validVm.allowSave()).toBe(true);
    expect(validVm.validate).toHaveBeenCalled();
    expect(validVm.attributes().validate).toHaveBeenCalled();

    const invalidVm = stubbingValidate(simple(), {id: "ID is uncreative. meh."});
    expect(invalidVm.allowSave()).toBe(false);
    expect(invalidVm.validate).toHaveBeenCalled();
    expect(invalidVm.attributes().validate).toHaveBeenCalled();
  });

  it("allowSave() result depends on both local and child (material) validations", () => {
    // basic logical `AND` truth table
    expect(stubbingIsValid(simple(), true, true).allowSave()).toBe(true);
    expect(stubbingIsValid(simple(), true, false).allowSave()).toBe(false);
    expect(stubbingIsValid(simple(), false, true).allowSave()).toBe(false);
    expect(stubbingIsValid(simple(), false, false).allowSave()).toBe(false);
  });

  it("should validate fields", () => {
    const repo = simple();
    expect(repo.isValid()).toBe(true);

    repo.id(null);
    repo.pluginId(null);
    repo.type(null);

    expect(repo.validate().errorsForDisplay("id")).toBe("ID cannot be blank.");
    expect(repo.validate().errorsForDisplay("pluginId")).toBe("Plugin must be selected.");
    expect(repo.validate().errorsForDisplay("type")).toBe("Type must be selected.");

    repo.id("I'm invalid");
    expect(repo.validate().errorsForDisplay("id")).toMatch(/Invalid ID/);
  });

  it("serializes to plain JSON, suitable as an API payload", () => {
    expect(simple().toJSON()).toEqual(basicConfig());
  });

  it("clone() creates a deep copy", () => {
    const original = simple();
    const copy = original.clone();

    expect(original === copy).toBe(false);
    expect(original.toJSON()).toEqual(copy.toJSON());

    // prove these are independent
    copy.id("carbon-copy");
    expect(original.toJSON()).not.toEqual(copy.toJSON());
  });

  it("should be able to test connectivity for url", (done) => {
    const repo = simple();

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(Routes.apiv1AdminInternalMaterialTestPath(), undefined, "POST").andReturn({
        responseText: JSON.stringify({ message: "Connection OK." }),
        status: 200,
        responseHeaders: {
          "Content-Type": "application/vnd.go.cd.v1+json"
        }
      });

      repo.testConnection().then((data) => {
        const reqParams = JSON.parse(jasmine.Ajax.requests.mostRecent().params);
        expect(reqParams.attributes.url).toBe("https://bitnugget.org/unicorns");
        expect(data.message).toBe("Connection OK.");
        done();
      }, () => done.fail("request should be successful"));
    });

  });
});

// utility functions

function stubbingIsValid(vm, resultParent=true, resultChild=true) {
  vm.isValid = jasmine.createSpy("isValid()").and.returnValue(resultParent);
  vm.attributes().isValid = jasmine.createSpy("isValid()").and.returnValue(resultChild);

  return vm;
}

function stubbingValidate(vm, resultParent={}, resultChild={}) {
  addErrors(vm.errors(), resultParent);
  addErrors(vm.attributes().errors(), resultChild);
  vm.validate = jasmine.createSpy("validate()").and.returnValue(vm.errors());
  vm.attributes().validate = jasmine.createSpy("validate()").and.returnValue(vm.attributes().errors());

  return vm;
}

function simple() {
  return new ConfigRepoVM(basicConfig());
}

function basicConfig() {
  return {
    /* eslint-disable camelcase */
    id: "repo-01",
    plugin_id: "test.configrepo.plugin",
    material: {
      type: "hg",
      attributes: {
        name: "my-hg-repo", url: "https://bitnugget.org/unicorns", auto_update: true
      }
    },
    configuration: []
    /* eslint-enable camelcase */
  };
}
function addErrors(errorsObj, expectedErrors) {
  for (const attr in expectedErrors) {
    errorsObj.add(attr, expectedErrors[attr]);
  }
}
