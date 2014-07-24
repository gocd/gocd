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

require File.join(File.dirname(__FILE__), "..", "..", "spec_helper")

describe "layouts/pipeline.html.erb" do
  include GoUtil, StageModelMother

  before do
    stub_server_health_messages
    assigns[:user] = @user = Username.new(CaseInsensitiveString.new("blah-name"), "blah diaply name")
    now = org.joda.time.DateTime.new
    @stages = PipelineHistoryMother.stagePerJob("stage", [PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, now.toDate()),
                                                          PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, now.plusDays(1).toDate())])
    @stages.get(0).setId(12)
    @stages.get(1).setId(13)
    @stages.get(0).setOperatePermission(true)

    @stage_history_page = assigns[:stage_history_page] = last_stage_history_page(1)

    @request.path_parameters[:pipeline_name] = 'cruise'
    @request.path_parameters[:pipeline_counter] = '1'

    @pim = PipelineHistoryMother.singlePipeline("pipeline-name", @stages)
    assigns[:pipeline] = @pim
    params[:pipeline_name] = "cruise"
    params[:pipeline_counter] = "1"
    @request.path_parameters.reverse_merge!(params)
    template.stub!(:can_view_admin_page?).and_return(true)
    class << template
      include PipelinesHelper
      include ApplicationHelper
      include StagesHelper

      def url_for_with_stub(*args)
        args.empty? ? "/go/" : url_for_without_stub(*args)
      end

      alias_method_chain :url_for, :stub
    end
    template.stub!(:stage_detail_path)
    template.stub!(:stage_detail_path).with(:stage_name => 'stage-0', :stage_counter => "1").and_return("url_to_0")
    template.stub!(:stage_detail_path).with(:stage_name => 'stage-1', :stage_counter => '1').and_return("url_to_1")

    in_params :action => 'overview', :controller => "stages"
    template.stub!(:stage_detail_tab_path).with(:pipeline_name => 'pipeline', :pipeline_counter => 1, :stage_name => 'stage-0', :stage_counter => "1", :action => 'overview').and_return("url_to_0")
    template.stub!(:stage_detail_tab_path).with(:pipeline_name => 'pipeline', :pipeline_counter => 1, :stage_name => 'stage-1', :stage_counter => "1", :action => 'overview').and_return("url_to_1")
    template.stub!(:stage_detail_tab_path).with(:pipeline_name => "cruise", :pipeline_counter => 1,
                                                :stage_name => 'dev', :stage_counter => "1", :action => 'overview').and_return("url_to_historical_stage")
    template.stub!(:stage_detail_tab_path).with(:pipeline_name=>"pipeline-name", :pipeline_counter=>1, :stage_name=>"stage-1", :stage_counter=>"1", :action=>"pipeline").and_return("url_to_pipeline")
    template.stub!(:stage_history_path).and_return("historical_stage_page_number")
    template.stub!(:is_user_an_admin?).and_return(true)
    template.stub!(:config_change_path)
  end

  describe "stage configuration out of sync notification" do

    before :each do
      stage_summary_model = mock('stage_summary_model')
      @stage = mock('stage')
      stage_summary_model.should_receive(:getStage).any_number_of_times.and_return(@stage)
      stage_summary_model.should_receive(:getName).any_number_of_times.and_return('stage-0')
      stage_summary_model.should_receive(:getState).any_number_of_times.and_return(nil)
      assigns[:stage] = stage_summary_model
      assigns[:current_config_version] = 'current_config_version'
      assigns[:stage] = stage_summary_model
    end

    it "should display message indicating that config is out of date and any actions performed on this page will use the latest config" do
      @stage.should_receive(:getConfigVersion).any_number_of_times.and_return('stage_config_version')
      template.should_receive(:is_config_used_to_run_this_stage_out_of_sync_with_current?).with("current_config_version", "stage_config_version").and_return(true)
      render :inline => '<div>content</div>', :layout => "pipelines"
      response.should have_tag("div.config_changed_info.notification") do
        with_tag("p.information", "Configuration has since been updated and any operations performed will use the current configuration")
      end
    end

    it "should not display message indicating that config is out of date and any actions performed on this page will use the latest config when configuration has not changed since" do
      @stage.should_receive(:getConfigVersion).any_number_of_times.and_return('current_config_version')
      template.should_receive(:is_config_used_to_run_this_stage_out_of_sync_with_current?).with("current_config_version", "current_config_version").and_return(false)
      render :inline => '<div>content</div>', :layout => "pipelines"
      response.should_not have_tag(".notification.config_changed_info") do
        without_tag("p", "Configuration has since been updated and any operations performed will use the current configuration")
      end
    end
  end

  describe "pipline bar" do
    before do
      @first_stage = @stages.get(0)
      assigns[:stage] = @first_stage
      stage = mock('stage')
      stage.should_receive(:getConfigVersion).any_number_of_times.and_return('current_version')
      @first_stage.should_receive(:getStage).any_number_of_times.and_return(stage)
      template.should_receive(:is_config_used_to_run_this_stage_out_of_sync_with_current?).with(anything, anything).and_return(false)
    end

    describe :other_stage_runs do

      it "should add javascript to initialize other stage runs microcontent" do
        assigns[:show_stage_status_bar] = true
        assigns[:stage_history_page] = assigns[:stage_history_page] = last_stage_history_page(1)
        assigns[:stage] = stage_with_three_runs
        template.stub!(:stage_detail_tab_path).and_return("some_tab_path");
        render :inline => '<div>content</div>', :layout => "pipelines"
        response.should have_tag("script[type='text/javascript']", /new\sMicroContentPopup\(\$\('other_stage_runs'\),\snew\sMicroContentPopup\.NoOpHandler\(\)\)/)
      end
    end

    it "should display pipline name and label" do
      render :inline => '<div>content</div>', :layout => "pipelines"
      response.should have_tag ".entity_status_wrapper .entity_title .name a[href='/tab/pipeline/history/pipeline-name']", "pipeline-name"
      response.should have_tag ".entity_status_wrapper .entity_title li", "1"
      response.should have_tag ".entity_status_wrapper .entity_title .last h1", "stage-0"
    end

    it "should show the rss feed link" do
      render :inline => '<div>content</div>', :layout => "pipelines"
      response.body.should have_tag("a[href='/api/pipelines/pipeline-name/stages.xml'] .feed")
    end

    it "should display stage links" do
      params[:stage_name] = "stage-1"
      render :inline => '<div>content</div>', :layout => "pipelines"
      response.should have_tag ".pipeline .stages .stage a.stage_bar[title='stage-0 (Cancelled)'][href='url_to_0']"
      response.should have_tag ".pipeline .stages .selected a.stage_bar[title='stage-1 (Cancelled)'][href='url_to_1']"
    end

    it "should not display stage links for stages not run" do
      params[:stage_name] = "stage-1"
      template.should_not_receive(:stage_detail_url).with(:stage_name => 'blah-stage', :stage_counter => '0')

      @pim.getStageHistory().add(NullStageHistoryItem.new('blah-stage'))

      template.stub!(:stage_detail_tab_path).with(:stage_name => 'stage-0', :stage_counter => "1", :action => 'overview').and_return("url_to_0")
      template.stub!(:stage_detail_tab_path).with(:stage_name => 'stage-1', :stage_counter => "1", :action => 'overview').and_return("url_to_1")

      render :inline => '<div>content</div>', :layout => "pipelines"

      response.should have_tag ".pipeline .stages .stage_bar[title='stage-0 (Cancelled)'][href=url_to_0]"
      response.should have_tag ".pipeline .stages .selected .stage_bar[title='stage-1 (Cancelled)'][href=url_to_1]"
      response.should have_tag ".pipeline .stages div .stage_bar[title='blah-stage (Unknown)']"
    end

    it "should not show rerun link when user does not have operate permission" do
      render :inline => '<div>content</div>', :layout => "pipelines"
      response.should_not have_tag "#stage_bar_1 .operate a"
    end

    describe "run action" do
      before do
        @stage_0 = @stages.get(0)
        @stage_0.setCanRun(true)

        @stage_1 = @stages.get(1)
        @stage_1.setCanRun(false)
        @stage_1.setOperatePermission(true)

        @stage_2 = NullStageHistoryItem.new('blah-stage')
        @stage_2.setCanRun(true)
        @stage_2.setOperatePermission(true)

        @pim.getStageHistory().add(@stage_2)
      end

      it "should show rerun when scheduled and can run" do
        render :inline => '<div>content</div>', :layout => "pipelines"
        response.should have_tag "#stage_bar_0 .action_rerun a", ""
      end

      it "should show trigger when not scheduled but can run" do
        render :inline => '<div>content</div>', :layout => "pipelines"

        response.should have_tag "a#stage_bar_trigger_blah-stage", ""
      end

      it "should show up only when can run" do
        render :inline => '<div>content</div>', :layout => "pipelines"
        response.should_not have_tag "#stage_bar_1 .operate a", /Trigger/
        response.should_not have_tag "#stage_bar_1 .operate a", /Rerun/
      end
    end

    describe "cancel action" do
      before do
        @stage_0 = @stages.get(0)
        @stage_0.getBuildHistory().get(0).setState(JobState::Building)

        @stage_1 = @stages.get(1)
        @stage_1.setCanRun(false)
        @stage_1.setOperatePermission(true)
      end

      it "should show up" do
        render :inline => '<div>content</div>', :layout => "pipelines"
        response.should have_tag "#stage_bar_0 .action_cancel a", ""
      end

      it "should not show up when stage is not running" do
        render :inline => '<div>content</div>', :layout => "pipelines"
        response.should_not have_tag "#stage_bar_1 .operate a", /Cancel/
      end
    end

    describe "lock link" do

      it "should show unlock action if the current pipeline is the locked one" do
        assigns[:pipeline] = @pim
        assigns[:lockedPipeline] = @pim.latestStage().getIdentifier()
        @pim.setCanUnlock(true)
        render :inline => '<div>content</div>', :layout => "pipelines"
        response.should have_tag ".locked .locked_instance a", "Click to unlock"
      end

      it "should show LOCKED if the current pipeline is the locked one and cannot be unlocked" do
        assigns[:pipeline] = @pim
        assigns[:lockedPipeline] = @pim.latestStage().getIdentifier()
        @pim.setCanUnlock(false)
        render :inline => '<div>content</div>', :layout => "pipelines"

        response.should have_tag ".locked .locked_instance span", "LOCKED"
      end

      it "should show UNLOCKED if the current pipeline is lockable but no instance is currently locked" do
        assigns[:pipeline] = @pim
        assigns[:lockedPipeline] = nil
        @pim.setIsLockable(true)
        render :inline => '<div>content</div>', :layout => "pipelines"
        response.should_not have_tag ".locked"
        response.should have_tag ".locked_instance span", "UNLOCKED"
      end

      it "should show link to locked pipeline if current one is not locked" do
        assigns[:pipeline] = @pim
        assigns[:lockedPipeline] = StageIdentifier.new("blah", 1, "cool-bug", "stage", "2")
        @pim.setCanUnlock(true)

        template.stub!(:stage_detail_tab_path).with(:pipeline_name=>"blah", :pipeline_counter=>1, :stage_name=>"stage", :stage_counter=>"2", :action=>"pipeline").and_return("pipeline2")

        render :inline => '<div>content</div>', :layout => "pipelines"
        response.should have_tag ".locked .locked_instance a[href='pipeline2']", "Locked by cool-bug"
      end

      it "should not show link when no instance is locked" do
        render :inline => '<div>content</div>', :layout => "pipelines"
        response.should_not have_tag ".locked .locked_instance"
      end

    end

    describe "graphs tab" do
      it "should not render stage history widget pane" do
        template.stub!(:stage_detail_tab_path).and_return("some_tab_path");
        in_params(:action => "stats")

        render :inline => '<div>content</div>', :layout => "pipelines"

        response.body.should have_tag("div.rail") do
          without_tag ("div#stage_history")
        end
      end
    end
  end



end
