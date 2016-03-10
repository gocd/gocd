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

module ApiV1
  module Scms
    class ConfigurationRepresenter < BaseRepresenter
      alias_method :configuration, :represented

      property :key,
               getter: lambda { |args|
                 self.getConfigurationKey.getName
               }

      property :value, skip_nil: true, exec_context: :decorator
      property :encrypted_value, skip_nil: true, exec_context: :decorator

      def value
        configuration.getConfigurationValue.getValue if !configuration.isSecure
      end

      def encrypted_value
        configuration.getEncryptedValue.getValue if configuration.isSecure
      end
    end
  end
end
