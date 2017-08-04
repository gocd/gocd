##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
  module Shared
    module ConfigOrigin
      class ConfigXmlOriginRepresenter < BaseRepresenter
        alias_method :config_xml_config, :represented

        property :type, exec_context: :decorator

        property :file,
                 exec_context: :decorator,
                 decorator: ApiV1::Shared::ConfigOrigin::ConfigXmlSummaryRepresenter

        def type
          'local'
        end

        def file
          config_xml_config
        end
      end
    end
  end
end
