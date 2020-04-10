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
import {PackageUsages} from "models/package_repositories/package_repositories";
import {ModalManager} from "views/components/modal/modal_manager";
import {TestHelper} from "views/pages/spec/test_helper";
import {UsagePackageModal} from "../package_modals";

describe('PackageUsageModalSpec', () => {

  afterEach(() => ModalManager.closeAll());

  it('should render no usages found msg', () => {
    const modal = new UsagePackageModal("scm-id", new PackageUsages());
    modal.render();
    m.redraw.sync();

    expect(modal).toContainTitle("Usages for scm-id");
    expect(modal).toContainButtons(["OK"]);
    expect(modal).toContainBody("No usages for scm 'scm-id' found.");
  });

  it('should render usages with link to material settings', () => {
    const modal = new UsagePackageModal("scm-id", PackageUsages.fromJSON({usages: [{group: 'group1', pipeline: 'pipeline1'}]}));
    modal.render();
    m.redraw.sync();

    const helper = new TestHelper().forModal();

    expect(helper.byTestId('table')).toBeInDOM();
    const tableHeaders = helper.qa('th');
    expect(tableHeaders.length).toBe(3);
    expect(tableHeaders[0].textContent).toBe('Group');
    expect(tableHeaders[1].textContent).toBe('Pipeline');

    const tableRows = helper.qa('td');
    expect(tableRows.length).toBe(3);
    expect(tableRows[0].textContent).toBe('group1');
    expect(tableRows[1].textContent).toBe('pipeline1');
    expect(tableRows[2].textContent).toBe('Pipeline Material Settings');
    expect(helper.q('a', tableRows[2])).toHaveAttr('href', '/go/admin/pipelines/pipeline1/materials');
  });
});
