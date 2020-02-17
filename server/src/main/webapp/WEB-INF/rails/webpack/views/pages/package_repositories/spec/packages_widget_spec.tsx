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
import {getPackage} from "models/package_repositories/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {PackagesWidget} from "../packages_widget";

describe('PackagesWidgetSpec', () => {
  const helper = new TestHelper();
  let packages: Stream<Packages>;

  beforeEach(() => {
    packages = Stream(new Packages());
  });
  afterEach((done) => helper.unmount(done));

  function mount() {
    helper.mount(() => <PackagesWidget packages={packages}/>);
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
