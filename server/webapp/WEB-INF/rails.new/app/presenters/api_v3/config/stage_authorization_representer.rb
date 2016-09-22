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
    class StageAuthorizationRepresenter < ApiV3::BaseRepresenter
      alias_method :authorization, :represented

      error_representer

      collection :roles,
                 getter: lambda { |roles| roles().map { |role| role.getName().to_s } },
                 setter: lambda { |val, options| val.map { |role| self.add(com.thoughtworks.go.config.AdminRole.new(CaseInsensitiveString.new(role))) } }
      collection :users, getter: lambda { |users| users().map { |user| user.getName().to_s } },
                 setter:         lambda { |val, options| val.each { |user| self.add(com.thoughtworks.go.config.AdminUser.new(CaseInsensitiveString.new(user))) } }
    end
  end
end
