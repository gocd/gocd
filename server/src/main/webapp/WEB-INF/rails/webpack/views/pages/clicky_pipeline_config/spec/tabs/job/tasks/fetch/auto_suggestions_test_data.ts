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

export function suggestions() {
  return {
    pipeline1: {
      pipeline1_stage1: {
        pipeline1_stage1_job1: {
          id1: "docker-plugin",
          id2: "rkt-plugin"
        },
        pipeline1_stage1_job2: {}
      },
      pipeline1_stage2: {
        pipeline1_stage2_job1: {},
        pipeline1_stage2_job2: {}
      }
    },
    pipeline2: {
      pipeline2_stage1: {
        pipeline2_stage1_job1: {},
        pipeline2_stage1_job2: {}
      },
      pipeline2_stage2: {
        pipeline2_stage2_job1: {},
        pipeline2_stage2_job2: {}
      }
    }
  };
}
