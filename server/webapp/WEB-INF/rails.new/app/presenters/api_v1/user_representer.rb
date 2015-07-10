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
  class UserRepresenter < ApiV1::UserSummaryRepresenter
  attr_reader :user

    def initialize(user)
      @user = user
      super(user.name)
    end

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

    def represented
      user
    end
  end
end
