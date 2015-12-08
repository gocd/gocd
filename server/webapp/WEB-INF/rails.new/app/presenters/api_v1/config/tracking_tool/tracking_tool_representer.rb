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
      class TrackingToolRepresenter < ApiV1::BaseRepresenter
        TRACKING_TOOL_TYPE_TO_REPRESENTER_MAP = {
          com.thoughtworks.go.config.MingleConfig => MingleTrackingToolRepresenter,
          com.thoughtworks.go.config.TrackingTool => ExternalTrackingToolRepresenter
        }

        TRACKING_TOOL_TYPE_TO_CLASS_MAP = {
          'generic' => com.thoughtworks.go.config.TrackingTool,
          'mingle'  => com.thoughtworks.go.config.MingleConfig
        }.freeze

        TRACKING_TOOL_CLASS_TO_TYPE_MAP = TRACKING_TOOL_TYPE_TO_CLASS_MAP.invert.freeze

        alias_method :tracking_tool, :represented

        error_representer(lambda { |tracking_tool|
          if tracking_tool
            TRACKING_TOOL_TYPE_TO_REPRESENTER_MAP[tracking_tool.class()]::ERROR_KEYS
          end
        })
        property :type, exec_context: :decorator, skip_parse: true
        nested :attributes,
               decorator: lambda { |tracking_tool, *|
                 TRACKING_TOOL_TYPE_TO_REPRESENTER_MAP[tracking_tool.class]
               }

        class << self
          def get_class(type)
            TRACKING_TOOL_TYPE_TO_CLASS_MAP[type] || (raise ApiV1::UnprocessableEntity, "Invalid Tracking Tool type '#{type}'. It has to be one of '#{TRACKING_TOOL_TYPE_TO_CLASS_MAP.keys.join(', ')}.'")
          end
        end

        private

        def type
          TRACKING_TOOL_CLASS_TO_TYPE_MAP[tracking_tool.class] || (raise ApiV1::UnprocessableEntity, "Invalid Tracking Tool type '#{tracking_tool.class}'. It has to be one of '#{TRACKING_TOOL_CLASS_TO_TYPE_MAP.keys.join(', ')}.'")
        end
      end
    end
  end
end
