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
    class ConfigurationPropertyRepresenter < BaseRepresenter
      alias_method :configuration_property, :represented

      error_representer

      property :secure, skip_render: true
      property :configKeyName, as: :key
      property :value, skip_nil: true, exec_context: :decorator
      property :encrypted_value, skip_nil: true, exec_context: :decorator
      property :errors, exec_context: :decorator, decorator: ApiV1::Config::ErrorRepresenter, skip_parse: true, skip_render: lambda { |object, options| object.empty? }


      def value
        configuration_property.getConfigurationValue.getValue unless configuration_property.isSecure
      end

      def encrypted_value
        configuration_property.getEncryptedValue.getValue if configuration_property.isSecure
      end

      def from_hash(data, options={})
        data = data.with_indifferent_access
        configuration_property.deserialize(data[:key], data[:value], data[:secure].to_bool, data[:encrypted_value])
        configuration_property
      end
    end
  end
end
