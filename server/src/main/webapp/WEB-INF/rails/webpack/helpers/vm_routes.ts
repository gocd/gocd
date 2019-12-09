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

export class VMRoutes {
  static pipelineHistoryPath(pipelineName: string) {
    return `/go/pipeline/activity/${pipelineName}`;
  }

  static vsmPath(pipelineName: string, pipelineCounter: string) {
    return `/go/pipelines/value_stream_map/${pipelineName}/${pipelineCounter}`;
  }

  static comparePath(pipelineName: string, fromCounter: string, toCounter: string) {
    return `/go/compare/${pipelineName}/${fromCounter}/with/${toCounter}`;
  }
}
