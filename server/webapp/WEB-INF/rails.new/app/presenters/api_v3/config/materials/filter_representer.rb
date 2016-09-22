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
    module Materials
      class FilterRepresenter < ApiV3::BaseRepresenter
        alias_method :filter, :represented

        collection :ignore, exec_context: :decorator

        def to_hash(*options)
          ignored_files=filter.map { |item| item.getPattern() }
          {ignore: ignored_files} if !ignored_files.empty?
        end

        def ignore=(value)
          filter.clear()
          value.each { |pattern| filter.add(IgnoredFiles.new(pattern)) }
        end
      end
    end
  end
end
