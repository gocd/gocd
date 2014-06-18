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
  module TaskHelper
    include JavaImports

    def task_options
      available_models = task_view_service.getTaskViewModels().select do |tvm| !(is_fetch_task?(tvm.getTaskType()) || is_exec_task?(tvm.getTaskType())) end

      result = available_models.collect do |tvm| [tvm.getTypeForDisplay(), tvm.getTaskType()] end
      result << ["More...", ExecTask.new.getTaskType()]
    end

    def selected_option oncancel_config
      oncancel_config.getTask().getTaskType()
    end

    def task_css_class task_type
      return 'lookup_icon' if is_exec_task? task_type
      ''
    end

    def is_fetch_task? task_type
      task_type == FetchTask.new.getTaskType()
    end

    def is_exec_task? task_type
      task_type == ExecTask.new.getTaskType()
    end
  end
end