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
import {PipelineConfig, TrackingTool} from "models/pipeline_configs/pipeline_config";
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

    expect(tableRow[0].textContent).toBe("some-random-sha");
    expect(tableRow[1].textContent).toBe("username <username@github.com>");
    expect(tableRow[2].textContent).toBe(timeFormatter.format(materialRevisions[0].modifiedAt));
    expect(tableRow[3].textContent).toBe("some commit message");
  });

  it('should render comment with link wrt tracking tool', () => {
    const pipelineConfig = new PipelineConfig();
    pipelineConfig.trackingTool(new TrackingTool());
    pipelineConfig.trackingTool().urlPattern("http://example.com/${ID}");
    pipelineConfig.trackingTool().regex("##(\\d+)");
    const materialRevisions            = defaultRevs();
    materialRevisions[0].commitMessage = "#123 some commit message related to another issue #456";
    mount(materialRevisions, pipelineConfig);

    expect(helper.byTestId("material-revisions-widget")).toBeInDOM();
    const tableRow = helper.qa("td", helper.byTestId("table-row"));

    expect(tableRow[3].textContent).toBe("#123 some commit message related to another issue #456");
    expect(helper.qa('a', tableRow[3])[0].textContent).toBe("#123");
    expect(helper.qa('a', tableRow[3])[0]).toHaveAttr("href", "http://example.com/123");
    expect(helper.qa('a', tableRow[3])[1].textContent).toBe("#456");
    expect(helper.qa('a', tableRow[3])[1]).toHaveAttr("href", "http://example.com/456");
  });

  function defaultRevs() {
    return MaterialRevisions.fromJSON(ComparisonData.materialRevisions());
  }

  function mount(revisions: MaterialRevisions = defaultRevs(), pipelineConfig = new PipelineConfig()) {
    helper.mount(() => <MaterialRevisionsWidget result={revisions} pipelineConfig={pipelineConfig}/>);
  }
});
