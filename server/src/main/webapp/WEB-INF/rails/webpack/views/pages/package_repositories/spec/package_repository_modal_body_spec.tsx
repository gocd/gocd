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
import {PackageRepository} from "models/package_repositories/package_repositories";
import {getPackageRepository, pluginInfoWithPackageRepositoryExtension} from "models/package_repositories/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {TestHelper} from "views/pages/spec/test_helper";
import {PackageRepositoryModalBody} from "../package_repository_modal_body";

describe('PackageRepositoryModalBodySpec', () => {
  const helper        = new TestHelper();
  const spy           = jasmine.createSpy("pluginIdProxy");
  const pluginIdProxy = () => {
    spy();
    return "npm";
  };
  let pluginInfos: PluginInfos;
  let packageRepo: PackageRepository;
  let disabled: boolean;
  let disablePluginField: boolean;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    pluginInfos        = new PluginInfos(PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension()));
    packageRepo        = PackageRepository.fromJSON(getPackageRepository());
    disabled           = false;
    disablePluginField = false;
  });

  function mount() {
    helper.mount(() => <PackageRepositoryModalBody pluginInfos={pluginInfos} packageRepo={packageRepo}
                                                   disableId={disabled} disablePluginField={disablePluginField}
                                                   pluginIdProxy={pluginIdProxy}/>);
  }

  it('should render input fields for id, name and package repo', () => {
    mount();

    expect(helper.byTestId('form-field-input-repo-id')).toBeInDOM();
    expect(helper.byTestId('form-field-input-repo-id')).not.toBeDisabled();
    expect(helper.byTestId("form-field-input-repo-id")).toHaveValue(packageRepo.repoId());

    expect(helper.byTestId('form-field-input-name')).toBeInDOM();
    expect(helper.byTestId('form-field-input-name')).not.toBeDisabled();
    expect(helper.byTestId("form-field-input-name")).toHaveValue(packageRepo.name());

    expect(helper.byTestId('form-field-input-plugin')).toBeInDOM();
    expect(helper.byTestId('form-field-input-plugin')).not.toBeDisabled();
    expect(helper.byTestId("form-field-input-plugin").children.length).toBe(1);
    expect(helper.byTestId("form-field-input-plugin").children[0].textContent).toBe('NPM plugin for package repo');
  });

  it('should render the id and name as readonly', () => {
    disabled = true;
    mount();

    expect(helper.byTestId('form-field-input-repo-id')).toBeDisabled();
    expect(helper.byTestId('form-field-input-name')).toBeDisabled();
  });

  it('should call the spy method on plugin change', () => {
    const json       = pluginInfoWithPackageRepositoryExtension();
    json.id          = 'new-plugin-id';
    json.about!.name = 'new-plugin-name';
    const pluginInfo = PluginInfo.fromJSON(json);

    pluginInfos.push(pluginInfo);
    mount();

    helper.onchange(helper.byTestId("form-field-input-plugin"), "new-plugin-id");

    expect(spy).toHaveBeenCalled();
  });

  it('should render the plugin field as disabled', () => {
    disablePluginField = true;
    mount();

    expect(helper.byTestId('form-field-input-plugin')).toBeInDOM();
    expect(helper.byTestId('form-field-input-plugin')).toBeDisabled();
  });
});
