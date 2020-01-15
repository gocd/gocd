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
import Stream from "mithril/stream";
import {PipelineInstance, PipelineInstances} from "models/compare/pipeline_instance";
import {PipelineInstanceData} from "models/compare/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {ApiService} from "../instance_selection_widget";
import {SelectInstanceWidget} from "../select_instance_widget";

describe('SelectInstanceWidgetSpec', () => {
  const helper              = new TestHelper();
  const onInstanceChangeSpy = jasmine.createSpy("onInstanceChange");
  let instance: PipelineInstance;

  beforeEach(() => {
    instance = PipelineInstance.fromJSON(PipelineInstanceData.pipeline(9));
  });
  afterEach((done) => helper.unmount(done));

  function mount(service: ApiService = new DummyService()) {
    const show: Stream<boolean> = Stream();
    helper.mount(() => <SelectInstanceWidget show={show} instance={instance}
                                             onInstanceChange={onInstanceChangeSpy}
                                             apiService={service}/>);
  }

  it('should render text box with current selected counter and help text', () => {
    mount();
    const placeHolder = "Search for a pipeline instance by label, committer, date, etc.";

    expect(helper.byTestId("form-field-input-")).toBeInDOM();
    expect(helper.byTestId("form-field-input-")).toHaveValue("9");
    expect(helper.byTestId("form-field-input-")).toHaveAttr("placeholder", placeHolder);
    expect(helper.q("label")).toBeInDOM();
    expect(helper.q("label").innerText).toBe(placeHolder);
    expect(helper.q("span[id='help-text']")).toBeInDOM();
    expect(helper.q("span[id='help-text']").innerText).toBe("Browse the timeline");
  });

  it('should render no results found if no results found on input change', () => {
    mount(new class implements ApiService {
      getMatchingInstances(pipelineName: string, pattern: string, onSuccess: (data: PipelineInstances) => void, onError: (message: string) => void): void {
        onSuccess([]);
      }
    }());
    helper.oninput(helper.byTestId("form-field-input-"), "ab");

    expect(helper.byTestId("no-results-div")).toBeInDOM();
    expect(helper.textByTestId("no-results-div")).toBe("No matching results!");
  });

  it('should render results on input change', () => {
    mount();
    helper.oninput(helper.byTestId("form-field-input-"), "cb");

    const element = helper.byTestId("matching-instances-results");
    expect(element).toBeInDOM();
    expect(helper.qa("li", element).length).toBe(1);

    const instanceElement = helper.byTestId(`instance-4`);
    const triggeredByMsg  = `Triggered by ${instance.buildCause().getApprover()} on ${timeFormatter.format(instance.scheduledDate())}`;
    expect(instanceElement).toBeInDOM();
    expect(helper.q("h5", instanceElement)).toBeInDOM();
    expect(helper.q("h5", instanceElement).innerText).toBe("4");
    expect(helper.byTestId("stages", instanceElement)).toBeInDOM();
    expect(helper.byTestId("triggered-by", instanceElement)).toBeInDOM();
    expect(helper.byTestId("triggered-by", instanceElement).innerText).toBe(triggeredByMsg);

    const modificationTable = helper.byTestId("modification-0", instanceElement);
    const modification      = instance.buildCause().materialRevisions()[0].modifications()[0];
    const modifiedByMsg     = `${modification.userName()} on ${timeFormatter.format(modification.modifiedTime())}`;
    expect(modificationTable).toBeInDOM();
    expect(helper.qa("tr", modificationTable).length).toBe(3);
    expect(helper.qa("th", modificationTable)[0].innerText).toBe("Revision");
    expect(helper.qa("th", modificationTable)[1].innerText).toBe("Comment");
    expect(helper.qa("th", modificationTable)[2].innerText).toBe("Modified by");
    expect(helper.qa("td", modificationTable)[0].innerText).toBe("11932fecb6d3f7ec22e1b0cb3a88553a");
    expect(helper.qa("td", modificationTable)[1].innerText).toBe("some comment");
    expect(helper.qa("td", modificationTable)[2].innerText).toBe(modifiedByMsg);
  });

  it('should not render revisions it does contain the input pattern', () => {
    mount();
    helper.oninput(helper.byTestId("form-field-input-"), "ab");

    const element = helper.byTestId("matching-instances-results");
    expect(element).toBeInDOM();
    expect(helper.qa("li", element).length).toBe(1);

    const instanceElement = helper.byTestId(`instance-4`);
    const triggeredByMsg  = `Triggered by ${instance.buildCause().getApprover()} on ${timeFormatter.format(instance.scheduledDate())}`;
    expect(instanceElement).toBeInDOM();
    expect(helper.q("h5", instanceElement)).toBeInDOM();
    expect(helper.q("h5", instanceElement).innerText).toBe(`4`);
    expect(helper.byTestId("stages", instanceElement)).toBeInDOM();
    expect(helper.byTestId("triggered-by", instanceElement)).toBeInDOM();
    expect(helper.byTestId("triggered-by", instanceElement).innerText).toBe(triggeredByMsg);

    const modificationTable = helper.byTestId("modification-0", instanceElement);
    expect(modificationTable).not.toBeInDOM();
  });

  it('should call the spy on click on an instance from the list', () => {
    mount();
    helper.oninput(helper.byTestId("form-field-input-"), "ab");

    helper.clickByTestId("instance-4");
    expect(onInstanceChangeSpy).toHaveBeenCalledWith(4);
  });
});

class DummyService implements ApiService {
  getMatchingInstances(pipelineName: string, pattern: string, onSuccess: (data: PipelineInstances) => void, onError: (message: string) => void): void {
    const instances = new PipelineInstances();
    const ins       = PipelineInstance.fromJSON(PipelineInstanceData.pipeline(4));
    ins.name(pipelineName);
    ins.label("4");
    instances.push(ins);

    onSuccess(instances);
  }
}
