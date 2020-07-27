import {StageInstanceJSON} from "../models/types";

export class TestData {
  static stageInstanceJSON(): StageInstanceJSON {
    return {
      name:                    "up42_stage",
      counter:                 1,
      approval_type:           "success",
      approved_by:             "admin",
      result:                  "Passed",
      rerun_of_counter:        null,
      fetch_materials:         true,
      clean_working_directory: false,
      artifacts_deleted:       false,
      pipeline_name:           "up42",
      pipeline_counter:        1,
      jobs:                    [{
        name:                  "up42_job",
        state:                 "Completed",
        result:                "Passed",
        scheduled_date:        1595825414079,
        rerun:                 false,
        original_job_id:       null,
        agent_uuid:            "638f3328-0e3e-474e-9355-35d5383cd704",
        pipeline_name:         null,
        pipeline_counter:      null,
        stage_name:            null,
        stage_counter:         null,
        job_state_transitions: [{
          state:             "Scheduled",
          state_change_time: 1595825414079
        }, {
          state:             "Assigned",
          state_change_time: 1595825417418
        }, {
          state:             "Preparing",
          state_change_time: 1595825427423
        }, {
          state:             "Building",
          state_change_time: 1595825430033
        }, {
          state:             "Completing",
          state_change_time: 1595825440145
        }, {
          state:             "Completed",
          state_change_time: 1595825440172
        }]
      }]
    } as StageInstanceJSON;
  }
}
