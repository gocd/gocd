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
    class PluginConfigurationPropertyRepresenter < ApiV1::BaseRepresenter
      alias_method :configuration_property, :represented

      error_representer(
        {
          'encryptedValue' => 'encrypted_value',
          'configurationValue' => 'configuration_value',
          'configurationKey' => 'configuration_key'
        }
      )
      property :key, exec_context: :decorator
      property :value, skip_nil: true, exec_context: :decorator
      property :encrypted_value, skip_nil: true, exec_context: :decorator

      def value
        configuration_property.getValue unless configuration_property.isSecure
      end

      def encrypted_value
        configuration_property.getEncryptedValue if configuration_property.isSecure
      end

      def value=(val)
        configuration_property.setConfigurationValue(ConfigurationValue.new(val))
      end

      def encrypted_value=(encrypted_val)
        configuration_property.setEncryptedConfigurationValue(EncryptedConfigurationValue.new(encrypted_val))
      end

      def key
        configuration_property.getConfigurationKey.getName
      end

      def key=(value)
        configuration_property.setConfigurationKey(ConfigurationKey.new(value))
      end
    end
  end
end
