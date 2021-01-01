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
import {MaintenanceModeInfo, MaintenanceModeInfoJSON} from "models/maintenance_mode/types";

export class TestData {
  static UPDATED_ON = "2018-12-10T04:19:31Z";

  static info(hasRunningSystems: boolean = true) {
    return MaintenanceModeInfo.fromJSON(this.infoJSON(hasRunningSystems) as MaintenanceModeInfoJSON);
  }

  static noRunningSystemsInfo() {
    return MaintenanceModeInfo.fromJSON(this.noRunningSystemsJSON() as MaintenanceModeInfoJSON);
  }

  static noRunningSystemsJSON() {
    return {
      is_maintenance_mode: true,
      metadata: {
        updated_by: "Admin",
        updated_on: this.UPDATED_ON
      },
      attributes: {
        has_running_systems: true,
        running_systems: {
          material_update_in_progress: [],
          building_jobs: []
        }
      }
    };
  }

  static infoJSON(hasRunningSystems: boolean) {
    return {
      is_maintenance_mode: hasRunningSystems,
      metadata: {
        updated_by: "Admin",
        updated_on: this.UPDATED_ON
      },
      attributes: {
        has_running_systems: true,
        running_systems: {
          material_update_in_progress: this.getMdu(),
          building_jobs: this.getJobs()
        }
      }
    };
  }

  private static getJobs() {
    return [
      {
        pipeline_name: "up42",
        pipeline_counter: 8,
        stage_name: "up42_stage",
        stage_counter: "1",
        name: "centos",
        state: "Scheduled",
        scheduled_date: "2018-12-07T08:59:15Z",
        agent_uuid: null
      },
      {
        pipeline_name: "up42",
        pipeline_counter: 8,
        stage_name: "up42_stage",
        stage_counter: "1",
        name: "ubuntu",
        state: "Scheduled",
        scheduled_date: "2018-12-07T08:59:15Z",
        agent_uuid: null
      },
      {
        pipeline_name: "bitbucket",
        pipeline_counter: 1,
        stage_name: "up42_stage",
        stage_counter: "1",
        name: "centos",
        state: "Scheduled",
        scheduled_date: "2018-12-07T16:33:46Z",
        agent_uuid: null
      },
      {
        pipeline_name: "bitbucket",
        pipeline_counter: 1,
        stage_name: "up42_stage",
        stage_counter: "1",
        name: "ubuntu",
        state: "Scheduled",
        scheduled_date: "2018-12-07T16:33:46Z",
        agent_uuid: null
      }
    ];
  }

  private static getMdu() {
    return [
      {
        type: "hg",
        mdu_start_time: "2018-12-10T04:19:31Z",
        attributes: {
          url: "https://bdpiparva@bitbucket.org/bdpiparva/bar-mercurial",
          name: "Foobar",
          auto_update: true,
          invert_filter: false
        }
      }
    ];
  }
}
