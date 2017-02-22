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
  module Shared
    module Stages
      module Tasks
        class NantTaskRepresenter < BaseTaskRepresenter
          alias_method :task, :represented
          ERROR_KEYS = {
            'buildFile' => 'build_file',
            'onCancelConfig' => 'on_cancel',
            'runIf' => 'run_if',
            'nantPath' => 'nant_path'
          }

          property :working_directory, skip_parse: SkipParseOnBlank
          property :build_file, skip_parse: SkipParseOnBlank
          property :target, skip_parse: SkipParseOnBlank
          property :nant_path, skip_parse: SkipParseOnBlank

        end
      end
    end
  end
end