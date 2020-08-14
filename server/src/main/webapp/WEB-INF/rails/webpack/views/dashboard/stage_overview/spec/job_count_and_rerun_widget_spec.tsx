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
import {Agents} from "../../../../models/agents/agents";
import {FlashMessageModelWithTimeout} from "../../../components/flash_message";
import {TestHelper} from "../../../pages/spec/test_helper";
import {JobCountAndRerunWidget} from "../job_count_and_rerun_widget";
import {JobsViewModel} from "../models/jobs_view_model";
import {Result} from "../models/types";
import {TestData} from "./test_data";

describe("Job Count And Rerun Widget", () => {
  const helper = new TestHelper();

  let jobs: JobsViewModel;
  beforeEach(() => {
    jobs = new JobsViewModel(TestData.stageInstanceJSON().jobs, new Agents());
    mount();
  });
  afterEach(helper.unmount.bind(helper));

  it("should render job count container", () => {
    expect(helper.byTestId('job-cont-container')).toBeInDOM();
  });

  it("should render building job count", () => {
    expect(helper.byTestId('in-progress-jobs-container')).toBeInDOM();
    expect(helper.byTestId('in-progress-jobs-container')).toContainText('Building');
    expect(helper.byTestId('in-progress-jobs-container')).toContainText(`0`);
  });

  it("should render passed job count", () => {
    expect(helper.byTestId('passed-jobs-container')).toBeInDOM();
    expect(helper.byTestId('passed-jobs-container')).toContainText('Passed');
    expect(helper.byTestId('passed-jobs-container')).toContainText(`1`);
  });

  it("should render failed job count", () => {
    expect(helper.byTestId('failed-jobs-container')).toBeInDOM();
    expect(helper.byTestId('failed-jobs-container')).toContainText('Failed');
    expect(helper.byTestId('failed-jobs-container')).toContainText(`0`);
  });

  it('should render job rerun buttons', () => {
    expect(helper.byTestId('job-rerun-container')).toBeInDOM();
    expect(helper.qa('button')).toHaveLength(2);
    expect(helper.qa('button')[0]).toContainText('Rerun Failed');
    expect(helper.qa('button')[1]).toContainText('Rerun Selected');
  });

  it('should render rerun failed button in enabled state with title', () => {
    helper.unmount();
    const json = TestData.stageInstanceJSON().jobs;
    json[0].result = Result[Result.Failed];
    jobs = new JobsViewModel(json, new Agents());
    mount();

    expect(helper.qa('button')[0]).not.toBeDisabled();
    const expectedTitle = 'Rerun all the failed jobs from the stage. Reruning failed jobs will reschedule \'up42_job\' job(s).';
    expect(helper.qa('button')[0].title).toBe(expectedTitle);
  });

  it('should render rerun selected button in enabled state with title', () => {
    helper.unmount();
    const json = TestData.stageInstanceJSON().jobs;
    json[0].result = Result[Result.Failed];
    jobs = new JobsViewModel(json, new Agents());
    jobs.checkedState.get(json[0].name)!(true);
    mount();

    expect(helper.qa('button')[1]).not.toBeDisabled();
    const expectedTitle = 'Rerun selected jobs from the stage.';
    expect(helper.qa('button')[1].title).toBe(expectedTitle);
  });

  it('should render rerun failed button in disabled state with title when no jobs have failed', () => {
    expect(helper.qa('button')[0]).toBeDisabled();
    const expectedTitle = 'Can not rerun failed jobs. No jobs from the current stage are in failed state.';
    expect(helper.qa('button')[0].title).toBe(expectedTitle);
  });

  it('should render rerun selected button in disabled state with title when no jobs are selected for rerun', () => {
    expect(helper.qa('button')[1]).toBeDisabled();
    const expectedTitle = 'Can not rerun selected jobs. No jobs have been selected for rerun.';
    expect(helper.qa('button')[1].title).toBe(expectedTitle);
  });

  it('should render rerun failed button in disabled state with title when jobs are in progress', () => {
    helper.unmount();
    const json = TestData.stageInstanceJSON().jobs;
    json.push(TestData.stageInstanceJSON().jobs[0]);
    json[1].name = json[1].name + `2`;

    json[0].result = Result[Result.Failed];
    json[1].result = Result[Result.Unknown];
    jobs = new JobsViewModel(json, new Agents());
    mount();

    expect(helper.qa('button')[0]).toBeDisabled();
    const expectedTitle = 'Can not rerun failed jobs. Some jobs from the stage are still in progress.';
    expect(helper.qa('button')[0].title).toBe(expectedTitle);
  });

  it('should render rerun selected button in disabled state with title when jobs are in progress', () => {
    helper.unmount();
    const json = TestData.stageInstanceJSON().jobs;
    json.push(TestData.stageInstanceJSON().jobs[0]);
    json[1].name = json[1].name + `2`;

    json[0].result = Result[Result.Failed];
    json[1].result = Result[Result.Unknown];
    jobs = new JobsViewModel(json, new Agents());
    jobs.checkedState.get(json[0].name)!(true);
    mount();

    expect(helper.qa('button')[1]).toBeDisabled();
    const expectedTitle = 'Can not rerun selected jobs. Some jobs from the stage are still in progress.';
    expect(helper.qa('button')[1].title).toBe(expectedTitle);
  });

  function mount() {
    helper.mount(() => {
      return <JobCountAndRerunWidget pipelineName="up42"
                                     pipelineCounter={1}
                                     stageName="up42_stage"
                                     stageCounter={1}
                                     flashMessage={new FlashMessageModelWithTimeout()}
                                     jobsVM={Stream(jobs)} inProgressStageFromPipeline={Stream()}/>;
    });
  }
});
