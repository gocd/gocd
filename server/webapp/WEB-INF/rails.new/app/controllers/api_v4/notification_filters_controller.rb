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

module ApiV4
  class NotificationFiltersController < ApiV4::BaseController
    before_action :check_user_and_404
    before_action :load_user

    def index
      render json: filters_for_current_user.to_json
    end

    def destroy
      user_service.remove_notification_filter(@user.id, params["id"].to_i)
      @user = user_service.load(@user.id)

      render json: filters_for_current_user.to_json
    end

    private

    def filters_for_current_user
      @user.notification_filters.to_a.map { |nf| Hash(nf.toMap) }
    end

    def load_user
      @user = user_service.findUserByName(current_user.username.to_s)

      if !@user || @user.instance_of?(com.thoughtworks.go.domain.NullUser)
        raise ApiV1::RecordNotFound
      end
    end
  end
end
