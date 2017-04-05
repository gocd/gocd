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

module ApiV1
  class NotificationFiltersController < ApiV1::BaseController

    include ApiV1::UsersHelper

    before_action :check_user_and_404
    before_action :load_current_user
    before_action :check_filter_params, only: :create

    def index
      render_user_notification_filters
    end

    def create
      filter = filter_from_params
      user_service.addNotificationFilter(@user_to_operate.id, filter)
      @user_to_operate = user_service.load(@user_to_operate.id)

      render_user_notification_filters
    rescue UncheckedValidationException => e
      render_message(e.message, :bad_request)
    end

    def destroy
      user_service.removeNotificationFilter(@user_to_operate.id, params["id"].to_i)
      @user_to_operate = user_service.load(@user_to_operate.id)

      render_user_notification_filters
    end

    private

    def render_user_notification_filters
      render DEFAULT_FORMAT => filters_for_current_user.to_json
    end

    def check_filter_params
      unless %w(pipeline stage event).all? { |key| params[key].is_a?(String) }
        render_message("You must specify pipeline, stage, and event.", :bad_request)
      end
    end

    def filters_for_current_user
      @user_to_operate.notificationFilters.to_a.map { |nf| Hash(nf.toMap) }
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

  end
end
