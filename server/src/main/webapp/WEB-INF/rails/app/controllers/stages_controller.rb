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

class StagesController < ApplicationController
  helper :all
  include ApplicationHelper
  include StagesHelper

  STAGE_DETAIL_ACTIONS = [:overview, :pipeline, :materials, :jobs, :rerun_jobs, :stats, :stage_config]
  BASE_TIME = Time.parse("00:00:00")
  STAGE_DURATION_RANGE = 300
  layout "pipelines", :only => STAGE_DETAIL_ACTIONS
  before_action :load_stage_details, :only => STAGE_DETAIL_ACTIONS
  before_action :load_stage_history, :only => STAGE_DETAIL_ACTIONS - [:pipeline, :stats]
  before_action :load_current_config_version, :only => STAGE_DETAIL_ACTIONS << :history
  before_action :load_pipeline_instance, :only => :redirect_to_first_stage

  STAGE_HISTORY_PAGE_SIZE = 10

  def history
    load_stage_history_for_page params[:page]
    render layout: nil
  end

  def overview
    render_stage
  end

  def jobs
    @has_operate_permissions = security_service.hasOperatePermissionForStage(params[:pipeline_name], params[:stage_name], current_user.getUsername().to_s)
    @jobs = job_presentation_service.jobInstanceModelFor(@stage.getJobs())
    render_stage
  end

  def stats
    page_number = params[:page_number].nil? ? 1 : params[:page_number].to_i
    stage_summary_models = stage_service.findStageHistoryForChart(@stage.getPipelineName(), @stage.getName(), page_number, STAGE_DURATION_RANGE, current_user)
    stage_summary_models.sort! { |s1, s2| s1.getPipelineCounter() <=> s2.getPipelineCounter() }
    @no_chart_to_render = false
    if (stage_summary_models.size() > 0)

      @graph_data = stage_summary_models.select do |stage_summary|
        [StageState::Passed, StageState::Failed].include?(stage_summary.getStage().getState())
      end.map do |stage_summary|
        pipeline_label = stage_summary.getStageCounter().to_i > 1 ? stage_summary.getPipelineLabel() + " (run #{stage_summary.getStageCounter()})" : stage_summary.getPipelineLabel()

        {
            pipeline_counter: stage_summary.getPipelineCounter(),
            status: stage_summary.getStage().getState().toString(),
            stage_link: stage_detail_tab_jobs_path(:stage_name => stage_summary.getName(), :stage_counter => stage_summary.getStageCounter(), :pipeline_name => stage_summary.getPipelineName(), :pipeline_counter => stage_summary.getPipelineCounter()),
            duration:  stage_summary.isActive() ? nil : stage_summary.getActualDuration().getTotalSeconds()*1000,
            schedule_date: com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate(stage_summary.getStage().scheduledDate()),
            pipeline_label: pipeline_label
        }
      end
      @pagination = stage_summary_models.getPagination()
      @start_end_dates = date_range(stage_summary_models)
    else
      @no_chart_to_render = true
    end

    render_stage
  end

  def stage_config #_need_to_rename #/Users/jyoti/projects/mygocd/server/rails/gems/jruby/1.9/gems/actionpack-4.0.4/lib/action_controller/test_case.rb line 656
    @ran_with_config_revision = go_config_service.getConfigAtVersion(@stage.getStage().getConfigVersion())
    render_stage
  end

  def materials
    render_stage
  end

  def rerun_jobs
    stage = if params['rerun-failed'] == 'true'
              schedule_service.rerunFailedJobs(@stage.getStage(), result = HttpOperationResult.new)
            else
              schedule_service.rerunJobs(@stage.getStage(), params[:jobs], result = HttpOperationResult.new)
            end
    if result.canContinue()
      identifier = stage.getIdentifier()
      redirect_to(stage_detail_tab_path_for({:pipeline_name => identifier.getPipelineName(),
                                             :pipeline_counter => identifier.getPipelineCounter(),
                                             :stage_name => identifier.getStageName(),
                                             :stage_counter => identifier.getStageCounter()}, params[:tab]))
    else
      redirect_with_flash(result.message(), :action => params[:tab], :class => "error")
    end
  end

  def redirect_to_first_stage
    stage_instance = @pipeline_instance.getStageHistory.first
    stage_name = stage_instance.getName
    stage_counter = stage_instance.getCounter

    redirect_to stage_detail_tab_default_path(pipeline_name: params[:pipeline_name], pipeline_counter: params[:pipeline_counter], stage_name: stage_name, stage_counter: stage_counter)
  end

  private

  def load_pipeline_instance
    pipeline_name = params[:pipeline_name]
    pipeline_counter = params[:pipeline_counter].to_i
    @pipeline_instance = pipeline_history_service.findPipelineInstance(pipeline_name, pipeline_counter, current_user, HttpOperationResult.new)
    if @pipeline_instance.nil?
      render_error_template "Pipeline instance for the pipeline with name:'#{pipeline_name}', counter:#{pipeline_counter} not found.", 404
    end
  end

  def can_continue result
    unless result.canContinue()
      render_operation_result(result)
      return false
    end
    true
  end

  def load_current_config_version
    @current_config_version = go_config_service.getCurrentConfig().getMd5()
  end

  def load_stage_history
    pageNum = params["stage-history-page"]
    if (pageNum)
      load_stage_history_for_page pageNum
    else
      @stage_history_page = stage_service.findStageHistoryPage(@stage.getStage(), STAGE_HISTORY_PAGE_SIZE)
    end
  end

  def load_stage_history_for_page page
    pipeline_name = params[:pipeline_name]
    @stage_history_page = stage_service.findStageHistoryPageByNumber(pipeline_name, params[:stage_name], page.to_i, STAGE_HISTORY_PAGE_SIZE)
    @pipeline = pipeline_history_service.findPipelineInstance(pipeline_name, params[:pipeline_counter].to_i, current_user, result = HttpOperationResult.new)
  end

  def render_stage(status = 200)
    respond_to do |format|
      format.html { render action: 'stage', status: status }
      format.json { render action: 'stage', status: status }
      format.xml { redirect_to stage_path(:id => @stage.getId()) }
    end
  end

  def load_stage_details
    pipeline_name = params[:pipeline_name]
    stage_identifier = StageIdentifier.new(pipeline_name, params[:pipeline_counter].to_i, params[:stage_name], params[:stage_counter])

    @can_user_view_settings = can_view_settings?
    pipeline_history_service.validate(pipeline_name, current_user, result = HttpOperationResult.new)
    return unless can_continue result

    @stage = stage_service.findStageSummaryByIdentifier(stage_identifier, current_user, stage_result = HttpLocalizedOperationResult.new)
    unless stage_result.isSuccessful()
      render_localized_operation_result(stage_result)
      return
    end

    @pipeline = pipeline_history_service.findPipelineInstance(pipeline_name, params[:pipeline_counter].to_i, @stage.getPipelineId(), current_user, result = HttpOperationResult.new)
    @lockedPipeline = pipeline_lock_service.lockedPipeline(pipeline_name)
  end

  def date_range(stage_summary_models)
    [DateUtils::formatToSimpleDate(stage_summary_models.first.getStage().scheduledDate()), DateUtils::formatToSimpleDate(stage_summary_models.last.getStage().scheduledDate())]
  end

  def can_view_settings?
    group_name = go_config_service.findGroupNameByPipeline(CaseInsensitiveString.new(params[:pipeline_name]))
    permission = !group_name.blank? && security_service.isUserAdminOfGroup(current_user.getUsername, group_name)
    go_config_service.isPipelineEditable(params[:pipeline_name]) && permission
  end
end
