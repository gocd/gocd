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

import {timeFormatter} from "../../../../../helpers/time_formatter";
import {JobDurationStrategyHelper} from "../../models/job_duration_stratergy_helper";
import {StageInstance} from "../../models/stage_instance";
import {Result} from "../../models/types";
import {TestData} from "../test_data";

const moment = require('moment');

describe("Job Duration Strategy Helper", () => {
  describe("Without Last Passed Stage Instance", () => {
    it("should return job duration strategy when job is scheduled", () => {
      const job = TestData.stageInstanceJSON().jobs[0];
      const currentTime = new Date();
      const scheduledTime = currentTime.getTime() - (1000 * 60 * 5);

      job.result = Result[Result.Unknown];
      job.job_state_transitions = [
        {
          state:             "Scheduled",
          state_change_time: scheduledTime // scheduled 5 minute ago
        }
      ];

      expect(JobDurationStrategyHelper.getDuration(job, undefined)).toEqual({
        isJobInProgress:                 true,
        waitTimePercentage:              100,
        preparingTimePercentage:         0,
        buildTimePercentage:             0,
        uploadingArtifactTimePercentage: 0,
        unknownTimePercentage:           0,

        waitTimeForDisplay:              "05m 00s",
        preparingTimeForDisplay:         "00s",
        buildTimeForDisplay:             "00s",
        uploadingArtifactTimeForDisplay: "00s",

        totalTimeForDisplay: "unknown",

        endTimeForDisplay:   "unknown",
        startTimeForDisplay: timeFormatter.format(moment.unix(scheduledTime / 1000))
      });
    });

    it("should return job duration strategy when job is assigned", () => {
      const job = TestData.stageInstanceJSON().jobs[0];
      const currentTime = new Date();
      const scheduledTime = currentTime.getTime() - (1000 * 60 * 10); // 10 mins ago
      const assignedTime = currentTime.getTime() - (1000 * 60 * 5); // 8 mins ago

      job.result = Result[Result.Unknown];
      job.job_state_transitions = [
        {
          state:             "Scheduled",
          state_change_time: scheduledTime
        },
        {
          state:             "Assigned",
          state_change_time: assignedTime
        }
      ];

      expect(JobDurationStrategyHelper.getDuration(job, undefined)).toEqual({
        isJobInProgress:                 true,
        waitTimePercentage:              100,
        preparingTimePercentage:         0,
        buildTimePercentage:             0,
        uploadingArtifactTimePercentage: 0,
        unknownTimePercentage:           0,

        waitTimeForDisplay:              "10m 00s",
        preparingTimeForDisplay:         "00s",
        buildTimeForDisplay:             "00s",
        uploadingArtifactTimeForDisplay: "00s",

        totalTimeForDisplay: "unknown",

        endTimeForDisplay:   "unknown",
        startTimeForDisplay: timeFormatter.format(moment.unix(scheduledTime / 1000))
      });
    });

    it("should return job duration strategy when job is preparing", () => {
      const job = TestData.stageInstanceJSON().jobs[0];
      const currentTime = new Date();
      const scheduledTime = currentTime.getTime() - (1000 * 60 * 10); // 10 mins ago
      const assignedTime = currentTime.getTime() - (1000 * 60 * 8); // 8 mins ago
      const preparingTime = currentTime.getTime() - (1000 * 60 * 7); // 7 mins ago

      job.result = Result[Result.Unknown];
      job.job_state_transitions = [
        {
          state:             "Scheduled",
          state_change_time: scheduledTime
        },
        {
          state:             "Assigned",
          state_change_time: assignedTime
        },
        {
          state:             "Preparing",
          state_change_time: preparingTime
        }
      ];

      expect(JobDurationStrategyHelper.getDuration(job, undefined)).toEqual({
        isJobInProgress:                 true,
        waitTimePercentage:              30,
        preparingTimePercentage:         70,
        buildTimePercentage:             0,
        uploadingArtifactTimePercentage: 0,
        unknownTimePercentage:           0,

        waitTimeForDisplay:              "03m 00s",
        preparingTimeForDisplay:         "07m 00s",
        buildTimeForDisplay:             "00s",
        uploadingArtifactTimeForDisplay: "00s",

        totalTimeForDisplay: "unknown",

        endTimeForDisplay:   "unknown",
        startTimeForDisplay: timeFormatter.format(moment.unix(scheduledTime / 1000))
      });
    });

    it("should return job duration strategy when job is building", () => {
      const job = TestData.stageInstanceJSON().jobs[0];
      const currentTime = new Date();
      const scheduledTime = currentTime.getTime() - (1000 * 60 * 10); // 10 mins ago
      const assignedTime = currentTime.getTime() - (1000 * 60 * 8); // 8 mins ago
      const preparingTime = currentTime.getTime() - (1000 * 60 * 7); // 7 mins ago
      const buildingTime = currentTime.getTime() - (1000 * 60 * 5); // 7 mins ago

      job.result = Result[Result.Unknown];
      job.job_state_transitions = [
        {
          state:             "Scheduled",
          state_change_time: scheduledTime
        },
        {
          state:             "Assigned",
          state_change_time: assignedTime
        },
        {
          state:             "Preparing",
          state_change_time: preparingTime
        },
        {
          state:             "Building",
          state_change_time: buildingTime
        }
      ];

      expect(JobDurationStrategyHelper.getDuration(job, undefined)).toEqual({
        isJobInProgress:                 true,
        waitTimePercentage:              30,
        preparingTimePercentage:         20,
        buildTimePercentage:             50,
        uploadingArtifactTimePercentage: 0,
        unknownTimePercentage:           0,

        waitTimeForDisplay:              "03m 00s",
        preparingTimeForDisplay:         "02m 00s",
        buildTimeForDisplay:             "05m 00s",
        uploadingArtifactTimeForDisplay: "00s",

        totalTimeForDisplay: "unknown",

        endTimeForDisplay:   "unknown",
        startTimeForDisplay: timeFormatter.format(moment.unix(scheduledTime / 1000))
      });
    });

    it("should return job duration strategy when job is uploading artifacts", () => {
      const job = TestData.stageInstanceJSON().jobs[0];
      const currentTime = new Date();
      const scheduledTime = currentTime.getTime() - (1000 * 60 * 10); // 10 mins ago
      const assignedTime = currentTime.getTime() - (1000 * 60 * 8); // 8 mins ago
      const preparingTime = currentTime.getTime() - (1000 * 60 * 7); // 7 mins ago
      const buildingTime = currentTime.getTime() - (1000 * 60 * 5); // 7 mins ago
      const uploadingArtifactsTime = currentTime.getTime() - (1000 * 60 * 2); // 7 mins ago

      job.result = Result[Result.Unknown];
      job.job_state_transitions = [
        {
          state:             "Scheduled",
          state_change_time: scheduledTime
        },
        {
          state:             "Assigned",
          state_change_time: assignedTime
        },
        {
          state:             "Preparing",
          state_change_time: preparingTime
        },
        {
          state:             "Building",
          state_change_time: buildingTime
        },
        {
          state:             "Completing",
          state_change_time: uploadingArtifactsTime
        }
      ];

      expect(JobDurationStrategyHelper.getDuration(job, undefined)).toEqual({
        isJobInProgress:                 true,
        waitTimePercentage:              30,
        preparingTimePercentage:         20,
        buildTimePercentage:             30,
        uploadingArtifactTimePercentage: 20,
        unknownTimePercentage:           0,

        waitTimeForDisplay:              "03m 00s",
        preparingTimeForDisplay:         "02m 00s",
        buildTimeForDisplay:             "03m 00s",
        uploadingArtifactTimeForDisplay: "02m 00s",

        totalTimeForDisplay: "unknown",

        endTimeForDisplay:   "unknown",
        startTimeForDisplay: timeFormatter.format(moment.unix(scheduledTime / 1000))
      });
    });

    it("should return job duration strategy when job is completed", () => {
      const job = TestData.stageInstanceJSON().jobs[0];
      const currentTime = new Date();
      const scheduledTime = currentTime.getTime() - (1000 * 60 * 10); // 10 mins ago
      const assignedTime = currentTime.getTime() - (1000 * 60 * 8); // 8 mins ago
      const preparingTime = currentTime.getTime() - (1000 * 60 * 7); // 7 mins ago
      const buildingTime = currentTime.getTime() - (1000 * 60 * 5); // 7 mins ago
      const uploadingArtifactsTime = currentTime.getTime() - (1000 * 60 * 2); // 7 mins ago

      job.result = Result[Result.Passed];
      job.job_state_transitions = [
        {
          state:             "Scheduled",
          state_change_time: scheduledTime
        },
        {
          state:             "Assigned",
          state_change_time: assignedTime
        },
        {
          state:             "Preparing",
          state_change_time: preparingTime
        },
        {
          state:             "Building",
          state_change_time: buildingTime
        },
        {
          state:             "Completing",
          state_change_time: uploadingArtifactsTime
        },
        {
          state:             "Completed",
          state_change_time: currentTime.getTime()
        }
      ];

      expect(JobDurationStrategyHelper.getDuration(job, undefined)).toEqual({
        isJobInProgress:                 false,
        waitTimePercentage:              30,
        preparingTimePercentage:         20,
        buildTimePercentage:             30,
        uploadingArtifactTimePercentage: 20,
        unknownTimePercentage:           0,

        waitTimeForDisplay:              "03m 00s",
        preparingTimeForDisplay:         "02m 00s",
        buildTimeForDisplay:             "03m 00s",
        uploadingArtifactTimeForDisplay: "02m 00s",

        totalTimeForDisplay: "10m 00s",

        endTimeForDisplay:   timeFormatter.format(moment.unix(currentTime.getTime() / 1000)),
        startTimeForDisplay: timeFormatter.format(moment.unix(scheduledTime / 1000))
      });
    });
  });

  describe("With Last Passed Stage Instance", () => {
    it("should return job duration strategy when job is scheduled", () => {
      const job = TestData.stageInstanceJSON().jobs[0];
      const currentTime = new Date();
      const scheduledTime = currentTime.getTime() - (1000 * 60 * 5);

      job.result = Result[Result.Unknown];
      job.job_state_transitions = [
        {
          state:             "Scheduled",
          state_change_time: scheduledTime // scheduled 5 minute ago
        }
      ];

      expect(JobDurationStrategyHelper.getDuration(job, StageInstance.fromJSON(TestData.stageInstanceJSON()))).toEqual({
        isJobInProgress:                 true,
        waitTimePercentage:              51,
        preparingTimePercentage:         0,
        buildTimePercentage:             0,
        uploadingArtifactTimePercentage: 0,
        unknownTimePercentage:           0,

        waitTimeForDisplay:              "05m 00s",
        preparingTimeForDisplay:         "00s",
        buildTimeForDisplay:             "00s",
        uploadingArtifactTimeForDisplay: "00s",

        totalTimeForDisplay: "unknown",

        endTimeForDisplay:   "unknown",
        startTimeForDisplay: timeFormatter.format(moment.unix(scheduledTime / 1000))
      });
    });
  });
});
