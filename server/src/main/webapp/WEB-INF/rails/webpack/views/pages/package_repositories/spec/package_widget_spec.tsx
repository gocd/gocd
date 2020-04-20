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
import {stubAllMethods, TestHelper} from "views/pages/spec/test_helper";
import {PackageRepoScrollOptions} from "../package_repositories_widget";
import {PackageWidget} from "../package_widget";

describe('PackageWidgetSpec', () => {
  const helper       = new TestHelper();
  const onEdit       = jasmine.createSpy("onEdit");
  const onClone      = jasmine.createSpy("onClone");
  const onDelete     = jasmine.createSpy("onDelete");
  const onShowUsages = jasmine.createSpy("showUsages");
  let pkg: Package;
  let disableActions: boolean;
  let scrollOptions: PackageRepoScrollOptions;

  beforeEach(() => {
    pkg            = Package.fromJSON(getPackage());
    disableActions = false;
    scrollOptions  = {
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
    helper.mount(() => <PackageWidget package={pkg}
                                      disableActions={disableActions}
                                      onEdit={onEdit}
                                      onClone={onClone}
                                      onDelete={onDelete}
                                      showUsages={onShowUsages} scrollOptions={scrollOptions}/>);
  }

  it('should render package information and action buttons', () => {
    mount();

    expect(helper.byTestId('package-panel')).toBeInDOM();
    expect(helper.textByTestId('package-id')).toBe(pkg.name());

    const configElements = helper.qa('li');
    expect(configElements.length).toBe(4);

    expect(configElements[0].textContent).toBe('Idpkg-id');
    expect(configElements[1].textContent).toBe('Namepkg-name');
    expect(configElements[2].textContent).toBe('Auto Updatetrue');
    expect(configElements[3].textContent).toBe('PACKAGE_IDpkg');

    const buttonKeys: { [key: string]: string } = {
      'package-edit':   "Edit package 'pkg-name'",
      'package-clone':  "Clone package 'pkg-name'",
      'package-delete': "Delete package 'pkg-name'",
      'package-usages': "Show usages for package 'pkg-name'"
    };
    Object.keys(buttonKeys)
          .forEach((key) => {
            expect(helper.byTestId(key)).toBeInDOM();
            expect(helper.byTestId(key)).not.toBeDisabled();
            expect(helper.byTestId(key)).toHaveAttr('title', buttonKeys[key]);
          });
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

  it('should disabled action buttons when disabled is set to true', () => {
    disableActions = true;
    mount();

    ['package-edit', 'package-clone'].forEach((key) => {
      expect(helper.byTestId(key)).toBeDisabled();
      expect(helper.byTestId(key)).toHaveAttr('title', "Plugin not found!");
    });
    expect(helper.byTestId('package-delete')).not.toBeDisabled();
    expect(helper.byTestId('package-usages')).not.toBeDisabled();
  });

});
