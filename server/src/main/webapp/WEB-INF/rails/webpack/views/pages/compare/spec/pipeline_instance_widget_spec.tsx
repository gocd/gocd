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

import {timeFormatter} from "helpers/time_formatter";
import m from "mithril";
import {PipelineInstance} from "models/compare/pipeline_instance";
import {PipelineInstanceData} from "models/compare/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../modal.scss";
import {PipelineInstanceWidget} from "../pipeline_instance_widget";

describe('PipelineInstanceWidgetSpec', () => {
  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  function mount(instance: PipelineInstance) {
    helper.mount(() => <PipelineInstanceWidget instance={instance}/>);
  }

  it('should render a flash message if instance is not passed', () => {
    helper.mount(() => <PipelineInstanceWidget/>);

    expect(helper.byTestId("pipeline-instance-widget")).not.toBeInDOM();
    expect(helper.byTestId("flash-message-alert")).toBeInDOM();
    expect(helper.textByTestId("flash-message-alert")).toEqual("Please select an instance!");
  });

  it('should render pipeline counter', () => {
    const instance = PipelineInstance.fromJSON(PipelineInstanceData.pipeline());
    mount(instance);

    const element = helper.byTestId("pipeline-instance-widget");
    expect(element).toBeInDOM();
    expect(helper.q("h3", element).innerText).toEqual(instance.counter() + "");
  });

  describe('RenderStageSpec', () => {
    const parameters = [
      {description: "should render stage as passed", input: "passed", output: styles.passed}
      , {description: "should render stage as building", input: "building", output: styles.building}
      , {description: "should render stage as failed", input: "failed", output: styles.failed}
      , {description: "should render stage as failing", input: "failing", output: styles.failing}
      , {description: "should render stage as cancelled", input: "cancelled", output: styles.cancelled}
      , {description: "should render stage as waiting", input: "waiting", output: styles.waiting}
      , {description: "should render stage as unknown", input: "unknown", output: styles.unknown}
    ];
    parameters.forEach((parameter) => {
      it(parameter.description, () => {
        const instance = PipelineInstance.fromJSON(PipelineInstanceData.pipeline());
        instance.stages()[0].status(parameter.input);
        instance.stages()[0].result(parameter.input);
        mount(instance);

        const stages = helper.byTestId("pipeline-instance-stages");
        expect(stages).toBeInDOM();
        expect(helper.qa("td", stages).length).toBe(1);
        expect(helper.q("span", stages)).toHaveAttr("class", parameter.output);
      });
    });
  });

  it('should render triggered by info', () => {
    const instance   = PipelineInstance.fromJSON(PipelineInstanceData.pipeline());
    const triggerMsg = `Triggered by ${instance.buildCause().approver()} on ${timeFormatter.format(instance.stages().getScheduledDate())}`;

    mount(instance);

    expect(helper.textByTestId("triggered-by")).toEqual(triggerMsg);
    expect(helper.q("span", helper.byTestId("triggered-by"))).toHaveAttr("title", timeFormatter.formatInServerTime(instance.stages().getScheduledDate()));
  });

  it('should render material revisions', () => {
    const instance = PipelineInstance.fromJSON(PipelineInstanceData.pipeline());
    mount(instance);

    expect(helper.byTestId("instance-material-revisions")).toBeInDOM();
    expect(helper.qa("ul", helper.byTestId("instance-material-revisions")).length).toBe(1);
    expect(helper.qa("[data-test-id^='key-value-key-']").length).toBe(4);
    expect(helper.qa("[data-test-id^='key-value-value-']").length).toBe(4);
  });

  it('should render dependency revisions', () => {
    const instance = PipelineInstance.fromJSON(PipelineInstanceData.pipeline());
    // just replacing material type
    instance.buildCause().materialRevisions()[0].material().type("pipeline");
    mount(instance);

    expect(helper.byTestId("instance-material-revisions")).toBeInDOM();
    expect(helper.qa("ul", helper.byTestId("instance-material-revisions")).length).toBe(1);
    expect(helper.qa("[data-test-id^='key-value-key-']").length).toBe(3);
    expect(helper.qa("[data-test-id^='key-value-value-']").length).toBe(3);
  });
});
