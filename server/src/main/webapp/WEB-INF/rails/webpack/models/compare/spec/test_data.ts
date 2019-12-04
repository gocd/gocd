/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {ComparisonJSON} from "../compare_json";
import {PipelineInstanceJSON} from "../pipeline_instance_json";

export class PipelineInstanceData {
  static pipeline() {
    return {
      id:                    34,
      name:                  "up42",
      counter:               2,
      label:                 "2",
      natural_order:         2.0,
      can_run:               true,
      preparing_to_schedule: false,
      comment:               "",
      build_cause:           {
        trigger_message:    "Forced by anonymous",
        trigger_forced:     true,
        approver:           "anonymous",
        material_revisions: [
          {
            changed:       false,
            material:      {
              id:          1,
              name:        "test-repo",
              fingerprint: "de08b060ebfc1fb988b6683bf21",
              type:        "Git",
              description: "URL: https://github.com/gocd/gocd, Branch: master"
            },
            modifications: [
              {
                id:            1,
                revision:      "11932fecb6d3f7ec22e1b0cb3a88553a",
                modified_time: "2019-10-07T09:09:43Z",
                user_name:     "dummyuser <user@users.noreply.github.com>",
                comment:       "some comment",
                email_address: ""
              }
            ]
          }
        ]
      },
      stages:                [
        {
          result:             "Passed",
          id:                 65,
          name:               "up42_stage",
          counter:            "2",
          scheduled:          true,
          approval_type:      "success",
          approved_by:        "admin",
          operate_permission: true,
          can_run:            true,
          jobs:               [
            {
              id:             65,
              name:           "up42_job",
              scheduled_date: "2019-11-01T10:37:17Z",
              state:          "Completed",
              result:         "Passed"
            }
          ]
        }
      ]
    } as PipelineInstanceJSON;
  }
}

export class ComparisonData {
  static compare(pipelineName: string = "pipeline1", fromCounter: number = 1, toCounter: number = 3) {
    return {
      pipeline_name: pipelineName,
      from_counter:  fromCounter,
      to_counter:    toCounter,
      is_bisect:     false,
      changes:       [
        {
          material: {
            type:       "git",
            attributes: {
              destination:      null,
              filter:           null,
              invert_filter:    false,
              name:             null,
              auto_update:      true,
              url:              "git@github.com:sample_repo/example.git",
              branch:           "master",
              submodule_folder: null,
              shallow_clone:    false
            }
          },
          revision: this.materialRevisions()
        },
        {
          material: {
            type:       "dependency",
            attributes: {
              pipeline:    "upstream",
              stage:       "upstream_stage",
              name:        "upstream_material",
              auto_update: true
            }
          },
          revision: this.dependencyMaterialRevisions()
        }
      ]
    } as ComparisonJSON;
  }

  static materialRevisions() {
    return [
      {
        revision_sha:   "some-random-sha",
        modified_by:    "username <username@github.com>",
        modified_at:    "2019-10-15T12:32:37Z",
        commit_message: "some commit message"
      }
    ];
  }

  static dependencyMaterialRevisions() {
    return [
      {
        revision:         "upstream/1/upstream_stage/1",
        pipeline_counter: "1",
        completed_at:     "2019-10-17T06:55:07Z"
      }
    ];
  }
}
