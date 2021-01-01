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

import {timeFormatter} from "../../../../helpers/time_formatter";
import {JobStateTransitionJSON, State} from "../../../../models/agent_job_run_history";
import {StageInstance} from "./stage_instance";
import {JobJSON, Result} from "./types";

const moment = require('moment');

export interface JobDuration {
  isJobInProgress: boolean;
  waitTimePercentage: number;
  preparingTimePercentage: number;
  buildTimePercentage: number;
  uploadingArtifactTimePercentage: number;
  unknownTimePercentage: number;
  totalTime: any;

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

  static getJobDuration(job: JobJSON): any {
    const isJobInProgress = this.isJobInProgress(job);
    if (isJobInProgress) {
      return undefined;
    }

    const end = moment.unix(this.getJobStateTime(job, "Completed"));
    const start = moment.unix(this.getJobStateTime(job, "Scheduled"));
    return moment.utc(end.diff(start));
  }

  static getJobDurationForDisplay(job: JobJSON): string {
    const duration = JobDurationStrategyHelper.getJobDuration(job);
    if (duration === undefined) {
      return "in progress";
    }

    return this.formatTimeForDisplay(duration);
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
    const isJobInProgress = this.isJobInProgress(job);

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

    const waitTime = this.calculateDifference(preparing, start);
    const waitTimeForDisplay = this.formatTimeForDisplay(waitTime);

    const preparingTime = this.calculateDifference(building, preparing);
    const preparingTimeForDisplay = this.formatTimeForDisplay(preparingTime);

    const buildTime = this.calculateDifference(completing, building);
    const buildTimeForDisplay = this.formatTimeForDisplay(buildTime);

    const uploadingArtifactTime = this.calculateDifference(end, completing);
    const uploadingArtifactTimeForDisplay = this.formatTimeForDisplay(uploadingArtifactTime);

    let waitTimePercentage = this.calculatePercentage(totalTime, waitTime);
    const preparingTimePercentage = this.calculatePercentage(totalTime, preparingTime);
    const buildTimePercentage = this.calculatePercentage(totalTime, buildTime);
    const uploadingArtifactTimePercentage = this.calculatePercentage(totalTime, uploadingArtifactTime);

    // math.round may result into total percentage being 99, example 33.3 + 33.3 + 33.4 is 100,
    // but Math.round(33.3) + Math.round(33.3) + Math.round(33.4) is 99 :/
    // showing 99 percentage on the bar sometimes causes an empty space on the bar for a completed job
    if (waitTimePercentage + preparingTimePercentage + buildTimePercentage + uploadingArtifactTimePercentage !== 100) {
      waitTimePercentage += 1;
    }

    const startTimeForDisplay = timeFormatter.format(start);
    const endTimeForDisplay = isJobInProgress ? "unknown" : timeFormatter.format(end);

    return {
      isJobInProgress,
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
      endTimeForDisplay,
      totalTime
    };
  }

  private static calculateDifference(end: any, start: any) {
    return end.isBefore(start) ? moment.utc(0) : moment.utc(end.diff(start));
  }

  private static getJobStateTime(job: JobJSON, state: State): number {
    const isJobCompleted = job.result !== Result[Result.Unknown];

    let jobState: JobStateTransitionJSON = job.job_state_transitions.find(t => t.state === state)!;
    if (!jobState) {
      jobState = {
        state,
        state_change_time: isJobCompleted ? job.job_state_transitions[job.job_state_transitions.length - 1].state_change_time : moment().valueOf()
      } as JobStateTransitionJSON;
    }

    return +jobState.state_change_time / 1000;
  }

  private static calculatePercentage(total: number, part: number) {
    return Math.round((part / total) * 100);
  }

  private static findJobFromLastCompletedStage(jobName: string, passedStageInstance: StageInstance | undefined): JobJSON | undefined {
    return passedStageInstance?.jobs().find(j => j.name === jobName);
  }

  private static isJobInProgress(job: JobJSON): boolean {
    return !(job.job_state_transitions.find(t => t.state === "Completed"));
  }
}
