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
import {getPackage} from "models/package_repositories/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {PackageWidget} from "../package_widget";

describe('PackageWidgetSpec', () => {
  const helper = new TestHelper();
  let pkg: Package;

  beforeEach(() => {
    pkg = Package.fromJSON(getPackage());
  });
  afterEach((done) => helper.unmount(done));

  function mount() {
    helper.mount(() => <PackageWidget package={pkg}/>);
  }

  it('should render package information', () => {
    mount();

    expect(helper.byTestId('package-panel')).toBeInDOM();
    expect(helper.textByTestId('package-id')).toBe('pkg-id');

    const configElements = helper.qa('li');
    expect(configElements.length).toBe(2);

    expect(configElements[0].textContent).toBe('Namepkg-name');
    expect(configElements[1].textContent).toBe('PACKAGE_IDpkg');
  });

});
