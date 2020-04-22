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
import {PackageOperations} from "views/pages/package_repositories";
import {stubAllMethods, TestHelper} from "views/pages/spec/test_helper";
import {PackagesWidget} from "../packages_widget";
import {PackageRepoScrollOptions} from "../package_repositories_widget";

describe('PackagesWidgetSpec', () => {
  const helper        = new TestHelper();
  const pkgOperations = new PackageOperations();
  let packages: Stream<Packages>;
  let disableActions: boolean;
  let scrollOptions: PackageRepoScrollOptions;

  beforeEach(() => {
    packages      = Stream(new Packages());
    scrollOptions = {
      package_repo_sm: {
        sm:                          stubAllMethods(["shouldScroll", "getTarget", "setTarget", "scrollToEl", "hasTarget"]),
        shouldOpenEditView:          false,
        shouldOpenCreatePackageView: false
      },
      package_sm:      {
        sm:                 stubAllMethods(["shouldScroll", "getTarget", "setTarget", "scrollToEl", "hasTarget"]),
        shouldOpenEditView: false
      }
    };
  });
  afterEach((done) => helper.unmount(done));

  function mount() {
    pkgOperations.onAdd      = jasmine.createSpy("onAdd");
    pkgOperations.onClone    = jasmine.createSpy("onClone");
    pkgOperations.onEdit     = jasmine.createSpy("onEdit");
    pkgOperations.onDelete   = jasmine.createSpy("onDelete");
    pkgOperations.showUsages = jasmine.createSpy("showUsages");
    disableActions           = false;
    helper.mount(() => <PackagesWidget packageRepoName={"pkg-repo-name"} packages={packages}
                                       disableActions={disableActions}
                                       packageOperations={pkgOperations} scrollOptions={scrollOptions}/>);
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

  it('should render error info if the element specified in the anchor does not exist', () => {
    const pkg = Package.fromJSON(getPackage());
    packages().push(pkg);
    scrollOptions.package_repo_sm.sm = {
      hasTarget:    jasmine.createSpy().and.callFake(() => true),
      getTarget:    jasmine.createSpy().and.callFake(() => pkg.packageRepo().name()),
      shouldScroll: jasmine.createSpy(),
      setTarget:    jasmine.createSpy(),
      scrollToEl:   jasmine.createSpy()
    };
    scrollOptions.package_sm.sm      = {
      hasTarget:    jasmine.createSpy().and.callFake(() => true),
      getTarget:    jasmine.createSpy().and.callFake(() => `${pkg.packageRepo().name()}:non-pkg`),
      shouldScroll: jasmine.createSpy(),
      setTarget:    jasmine.createSpy(),
      scrollToEl:   jasmine.createSpy()
    };
    mount();

    expect(helper.byTestId("anchor-package-not-present")).toBeInDOM();
    expect(helper.textByTestId("anchor-package-not-present")).toBe("'non-pkg' package has not been set up.");
  });

  it('should not render error if package name is not set in the anchor', () => {
    const pkg = Package.fromJSON(getPackage());
    packages().push(pkg);
    scrollOptions.package_repo_sm.sm = {
      hasTarget:    jasmine.createSpy().and.callFake(() => true),
      getTarget:    jasmine.createSpy().and.callFake(() => pkg.packageRepo().name()),
      shouldScroll: jasmine.createSpy(),
      setTarget:    jasmine.createSpy(),
      scrollToEl:   jasmine.createSpy()
    };
    scrollOptions.package_sm.sm      = {
      hasTarget:    jasmine.createSpy().and.callFake(() => true),
      getTarget:    jasmine.createSpy().and.callFake(() => `${pkg.packageRepo().name()}:`),
      shouldScroll: jasmine.createSpy(),
      setTarget:    jasmine.createSpy(),
      scrollToEl:   jasmine.createSpy()
    };
    mount();

    expect(helper.byTestId("anchor-package-not-present")).not.toBeInDOM();
    expect(helper.byTestId("packages-widget")).toBeInDOM();
  });
});
