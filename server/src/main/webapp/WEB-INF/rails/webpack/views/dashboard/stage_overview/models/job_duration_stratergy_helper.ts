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
import {JobStateTransitionJSON, State} from "../../../../models/agent_job_run_history";
import {StageInstance} from "./stage_instance";
import {JobJSON, Result} from "./types";

const moment = require('moment');

export interface JobDuration {
  waitTimePercentage: number;
  preparingTimePercentage: number;
  buildTimePercentage: number;
  uploadingArtifactTimePercentage: number;
  unknownTimePercentage: number;

  waitTimeForDisplay: string;
  preparingTimeForDisplay: string;
  buildTimeForDisplay: string;
  uploadingArtifactTimeForDisplay: string;
  totalTimeForDisplay: string;

  startTimeForDisplay: string;
  endTimeForDisplay: string;
}

export class JobDurationStrategyHelper {
  static readonly DEFAULT_TIME_FORMAT_FOR_HOURS: string = "HH[h] mm[m] ss[s]";
  static readonly DEFAULT_TIME_FORMAT_FOR_MINUTES: string = "mm[m] ss[s]";
  static readonly DEFAULT_TIME_FORMAT_FOR_SECONDS: string = "ss[s]";

  static getDuration(job: JobJSON, passedStageInstance: StageInstance | undefined): JobDuration {
    return this.getCompletedJobDuration(job, passedStageInstance);
  }

  static getJobDurationForDisplay(job: JobJSON): string {
    const isJobInProgress = job.result === Result[Result.Unknown];
    if (isJobInProgress) {
      return "in progress";
    }

    const end = moment.unix(this.getJobStateTime(job, "Completed"));
    const start = moment.unix(this.getJobStateTime(job, "Scheduled"));
    return this.formatTimeForDisplay(moment.utc(end.diff(start)));
  }

  static formatTimeForDisplay(time: any) {
    if (time.hours() > 0) {
      return time.format(this.DEFAULT_TIME_FORMAT_FOR_HOURS);
    }

    if (time.minutes() > 0) {
      return time.format(this.DEFAULT_TIME_FORMAT_FOR_MINUTES);
    }

    return time.format(this.DEFAULT_TIME_FORMAT_FOR_SECONDS);
  }

  private static getCompletedJobDuration(job: JobJSON, passedStageInstance: StageInstance | undefined) {
    const isJobInProgress = job.result === Result[Result.Unknown];

    const end = moment.unix(this.getJobStateTime(job, "Completed"));
    const completing = moment.unix(this.getJobStateTime(job, "Completing"));
    const building = moment.unix(this.getJobStateTime(job, "Building"));
    const preparing = moment.unix(this.getJobStateTime(job, "Preparing"));
    const start = moment.unix(this.getJobStateTime(job, "Scheduled"));

    let totalTime, totalTimeForDisplay = "unknown";
    if (isJobInProgress) {
      const lastCompletedJob = this.findJobFromLastCompletedStage(job.name, passedStageInstance);

      if (lastCompletedJob) {
        const lastJobEnd = moment.unix(this.getJobStateTime(lastCompletedJob, "Completed"));
        const lastJobStart = moment.unix(this.getJobStateTime(lastCompletedJob, "Scheduled"));
        totalTime = moment.utc(lastJobEnd.diff(lastJobStart));
      } else {
        totalTime = moment.utc(end.diff(start));
      }
    } else {
      totalTime = moment.utc(end.diff(start));
      totalTimeForDisplay = this.formatTimeForDisplay(totalTime);
    }

    const waitTime = moment.utc(preparing.diff(start));
    const waitTimeForDisplay = this.formatTimeForDisplay(waitTime);

    const preparingTime = moment.utc(building.diff(preparing));
    const preparingTimeForDisplay = this.formatTimeForDisplay(preparingTime);

    const buildTime = moment.utc(completing.diff(building));
    const buildTimeForDisplay = this.formatTimeForDisplay(buildTime);

    const uploadingArtifactTime = moment.utc(end.diff(completing));
    const uploadingArtifactTimeForDisplay = this.formatTimeForDisplay(buildTime);

    const waitTimePercentage = this.calculatePercentage(totalTime, waitTime);
    const preparingTimePercentage = this.calculatePercentage(totalTime, preparingTime);
    const buildTimePercentage = this.calculatePercentage(totalTime, buildTime);
    // Math.round will round up values round(1.3) + round(1.3) + round(1.4) = 3
    // where the result should've been 4. Add 1 to the uploading artifact percentage,
    // if the view overflowed, overflow: hidden will remove the extra 1 percent
    const uploadingArtifactTimePercentage = this.calculatePercentage(totalTime, uploadingArtifactTime) + (isJobInProgress ? 0 : 1);

    const startTimeForDisplay = timeFormatter.format(start);
    const endTimeForDisplay = isJobInProgress ? "unknown" : timeFormatter.format(end);

    return {
      unknownTimePercentage: 0,
      waitTimePercentage,
      preparingTimePercentage,
      buildTimePercentage,
      uploadingArtifactTimePercentage,

      waitTimeForDisplay,
      preparingTimeForDisplay,
      buildTimeForDisplay,
      uploadingArtifactTimeForDisplay,
      totalTimeForDisplay,

      startTimeForDisplay,
      endTimeForDisplay
    };
  }

  private static getJobStateTime(job: JobJSON, state: State): number {
    let state: JobStateTransitionJSON = job.job_state_transitions.find(t => t.state === state);
    if (!state) {
      state = {
        state,
        state_change_time: moment().valueOf()
      } as JobStateTransitionJSON;
    }

    return state.state_change_time / 1000;
  }

  private static calculatePercentage(total: number, part: number) {
    return Math.round((part / total) * 100);
  }

  private static findJobFromLastCompletedStage(jobName: string, passedStageInstance: StageInstance | undefined): JobJSON | undefined {
    return passedStageInstance?.jobs().find(j => j.name === jobName);
  }
}
