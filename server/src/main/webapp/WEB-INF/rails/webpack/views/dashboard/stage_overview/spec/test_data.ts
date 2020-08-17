import moment from "moment";
import {JobJSON, StageInstanceJSON} from "../models/types";

export class TestData {
  static unixTime(val: number): any {
    const LOCAL_TIME_FORMAT = "DD MMM, YYYY [at] HH:mm:ss";
    return `${moment.unix(val).format(LOCAL_TIME_FORMAT)}`;
  }

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
      jobs:                    [
        {
          name:                  "up42_job",
          state:                 "Building",
          result:                "Passed",
          scheduled_date:        1595825414079,
          rerun:                 false,
          original_job_id:       null,
          agent_uuid:            "638f3328-0e3e-474e-9355-35d5383cd704",
          // @ts-ignore
          pipeline_name:         null,
          pipeline_counter:      null,
          stage_name:            null,
          stage_counter:         null,
          job_state_transitions: [
            {
              state:             "Scheduled",
              state_change_time: 1595830864796
            },
            {
              state:             "Assigned",
              state_change_time: 1595830984796
            },
            {
              state:             "Preparing",
              state_change_time: 1595831044796
            },
            {
              state:             "Building",
              state_change_time: 1595831164796
            },
            {
              state:             "Completing",
              state_change_time: 1595831344796
            },
            {
              state:             "Completed",
              state_change_time: 1595831464796
            }
          ]
        }
      ]
    } as StageInstanceJSON;
  }

  static jobsJSON(): JobJSON[] {
    return [
      {
        name:                  "waiting-job",
        state:                 "Scheduled",
        result:                "Unknown",
        scheduled_date:        1597384671236,
        rerun:                 false,
        original_job_id:       null,
        agent_uuid:            null,
        // @ts-ignore
        pipeline_name:         null,
        pipeline_counter:      null,
        stage_name:            null,
        stage_counter:         null,
        job_state_transitions: [{
          state:             "Scheduled",
          state_change_time: 1597384671236
        }]
      },
      {
      name:                  "running-job",
      state:                 "Completed",
      result:                "Unknown",
      scheduled_date:        1597384671236,
      rerun:                 false,
      original_job_id:       null,
      agent_uuid:            "a45a2f9f-e112-4e38-9f7c-8336305e53e3",
      // @ts-ignore
      pipeline_name:         null,
      pipeline_counter:      null,
      stage_name:            null,
      stage_counter:         null,
      job_state_transitions: [{
        state:             "Scheduled",
        state_change_time: 1597384671236
      }, {
        state:             "Assigned",
        state_change_time: 1597384821011
      }, {
        state:             "Preparing",
        state_change_time: 1597384831027
      }, {
        state:             "Building",
        state_change_time: 1597384833217
      }]
    }, {
      name:                  "failed-job",
      state:                 "Completed",
      result:                "Failed",
      scheduled_date:        1597384671236,
      rerun:                 false,
      original_job_id:       null,
      agent_uuid:            "a45a2f9f-e112-4e38-9f7c-8336305e53e3",
      // @ts-ignore
      pipeline_name:         null,
      pipeline_counter:      null,
      stage_name:            null,
      stage_counter:         null,
      job_state_transitions: [{
        state:             "Scheduled",
        state_change_time: 1597384671236
      }, {
        state:             "Assigned",
        state_change_time: 1597384735445
      }, {
        state:             "Preparing",
        state_change_time: 1597384745602
      }, {
        state:             "Building",
        state_change_time: 1597384765964
      }, {
        state:             "Completing",
        state_change_time: 1597384766005
      }, {
        state:             "Completed",
        state_change_time: 1597384766045
      }]
    }, {
      name:                  "cancelled-job",
      state:                 "Completed",
      result:                "Cancelled",
      scheduled_date:        1597384671236,
      rerun:                 false,
      original_job_id:       null,
      agent_uuid:            "a45a2f9f-e112-4e38-9f7c-8336305e53e3",
      // @ts-ignore
      pipeline_name:         null,
      pipeline_counter:      null,
      stage_name:            null,
      stage_counter:         null,
      job_state_transitions: [{
        state:             "Scheduled",
        state_change_time: 1597384671236
      }, {
        state:             "Assigned",
        state_change_time: 1597384776066
      }, {
        state:             "Preparing",
        state_change_time: 1597384786080
      }, {
        state:             "Building",
        state_change_time: 1597384788245
      }, {
        state:             "Completing",
        state_change_time: 1597384788267
      }, {
        state:             "Completed",
        state_change_time: 1597384788304
      }]
    }, {
      name:                  "another-failed-job",
      state:                 "Completed",
      result:                "Failed",
      scheduled_date:        1597384671236,
      rerun:                 false,
      original_job_id:       null,
      agent_uuid:            "c3c89976-8ea0-4e0f-afb9-230025b8c7e4",
      // @ts-ignore
      pipeline_name:         null,
      pipeline_counter:      null,
      stage_name:            null,
      stage_counter:         null,
      job_state_transitions: [{
        state:             "Scheduled",
        state_change_time: 1597384671236
      }, {
        state:             "Assigned",
        state_change_time: 1597384777141
      }, {
        state:             "Preparing",
        state_change_time: 1597384787155
      }, {
        state:             "Building",
        state_change_time: 1597384789363
      }, {
        state:             "Completing",
        state_change_time: 1597384789384
      }, {
        state:             "Completed",
        state_change_time: 1597384789415
      }]
    }, {
      name:                  "passing-job",
      state:                 "Completed",
      result:                "Passed",
      scheduled_date:        1597384671236,
      rerun:                 false,
      original_job_id:       null,
      agent_uuid:            "c3c89976-8ea0-4e0f-afb9-230025b8c7e4",
      // @ts-ignore
      pipeline_name:         null,
      pipeline_counter:      null,
      stage_name:            null,
      stage_counter:         null,
      job_state_transitions: [{
        state:             "Scheduled",
        state_change_time: 1597384671236
      }, {
        state:             "Assigned",
        state_change_time: 1597384799431
      }, {
        state:             "Preparing",
        state_change_time: 1597384809477
      }, {
        state:             "Building",
        state_change_time: 1597384811929
      }, {
        state:             "Completing",
        state_change_time: 1597384812057
      }, {
        state:             "Completed",
        state_change_time: 1597384812096
      }]
    }];
  }

  static jobWithNoPreparingTime(): JobJSON {
    return {
        name:                  "waiting-job",
        state:                 "Building",
        result:                "Unknown",
        scheduled_date:        1597384671236,
        rerun:                 false,
        original_job_id:       null,
        agent_uuid:            null,
        // @ts-ignore
        pipeline_name:         null,
        pipeline_counter:      null,
        stage_name:            null,
        stage_counter:         null,
        job_state_transitions: [{
          state:             "Scheduled",
          state_change_time: 1597384671236
        }, {
          state:             "Assigned",
          state_change_time: 1597384799431
        }, {
          state:             "Preparing",
          state_change_time: 1597384809477
        }, {
          state:             "Building",
          state_change_time: 1597384809477
        }, {
          state:             "Completing",
          state_change_time: 1597384812057
        }]
      };
  }
}
