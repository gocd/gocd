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
import {Package, PackageRepositories, PackageRepository} from "models/package_repositories/package_repositories";
import {getPackage, getPackageRepository, pluginInfoWithPackageRepositoryExtension} from "models/package_repositories/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {TestHelper} from "views/pages/spec/test_helper";
import {PackageModalBody} from "../package_modal_body";

describe('PackageModalBodySpec', () => {
  const helper              = new TestHelper();
  const onPackageRepoChange = jasmine.createSpy("onPackageRepoChange");
  let pluginInfos: PluginInfos;
  let packageRepos: PackageRepositories;
  let pkg: Package;
  let disabled: boolean;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    pluginInfos  = new PluginInfos(PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension()));
    pkg          = Package.fromJSON(getPackage());
    packageRepos = new PackageRepositories(PackageRepository.fromJSON(getPackageRepository()));
    disabled     = false;
  });

  function mount() {
    helper.mount(() => <PackageModalBody pluginInfos={pluginInfos}
                                         packageRepositories={packageRepos}
                                         package={pkg}
                                         disableId={disabled}
                                         onPackageRepoChange={onPackageRepoChange}/>);
  }

  it('should render info msg', () => {
    mount();

    expect(helper.byTestId('flash-message-info')).toBeInDOM();
    expect(helper.textByTestId('flash-message-info')).toBe('The new package will be available to be used as material in all pipelines. Other admins might be able to edit this package.');
  });

  it('should render input fields for id, name and package repo', () => {
    mount();

    expect(helper.byTestId('form-field-input-id')).toBeInDOM();
    expect(helper.byTestId('form-field-input-id')).not.toBeDisabled();
    expect(helper.byTestId("form-field-input-id")).toHaveValue(pkg.id());

    expect(helper.byTestId('form-field-input-name')).toBeInDOM();
    expect(helper.byTestId('form-field-input-name')).not.toBeDisabled();
    expect(helper.byTestId("form-field-input-name")).toHaveValue(pkg.name());

    expect(helper.byTestId('form-field-input-package-repository')).toBeInDOM();
    expect(helper.byTestId("form-field-input-package-repository").children[0].textContent).toBe(pkg.packageRepo().name());
  });

  it('should call the spy method on package repo change', () => {
    const pkgRepo = PackageRepository.default();
    pkgRepo.repoId('new-repo-id');
    pkgRepo.name('new-repo-name');

    packageRepos.push(pkgRepo);
    mount();

    helper.onchange(helper.byTestId("form-field-input-package-repository"), "new-repo-id");

    expect(onPackageRepoChange).toHaveBeenCalledWith(pkgRepo);
  });

  it('should not render the msg and make the id and name as readonly', () => {
    disabled = true;
    mount();

    expect(helper.byTestId('flash-message-info')).not.toBeInDOM();
    expect(helper.byTestId('form-field-input-id')).toBeDisabled();
    expect(helper.byTestId('form-field-input-name')).toBeDisabled();
  });
});
