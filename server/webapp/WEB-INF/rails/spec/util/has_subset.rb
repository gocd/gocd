##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

module Spec
  module Rails
    module Matchers

      class HasSubset  #:nodoc:

        def initialize(expected)
          @expected = expected
        end

        def matches?(actual)                    
          @actual = HashWithIndifferentAccess.new actual
          @expected.each_pair do |key, value|
            @actual[key] != value && set_mismatch(key, value) && return
          end
          return true
        end
        
        def set_mismatch key, value
          @mismatched_value = value
          @mismatched_key = key
        end

        def failure_message
          "expected actual to have value '#{@expected[@mismatched_key].inspect}' whereas it was equal to '#{@mismatched_values}' for key=#{@mismatched_key}."
        end

        def negative_failure_message
          "expected #{@expected.inspect} got #{@actual.inspect}"
        end

        def to_s
          "has pair #{@expected.inspect}"
        end

        private
        attr_reader :expected
        attr_reader :actual
      end

      def has_subset(expected_pairs)
        HasSubset.new(expected_pairs)
      end

    end
  end
end
