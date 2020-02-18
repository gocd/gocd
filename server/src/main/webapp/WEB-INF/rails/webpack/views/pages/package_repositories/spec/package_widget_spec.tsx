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
import {Package} from "models/package_repositories/package_repositories";
import {getPackage, pluginInfoWithPackageRepositoryExtension} from "models/package_repositories/spec/test_data";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {TestHelper} from "views/pages/spec/test_helper";
import {PackageWidget} from "../package_widget";

describe('PackageWidgetSpec', () => {
  const helper       = new TestHelper();
  const onEdit       = jasmine.createSpy("onEdit");
  const onClone      = jasmine.createSpy("onClone");
  const onDelete     = jasmine.createSpy("onDelete");
  const onShowUsages = jasmine.createSpy("showUsages");
  let pkg: Package;
  let pluginInfo: PluginInfo;

  beforeEach(() => {
    pkg        = Package.fromJSON(getPackage());
    pluginInfo = PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension());
  });
  afterEach((done) => helper.unmount(done));

  function mount() {
    helper.mount(() => <PackageWidget package={pkg}
                                      pluginInfo={pluginInfo}
                                      onEdit={onEdit}
                                      onClone={onClone}
                                      onDelete={onDelete}
                                      showUsages={onShowUsages}/>);
  }

  it('should render package information', () => {
    mount();

    expect(helper.byTestId('package-panel')).toBeInDOM();
    expect(helper.textByTestId('package-id')).toBe(pkg.name());

    const configElements = helper.qa('li');
    expect(configElements.length).toBe(3);

    expect(configElements[0].textContent).toBe('Namepkg-name');
    expect(configElements[1].textContent).toBe('Auto Updatetrue');
    expect(configElements[2].textContent).toBe('PACKAGE_IDpkg');
  });

  it('should give a call to the callbacks on relevant button clicks', () => {
    mount();

    helper.clickByTestId('package-edit');
    expect(onEdit).toHaveBeenCalled();

    helper.clickByTestId('package-clone');
    expect(onClone).toHaveBeenCalled();

    helper.clickByTestId('package-delete');
    expect(onDelete).toHaveBeenCalled();

    helper.clickByTestId('package-usages');
    expect(onShowUsages).toHaveBeenCalled();
  });

});
