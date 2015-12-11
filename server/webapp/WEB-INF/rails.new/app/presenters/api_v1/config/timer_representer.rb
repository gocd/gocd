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
    class TimerRepresenter < ApiV1::BaseRepresenter
      alias_method :timer, :represented

      property :timer_spec, as: :spec, setter: lambda { |value, args|
                            self.timer_spec = value unless value.blank?
                          }
      property :onlyOnChanges, as: :only_on_changes
      property :errors,
               exec_context: :decorator,
               decorator:    ApiV1::Config::ErrorRepresenter,
               skip_parse:   true,
               skip_render:  lambda { |object, options| object.empty? }

      def errors
        mapped_errors = {}
        timer.errors.each do |key, value|
          mapped_errors[matching_error_key(key)] = value
        end
        mapped_errors
      end

      private
      def error_keys
        {"timerSpec" => "spec"}
      end

      def matching_error_key key
        return error_keys[key] if error_keys[key]
        key
      end
    end
  end
end
