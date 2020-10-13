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
package com.thoughtworks.go.apiv12.shared.representers.stages.tasks


import com.thoughtworks.go.config.pluggabletask.PluggableTask
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.config.PluginConfiguration
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference
import com.thoughtworks.go.plugin.api.config.Property
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.BeforeEach

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create

class PluggableTaskRepresenterTest implements TaskRepresenterTrait {
  @BeforeEach
  void setUpPlugin() {
    def simpleProperty = new TaskConfigProperty('simple_key', 'value')
    def secureProperty = new TaskConfigProperty('secure_key', 'mJxU96GFuVY=').with(Property.SECURE, true)
    def task = new com.thoughtworks.go.apiv12.shared.representers.helpers.TaskMother.StubTask()
    task.config().add(simpleProperty)
    task.config().add(secureProperty)
    def taskPreference = new TaskPreference(task)
    PluggableTaskConfigStore.store().setPreferenceFor("curl", taskPreference);
  }

  def existingTask() {
    def simpleProperty = create("simple_key", false, "value")
    def secureProperty = create("secure_key", true, "value")
    def pluginConfiguration = new PluginConfiguration("curl", "1.0")
    def configuration = new Configuration(simpleProperty, secureProperty)
    def task = new PluggableTask(pluginConfiguration, configuration)
    return task
  }

  def expectedTaskHash =
    [
      type:       'pluggable_task',
      attributes: [
        plugin_configuration: [
          id:      'curl',
          version: '1.0'
        ],
        configuration:        [
          [
            key:   'simple_key',
            value: 'value'
          ],
          [
            key:             'secure_key',
            encrypted_value: new GoCipher().encrypt('value')
          ]
        ],
        run_if           : []
      ]
    ]

  def expectedTaskHashWithRunIf =
    [
      type:       'pluggable_task',
      attributes: [
        plugin_configuration: [
          id:      'curl',
          version: '1.0'
        ],
        configuration:        [
          [
            key:   'simple_key',
            value: 'value'
          ],
          [
            key:             'secure_key',
            encrypted_value: new GoCipher().encrypt('value')
          ]
        ],
        run_if           : ['passed', 'failed', 'any']
      ]
    ]

  def expectedTaskHashWithOnCancelConfig =
    [
      type:       'pluggable_task',
      attributes: [
        plugin_configuration: [
          id:      'curl',
          version: '1.0'
        ],
        configuration:        [
          [
            key:   'simple_key',
            value: 'value'
          ],
          [
            key:             'secure_key',
            encrypted_value: new GoCipher().encrypt('value')
          ]
        ],
        run_if           : [],
        on_cancel      : ["type": "ant", attributes:[
          run_if: [],
          working_directory: null,
          build_file: null,
          target: null
        ]]
      ]
    ]

}
