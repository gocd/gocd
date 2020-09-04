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
package com.thoughtworks.go.apiv11.shared.representers.stages.tasks

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.FetchPluggableArtifactTask
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother

class FetchExternalTaskRepresenterTest implements TaskRepresenterTrait {

  def existingTask() {
    def task = new FetchPluggableArtifactTask()
    task.setPipelineName(new CaseInsensitiveString('pipeline'))
    task.setStage(new CaseInsensitiveString('stage'))
    task.setJob(new CaseInsensitiveString('job'))
    task.setArtifactId("yay")
    task.setConfiguration(new Configuration(ConfigurationPropertyMother.create("foo", false, "bar")))
    return task
  }

  def expectedTaskHash =
    [
      type      : 'fetch',
      attributes: [
        artifact_origin: 'external',
        pipeline       : 'pipeline',
        stage          : 'stage',
        job            : 'job',
        artifact_id    : 'yay',
        configuration  : [
          [
            key  : 'foo',
            value: 'bar'
          ]
        ],
        run_if         : []
      ]
    ]

  def expectedTaskHashWithRunIf =
    [
      type      : 'fetch',
      attributes: [
        artifact_origin: 'external',
        run_if         : ['passed', 'failed', 'any'],
        pipeline       : 'pipeline',
        stage          : 'stage',
        job            : 'job',
        artifact_id    : 'yay',
        configuration  : [
          [
            key  : 'foo',
            value: 'bar'
          ]
        ]
      ]
    ]

  def expectedTaskHashWithOnCancelConfig =
    [
      type      : 'fetch',
      attributes: [
        artifact_origin: 'external',
        pipeline       : 'pipeline',
        stage          : 'stage',
        job            : 'job',
        artifact_id    : 'yay',
        configuration  : [
          [
            key  : 'foo',
            value: 'bar'
          ]
        ],
        run_if         : [],
        on_cancel      : ["type": "ant", attributes: [
          run_if           : [],
          working_directory: null,
          build_file       : null,
          target           : null
        ]]
      ]
    ]
}
