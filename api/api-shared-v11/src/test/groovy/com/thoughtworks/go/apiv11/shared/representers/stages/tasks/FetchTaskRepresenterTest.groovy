/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.apiv11.shared.representers.stages.tasks

import com.thoughtworks.go.config.FetchTask
import com.thoughtworks.go.domain.Task

import static com.thoughtworks.go.config.CaseInsensitiveString.cis

class FetchTaskRepresenterTest implements TaskRepresenterTrait {
  Task existingTask() {
    def task = new FetchTask()
    task.setPipelineName(cis('pipeline'))
    task.setStage(cis('stage'))
    task.setJob(cis('job'))
    task.setSrcfile("src")
    task.setDest("dest")
    return task
  }

  Map<?, ?> expectedTaskHash =
    [
      type      : 'fetch',
      attributes: [
        artifact_origin : 'gocd',
        pipeline        : 'pipeline',
        stage           : 'stage',
        job             : 'job',
        is_source_a_file: true,
        source          : 'src',
        destination     : 'dest',
        run_if          : []
      ]
    ]

  Map<?, ?> expectedTaskHashWithRunIf =
    [
      type      : 'fetch',
      attributes: [
        artifact_origin : 'gocd',
        run_if          : ['passed', 'failed', 'any'],
        pipeline        : 'pipeline',
        stage           : 'stage',
        job             : 'job',
        source          : 'src',
        is_source_a_file: true,
        destination     : 'dest'
      ]
    ]

  Map<?, ?> expectedTaskHashWithOnCancelConfig =
    [
      type      : 'fetch',
      attributes: [
        artifact_origin : 'gocd',
        pipeline        : 'pipeline',
        stage           : 'stage',
        job             : 'job',
        source          : 'src',
        is_source_a_file: true,
        destination     : 'dest',
        run_if          : [],
        on_cancel       : ["type": "ant", attributes: [
          run_if           : [],
          working_directory: null,
          build_file       : null,
          target           : null
        ]]
      ]
    ]

}
