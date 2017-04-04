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

java_import "com.thoughtworks.go.domain.NotificationFilter",
            "com.thoughtworks.go.domain.StageEvent",
            "com.thoughtworks.go.domain.exception.UncheckedValidationException"

module ApiV4
  class NotificationFiltersController < ApiV4::BaseController
    before_action :check_user_and_404
    before_action :load_user
    before_action :check_filter_params, only: :create

    def index
      render json: filters_for_current_user.to_json
    end

    def create
      filter = filter_from_params
      user_service.add_notification_filter(@user.id, filter)
      @user = user_service.load(@user.id)
      render json: filters_for_current_user.to_json
    rescue com.thoughtworks.go.domain.exception::UncheckedValidationException => e
      render json: {message: e.message}, status: 400
    end

    def destroy
      user_service.remove_notification_filter(@user.id, params["id"].to_i)
      @user = user_service.load(@user.id)

      render json: filters_for_current_user.to_json
    end

    private

    def check_filter_params
      (params["pipeline"].is_a?(String) && params["stage"].is_a?(String) && params["event"].is_a?(String)).tap do |params_present|
        render(json: {message: "You must specify pipeline, stage, and event."}.to_json, status: 400) unless params_present
      end
    end

    def filters_for_current_user
      @user.notification_filters.to_a.map { |nf| Hash(nf.toMap) }
    end

    def filter_from_params
      NotificationFilter.new(
        params["pipeline"],
        params["stage"],
        StageEvent.valueOf(params["event"]),
        # default checkbox behavior is to send value (defaults to "on") if checked, or to send
        # nothing if unchecked; thus, only check for presence of the key
        params.has_key?("myCheckin")
      )
    end

    def load_user
      @user = user_service.findUserByName(current_user.username.to_s)

      if !@user || @user.instance_of?(com.thoughtworks.go.domain.NullUser)
        raise ApiV1::RecordNotFound
      end
    end
  end
end
