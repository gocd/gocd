##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

module ApiV1
  module Config
    module Tasks
      class ExecTaskRepresenter < ApiV1::Config::Tasks::BaseTaskRepresenter
        alias_method :task, :represented
        ERROR_KEYS = {
          "workingDirectory" => "working_directory",
          "buildFile"        => "build_file",
          "onCancelConfig"   => "on_cancel",
          "runIf"            => "run_if",
          "nantPath"         => "nant_path",
          "command"          => "executable"
        }

        property :command, as: :executable
        collection :arguments, skip_nil: true, exec_context: :decorator
        property :args, skip_nil: true, exec_context: :decorator
        property :working_directory

        def arguments
          task.getArgList().map { |arg| arg.getValue() } unless task.getArgList().empty?
        end

        def arguments=(value)
          task.setArgsList(value)
        end

        def args
          task.getArgs unless task.getArgs.blank?
        end

        def args=(value)
          task.setArgs(value)
        end
      end
    end
  end
end
