#
# Copyright 2020 ThoughtWorks, Inc.
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
#

module CacheStoreForTest
  unless $has_loaded_one_time_enhancements
    GoCacheStore.class_eval do
      def write_with_recording(name, value, options = nil)
        writes[key(name, options)] = value
        write_without_recording(name, value, options)
      end

      alias_method_chain :write, :recording

      def read_with_recording(name, options = nil)
        value = read_without_recording(name, options)
        reads[key(name, options)] = value
      end

      alias_method_chain :read, :recording

      def clear_with_recording
        clear_without_recording
        writes.clear
        reads.clear
      end

      alias_method_chain :clear, :recording

      def writes
        @writes ||= {}
      end

      def reads
        @reads ||= {}
      end
    end

    $has_loaded_one_time_enhancements = true
  end
end