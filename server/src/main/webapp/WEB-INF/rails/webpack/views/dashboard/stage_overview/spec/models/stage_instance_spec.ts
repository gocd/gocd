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


import {Result, StageInstance, StageInstanceJSON} from "../../models/stage_instance";

describe('Stage Instance', function () {

  let stageInstanceJson: StageInstanceJSON;

  beforeEach(function () {
    stageInstanceJson = {
      "name":                    "up42_stage",
      "counter":                 1,
      "approval_type":           "success",
      "approved_by":             "admin",
      "result":                  "Cancelled",
      "cancelled_by":            "admin",
      "rerun_of_counter":        null,
      "fetch_materials":         true,
      "clean_working_directory": false,
      "artifacts_deleted":       false,
      "pipeline_name":           "up42",
      "pipeline_counter":        2,
      "jobs":                    [{
        "name":                  "up42_job",
        "state":                 "Completed",
        "result":                "Cancelled",
        "scheduled_date":        1594278364183,
        "rerun":                 false,
        "original_job_id":       null,
        "agent_uuid":            null,
        "pipeline_name":         null,
        "pipeline_counter":      null,
        "stage_name":            null,
        "stage_counter":         null,
        "job_state_transitions": [{
          "state":             "Scheduled",
          "state_change_time": 1594278364183
        }, {
          "state":             "Completed",
          "state_change_time": 1594278568644
        }]
      }]
    } as StageInstanceJSON;
  });

  it('should deserialize from json', () => {
    const _json = StageInstance.fromJSON(stageInstanceJson);
  });

  it('should provide triggered by information', () => {
    const json = StageInstance.fromJSON(stageInstanceJson);
    expect(json.triggeredBy()).toBe('Triggered by admin');
  });

  it('should provide triggered on information', () => {
    const json = StageInstance.fromJSON(stageInstanceJson);
    expect(json.triggeredOn()).toBe('on 09 Jul, 2020 at 12:36:04 Local Time');
  });

  describe("Stage Duration", () => {
    it("should provide stage duration as in progress when some of the jobs from the stage are still in progress", () => {
      stageInstanceJson.result = Result[Result.Unknown];
      const json = StageInstance.fromJSON(stageInstanceJson);
      expect(json.stageDuration()).toBe('in progress');
    });

    it("should provide stage duration", () => {
      // modify the json
      const json = StageInstance.fromJSON(stageInstanceJson);
      expect(json.stageDuration()).toBe('00:03:24');
    });
  });
});
