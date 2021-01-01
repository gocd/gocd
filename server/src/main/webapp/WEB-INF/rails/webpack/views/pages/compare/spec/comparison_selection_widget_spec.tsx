/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {PipelineInstance} from "models/compare/pipeline_instance";
import {PipelineInstanceData} from "models/compare/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {ComparisonSelectionWidget} from "../comparison_selection_widget";
import styles from "../index.scss";

describe('ComparisonSelectionWidgetSpec', () => {
  const helper = new TestHelper();
  const spy    = jasmine.createSpy("reloadWithNewCounters");

  afterEach(() => helper.unmount());

  function mount(fromInstance: PipelineInstance, toInstance: PipelineInstance) {
    helper.mount(() => <ComparisonSelectionWidget pipelineName="up42"
                                                  fromInstance={fromInstance}
                                                  toInstance={toInstance}
                                                  reloadWithNewCounters={spy}/>);
  }

  it('should render the comparison header', () => {
    const fromInstance = PipelineInstance.fromJSON(PipelineInstanceData.pipeline(4));
    const toInstance   = PipelineInstance.fromJSON(PipelineInstanceData.pipeline(5));

    mount(fromInstance, toInstance);

    const widget       = helper.byTestId("comparison-selection-widget");
    const tableElement = helper.q("table", widget);
    const tableCols    = helper.qa("tbody > tr > td", tableElement);

    expect(widget).toBeInDOM();
    expect(tableElement).toBeInDOM();
    expect(tableCols.length).toBe(3);
    expect(tableCols[0]).toHaveClass(styles.pipelineInstanceSelection);
    expect(helper.byTestId("pipeline-from-instance", tableCols[0])).toBeInDOM();
    expect(tableCols[1]).toHaveClass(styles.pipelineComparisonText);
    expect(tableCols[1].innerText).toBe("compared to");
    expect(tableCols[2]).toHaveClass(styles.pipelineInstanceSelection);
    expect(helper.byTestId("pipeline-to-instance", tableCols[2])).toBeInDOM();
  });

});
