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

module ApiV3
  module Config
    module Tasks
      class TaskRepresenter < ApiV3::BaseRepresenter

        TASK_TYPE_TO_REPRESENTER_MAP = {
          ExecTask::TYPE  => ExecTaskRepresenter,
          AntTask::TYPE   => AntTaskRepresenter,
          NantTask::TYPE  => NantTaskRepresenter,
          RakeTask::TYPE  => RakeTaskRepresenter,
          FetchTask::TYPE => FetchTaskRepresenter
        }

        TASK_TYPE_TO_TASK_CLASS_MAP = {
          ExecTask::TYPE      => ExecTask,
          AntTask::TYPE       => AntTask,
          NantTask::TYPE      => NantTask,
          RakeTask::TYPE      => RakeTask,
          FetchTask::TYPE     => FetchTask,
          PluggableTask::TYPE => PluggableTask,
        }

        alias_method :task, :represented
        property :type, exec_context: :decorator, skip_parse: true

        error_representer do |task|
          if task
            unless task.instance_of? PluggableTask
              TASK_TYPE_TO_REPRESENTER_MAP[task.getTaskType()]::ERROR_KEYS
            end
          end
        end

        nested :attributes,
               skip_parse: lambda { |fragment, options|
                 !fragment.respond_to?(:has_key?) || fragment.empty?
               },
               decorator:  lambda { |task, *|
                 if task.instance_of? PluggableTask
                   PluggableTaskRepresenter
                 else
                   TASK_TYPE_TO_REPRESENTER_MAP[task.getTaskType()]
                 end
               }

        def type
          (task.instance_of? PluggableTask) ? 'pluggable_task' : task.getTaskType
        end

        def task_attributes
          task
        end

        def task_attributes=(value)
          @represented = value
        end

        class << self
          def from_hash(hash, options={})
            task_type = hash[:type]
            if task_klass = task_class_for_type(task_type)
              representer = TaskRepresenter.new(task_klass.new)
              representer.from_hash(hash, options)
              representer
            end
          end

          def task_class_for_type(task_type)
            TASK_TYPE_TO_TASK_CLASS_MAP[task_type] || (raise ApiV3::UnprocessableEntity, "Invalid task type '#{task_type}'. It has to be one of '#{TASK_TYPE_TO_TASK_CLASS_MAP.keys.join(', ')}.'")
          end
        end
      end
    end
  end
end

