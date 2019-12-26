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

import {PipelineHistory} from "models/compare/pipeline_instance";
import {PipelineHistoryJSON, stringOrUndefined} from "models/compare/pipeline_instance_json";
import {PipelineInstanceData} from "models/compare/spec/test_data";
import {ModalState} from "views/components/modal";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../modal.scss";
import {ApiService, TimelineModal} from "../timeline_modal";

describe('TimelineModalSpec', () => {
  const helper                 = new TestHelper();
  const pipelineName           = "up42";
  let pipelineHistory: PipelineHistory;
  const onInstanceSelectionSpy = jasmine.createSpy("onInstanceSelection");
  let modal: TimelineModal;

  beforeEach(() => {
    const pipelineJson = PipelineInstanceData.pipeline();
    const json         = {
      _links:    {
        next:     {
          href: "next-link"
        },
        previous: {
          href: "previous-link"
        }
      },
      pipelines: [pipelineJson]
    } as PipelineHistoryJSON;

    pipelineHistory = PipelineHistory.fromJSON("up42", json);
  });
  afterEach((done) => helper.unmount(done));

  function mount(history: PipelineHistory = pipelineHistory) {
    modal = new TimelineModal(pipelineName, onInstanceSelectionSpy, new DummyFetchService(history));
    helper.mount(modal.view.bind(modal));
  }

  it('should render title', () => {
    mount();
    expect(modal.title()).toBe("Select a pipeline to compare");
  });

  it('should disable save and cancel button and show a spinner if modal state is loading', () => {
    mount();
    modal.modalState = ModalState.LOADING;
    helper.redraw();
    expect(helper.byTestId("button-select-instance")).toBeDisabled();
    expect(helper.byTestId("button-cancel")).toBeDisabled();
    expect(helper.byTestId("spinner")).toBeInDOM();
    expect(helper.byTestId("timeline-modal-body")).not.toBeInDOM();
  });

  it('should show a flash message with a msg if any', () => {
    mount();
    modal.errorMessage("Some error has occurred!!!");
    helper.redraw();

    expect(helper.byTestId("flash-message-alert")).toBeInDOM();
    expect(helper.byTestId("timeline-modal-body")).not.toBeInDOM();
    expect(helper.textByTestId("flash-message-alert")).toEqual("Some error has occurred!!!");
  });

  it('should render the left pane with the latest history of pipeline instances', () => {
    mount();
    expect(helper.byTestId("timeline-modal-body")).toBeInDOM();

    expect(helper.qa("tr", helper.byTestId("left-pane-history")).length).toBe(1);

    expect(helper.q("td", helper.byTestId("instance-2")).innerText).toBe("2");
    expect(helper.qa("td", helper.byTestId("instance-2")).length).toBe(2);
  });

  it('should have enabled next and previous buttons if both links are available', () => {
    mount();

    const pageDiv = helper.byTestId("pagination");
    expect(pageDiv).toBeInDOM();
    expect(helper.q("a[title='Previous']", pageDiv)).not.toHaveClass(styles.disabled);
    expect(helper.q("a[title='Next']", pageDiv)).not.toHaveClass(styles.disabled);
  });

  it('should disabled previous button if no link is present', () => {
    pipelineHistory.previousLink = undefined;
    mount(pipelineHistory);

    const pageDiv = helper.byTestId("pagination");
    expect(pageDiv).toBeInDOM();
    expect(helper.q("a[title='Previous']", pageDiv)).toHaveClass(styles.disabled);
    expect(helper.q("a[title='Next']", pageDiv)).not.toHaveClass(styles.disabled);
  });

  it('should disabled next button if no link is present', () => {
    pipelineHistory.nextLink = undefined;
    mount(pipelineHistory);

    const pageDiv = helper.byTestId("pagination");
    expect(pageDiv).toBeInDOM();
    expect(helper.q("a[title='Previous']", pageDiv)).not.toHaveClass(styles.disabled);
    expect(helper.q("a[title='Next']", pageDiv)).toHaveClass(styles.disabled);
  });
});

class DummyFetchService implements ApiService {
  pipelineHistory: PipelineHistory;

  constructor(pipelineHistory: PipelineHistory) {
    this.pipelineHistory = pipelineHistory;
  }

  fetchHistory(pipelineName: string, link: stringOrUndefined,
               onSuccess: (data: PipelineHistory) => void,
               onError: (message: string) => void): void {
    onSuccess(this.pipelineHistory);
  }

}
