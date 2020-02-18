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
import Stream from "mithril/stream";
import {Package, Packages} from "models/package_repositories/package_repositories";
import {getPackage, pluginInfoWithPackageRepositoryExtension} from "models/package_repositories/spec/test_data";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PackageOperations} from "views/pages/package_repositories";
import {TestHelper} from "views/pages/spec/test_helper";
import {PackagesWidget} from "../packages_widget";

describe('PackagesWidgetSpec', () => {
  const helper        = new TestHelper();
  const pkgOperations = new PackageOperations();
  let packages: Stream<Packages>;
  let pluginInfo: PluginInfo;

  beforeEach(() => {
    packages = Stream(new Packages());
  });
  afterEach((done) => helper.unmount(done));

  function mount() {
    pkgOperations.onAdd      = jasmine.createSpy("onAdd");
    pkgOperations.onClone    = jasmine.createSpy("onClone");
    pkgOperations.onEdit     = jasmine.createSpy("onEdit");
    pkgOperations.onDelete   = jasmine.createSpy("onDelete");
    pkgOperations.showUsages = jasmine.createSpy("showUsages");
    pluginInfo               = PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension());
    helper.mount(() => <PackagesWidget packages={packages}
                                       pluginInfo={pluginInfo}
                                       packageOperations={pkgOperations}/>);
  }

  it('should render message saying no packages configured', () => {
    mount();

    expect(helper.byTestId('packages-widget')).not.toBeInDOM();
    expect(helper.byTestId('flash-message-info')).toBeInDOM();
    expect(helper.textByTestId('flash-message-info')).toBe('There are no packages defined in this package repository.');
  });

  it('should render packages', () => {
    const pkg = Package.fromJSON(getPackage());
    packages().push(pkg);

    mount();

    expect(helper.byTestId('packages-widget')).toBeInDOM();
    expect(helper.q('h4').textContent).toBe('Packages');
    expect(helper.byTestId('package-panel')).toBeInDOM();
  });
});
