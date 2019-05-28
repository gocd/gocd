#
# Copyright 2019 ThoughtWorks, Inc.
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

module ApiV1
  module Admin
    module Authorization
      class AuthorizationConfigRepresenter < BaseRepresenter
        alias_method :authorization, :represented

        error_representer

        property :isAllowGroupAdmins, as: :all_group_admins_are_view_users,
                 setter: lambda {|val, options| self.setAllowGroupAdmins(val)}

        property :adminsConfig, as: :admin,
                 skip_render: lambda { |object, options| object.empty? },
                 decorator: ApiV1::Admin::Authorization::UserRoleConfigRepresenter,
                 class: com.thoughtworks.go.config.AdminsConfig

        property :operationConfig, as: :operate,
                 skip_render: lambda { |object, options| object.empty? },
                 decorator: ApiV1::Admin::Authorization::UserRoleConfigRepresenter,
                 class: com.thoughtworks.go.config.OperationConfig

        property :viewConfig, as: :view,
                 skip_render: lambda { |object, options| object.empty? },
                 decorator: ApiV1::Admin::Authorization::UserRoleConfigRepresenter,
                 class: com.thoughtworks.go.config.ViewConfig

      end
    end
  end
end
