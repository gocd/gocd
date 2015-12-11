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
    class EnvironmentVariableRepresenter < ApiV1::BaseRepresenter
      alias_method :environment_variable, :represented

      property :isSecure, as: :secure, writable: false
      property :name, writable: false
      property :value, skip_nil: true, writable: false, exec_context: :decorator
      property :encrypted_value, skip_nil: true, writable: false, exec_context: :decorator
      property :errors, exec_context: :decorator, decorator: ApiV1::Config::ErrorRepresenter, skip_parse: true, skip_render: lambda { |object, options| object.empty? }

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

      private

      def errors
        mapped_errors = {}
        environment_variable.errors.each do |key, value|
          mapped_errors[matching_error_key(key)] = value
        end
        mapped_errors
      end

      def error_keys
        { 'encryptedValue' => 'encrypted_value' }
      end

      def matching_error_key key
        return error_keys[key] if error_keys[key]
        key
      end

    end
  end
end
