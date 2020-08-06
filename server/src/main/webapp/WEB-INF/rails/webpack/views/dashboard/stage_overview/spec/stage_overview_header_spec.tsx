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

import m from "mithril";
import Stream from "mithril/stream";
import {FlashMessageModelWithTimeout, MessageType} from "../../../components/flash_message";
import {TestHelper} from "../../../pages/spec/test_helper";
import {StageInstance} from "../models/stage_instance";
import {Result} from "../models/types";
import {StageHeaderWidget} from "../stage_overview_header";
import {TestData} from "./test_data";

describe('Stage Overview Header', () => {
  const helper = new TestHelper();

  const pipelineName = "up42";
  const pipelineCounter = 20;
  const stageName = "up42_stage";
  const stageCounter = 1;

  let stageInstance: StageInstance, flashMessage: FlashMessageModelWithTimeout;

  beforeEach(() => {
    flashMessage = new FlashMessageModelWithTimeout();
    stageInstance = new StageInstance(TestData.stageInstanceJSON());
    mount();
  });

  afterEach(helper.unmount.bind(helper));

  it('should render stage overview header container', () => {
    expect(helper.byTestId('stage-overview-header')).toBeInDOM();
  });

  it('should render pipeline name', () => {
    expect(helper.byTestId('pipeline-name-container')).toContainText('Pipeline');
    expect(helper.byTestId('pipeline-name-container')).toContainText('up42');
  });

  it('should render stage name', () => {
    expect(helper.byTestId('stage-name-container')).toContainText('Stage');
    expect(helper.byTestId('stage-name-container')).toContainText('up42_stage');
  });

  it('should render stage instance', () => {
    expect(helper.byTestId('stage-instance-container')).toContainText('Instance');
    expect(helper.byTestId('stage-instance-container')).toContainText('1');
  });

  it('should render stage operations container', () => {
    expect(helper.byTestId('stage-operations-container')).toBeInDOM();
  });

  it('should render stage rerun button whens stage is completed', () => {
    expect(helper.byTestId('stage-operations-container')).toBeInDOM();
    expect(helper.byTestId('rerun-stage')).toBeInDOM();
    expect(helper.byTestId('cancel-stage')).not.toBeInDOM();
  });

  it('should render stage cancel button whens stage is in progress', () => {
    stageInstance.isCompleted = () => false;
    helper.redraw();

    expect(helper.byTestId('stage-operations-container')).toBeInDOM();
    expect(helper.byTestId('rerun-stage')).not.toBeInDOM();
    expect(helper.byTestId('cancel-stage')).toBeInDOM();
  });

  it('should render triggered by information', () => {
    expect(helper.byTestId('stage-trigger-and-timing-container')).toBeInDOM();
    expect(helper.byTestId('stage-trigger-and-timing-container')).toContainText('Triggered by admin');
  });

  it('should render triggered on information', () => {
    expect(helper.byTestId('stage-trigger-and-timing-container')).toBeInDOM();
    expect(helper.byTestId('stage-trigger-and-timing-container')).toContainText('on 27 Jul, 2020 at 10:20:14 Local Time');
  });

  it('should render stage duration', () => {
    expect(helper.byTestId('stage-trigger-and-timing-container')).toContainText('Duration');
    expect(helper.byTestId('stage-trigger-and-timing-container')).toContainText('01h 40m 50s');
  });

  it('should render stage cancelled by when stage is cancelled', () => {
    helper.unmount();
    const json = TestData.stageInstanceJSON();
    json.result = Result[Result.Cancelled];
    json.cancelled_by = 'admin';
    stageInstance = new StageInstance(json);
    mount();

    expect(helper.byTestId('cancelled-by-container')).toBeInDOM();
    expect(helper.byTestId('cancelled-on-container')).toBeInDOM();
    expect(helper.byTestId('cancelled-by-container')).toContainText('Cancelled by admin');
    expect(helper.byTestId('cancelled-on-container')).toContainText('on 27 Jul, 2020 at 12:01:04 Local Time');
  });

  it('should render link to stage details page', () => {
    const expectedLink = `/go/pipelines/up42/20/up42_stage/1`;
    const link = helper.q('a');

    expect(helper.byTestId('stage-details-page-link')).toBeInDOM();
    expect(link).toContainText('Go to Stage Details Page >>');
    expect(link.href.indexOf(expectedLink)).not.toBe(-1);
  });

  it('should not render flash message when no message is present', () => {
    expect(flashMessage.message).toBe(undefined);
    expect(helper.byTestId('stage-overview-flash-message')).not.toBeInDOM();
  });

  it('should render flash message when message is present', () => {
    flashMessage.setMessage(MessageType.success, "Success!");
    helper.redraw();

    expect(helper.byTestId('stage-overview-flash-message')).toBeInDOM();
  });

  function mount() {
    helper.mount(() => {
      const stageInstanceFromDashboard = {
        canOperate: true
      };

      return <StageHeaderWidget pipelineName={pipelineName}
                                stageInstanceFromDashboard={stageInstanceFromDashboard}
                                pipelineCounter={pipelineCounter}
                                stageName={stageName}
                                stageCounter={stageCounter}
                                flashMessage={flashMessage}
                                stageInstance={Stream(stageInstance)}/>;
    });
  }
});
