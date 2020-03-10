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

import {ComparisonJSON} from "../compare_json";
import {PipelineInstanceJSON} from "../pipeline_instance_json";
import {MaterialJSON} from "../material_json";

export class PipelineInstanceData {
  static pipeline(counter: number = 2) {
    return {
      id:                    34,
      name:                  "up42",
      counter,
      label:                 "2",
      natural_order:         2.0,
      can_run:               true,
      preparing_to_schedule: false,
      comment:               "",
      scheduled_date:        1577166804163,
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
          },
          {
            changed:       true,
            material:      {
              name:        "pipeline-name",
              fingerprint: "34b6f93309fa058e52e5e9a51d173b6ba4",
              type:        "Pipeline",
              description: "template-name"
            },
            modifications: [
              {
                revision:       "template-name/11/stage2/1",
                modified_time:  "2020-01-30T11:25:18Z",
                pipeline_label: "label-11"
              }
            ]
          }
        ]
      },
      stages:                [
        {
          result:             "Passed",
          status:             "Passed",
          id:                 65,
          name:               "up42_stage",
          counter:            counter + "2",
          scheduled:          true,
          approval_type:      "success",
          approved_by:        "admin",
          operate_permission: true,
          can_run:            true,
          jobs:               [
            {
              id:             65,
              name:           "up42_job",
              scheduled_date: 1577166804163,
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
              shallow_clone:    false,
              display_type:     "Git",
              description:      "URL: git@github.com:sample_repo/example.git, Branch: master"
            }
          },
          revision: this.materialRevisions()
        },
        {
          material: {
            type:       "dependency",
            attributes: {
              pipeline:     "upstream",
              stage:        "upstream_stage",
              name:         "upstream_material",
              auto_update:  true,
              display_type: "Pipeline",
              description:  "upstream [ upstream_stage ]"
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

export class MaterialData {
  static git() {
    return {
      type:       "git",
      attributes: {
        invert_filter: false,
        name:          null,
        auto_update:   true,
        url:           "git@github.com:sample_repo/example.git",
        branch:        "master",
        shallow_clone: false,
        display_type:  "Git",
        description:   "URL: git@github.com:sample_repo/example.git, Branch: master"
      }
    } as MaterialJSON;
  }

  static svn() {
    return {
      type:       "svn",
      attributes: {
        url:             "url",
        destination:     "svnDir",
        name:            "SvnMaterial",
        auto_update:     true,
        check_externals: true,
        username:        "user",
        display_type:    "Subversion",
        description:     "URL: url, Username: user, CheckExternals: true"
      }
    } as MaterialJSON;
  }

  static hg() {
    return {
      type:       "hg",
      attributes: {
        url:          "hg-url",
        destination:  "foo_bar",
        name:         null,
        auto_update:  true,
        display_type: "Mercurial",
        description:  "URL: hg-url"
      },
    } as MaterialJSON;
  }

  static p4() {
    return {
      type:       "p4",
      attributes: {
        destination:  "bar",
        name:         "Dummy git",
        auto_update:  true,
        use_tickets:  false,
        view:         "some-view",
        port:         "some-port",
        display_type: "Perforce",
        description:  "URL: some-port, View: some-view, Username: "
      }
    } as MaterialJSON;
  }

  static tfs() {
    return {
      type:       "tfs",
      attributes: {
        url:          "foo/bar",
        destination:  "bar",
        name:         "Dummy tfs",
        auto_update:  true,
        domain:       "foo.com",
        project_path: "/var/project",
        username:     "bob",
        display_type: "Tfs",
        description:  "URL: foo/bar, Username: bob, Domain: foo.com, ProjectPath: /var/project"
      }
    } as MaterialJSON;
  }

  static dependency() {
    return {
      type:       "dependency",
      attributes: {
        pipeline:     "upstream",
        stage:        "upstream_stage",
        name:         "upstream_material",
        auto_update:  true,
        display_type: "Pipeline",
        description:  "upstream [ upstream_stage ]"
      }
    } as MaterialJSON;
  }

  static package() {
    return {
      type:       'package',
      attributes: {
        ref:          "pkg-id",
        display_type: "Package",
        description:  "Repository: [k1=repo-v1, k2=repo-v2] - Package: [k3=package-v1]",
      }
    } as MaterialJSON;
  }

  static pluggable() {
    return {
      type:       'plugin',
      attributes: {
        ref:          "scm-id",
        filter:       {
          ignore: ["**/*.html", "**/foobar/"]
        },
        destination:  'des-folder',
        display_type: "Github",
        description:  "k1:v1",
      }
    } as MaterialJSON;
  }
}
