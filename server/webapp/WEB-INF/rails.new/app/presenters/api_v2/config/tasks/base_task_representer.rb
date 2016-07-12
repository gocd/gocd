##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
##########################################################################

module ApiV2
  module Config
    module Tasks
      class BaseTaskRepresenter < ApiV2::BaseRepresenter
        alias_method :task, :represented
        collection :run_if, embedded: false, exec_context: :decorator, expect_hash:  true
        property :on_cancel_config,
                 expect_hash:  true,
                 as:           :on_cancel,
                 exec_context: :decorator,
                 decorator:    OnCancelRepresenter,
                 class:        com.thoughtworks.go.config.OnCancelConfig


        def run_if
          task.getConditions().map { |condition| condition.to_s }
        end

        def run_if=(value)
          run_if_conditions= RunIfConfigs.new
          value.each { |condition|
            run_if_conditions.add(RunIfConfig.new(condition))
          }
          task.setConditions(run_if_conditions)
        end

        def on_cancel_config
          task.getOnCancelConfig() if task.hasCancelTask()
        end

        def on_cancel_config=(value)
          @represented.setOnCancelConfig(value)
        end
      end
    end
  end
end

