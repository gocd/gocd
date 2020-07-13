import {StageInstanceJSON} from "../models/stage_instance";

export class TestData {
  static stageInstanceJSON(): StageInstanceJSON {
    return {
      name:                    "up42_stage",
      counter:                 1,
      approval_type:           "success",
      approved_by:             "admin",
      result:                  "Cancelled",
      cancelled_by:            "admin",
      rerun_of_counter:        null,
      fetch_materials:         true,
      clean_working_directory: false,
      artifacts_deleted:       false,
      pipeline_name:           "up42",
      pipeline_counter:        2,
      jobs:                    [
        {
          name:                  "up42_job",
          state:                 "Completed",
          result:                "Cancelled",
          scheduled_date:        1594278364183,
          rerun:                 false,
          original_job_id:       null,
          agent_uuid:            null,
          pipeline_name:         null,
          pipeline_counter:      null,
          stage_name:            null,
          stage_counter:         null,
          job_state_transitions: [
            {
              state:             "Scheduled",
              state_change_time: 1594278364183
            },
            {
              state:             "Completed",
              state_change_time: 1594278568644
            }
          ]
        }
      ]
    } as StageInstanceJSON;
  }
}
