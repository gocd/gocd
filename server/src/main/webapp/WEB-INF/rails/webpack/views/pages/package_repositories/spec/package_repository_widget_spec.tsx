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
import {getPackageRepository} from "models/package_repositories/spec/test_data";
import {PackageOperations} from "views/pages/package_repositories";
import {stubAllMethods, TestHelper} from "views/pages/spec/test_helper";
import {PackageRepoScrollOptions} from "../package_repositories_widget";
import {PackageRepositoryWidget} from "../package_repository_widget";

describe('PackageRepositoryWidgetSpec', () => {
  const helper          = new TestHelper();
  const pkgOperations   = new PackageOperations();
  const onPkgRepoEdit   = jasmine.createSpy("onEdit");
  const onPkgRepoClone  = jasmine.createSpy("onClone");
  const onPkgRepoDelete = jasmine.createSpy("onDelete");
  let packageRepository: PackageRepository;
  let disableActions: boolean;
  let scrollOptions: PackageRepoScrollOptions;

  beforeEach(() => {
    packageRepository = PackageRepository.fromJSON(getPackageRepository());
    disableActions    = false;
    scrollOptions     = {
      package_repo_sm: {
        sm:                 stubAllMethods(["shouldScroll", "getTarget", "setTarget", "scrollToEl", "hasTarget"]),
        shouldOpenEditView: false
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
    helper.mount(() => <PackageRepositoryWidget packageRepository={packageRepository}
                                                disableActions={disableActions}
                                                packageOperations={pkgOperations}
                                                onEdit={onPkgRepoEdit} onClone={onPkgRepoClone}
                                                onDelete={onPkgRepoDelete} scrollOptions={scrollOptions}/>);
  }

  it('should render basic package repo details and action buttons', () => {
    mount();

    expect(helper.byTestId('package-repository-panel')).toBeInDOM();

    expect(helper.textByTestId('key-value-key-name')).toBe('Name');
    expect(helper.textByTestId('key-value-value-name')).toBe(packageRepository.name());
    expect(helper.textByTestId('key-value-key-plugin-id')).toBe('Plugin Id');
    expect(helper.textByTestId('key-value-value-plugin-id')).toBe('npm');

    expect(helper.byTestId('configuration-details-widget')).toBeInDOM();
    expect(helper.byTestId('packages-widget')).toBeInDOM();

    const buttonKeys: { [key: string]: string } = {
      'package-create':      "Create package for package repository 'pkg-repo-name'",
      'package-repo-edit':   "Edit package repository 'pkg-repo-name'",
      'package-repo-clone':  "Clone package repository 'pkg-repo-name'",
      'package-repo-delete': "Delete package repository 'pkg-repo-name'"
    };
    Object.keys(buttonKeys)
          .forEach((key) => {
            expect(helper.byTestId(key)).toBeInDOM();
            expect(helper.byTestId(key)).not.toBeDisabled();
            expect(helper.byTestId(key)).toHaveAttr('title', buttonKeys[key]);
          });
    expect(helper.byTestId('Info Circle-icon')).not.toBeInDOM();
  });

  it('should give a call to the callbacks on relevant button clicks', () => {
    mount();

    helper.clickByTestId('package-create');
    expect(pkgOperations.onAdd).toHaveBeenCalled();

    helper.clickByTestId('package-repo-edit');
    expect(onPkgRepoEdit).toHaveBeenCalled();

    helper.clickByTestId('package-repo-clone');
    expect(onPkgRepoClone).toHaveBeenCalled();

    helper.clickByTestId('package-repo-delete');
    expect(onPkgRepoDelete).toHaveBeenCalled();
  });

  it('should disabled action buttons and render a warning button when disabled is set to true', () => {
    disableActions = true;
    mount();

    ['package-create', 'package-repo-edit', 'package-repo-clone'].forEach((key) => {
      expect(helper.byTestId(key)).toBeDisabled();
      expect(helper.byTestId(key)).toHaveAttr('title', "Plugin not found!");
    });
    expect(helper.byTestId('package-repo-delete')).not.toBeDisabled();
    const warningIcon = helper.byTestId('Info Circle-icon');
    expect(warningIcon).toBeInDOM();
    expect(warningIcon).toHaveAttr('title', "Plugin 'npm' was not found!");
  });
});
