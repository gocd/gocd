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
import {PipelineInstance} from "models/compare/pipeline_instance";
import {PipelineInstanceData} from "models/compare/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {InstanceSelectionWidget} from "../instance_selection_widget";

describe('InstanceSelectionWidgetSpec', () => {
  const helper = new TestHelper();
  let instance: PipelineInstance;

  beforeEach(() => {
    instance = PipelineInstance.fromJSON(PipelineInstanceData.pipeline(9));
  });
  afterEach(() => helper.unmount());

  function mount() {
    helper.mount(() => <InstanceSelectionWidget instance={instance}
                                                onInstanceChange={jasmine.createSpy("onInstanceChange")}/>);
  }

  it('should render the widget to enter search query', () => {
    mount();

    expect(helper.byTestId("instance-selection-widget-9")).toBeInDOM();
  });

  it('should render the stages', () => {
    mount();

    const stageElement = helper.byTestId("stages");
    const stageRows    = helper.qa("tr", stageElement);
    const stages       = helper.qa("td", stageElement);

    expect(stageElement).toBeInDOM();
    expect(stageRows.length).toBe(1);
    expect(stages.length).toBe(1);
  });

  it('should render triggered by info', () => {
    mount();

    expect(helper.byTestId("triggered-by")).toBeInDOM();
    expect(helper.textByTestId("triggered-by")).toBe(`Triggered by ${instance.buildCause().getApprover()} on ${timeFormatter.format(instance.scheduledDate())}`);
  });

  it('should render warning msg if the instance is a bisect', () => {
    instance.naturalOrder(9.5);
    mount();

    expect(helper.byTestId("warning")).toBeInDOM();
    expect(helper.byTestId("Warning-icon")).toBeInDOM();
    expect(helper.textByTestId("warning")).toBe("This pipeline instance was triggered with a non-sequential material revision.");
  });
});
