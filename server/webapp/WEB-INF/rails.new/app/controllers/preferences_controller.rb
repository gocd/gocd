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

class PreferencesController < ApplicationController
  include AuthenticationHelper

  before_action :check_user_and_401, :load_pipelines

  layout "single_page_app"

  def notifications
    @view_title = 'Preferences'
  end

  private

  def load_pipelines
    @pipelines = pipeline_config_service.viewable_groups_for(current_user).inject([]) do |memo, pipeline_group|
      pipeline_group.each do |pipeline|
        memo << {pipeline: pipeline.name.to_s, stages: pipeline.map { |stage| stage.name.to_s }}
      end
      memo
    end.sort_by { |entry| entry[:pipeline] }
  end

end
