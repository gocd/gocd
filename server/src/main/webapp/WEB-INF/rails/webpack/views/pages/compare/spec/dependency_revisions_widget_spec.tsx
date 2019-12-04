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
import {DependencyRevisions} from "models/compare/compare";
import {ComparisonData} from "models/compare/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {DependencyRevisionsWidget} from "../dependency_revisions_widget";

describe('DependencyRevisionsWidgetSpec', () => {
  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  it('should showcase revisions related to a pipeline', () => {
    mount();
    expect(helper.byTestId("dependency-revisions-widget")).toBeInDOM();
    expect(helper.textByTestId("table-header-row")).toContain("RevisionInstanceCompleted At");

    const tableRow = helper.qa("td", helper.byTestId("table-row"));

    expect(tableRow[0].innerText).toContain("upstream/1/upstream_stage/1");
    expect(tableRow[1].innerText).toContain("1");
    expect(tableRow[2].innerText).toContain("17 Oct, 2019 at 12:25:07 Local Time");

    expect(helper.q("a", tableRow[0])).toBeInDOM();
    expect(helper.q("a", tableRow[0]).getAttribute("href")).toEqual("/go/pipelines/upstream/1/upstream_stage/1");
    expect(helper.q("a", tableRow[1])).toBeInDOM();
    expect(helper.q("a", tableRow[1]).getAttribute("href")).toEqual("/go/pipelines/value_stream_map/pipeline/1");
  });

  function mount(pipelineName: string = "pipeline", revisions: DependencyRevisions = defaultRevs()) {
    helper.mount(() => <DependencyRevisionsWidget pipelineName={pipelineName} result={revisions}/>);
  }

  function defaultRevs() {
    return DependencyRevisions.fromJSON(ComparisonData.dependencyMaterialRevisions());
  }
});
