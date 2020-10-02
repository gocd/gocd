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

# See https://github.com/ketan/gocd/commit/52ac14d21b9b4595df82f5b0391a57c0cfca1bc6
# Patches PasswordField to render plain text value :-(
require 'action_view/helpers/tags/password_field'
module ActionView
  module Helpers
    module Tags # :nodoc:
      class PasswordField < TextField # :nodoc:
        def render
          # This line is intentionally commented out
          # @options = { value: nil }.merge!(@options)
          super
        end
      end
    end
  end
end
