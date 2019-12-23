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
import {PipelineInstance, Stage, Stages} from "models/compare/pipeline_instance";
import {PipelineInstanceData} from "models/compare/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";
import {InstanceSelectionWidget} from "../instance_selection_widget";

describe('InstanceSelectionWidgetSpec', () => {
  const helper = new TestHelper();
  let instance: PipelineInstance;

  beforeEach(() => {
    instance = PipelineInstance.fromJSON(PipelineInstanceData.pipeline(9));
  });
  afterEach(() => helper.unmount());

  function mount() {
    helper.mount(() => <InstanceSelectionWidget instance={instance}/>);
  }

  it('should render text box with current selected counter and help text', () => {
    mount();
    const placeHolder = "Search for a pipeline instance by label, committer, date, etc.";

    expect(helper.byTestId("instance-selection-widget-9")).toBeInDOM();
    expect(helper.byTestId("form-field-input-")).toBeInDOM();
    expect(helper.byTestId("form-field-input-")).toHaveValue("9");
    expect(helper.byTestId("form-field-input-")).toHaveAttr("placeholder", placeHolder);
    expect(helper.q("span[id*='help-text']")).toBeInDOM();
    expect(helper.q("span[id*='help-text']").innerText).toBe(placeHolder + "\nor\n" + "Browse the timeline");
  });

  it('should render the stages in a single row if less than 5', () => {
    mount();

    const stageElement = helper.byTestId("stages");
    const stageRows    = helper.qa("tr", stageElement);

    expect(stageElement).toBeInDOM();
    expect(stageRows.length).toBe(1);
  });

  it('should render the stages in two rows', () => {
    const stages = new Stages();

    stages.push(new Stage(1, "stage", "1", false, "Building", "", "", false, false, []));
    stages.push(new Stage(2, "stage", "2", false, "Failed", "", "", false, false, []));
    stages.push(new Stage(3, "stage", "3", false, "Cancelled", "", "", false, false, []));
    stages.push(new Stage(4, "stage", "4", false, "unknown", "", "", false, false, []));
    stages.push(new Stage(5, "stage", "5", false, "Passed", "", "", false, false, []));
    stages.push(new Stage(6, "stage", "6", false, "Waiting", "", "", false, false, []));

    instance.stages(stages);
    mount();

    const stageElement = helper.byTestId("stages");
    const stageRows    = helper.qa("tr", stageElement);
    const stageCols    = helper.qa("td", stageElement);

    expect(stageElement).toBeInDOM();
    expect(stageRows.length).toBe(2);
    expect(stageCols.length).toBe(6);
    expect(helper.q("span", stageCols[0])).toHaveClass(styles.building);
    expect(helper.q("span", stageCols[1])).toHaveClass(styles.failed);
    expect(helper.q("span", stageCols[2])).toHaveClass(styles.cancelled);
    expect(helper.q("span", stageCols[3])).toHaveClass(styles.unknown);
    expect(helper.q("span", stageCols[4])).toHaveClass(styles.passed);
    expect(helper.q("span", stageCols[5])).toHaveClass(styles.waiting);
  });
});
