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

import {docsUrl} from "gen/gocd_version";
import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {DependencyMaterialAttributes, Material, PackageMaterialAttributes} from "models/materials/types";
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {
  getPackageRepository,
  pluginInfoWithPackageRepositoryExtension
} from "models/package_repositories/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {TestHelper} from "views/pages/spec/test_helper";
import {DependencyFields, PackageFields, SuggestionCache} from "../non_scm_material_fields";

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
  let packageRepositories: PackageRepositories;
  let pluginInfos: PluginInfos;

  beforeEach(() => {
    material            = new Material("package", new PackageMaterialAttributes());
    packageRepositories = PackageRepositories.fromJSON([getPackageRepository()]);
    pluginInfos         = new PluginInfos(PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension()));
  });
  afterEach((done) => helper.unmount(done));

  it('should render package structure', () => {
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    assertLabelledInputsPresent(helper, {
      "package-repository": "Package Repository*",
      "package":            "Package*"
    });
    assertLabelledInputsDisabledOrNot(helper, {
      "package-repository": false,
      "package":            true
    });
    expect(helper.textAll("option", helper.byTestId('form-field-input-package-repository'))).toEqual(['Select a package repository', 'pkg-repo-name']);
    expect(helper.textAll("option", helper.byTestId('form-field-input-package'))).toEqual(['Select a package']);
  });

  it('should update packages on selecting a package repository', () => {
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    const pkgRepoElement = helper.byTestId('form-field-input-package-repository');
    expect(helper.textAll("option", pkgRepoElement)).toEqual(['Select a package repository', 'pkg-repo-name']);
    expect(helper.textAll("option", helper.byTestId('form-field-input-package'))).toEqual(['Select a package']);

    helper.onchange(pkgRepoElement, 'pkg-repo-id');

    assertLabelledInputsDisabledOrNot(helper, {
      "package-repository": false,
      "package":            false
    });
    expect(helper.textAll("option", helper.byTestId('form-field-input-package'))).toEqual(['Select a package', 'pkg-name']);
  });

  it('should render plugin not found error message', () => {
    packageRepositories[0].pluginMetadata().id('non-existent-plugin');
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    helper.onchange(helper.byTestId('form-field-input-package-repository'), 'pkg-repo-id');

    const errorElement = helper.q('span[class*="forms__form-error-text"]');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe("Associated plugin 'non-existent-plugin' not found. Please contact the system administrator to install the plugin.");
  });

  it('should show an error text if no package repositories are defined', () => {
    packageRepositories = new PackageRepositories();
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    assertLabelledInputsDisabledOrNot(helper, {
      "package-repository": true,
      "package":            true
    });

    const errorElement = helper.byTestId('flash-message-alert');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe('No package repositories defined. Go to Package Repositories to define one.');
    expect(helper.q('a', errorElement)).toHaveAttr('href', SparkRoutes.packageRepositoriesSPA());
  });

  it('should show an error text if no packages are defined for a given package repo', () => {
    packageRepositories[0].packages(new Packages());
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    helper.onchange(helper.byTestId('form-field-input-package-repository'), 'pkg-repo-id');

    assertLabelledInputsDisabledOrNot(helper, {
      "package-repository": false,
      "package":            true
    });
    const errorElement = helper.byTestId('flash-message-alert');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe('No packages defined for the selected package repository. Go to Package Repositories to define one.');
    expect(helper.q('a', errorElement)).toHaveAttr('href', SparkRoutes.packageRepositoriesSPA());
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

function assertLabelledInputsDisabledOrNot(helper: TestHelper, idsMap: { [key: string]: boolean }) {
  const keys = Object.keys(idsMap);
  expect(keys.length > 0).toBe(true);

  for (const id of keys) {
    expect(helper.byTestId(`form-field-input-${id}`)).toBeInDOM();
    expect(helper.byTestId(`form-field-input-${id}`).hasAttribute('readonly')).toBe(idsMap[id]);
  }
}
