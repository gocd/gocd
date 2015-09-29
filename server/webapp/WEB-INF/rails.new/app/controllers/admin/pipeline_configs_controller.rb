##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

module Admin
  class PipelineConfigsController < ::ApplicationController
    layout 'pipeline_configs'

    def edit
      respond_to do |format|
        format.json {
          render json: JSON.pretty_generate(pipeline_data)
        }
        format.html {
          @view_title = "Edit Pipeline - #{params[:pipeline_name]}"
        }
      end
    end

    private
    def plugin_templates
      store = com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore.store()
      store.pluginIds().inject([]) do |memo, plugin_id|
        preference = store.preferenceFor(plugin_id)
        memo << {
          plugin_id:     plugin_id,
          version:       default_plugin_manager.getPluginDescriptorFor(plugin_id).version,
          name:          preference.getView().displayValue,
          template:      preference.getView().template,
          configuration: PluggableTaskConfigStore.store().preferenceFor(plugin_id).getConfig().list().map do |property|
            {
              name:  property.getKey(),
              value: property.getValue()
            }
          end

        }
      end
    end

    helper_method :plugin_templates

    def pipeline_data
      {
        name:                    'build-all',
        label_template:          'foo-1.0.${COUNT}-${svn}',
        enable_pipeline_locking: true,
        timer:                   {
          spec:            '0 0 22 ? * MON-FRI',
          only_on_changes: true,
        },
        template:                nil,
        parameters:              [
                                   { name: 'COMMAND', value: 'echo' },
                                   { name: 'WORKING_DIR', value: '/repo/branch' }
                                 ],
        tracking_tool:           { type:       'mingle',
                                   attributes: { base_url:            'http://mingle.example.com',
                                                 project_identifier:  'my_project',
                                                 grouping_conditions: "status > 'In Dev'" } },
        materials:               [
                                   {
                                     type:       'svn',
                                     attributes: { username:    'bob',
                                                   password:    "bob'sP@ssw0rd",
                                                   name:        'svn_material',
                                                   url:         'http://your-svn/',
                                                   destination: 'dest_folder',
                                                   auto_update: true,
                                                   filter:      {
                                                     ignore: ['*.doc', '*.xls']
                                                   }
                                     },
                                   }
                                 ],
        environment_variables:   [
                                   { name: 'MULTIPLE_LINES', value: 'multiplelines', secure: true },
                                   { name: 'COMPLEX', value: 'This has very <complex> data', secure: false }
                                 ],
        stages:                  [
                                   { name:                    'ut',
                                     fetch_materials:         true,
                                     clean_working_directory: false,
                                     never_clean_artifacts:   true,
                                     approval:                {
                                       type:          'manual|success',
                                       authorization: {
                                         roles: [],
                                         users: []
                                       }
                                     },
                                     environment_variables:   [
                                                                {
                                                                  name:   'stage-MULTIPLE_LINES',
                                                                  value:  'multiplelines',
                                                                  secure: true
                                                                },
                                                                { name:   'stage-COMPLEX',
                                                                  value:  'This has very <complex> data',
                                                                  secure: false }
                                                              ],
                                     jobs:                    [
                                                                { name:                  'build',
                                                                  run_on_all_agents:     false,
                                                                  run_instance_count:    0,
                                                                  timeout:               10,
                                                                  environment_variables: [],
                                                                  resources:             ['jdk5', 'tomcat5'],
                                                                  tasks:                 [
                                                                                           { type: 'ant', attributes: { target: 'clean', working_dir: 'dir' } },
                                                                                           {
                                                                                             type:       'plugin',
                                                                                             attributes: {
                                                                                               plugin_id:     'indix.s3fetch',
                                                                                               version:       1,
                                                                                               configuration: [
                                                                                                                { name: 'Repo', value: 'foo' },
                                                                                                                { name: 'Package', value: 'foobar-widgets' },
                                                                                                                { name: 'Destination', value: 'pkg/' }
                                                                                                              ]
                                                                                             }
                                                                                           }
                                                                                         ],
                                                                  artifacts:             [
                                                                                           { source: 'target/dist.jar', destination: 'pkg', type: 'build' },
                                                                                           { source:      'target/reports/**/*Test.xml',
                                                                                             destination: 'reports',
                                                                                             type:        'test'
                                                                                           }
                                                                                         ],
                                                                  tabs:                  [
                                                                                           { name: 'coverage',
                                                                                             path: 'Jcoverage/index.html'
                                                                                           }
                                                                                         ],
                                                                  properties:            [
                                                                                           {
                                                                                             name:   'coverage.class',
                                                                                             source: 'target/emma/coverage.xml',
                                                                                             xpath:  "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"
                                                                                           }
                                                                                         ]
                                                                }
                                                              ]
                                   },
                                   { name:                    'ut1',
                                     fetch_materials:         true,
                                     clean_working_directory: false,
                                     never_clean_artifacts:   true,
                                     approval:                { type: 'manual|success', authorization: { roles: [], users: [] } },
                                     environment_variables:   [{ name: 'stage-MULTIPLE_LINES', value: 'multiplelines', secure: true },
                                                               { name:   'stage-COMPLEX',
                                                                 value:  'This has very <complex> data',
                                                                 secure: false }],
                                     jobs:                    [{ name:                  'build',
                                                                 run_on_all_agents:     false,
                                                                 run_instance_count:    0,
                                                                 timeout:               nil,
                                                                 environment_variables: [],
                                                                 resources:             ['jdk5', 'tomcat5'],
                                                                 tasks:                 [{ type: 'ant', attributes: { target: 'clean', working_dir: 'dir' } }],
                                                                 artifacts:             [{ source: 'target/dist.jar', destination: 'pkg', type: 'build' },
                                                                                         { source:      'target/reports/**/*Test.xml',
                                                                                           destination: 'reports',
                                                                                           type:        'test' }],
                                                                 tabs:                  [{ name: 'coverage', path: 'Jcoverage/index.html' }],
                                                                 properties:            [{ name:   'coverage.class',
                                                                                           source: 'target/emma/coverage.xml',
                                                                                           xpath:  "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')" }] }] }
                                 ] }
    end
  end

end
