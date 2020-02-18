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

import {timeFormatter} from "helpers/time_formatter";
import m from "mithril";
import {MaterialRevisions} from "models/compare/compare";
import {ComparisonData} from "models/compare/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {MaterialRevisionsWidget} from "../material_revisions_widget";

describe('MaterialRevisionsWidgetSpec', () => {
  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  it('should showcase materials revisions', () => {
    const materialRevisions = defaultRevs();
    mount(materialRevisions);
    expect(helper.byTestId("material-revisions-widget")).toBeInDOM();
    expect(helper.textByTestId("table-header-row")).toContain("RevisionModified ByModified AtComment");

    const tableRow = helper.qa("td", helper.byTestId("table-row"));

    expect(tableRow[0].innerText).toContain("some-random-sha");
    expect(tableRow[1].innerText).toContain("username <username@github.com>");
    expect(tableRow[2].innerText).toContain(timeFormatter.format(materialRevisions[0].modifiedAt));
    expect(tableRow[3].innerText).toContain("some commit message");
  });

  function defaultRevs() {
    return MaterialRevisions.fromJSON(ComparisonData.materialRevisions());
  }

  function mount(revisions: MaterialRevisions = defaultRevs()) {
    helper.mount(() => <MaterialRevisionsWidget result={revisions}/>);
  }
});
