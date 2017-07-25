##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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

module ApiV2
  module UsersHelper

    def save_user(result, user)
      # sending empty arrays through JSON are deserialized to nil
      # yay rails. https://github.com/rails/rails/pull/8862
      params["checkin_aliases"] = [] if params.has_key?("checkin_aliases") && params["checkin_aliases"].blank?

      checkin_aliases = if params[:checkin_aliases].is_a?(Array)
                          params[:checkin_aliases].join(',')
                        else
                          params[:checkin_aliases]
                        end
      user_service.save(user, to_tristate(params[:enabled]), to_tristate(params[:email_me]), params[:email], checkin_aliases, result)
    end

    def load_user(username=params[:login_name])
      @user_to_operate = user_service.findUserByName(username)

      if !@user_to_operate || @user_to_operate.instance_of?(com.thoughtworks.go.domain.NullUser)
        raise ApiV2::RecordNotFound
      end
    end

    def load_current_user
      load_user(current_user.username.to_s)
    end

  end
end
