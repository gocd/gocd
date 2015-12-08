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
    module TrackingTool
      class MingleTrackingToolRepresenter < ApiV1::BaseRepresenter
        alias_method :mingle, :represented

        ERROR_KEYS = {
          'baseUrl'           => 'base_url',
          'projectIdentifier' => 'project_identifier'
        }

        property :base_url
        property :project_identifier
        property :mql_grouping_conditions, exec_context: :decorator

        def mql_grouping_conditions
          mingle.getMqlCriteria().getMql
        end

        def mql_grouping_conditions=(value)
          mingle.setMqlCriteria(value)
        end


      end
    end
  end
end
