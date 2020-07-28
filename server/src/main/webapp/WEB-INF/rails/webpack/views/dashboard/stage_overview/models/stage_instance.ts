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

import moment from "moment";
import {JobDurationStrategyHelper} from "./job_duration_stratergy_helper";
import {JobJSON, Result, StageInstanceJSON} from "./types";

export class StageInstance {
  private json: StageInstanceJSON;

  constructor(json: StageInstanceJSON) {
    this.json = json;
  }

  static fromJSON(json: StageInstanceJSON) {
    return new StageInstance(json);
  }

  jobs(): JobJSON[] {
    return this.json.jobs;
  }

  triggeredBy(): string {
    return this.json.approved_by;
  }

  triggeredOn(): string {
    const LOCAL_TIME_FORMAT = "DD MMM, YYYY [at] HH:mm:ss";
    return `${moment.unix(this.stageScheduledTime()).format(LOCAL_TIME_FORMAT)}`;
  }

  stageDuration(): string {
    if (this.isStageInProgress()) {
      return `in progress`;
    }

    const highestJobTime = this.json.jobs.reduce((first: number, next: JobJSON) => {
      const completed = next.job_state_transitions.find(t => t.state === "Completed");
      return first < completed.state_change_time ? completed.state_change_time : first;
    }, 0);

    const end = moment.unix(highestJobTime / 1000);
    const start = moment.unix(this.stageScheduledTime());

    return JobDurationStrategyHelper.formatTimeForDisplay(moment.utc(end.diff(start)));
  }

  isCancelled(): boolean {
    return this.json.result === Result[Result.Cancelled];
  }

  cancelledBy(): string | undefined {
    return this.json.cancelled_by;
  }

  cancelledOn(): string {
    if (!this.isCancelled()) {
      throw new Error(`Stage is not cancelled.`);
    }

    const LOCAL_TIME_FORMAT = "DD MMM, YYYY [at] HH:mm:ss";
    const completedTime = this.json.jobs[0].job_state_transitions.find(t => t.state === "Completed").state_change_time;
    return `${moment.unix(completedTime / 1000).format(LOCAL_TIME_FORMAT)}`;
  }

  private stageScheduledTime(): number {
    return this.json.jobs[0].scheduled_date / 1000;
  }

  private isStageInProgress(): boolean {
    //stage result is marked as failed even before all the jobs are completed.
    // hence check the status of all the jobs instead of the top level stage.
    return this.jobs().some(job => job.result === Result[Result.Unknown]);
  }
}
