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
    class EnvironmentVariableRepresenter < ApiV3::BaseRepresenter
      alias_method :environment_variable, :represented

      error_representer({'encryptedValue' => 'encrypted_value'})

      property :isSecure, as: :secure, writable: false
      property :name, writable: false
      property :value, skip_nil: true, writable: false, exec_context: :decorator
      property :encrypted_value, skip_nil: true, writable: false, exec_context: :decorator
      property :errors, exec_context: :decorator, decorator: ApiV3::Config::ErrorRepresenter, skip_parse: true, skip_render: lambda { |object, options| object.empty? }

      def value
        environment_variable.getValueForDisplay() if environment_variable.isPlain
      end

      def encrypted_value
        environment_variable.getValueForDisplay() if environment_variable.isSecure
      end

      def from_hash(data, options={})
        data = data.with_indifferent_access
        environment_variable.deserialize(data[:name], data[:value], data[:secure].to_bool, data[:encrypted_value])
        environment_variable
      end

    end
  end
end
