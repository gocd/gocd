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
  module Plugins
    class PropertyRepresenter < BaseRepresenter
      alias_method :property, :represented

      property :key
      property :required,
               getter: lambda { |options|
                 self.getOption(com.thoughtworks.go.plugin.api.config.Property::REQUIRED)
               }
      property :secure,
               getter: lambda { |options|
                 self.getOption(com.thoughtworks.go.plugin.api.config.Property::SECURE)
               }
      property :display_name,
               getter: lambda { |options|
                 self.getOption(com.thoughtworks.go.plugin.api.config.Property::DISPLAY_NAME)
               }
      property :display_order,
               getter: lambda { |options|
                 self.getOption(com.thoughtworks.go.plugin.api.config.Property::DISPLAY_ORDER)
               }

    end
  end
end
