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

import m from "mithril";
import Stream from "mithril/stream";
import {TestHelper} from "../../../pages/spec/test_helper";
import styles from "../index.scss";
import {JobProgressBarWidget} from "../job_progress_bar_widget";
import {JobDurationStrategyHelper} from "../models/job_duration_stratergy_helper";
import {JobJSON} from "../models/types";
import {TestData} from "./test_data";

describe("Job Progress Bar Widget", () => {
  const helper = new TestHelper();

  let json: JobJSON;
  beforeEach(() => {
    json = TestData.stageInstanceJSON().jobs[0];
    mount(json);
  });
  afterEach(helper.unmount.bind(helper));

  it("should render job progress bar", () => {
    expect(helper.byTestId('progress-bar-container-div')).toBeInDOM();
    const children = helper.byTestId('progress-bar-container-div').children;
    expect(children).toHaveLength(4);
  });

  it("should render job progress bar waiting portion", () => {
    const children = helper.byTestId('progress-bar-container-div').children;
    expect((children[0] as any).style.width).toBe("30%");
    expect(children[0].classList[0]).toBe(styles.waiting);
  });

  it("should render job progress bar preparing portion", () => {
    const children = helper.byTestId('progress-bar-container-div').children;
    expect((children[1] as any).style.width).toBe("20%");
    expect(children[1].classList[0]).toBe(styles.preparing);
  });

  it("should render job progress bar building portion", () => {
    const children = helper.byTestId('progress-bar-container-div').children;
    expect((children[2] as any).style.width).toBe("30%");
    expect(children[2].classList[0]).toBe(styles.building);
  });

  it("should render job progress bar uploading artifacts portion", () => {
    const children = helper.byTestId('progress-bar-container-div').children;
    expect((children[3] as any).style.width).toBe("20%");
    expect(children[3].classList[0]).toBe(styles.uploadingArtifacts);
  });

  it("should render job progress bar unknown portion", () => {
    const children = helper.byTestId('progress-bar-container-div').children;
    expect((children[4] as any)).not.toBeInDOM();
  });

  describe("Tooltip", () => {
    it("should not render progress bar tooltip by default", () => {
      const tooltip = helper.byTestId('progress-bar-tooltip');

      expect(tooltip.style.visibility).toBe('');
    });

    it("should render progress bar tooltip on mouse over", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');

      expect(tooltip.style.visibility).toBe('');

      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip.style.visibility).toBe('visible');
    });

    it("should hide progress bar tooltip on mouse out", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');

      expect(tooltip.style.visibility).toBe('');

      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip.style.visibility).toBe('visible');

      progressBar.dispatchEvent(new MouseEvent('mouseout'));

      expect(tooltip.style.visibility).toBe('hidden');
    });

    it("should render waiting time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Waiting for an Agent");
      expect(tooltip).toContainText("03m 00s");
    });

    it("should render preparing time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Checking out Materials");
      expect(tooltip).toContainText("02m 00s");
    });

    it("should render building time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Building Job");
      expect(tooltip).toContainText("03m 00s");
    });

    it("should render uploading artifacts time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Uploading Artifacts");
      expect(tooltip).toContainText("02m 00s");
    });

    it("should render total time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Total Time");
      expect(tooltip).toContainText("10m 00s");
    });

    it("should render scheduled at time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Scheduled At");
      expect(tooltip).toContainText(TestData.unixTime(json.job_state_transitions[0].state_change_time as number / 1000));
    });

    it("should render completed at time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Completed At");
      expect(tooltip).toContainText(TestData.unixTime((json.job_state_transitions[5].state_change_time as number) / 1000));
    });

    it('should not render state as unknown when it is completed in 0 seconds', () => {
      helper.unmount();
      mount(TestData.jobWithNoPreparingTime());

      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Building");
      expect(tooltip).toContainText("00s");

      const transitionCircles = helper.qa('[data-test-id="transition-circle"]');
      expect(transitionCircles[1]).toHaveClass(styles.preparing);
      expect(transitionCircles[1]).toHaveClass(styles.completed);
    });

    it('should render transition circle as completed when the state is completed in 0 seconds', () => {
      helper.unmount();
      mount(TestData.jobWithNoPreparingTime());

      const progressBar = helper.byTestId('progress-bar-container-div');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      const transitionCircles = helper.qa('[data-test-id="transition-circle"]');
      expect(transitionCircles[1]).toHaveClass(styles.preparing);
      expect(transitionCircles[1]).toHaveClass(styles.completed);
    });

    it('should render scheduled transition circle as in progress when preparing takes 0 seconds to complete', () => {
      helper.unmount();
      mount(TestData.jobWithNoPreparingTime());

      const progressBar = helper.byTestId('progress-bar-container-div');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      const transitionCircles = helper.qa('[data-test-id="transition-circle"]');
      expect(transitionCircles[0]).toHaveClass(styles.scheduled);
      expect(transitionCircles[0]).toHaveClass(styles.completed);
    });
  });

  function mount(job: JobJSON) {
    helper.mount(() => {
      return <JobProgressBarWidget job={job}
                                   jobDuration={JobDurationStrategyHelper.getDuration(job, undefined)}
                                   longestTotalTime={1000000000}
                                   lastPassedStageInstance={Stream()}/>;
    });
  }

});
