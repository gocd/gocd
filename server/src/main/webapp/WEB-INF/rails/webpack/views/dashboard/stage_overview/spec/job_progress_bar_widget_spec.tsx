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
import styles from "../index.scss";
import {JobProgressBarWidget} from "../job_progress_bar_widget";
import {JobJSON} from "../models/types";
import {TestData} from "./test_data";

describe("Job Progress Bar Widget", () => {
  const helper = new TestHelper();

  beforeEach(() => {
    mount(TestData.stageInstanceJSON().jobs[0]);
  });
  afterEach(helper.unmount.bind(helper));

  it("should render job progress bar", () => {
    expect(helper.byTestId('progress-bar-container-div')).toBeInDOM();
    const children = helper.byTestId('progress-bar-container-div').children;
    expect(children).toHaveLength(5);
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
    expect((children[4] as any).style.width).toBe("0%");
    expect(children[4].classList[0]).toBe(styles.unknown);
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

      expect(tooltip).toContainText("Waiting");
      expect(tooltip).toContainText("03m 00s");
    });

    it("should render preparing time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Preparing");
      expect(tooltip).toContainText("02m 00s");
    });

    it("should render building time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Building");
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
      expect(tooltip).toContainText("27 Jul, 2020 at 11:51:04 Local Time");
    });

    it("should render completed at time", () => {
      const progressBar = helper.byTestId('progress-bar-container-div');
      const tooltip = helper.byTestId('progress-bar-tooltip');
      progressBar.dispatchEvent(new MouseEvent('mouseover'));

      expect(tooltip).toContainText("Completed At");
      expect(tooltip).toContainText("27 Jul, 2020 at 12:01:04 Local Time");
    });
  });

  function mount(job: JobJSON) {
    helper.mount(() => {
      return <JobProgressBarWidget job={job} lastPassedStageInstance={Stream()}/>;
    });
  }

});
