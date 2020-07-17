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

import {timeFormatter} from "../../../../helpers/time_formatter";
import {JobJSON, Result} from "./types";
import {StageInstance} from "./stage_instance";
import {State} from "../../../../models/agent_job_run_history";

const moment = require('moment');

export interface JobDuration {
  waitTimePercentage: number;
  buildTimePercentage: number;
  unknownTimePercentage: number;

  waitTimeForDisplay: string;
  buildTimeForDisplay: string;
  totalTimeForDisplay: string;

  startTimeForDisplay: string;
  endTimeForDisplay: string;
}

export class JobDurationStrategyHelper {
  static readonly DEFAULT_TIME_FORMAT_FOR_HOURS: string = "HH[h] mm[m] ss[s]";
  static readonly DEFAULT_TIME_FORMAT_FOR_MINUTES: string = "mm[m] ss[s]";
  static readonly DEFAULT_TIME_FORMAT_FOR_SECONDS: string = "ss[s]";

  static getDuration(job: JobJSON, passedStageInstance: StageInstance | undefined): JobDuration {
    const isJobInProgress = job.result === Result[Result.Unknown];
    if (isJobInProgress) {
      return this.getInProgressJobDuration(job, passedStageInstance)
    }

    return this.getCompletedJobDuration(job);
  }

  private static getInProgressJobDuration(job: JobJSON, passedStageInstance: StageInstance | undefined) {
    return {} as JobDuration;
  }

  private static getCompletedJobDuration(job: JobJSON) {
    const end = moment.unix(this.getJobStateTime(job, "Completed"));
    const preparing = moment.unix(this.getJobStateTime(job, "Preparing"));
    const start = moment.unix(this.getJobStateTime(job, "Scheduled"));

    const totalTime = moment.utc(end.diff(start));
    const totalTimeForDisplay = this.formatTimeForDisplay(totalTime);

    const waitTime = moment.utc(preparing.diff(start));
    const waitTimeForDisplay = this.formatTimeForDisplay(waitTime);

    const buildTime = moment.utc(end.diff(preparing));
    const buildTimeForDisplay = this.formatTimeForDisplay(buildTime);

    const waitTimePercentage = this.calculatePercentage(totalTime, waitTime);
    const buildTimePercentage = this.calculatePercentage(totalTime, buildTime);

    const startTimeForDisplay = timeFormatter.format(start);
    const endTimeForDisplay = timeFormatter.format(end);

    return {
      unknownTimePercentage: 0,
      waitTimePercentage,
      buildTimePercentage,

      waitTimeForDisplay,
      buildTimeForDisplay,
      totalTimeForDisplay,

      startTimeForDisplay,
      endTimeForDisplay
    };
  }

  private static formatTimeForDisplay(time: any) {
    if (time.hours() > 0) {
      time.format(this.DEFAULT_TIME_FORMAT_FOR_HOURS)
    }

    if (time.minutes() > 0) {
      time.format(this.DEFAULT_TIME_FORMAT_FOR_MINUTES)
    }

    return time.format(this.DEFAULT_TIME_FORMAT_FOR_SECONDS)
  }

  private static getJobStateTime(job: JobJSON, state: State): number {
    return job.job_state_transitions.find(t => t.state === state).state_change_time / 1000;
  }

  private static calculatePercentage(total: number, part: number) {
    return Math.round((part / total) * 100);
  }
}
