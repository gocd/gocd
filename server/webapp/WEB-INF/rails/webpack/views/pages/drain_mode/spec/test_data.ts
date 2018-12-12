/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {DrainModeInfo, DrainModeInfoJSON} from "models/drain_mode/types";

export class TestData {
  static info() {
    return DrainModeInfo.fromJSON(this.infoJSON() as DrainModeInfoJSON);
  }

  static infoJSON() {
    return {
      _embedded: {
        is_drain_mode: true,
        is_completely_drained: false,
        metadata: {
          updated_by: "Admin",
          updated_on: "2018-12-10T04:19:31Z"
        },
        running_systems: {
          mdu: [
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
          ],
          jobs: [
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
          ]
        }
      }
    };
  }
}
