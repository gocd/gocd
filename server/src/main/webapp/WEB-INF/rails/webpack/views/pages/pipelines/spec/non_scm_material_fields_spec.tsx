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

import m from "mithril";
import {DependencyMaterialAttributes, Material, PackageMaterialAttributes} from "models/materials/types";
import {Packages} from "models/package_repositories/package_repositories";
import {getPackage} from "models/package_repositories/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {DependencyFields, PackageFields, SuggestionCache} from "../non_scm_material_fields";
import {docsUrl} from "gen/gocd_version";

describe("AddPipeline: Non-SCM Material Fields", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("DependencyFields structure", () => {
    const material = new Material("dependency", new DependencyMaterialAttributes());
    helper.mount(() => <DependencyFields material={material} cache={new DummyCache()}
                                         showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent(helper, {
      "upstream-pipeline": "Upstream Pipeline*",
      "upstream-stage":    "Upstream Stage*",
      "material-name":     "Material Name",
    });
    assertIgnoreForSchedulingSwitchPresent(helper);
  });

  it("does not display advanced settings when `showLocalWorkingCopyOptions` === false", () => {
    const material = new Material("dependency", new DependencyMaterialAttributes());
    helper.mount(() => <DependencyFields material={material} cache={new DummyCache()}
                                         showLocalWorkingCopyOptions={false}/>);

    assertLabelledInputsPresent(helper, {
      "upstream-pipeline": "Upstream Pipeline*",
      "upstream-stage":    "Upstream Stage*",
    });

    assertLabelledInputsAbsent(helper,
                               "material-name",
    );
    assertIgnoreForSchedulingSwitchAbsent(helper);
  });
});

class DummyCache implements SuggestionCache {
  ready() {
    return true;
  }

  // tslint:disable-next-line
  prime(onSuccess: () => void, onError?: () => void) {
  }

  contents() {
    return [];
  }

  pipelines() {
    return [];
  }

  stages(pipeline: string) {
    return [];
  }

  failureReason() {
    return undefined;
  }

  failed() {
    return false;
  }

  // tslint:disable-next-line
  invalidate() {
  }
}

describe('PackageFieldsSpec', () => {
  const helper = new TestHelper();
  let material: Material;
  let packages: Packages;

  beforeEach(() => {
    material = new Material("package", new PackageMaterialAttributes());
    packages = Packages.fromJSON([getPackage()]);
  });
  afterEach((done) => helper.unmount(done));

  it('should render package structure', () => {
    helper.mount(() => <PackageFields material={material} packages={packages}/>);

    assertLabelledInputsPresent(helper, {
      "package-repository": "Package Repository*",
      "package":            "Package*"
    });
    expect(helper.textAll("option", helper.byTestId('form-field-input-package-repository'))).toEqual(['Select a package repository', 'pkg-repo-name']);
    expect(helper.textAll("option", helper.byTestId('form-field-input-package'))).toEqual(['Select a package']);
  });

  it('should render advanced options if showLocalWorkingCopyOptions is set to true', () => {
    helper.mount(() => <PackageFields material={material} packages={packages} showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent(helper, {
      "package-repository": "Package Repository*",
      "package":            "Package*",
      "material-name":      "Material Name"
    });
  });

  it('should update packages on selecting a package repository', () => {
    helper.mount(() => <PackageFields material={material} packages={packages}/>);

    const pkgRepoElement = helper.byTestId('form-field-input-package-repository');
    expect(helper.textAll("option", pkgRepoElement)).toEqual(['Select a package repository', 'pkg-repo-name']);
    expect(helper.textAll("option", helper.byTestId('form-field-input-package'))).toEqual(['Select a package']);

    helper.onchange(pkgRepoElement, 'pkg-repo-id');

    expect(helper.textAll("option", helper.byTestId('form-field-input-package'))).toEqual(['Select a package', 'pkg-name']);
  });
});

function assertLabelledInputsPresent(helper: TestHelper, idsToLabels: { [key: string]: string }) {
  const keys = Object.keys(idsToLabels);
  expect(keys.length > 0).toBe(true);

  for (const id of keys) {
    expect(helper.byTestId(`form-field-label-${id}`)).toBeInDOM();
    expect(helper.byTestId(`form-field-label-${id}`).textContent!.startsWith(idsToLabels[id])).toBe(true);
    expect(helper.byTestId(`form-field-input-${id}`)).toBeInDOM();
  }
}

function assertLabelledInputsAbsent(helper: TestHelper, ...keys: string[]) {
  expect(keys.length > 0).toBe(true);

  for (const id of keys) {
    expect(helper.byTestId(`form-field-label-${id}`)).toBe(null!);
    expect(helper.byTestId(`form-field-input-${id}`)).toBe(null!);
  }
}

function assertIgnoreForSchedulingSwitchPresent(helper: TestHelper) {
  const helpTextElement = helper.q('#switch-btn-help-text');

  expect(helper.byTestId('material-ignore-for-scheduling')).toBeInDOM();
  expect(helper.textByTestId('switch-label')).toBe('Do not schedule the pipeline when this material is updated');
  expect(helpTextElement.textContent!.startsWith("When set to true, the pipeline will not be automatically scheduled for changes to this material.")).toBeTrue();
  expect(helper.q('a', helpTextElement)).toHaveAttr('href', docsUrl("configuration/configuration_reference.html#pipeline-1"));
}

function assertIgnoreForSchedulingSwitchAbsent(helper: TestHelper) {
  expect(helper.byTestId('material-ignore-for-scheduling')).not.toBeInDOM();
}
