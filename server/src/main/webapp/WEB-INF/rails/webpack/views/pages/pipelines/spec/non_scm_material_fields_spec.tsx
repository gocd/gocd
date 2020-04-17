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
import m from "mithril";
import {Filter} from "models/maintenance_mode/material";
import {Scm, Scms} from "models/materials/pluggable_scm";
import {DependencyMaterialAttributes, Material, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/types";
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {getPackageRepository, pluginInfoWithPackageRepositoryExtension} from "models/package_repositories/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {getPluggableScm, getScmPlugin} from "views/pages/pluggable_scms/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {DependencyFields, PackageFields, PluginFields, SuggestionCache} from "../non_scm_material_fields";

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
    expect(helper.byTestId('selected-pkg-repo-details')).toBeInDOM();
  });

  it('should render plugin not found error message', () => {
    packageRepositories[0].pluginMetadata().id('non-existent-plugin');
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    helper.onchange(helper.byTestId('form-field-input-package-repository'), 'pkg-repo-id');

    expect(helper.byTestId('selected-pkg-repo-details')).toBeInDOM();
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

    const errorElement = helper.byTestId('flash-message-warning');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe('No package repositories defined.');
    expect(helper.byTestId('selected-pkg-repo-details')).not.toBeInDOM();
    expect(helper.byTestId('selected-pkg-details')).not.toBeInDOM();
  });

  it('should show an error text if no packages are defined for a given package repo', () => {
    packageRepositories[0].packages(new Packages());
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    helper.onchange(helper.byTestId('form-field-input-package-repository'), 'pkg-repo-id');

    expect(helper.byTestId('selected-pkg-repo-details')).toBeInDOM();
    expect(helper.byTestId('selected-pkg-details')).not.toBeInDOM();
    assertLabelledInputsDisabledOrNot(helper, {
      "package-repository": false,
      "package":            true
    });
    const errorElement = helper.byTestId('flash-message-warning');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe('No packages defined for the selected package repository.');
  });

  it('should show selected package details', () => {
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    helper.onchange(helper.byTestId('form-field-input-package-repository'), 'pkg-repo-id');

    expect(helper.byTestId('selected-pkg-repo-details')).toBeInDOM();

    helper.onchange(helper.byTestId('form-field-input-package'), 'pkg-id');

    expect(helper.byTestId('selected-pkg-details')).toBeInDOM();
  });

  it('should pre-populate package config when ref is set', () => {
    (material.attributes() as PackageMaterialAttributes).ref(packageRepositories[0].packages()[0].id());
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    expect(helper.byTestId('selected-pkg-repo-details')).toBeInDOM();
    expect(helper.byTestId('selected-pkg-details')).toBeInDOM();
  });

  it('should pre-populate package config when ref is set with error if plugin is not found', () => {
    (material.attributes() as PackageMaterialAttributes).ref(packageRepositories[0].packages()[0].id());
    pluginInfos = new PluginInfos();
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    expect(helper.byTestId('selected-pkg-repo-details')).toBeInDOM();
    expect(helper.byTestId('selected-pkg-details')).toBeInDOM();
    const errorElement = helper.q('span[class*="forms__form-error-text"]');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe("Associated plugin 'npm' not found. Please contact the system administrator to install the plugin.");
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

describe('PluginFieldsSpec', () => {
  const helper = new TestHelper();
  let material: Material;
  let scms: Scms;
  let pluginInfos: PluginInfos;

  beforeEach(() => {
    material    = new Material("plugin", new PluggableScmMaterialAttributes("", false, "", "", new Filter([])));
    scms        = new Scms(Scm.fromJSON(getPluggableScm()));
    pluginInfos = new PluginInfos(PluginInfo.fromJSON(getScmPlugin()));
  });
  afterEach((done) => helper.unmount(done));

  it('should render plugin structure', () => {
    helper.mount(() => <PluginFields material={material} pluginInfos={pluginInfos} scms={scms}/>);

    assertLabelledInputsPresent(helper, {
      "scm-plugin": "SCM Plugin*",
      "scm":        "SCM*"
    });
    assertLabelledInputsDisabledOrNot(helper, {
      "scm-plugin": false,
      "scm":        true
    });
    expect(helper.textAll("option", helper.byTestId('form-field-input-scm-plugin'))).toEqual(['Select a plugin', 'SCM Plugin']);
    expect(helper.textAll("option", helper.byTestId('form-field-input-scm'))).toEqual(['Select a scm']);
  });

  it('should update scms list when a plugin is selected', () => {
    helper.mount(() => <PluginFields material={material} pluginInfos={pluginInfos} scms={scms}/>);

    const pluginElement = helper.byTestId('form-field-input-scm-plugin');
    expect(helper.textAll("option", pluginElement)).toEqual(['Select a plugin', 'SCM Plugin']);
    expect(helper.textAll("option", helper.byTestId('form-field-input-scm'))).toEqual(['Select a scm']);

    helper.onchange(pluginElement, 'scm-plugin-id');

    assertLabelledInputsDisabledOrNot(helper, {
      "scm-plugin": false,
      "scm":        false
    });
    expect(helper.textAll("option", helper.byTestId('form-field-input-scm'))).toEqual(['Select a scm', 'pluggable.scm.material.name']);
  });

  it('should render error if no scms are configured for the selected plugin', () => {
    scms[0].pluginMetadata().id('some-other-plugin');
    helper.mount(() => <PluginFields material={material} pluginInfos={pluginInfos} scms={scms}/>);

    assertLabelledInputsDisabledOrNot(helper, {
      "scm-plugin": false,
      "scm":        true
    });

    helper.onchange(helper.byTestId('form-field-input-scm-plugin'), 'scm-plugin-id');

    assertLabelledInputsDisabledOrNot(helper, {
      "scm-plugin": false,
      "scm":        true
    });
    const errorElement = helper.byTestId('flash-message-warning');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe('There are no SCMs configured for the selected plugin.');
  });

  it('should render scm configs if selected', () => {
    helper.mount(() => <PluginFields material={material} pluginInfos={pluginInfos} scms={scms}/>);

    helper.onchange(helper.byTestId('form-field-input-scm-plugin'), 'scm-plugin-id');
    expect(helper.byTestId('selected-scm-details')).not.toBeInDOM();

    helper.onchange(helper.byTestId('form-field-input-scm'), 'scm-id');

    expect(helper.byTestId('selected-scm-details')).toBeInDOM();
    assertConfigsPresent(helper, 'selected-scm-details', {
      "id":        "Id",
      "name":      "Name",
      "plugin-id": "Plugin Id",
      "url":       "url"
    });
  });

  it('should pre-populate configs if ref is set', () => {
    (material.attributes()! as PluggableScmMaterialAttributes).ref(scms[0].id());
    helper.mount(() => <PluginFields material={material} pluginInfos={pluginInfos} scms={scms}/>);

    expect(helper.byTestId('selected-scm-details')).toBeInDOM();
  });

  function assertConfigsPresent(helper: TestHelper, parentElementSelector: string, idsToValueMap: { [key: string]: string }) {
    const keys = Object.keys(idsToValueMap);
    expect(keys.length > 0).toBe(true);

    const parentElement = helper.byTestId(parentElementSelector);

    expect(helper.qa('label[data-test-id*="key-value-key"]', parentElement).length).toBe(keys.length);
    for (const id of keys) {
      expect(helper.byTestId(`key-value-key-${id}`, parentElement)).toBeInDOM();
      expect(helper.byTestId(`key-value-key-${id}`, parentElement).textContent).toBe(idsToValueMap[id]);
    }
  }
});
