#
# Copyright 2019 ThoughtWorks, Inc.
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
#

module ConfigView
  module ConfigViewHelper
    def plugin_task_type task
      task_view_service.getViewModel(task, "new").getTypeForDisplay()
    end

    def plugin_properties task
      props = task.getPropertiesForDisplay().collect do |property|
        [property.getName(), property.getValue()]
      end

      Hash[props]
    end

    def is_a_pluggable_task task
      task.getTaskType().start_with?(com.thoughtworks.go.config.pluggabletask.PluggableTask::PLUGGABLE_TASK_PREFIX)
    end
  end
end