##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

module Api
  class FeatureTogglesController < Api::ApiController
    layout false

    def index
      toggles = feature_toggle_service.allToggles().all().collect {|toggle| FeatureToggleAPIModel.new toggle}
      render :json => toggles
    end

    def update
      if params[:toggle_value].nil? or (params[:toggle_value] != "off" and params[:toggle_value] != "on")
        render :status => :unprocessable_entity, :json => {:message => "Value of property 'toggle_value' is invalid. Valid values are: 'on' and 'off'."} and return
      end

      begin
        feature_toggle_service.changeValueOfToggle(params[:toggle_key], params[:toggle_value] == "on" ? true : false)
      rescue => e
        render :status => :internal_server_error, :json => {:message => "Failed to change value of toggle. Message: #{e.message}"} and return
      end

      render :status => :ok, :json => {:message => "success"}
    end
  end
end
