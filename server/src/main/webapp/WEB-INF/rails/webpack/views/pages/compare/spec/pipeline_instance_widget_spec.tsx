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

import {timeFormatter} from "helpers/time_formatter";
import m from "mithril";
import {PipelineInstance} from "models/compare/pipeline_instance";
import {PipelineInstanceData} from "models/compare/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {PipelineInstanceWidget} from "../pipeline_instance_widget";

describe('PipelineInstanceWidgetSpec', () => {
  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  function mount(instance: PipelineInstance) {
    helper.mount(() => <PipelineInstanceWidget instance={instance}/>);
  }

  it('should render a flash message if instance is not passed', () => {
    //@ts-ignore
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

  it('should render stages and triggered by info', () => {
    const instance   = PipelineInstance.fromJSON(PipelineInstanceData.pipeline());
    const triggerMsg = `Triggered by ${instance.buildCause().approver()} on ${timeFormatter.format(instance.scheduledDate())}`;

    mount(instance);

    expect(helper.byTestId("stages")).toBeInDOM();
    expect(helper.textByTestId("triggered-by")).toEqual(triggerMsg);
    expect(helper.q("span", helper.byTestId("triggered-by"))).toHaveAttr("title", timeFormatter.formatInServerTime(instance.scheduledDate()));
  });

  it('should render material revisions', () => {
    const instance = PipelineInstance.fromJSON(PipelineInstanceData.pipeline());
    mount(instance);

    expect(helper.byTestId("instance-material-revisions")).toBeInDOM();
    const materials = helper.qa("ul", helper.byTestId("instance-material-revisions"));
    expect(materials.length).toBe(2);

    // verify non-pipeline material modifications
    let keys   = helper.qa("[data-test-id^='key-value-key-']", materials[0]);
    let values = helper.qa("[data-test-id^='key-value-value-']", materials[0]);
    expect(keys.length).toBe(4);
    expect(keys[0].innerText).toBe('Revision');
    expect(keys[1].innerText).toBe('Username');
    expect(keys[2].innerText).toBe('Comment');
    expect(keys[3].innerText).toBe('Modified On');
    expect(values.length).toBe(4);
    const modification = instance.buildCause().materialRevisions()[0].modifications()[0];
    expect(values[0].innerText).toBe(modification.revision());
    expect(values[1].innerText).toBe(modification.userName());
    expect(values[2].innerText).toBe(modification.comment() || "");
    expect(values[3].innerText).toBe(timeFormatter.format(modification.modifiedTime()));

    // verify pipeline material modifications
    keys   = helper.qa("[data-test-id^='key-value-key-']", materials[1]);
    values = helper.qa("[data-test-id^='key-value-value-']", materials[1]);
    expect(keys.length).toBe(3);
    expect(keys[0].innerText).toBe('Revision');
    expect(keys[1].innerText).toBe('Material');
    expect(keys[2].innerText).toBe('Modified On');
    expect(values.length).toBe(3);
    expect(values[0].innerText).toBe(instance.buildCause().materialRevisions()[1].modifications()[0].revision());
    expect(values[1].innerText).toBe(instance.buildCause().materialRevisions()[1].material().description());
    expect(values[2].innerText).toBe(timeFormatter.format(instance.buildCause().materialRevisions()[1].modifications()[0].modifiedTime()));
  });
});
