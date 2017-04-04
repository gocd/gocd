class PreferencesController < ApplicationController
  include ApiV1::AuthenticationHelper

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