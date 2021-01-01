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
package com.thoughtworks.go.apiv10.shared.representers.stages.tasks

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.FetchTask

class FetchTaskRepresenterTest implements TaskRepresenterTrait {
  def existingTask() {
    def task = new FetchTask()
    task.setPipelineName(new CaseInsensitiveString('pipeline'))
    task.setStage(new CaseInsensitiveString('stage'))
    task.setJob(new CaseInsensitiveString('job'))
    task.setSrcfile("src")
    task.setDest("dest")
    return task
  }

  def expectedTaskHash =
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

  def expectedTaskHashWithRunIf =
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

  def expectedTaskHashWithOnCancelConfig =
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
