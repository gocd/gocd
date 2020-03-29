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
import {
  DependencyMaterialAttributes,
  Material,
  PackageMaterialAttributes,
  PluggableScmMaterialAttributes
} from "models/materials/types";
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {
  getPackageRepository,
  pluginInfoWithPackageRepositoryExtension
} from "models/package_repositories/spec/test_data";
import {Filter} from "models/maintenance_mode/material";
import {Scm, ScmJSON, Scms} from "models/materials/pluggable_scm";
import {PluginInfoJSON} from "models/shared/plugin_infos_new/serialization";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
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
    expect(helper.textAll("option", helper.byTestId('form-field-input-package-repository'))).toEqual(['Select a package repository', 'pkg-repo-name']);
    expect(helper.textAll("option", helper.byTestId('form-field-input-package'))).toEqual(['Select a package']);
  });

  it('should render advanced options if showLocalWorkingCopyOptions is set to true', () => {
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}
                                      showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent(helper, {
      "package-repository": "Package Repository*",
      "package":            "Package*",
      "material-name":      "Material Name"
    });
  });

  it('should update packages on selecting a package repository', () => {
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    const pkgRepoElement = helper.byTestId('form-field-input-package-repository');
    expect(helper.textAll("option", pkgRepoElement)).toEqual(['Select a package repository', 'pkg-repo-name']);
    expect(helper.textAll("option", helper.byTestId('form-field-input-package'))).toEqual(['Select a package']);

    helper.onchange(pkgRepoElement, 'pkg-repo-id');

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

  it('should show an error text if no packages are defined for a given package repo', () => {
    packageRepositories[0].packages(new Packages());
    helper.mount(() => <PackageFields material={material} packageRepositories={packageRepositories}
                                      pluginInfos={pluginInfos}/>);

    helper.onchange(helper.byTestId('form-field-input-package-repository'), 'pkg-repo-id');

    const errorElement = helper.q('span[class*="advanced_settings__form-error-text"]');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe('No packages defined for the selected package repository. Go to Package Repositories SPA to define one.');
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

  it('should render advanced options if showLocalWorkingCopyOptions is set to true', () => {
    helper.mount(() => <PluginFields material={material} pluginInfos={pluginInfos} scms={scms}
                                     showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent(helper, {
      "scm-plugin":              "SCM Plugin*",
      "scm":                     "SCM*",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name"
    });
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
    const errorElement = helper.byTestId('flash-message-alert');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe('There are no SCMs configured for the selected plugin. Go to Pluggable SCM to define one.');
    expect(helper.q('a', errorElement)).toHaveAttr('href', SparkRoutes.pluggableScmSPA());
  });

  it('should render error if no scm plugins are installed', () => {
    pluginInfos = new PluginInfos();
    helper.mount(() => <PluginFields material={material} pluginInfos={pluginInfos} scms={scms}/>);

    assertLabelledInputsDisabledOrNot(helper, {
      "scm-plugin": true,
      "scm":        true
    });
    const errorElement = helper.byTestId('flash-message-alert');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe('There are no SCM plugins installed. Please see this page for a list of supported plugins.');
    expect(helper.q('a', errorElement)).toHaveAttr('href', 'https://www.gocd.org/plugins/#scm');
  });

  it('should render error if no scms are configured', () => {
    scms = new Scms();
    helper.mount(() => <PluginFields material={material} pluginInfos={pluginInfos} scms={scms}/>);

    assertLabelledInputsDisabledOrNot(helper, {
      "scm-plugin": true,
      "scm":        true
    });
    const errorElement = helper.byTestId('flash-message-alert');
    expect(errorElement).toBeInDOM();
    expect(errorElement.textContent).toBe('There are no SCMs configured. Go to Pluggable SCM to define one.');
    expect(helper.q('a', errorElement)).toHaveAttr('href', SparkRoutes.pluggableScmSPA());
  });
});

function getPluggableScm() {
  return {
    id:              "scm-id",
    name:            "pluggable.scm.material.name",
    plugin_metadata: {
      id:      "scm-plugin-id",
      version: "1"
    },
    auto_update:     true,
    configuration:   [
      {
        key:   "url",
        value: "https://github.com/sample/example.git"
      }
    ]
  } as ScmJSON;
}

function getScmPlugin() {
  return {
    _links:               {
      self: {
        href: "http://test-server:8153/go/api/admin/plugin_info/github.pr"
      },
      doc:  {
        href: "https://api.gocd.org/#plugin-info"
      },
      find: {
        href: "http://test-server:8153/go/api/admin/plugin_info/:id"
      }
    },
    id:                   "scm-plugin-id",
    status:               {
      state: "active"
    },
    plugin_file_location: "/tmp/abc.jar",
    bundled_plugin:       false,
    about:                {
      name:                     "SCM Plugin",
      version:                  "1.4.0-RC2",
      target_go_version:        "15.1.0",
      description:              "Plugin that polls a GitHub repository for pull requests and triggers a build for each of them",
      target_operating_systems: [],
      vendor:                   {
        name: "User",
        url:  "https://github.com/user/abc"
      }
    },
    extensions:           [{
      type:         "scm",
      display_name: "Github",
      scm_settings: {
        configurations: [{
          key:      "url",
          metadata: {
            secure:           false,
            required:         true,
            part_of_identity: true
          }
        }],
        view:           {
          template: "<div>some view template</div>"
        }
      }
    }]
  } as PluginInfoJSON;
}
