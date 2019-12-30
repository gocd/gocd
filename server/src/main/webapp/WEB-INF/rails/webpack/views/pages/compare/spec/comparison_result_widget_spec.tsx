/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import {Comparison} from "models/compare/compare";
import {ComparisonData} from "models/compare/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {ComparisonResultWidget} from "../comparison_result_widget";

describe('ComparisonResultWidgetSpec', () => {
  const helper = new TestHelper();
  let comparison: Comparison;

  beforeEach(() => {
    comparison = Comparison.fromJSON(ComparisonData.compare());
  });
  afterEach((done) => helper.unmount(done));

  function mount(comparison: Comparison) {
    helper.mount(() => <ComparisonResultWidget comparisonResult={comparison}/>);
  }

  it('should showcase comparison result', () => {
    mount(comparison);

    expect(helper.byTestId("info-msg")).not.toBeInDOM();
    expect(helper.byTestId("comparison-result-widget")).toBeInDOM();
    expect(helper.byTestId("material-changes")).toBeInDOM();
    expect(helper.byTestId("material-revisions-widget")).toBeInDOM();
    expect(helper.byTestId("dependency-revisions-widget")).toBeInDOM();
    expect(helper.qa("[data-test-id='material-header']")[0]).toHaveText("Git - URL: git@github.com:sample_repo/example.git, Branch: master");
    expect(helper.qa("[data-test-id='material-header']")[1]).toHaveText("Pipeline - upstream [ upstream_stage ]");
  });

  it('should showcase warning info if the comparison is between bisect instances', () => {
    comparison.isBisect = true;
    mount(comparison);

    expect(helper.byTestId("info-msg")).toBeInDOM();
    expect(helper.byTestId("comparison-result-widget")).toBeInDOM();
    expect(helper.textByTestId("info-msg")).toBe("This comparison involves a pipeline instance that was triggered with a non-sequential material revision.");
    expect(helper.byTestId("Info Circle-icon")).toBeInDOM();
  });
});
