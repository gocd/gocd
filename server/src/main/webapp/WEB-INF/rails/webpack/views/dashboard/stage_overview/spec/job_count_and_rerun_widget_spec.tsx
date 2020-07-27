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
import {JobCountAndRerunWidget} from "../job_count_and_rerun_widget";
import {JobsViewModel} from "../models/jobs_view_model";
import {TestData} from "./test_data";

describe("Job Count And Rerun Widget", () => {
  const helper = new TestHelper();

  beforeEach(mount);
  afterEach(helper.unmount.bind(helper));

  it("should render job count container", () => {
    expect(helper.byTestId('job-cont-container')).toBeInDOM();
  });

  it("should render building job count", () => {
    expect(helper.byTestId('in-progress-jobs-container')).toBeInDOM();
    expect(helper.byTestId('in-progress-jobs-container')).toContainText('Building');
    expect(helper.byTestId('in-progress-jobs-container')).toContainText(0);
  });

  it("should render passed job count", () => {
    expect(helper.byTestId('passed-jobs-container')).toBeInDOM();
    expect(helper.byTestId('passed-jobs-container')).toContainText('Passed');
    expect(helper.byTestId('passed-jobs-container')).toContainText(1);
  });

  it("should render failed job count", () => {
    expect(helper.byTestId('failed-jobs-container')).toBeInDOM();
    expect(helper.byTestId('failed-jobs-container')).toContainText('Failed');
    expect(helper.byTestId('failed-jobs-container')).toContainText(0);
  });

  it('should render job rerun buttons', () => {
    expect(helper.byTestId('job-rerun-container')).toBeInDOM();
    expect(helper.qa('button')).toHaveLength(2);
    expect(helper.qa('button')[0]).toContainText('Rerun Failed');
    expect(helper.qa('button')[1]).toContainText('Rerun Selected');
  });

  function mount() {
    helper.mount(() => {
      return <JobCountAndRerunWidget jobsVM={Stream(new JobsViewModel(TestData.stageInstanceJSON().jobs))}/>
    })
  }
});
