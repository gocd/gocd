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
import {TestHelper} from "../../../pages/spec/test_helper";
import {StageInstance} from "../models/stage_instance";
import {StageHeaderWidget} from "../stage_overview_header";
import {TestData} from "./test_data";

describe('Stage Overview Header', () => {
  const helper = new TestHelper();

  const pipelineName = "up42";
  const pipelineCounter = 20;
  const stageName = "up42_stage";
  const stageCounter = 1;

  let stageInstance: StageInstance;

  beforeEach(() => {
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

  it('should render triggered by information', () => {
    expect(helper.byTestId('stage-trigger-and-timing-container')).toBeInDOM();
    expect(helper.byTestId('stage-trigger-and-timing-container')).toContainText('Triggered by admin');
  });

  it('should render triggered on information', () => {
    expect(helper.byTestId('stage-trigger-and-timing-container')).toBeInDOM();
    expect(helper.byTestId('stage-trigger-and-timing-container')).toContainText('on 09 Jul, 2020 at 12:36:04 Local Time');
  });

  it('should render stage duration', () => {
    expect(helper.byTestId('stage-duration-container')).toBeInDOM();
    expect(helper.byTestId('stage-duration-container')).toContainText('Duration');
    expect(helper.byTestId('stage-duration-container')).toContainText('03m 24s');
  });

  function mount() {
    helper.mount(() => {
      return <StageHeaderWidget pipelineName={pipelineName}
                                pipelineCounter={pipelineCounter}
                                stageName={stageName}
                                stageCounter={stageCounter}
                                stageInstance={Stream(stageInstance)}/>;
    });
  }
});
