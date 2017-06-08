##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

class StagesController < ApplicationController
  include ApplicationHelper

  STAGE_DETAIL_ACTIONS = [:overview, :pipeline, :materials, :jobs, :tests, :rerun_jobs, :stats, :stage_config]
  BASE_TIME = Time.parse("00:00:00")
  STAGE_DURATION_RANGE = 300
  layout "pipelines", :only => STAGE_DETAIL_ACTIONS
  before_filter :load_stage_details, :only => STAGE_DETAIL_ACTIONS
  before_filter :load_stage_history, :only => STAGE_DETAIL_ACTIONS - [:pipeline]
  before_filter :load_current_config_version, :only => STAGE_DETAIL_ACTIONS << :history
  before_filter :set_format, :only => :tests

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
      @chart_stage_duration_passed, @chart_tooltip_data_passed, @chart_stage_duration_failed, @chart_tooltip_data_failed, @pagination, @start_end_dates = load_chart_parameters(stage_summary_models)
    else
      @no_chart_to_render = true
    end

    render_stage
  end

  def stage_config #_need_to_rename #/Users/jyoti/projects/mygocd/server/webapp/WEB-INF/rails.new/vendor/bundle/jruby/1.9/gems/actionpack-4.0.4/lib/action_controller/test_case.rb line 656
    @ran_with_config_revision = go_config_service.getConfigAtVersion(@stage.getStage().getConfigVersion())
    render_stage
  end

  def config_change
    @changes = go_config_service.configChangesFor(params[:later_md5], params[:earlier_md5], result = HttpLocalizedOperationResult.new)
    @config_change_error_message = result.isSuccessful ? (l.string("NO_CONFIG_DIFF") if @changes == nil) : result.message(localizer)
  end

  def pipeline
    @graph = pipeline_history_service.pipelineDependencyGraph(params[:pipeline_name], params[:pipeline_counter].to_i, current_user, result = result_for_graph())
    can_continue(result) && render_stage
  end

  def tests
    stage = @stage.getStage()
    unless fragment_exist?(view_cache_key.forFbhOfStagesUnderPipeline(stage.getIdentifier().pipelineIdentifier()), :subkey => view_cache_key.forFailedBuildHistoryStage(stage, @response_format))
      pipeline_name = params[:pipeline_name]
      stage_name = params[:stage_name]
      stage_identifier = StageIdentifier.new(pipeline_name, params[:pipeline_counter].to_i, stage_name, params[:stage_counter])
      result = SubsectionLocalizedOperationResult.new()
      if @stage.isActive()
        @failing_tests_error_message = l.string("TEST_RESULTS_WILL_BE_GENERATED_WHEN_THE_STAGE_COMPLETES")
        render_stage
        return
      end
      unless go_config_service.stageExists(pipeline_name, stage_name)
        @failing_tests = shine_dao.failedBuildHistoryForStage(stage_identifier, result)
        @failing_tests_error_message = result.replacementContent(Spring.bean('localizer')) unless result.isSuccessful()
        render_stage
        return
      end
      if go_config_service.stageHasTests(pipeline_name, stage_name)
        @failing_tests = shine_dao.failedBuildHistoryForStage(stage_identifier, result)
        result.isSuccessful() || (@failing_tests_error_message = result.replacementContent(Spring.bean('localizer')))
        render_stage
        return
      end
      @failing_tests_error_message = l.string("THERE_ARE_NO_TESTS_CONFIGURED_IN_THIS_STAGE")
    end
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
      redirect_to(stage_detail_tab_url(:pipeline_name => identifier.getPipelineName(),
                                       :pipeline_counter => identifier.getPipelineCounter(),
                                       :stage_name => identifier.getStageName(),
                                       :stage_counter => identifier.getStageCounter(),
                                       :action => params[:tab] || :overview))
    else
      redirect_with_flash(result.message(), :action => params[:tab], :class => "error")
    end
  end

  private

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

  def result_for_graph
    HttpOperationResult.new
  end

  def set_format
    @response_format = params[:format] || 'html'
  end

  def load_chart_parameters stage_summary_models
    chart_data_passed, tooltip_passed, passed_duration_array = get_chart_data(stage_summary_models) do |stage_summary|
      stage_summary.getStage().getState() == StageState::Passed
    end
    chart_data_failed, tooltip_failed, failed_duration_array = get_chart_data(stage_summary_models) do |stage_summary|
      stage_summary.getStage().getState() == StageState::Failed
    end
    total_durations = convert_to_scale(passed_duration_array + failed_duration_array)
    total = total_durations.length
    if !passed_duration_array.empty?
      total_durations[0..passed_duration_array.length-1].each_with_index do |duration, index|
        chart_data_passed[index]["y"] = duration
      end
    end
    total_durations[passed_duration_array.length..total-1].each_with_index do |duration, index|
      chart_data_failed[index]["y"] = duration
    end
    return chart_data_passed.to_json, tooltip_passed.to_json, chart_data_failed.to_json, tooltip_failed.to_json, stage_summary_models.getPagination(), date_range(stage_summary_models)
  end

  def date_range(stage_summary_models)
    [DateUtils::formatToSimpleDate(stage_summary_models.first.getStage().scheduledDate()), DateUtils::formatToSimpleDate(stage_summary_models.last.getStage().scheduledDate())]
  end

  def get_chart_data(stage_summary_models, &block)
    chart_data_array = []
    tooltip_data = {}
    duration_array = []
    stage_summary_models.each do |stage_summary|
      pipeline_counter = stage_summary.getPipelineCounter()
      pipeline_label = stage_summary.getStageCounter().to_i > 1 ? stage_summary.getPipelineLabel() + " (run #{stage_summary.getStageCounter()})" : stage_summary.getPipelineLabel()

      if block.call(stage_summary)
        duration = stage_summary.getActualDuration().getTotalSeconds()
        duration_array.push(duration)

        key = "#{pipeline_counter}_#{duration}"
        stage_link = stage_detail_tab_path(:action => "jobs", :stage_name => stage_summary.getName(), :stage_counter => stage_summary.getStageCounter(), :pipeline_name => stage_summary.getPipelineName(), :pipeline_counter => stage_summary.getPipelineCounter())
        chart_data_array.push({"link" => stage_link, "x" => pipeline_counter, "key" => key})

        tooltip_data[key] = [stage_summary.getDuration(), stage_summary.getStage().scheduledDate().to_long_display_date_time, pipeline_label]
      end
    end
    return chart_data_array, tooltip_data, duration_array
  end

  def convert_to_scale duration_array
    if duration_array.empty?
      return []
    end
    minute_threshold_in_secs = 300
    one_minute = 60.0
    highest_time = duration_array.sort.last
    if (highest_time > minute_threshold_in_secs)
      stage_duration_array = duration_array.map { |x| x/one_minute }
      @chart_scale = "mins"
    else
      stage_duration_array = duration_array
      @chart_scale = "secs"
    end
    stage_duration_array
  end

  def can_view_settings?
    group_name = go_config_service.findGroupNameByPipeline(CaseInsensitiveString.new(params[:pipeline_name]))
    permission = !group_name.blank? && security_service.isUserAdminOfGroup(current_user.getUsername, group_name)
    go_config_service.isPipelineEditableViaUI(params[:pipeline_name]) && permission
  end
end
