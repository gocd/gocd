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
  class UserRepresenter < ApiV1::BaseRepresenter

    alias_method :user, :represented

    link :doc do
      'http://api.go.cd/#users'
    end

    link :self do |opts|
      opts[:url_builder].apiv1_user_url(user.getName)
    end

    link :find do |opts|
      opts[:url_builder].apiv1_user_url(':login_name')
    end

    property :getName, as: :login_name
    property :displayName, as: :display_name
    property :isEnabled, as: :enabled
    property :email, exec_context: :decorator
    property :isEmailMe, as: :email_me
    property :checkin_aliases, exec_context: :decorator

    def email
      user.email.blank? ? nil : user.email
    end

    def checkin_aliases
      user.getMatchers().to_a
    end
  end
end
