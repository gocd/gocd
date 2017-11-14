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

require 'rails_helper'
require_relative 'layout_html_examples'

describe "layouts/pipelines.html.eb" do
  include GoUtil
  include StageModelMother

  before do
    @layout_name = 'layouts/pipelines'
    @user = Username.new(CaseInsensitiveString.new("blah-name"), "blah diaply name")
    assign(:user, @user)
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
    assign(:pipeline,@pim)
    params[:pipeline_name] = "cruise"
    params[:pipeline_counter] = "1"
    @request.path_parameters.reverse_merge!(params)
    allow(view).to receive(:can_view_admin_page?).and_return(true)
    view.extend PipelinesHelper
    view.extend ApplicationHelper
    view.extend StagesHelper
    class << view
      def url_for_with_stub(*args)
        args.empty? ? "/go/" : url_for_without_stub(*args)
      end

      alias_method_chain :url_for, :stub
    end

    in_params :action => 'overview', :controller => "stages"
    allow(view).to receive(:stage_history_path).and_return("historical_stage_page_number")
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    allow(view).to receive(:config_change_path)
  end

  describe "stage configuration out of sync notification" do

    before :each do
      stage_summary_model = double('stage_summary_model')
      @stage = double('stage')
      allow(stage_summary_model).to receive(:getStage).and_return(@stage)
      allow(stage_summary_model).to receive(:getName).and_return('stage-0')
      allow(stage_summary_model).to receive(:getState).and_return(nil)
      assign(:stage,stage_summary_model)
      assign(:current_config_version,'current_config_version')
      assign(:stage,stage_summary_model)
    end

    it "should display message indicating that config is out of date and any actions performed on this page will use the latest config" do
      allow(@stage).to receive(:getConfigVersion).and_return('stage_config_version')
      allow(view).to receive(:is_config_used_to_run_this_stage_out_of_sync_with_current?).with("current_config_version", "stage_config_version").and_return(true)
      render :inline => '<div>content</div>', :layout=>@layout_name
      Capybara.string(response.body).find("div.config_changed_info.notification").tap do |div|
        expect(div).to have_selector("p.information", :text=>"Configuration has since been updated and any operations performed will use the current configuration")
      end
    end

    it "should not display message indicating that config is out of date and any actions performed on this page will use the latest config when configuration has not changed since" do
      allow(@stage).to receive(:getConfigVersion).and_return('current_config_version')
      allow(view).to receive(:is_config_used_to_run_this_stage_out_of_sync_with_current?).with("current_config_version", "current_config_version").and_return(false)
      render :inline => '<div>content</div>', :layout=>@layout_name
      expect(response.body).to_not have_selector(".notification.config_changed_info p", :text=>"Configuration has since been updated and any operations performed will use the current configuration")
    end
  end

  describe "pipline bar" do
    before do
      @first_stage = StageSummaryModel.new(StageMother.scheduledStage("pipeline-name", 1, "stage-0", 1, "job"), Stages.new, JobDurationStrategy.ALWAYS_ZERO, nil)
      assign(:stage, @first_stage)
      stage = double('stage')
      allow(stage).to receive(:getConfigVersion).and_return('current_version')
      allow(view).to receive(:is_config_used_to_run_this_stage_out_of_sync_with_current?).with(anything, anything).and_return(false)
    end

    describe "other_stage_runs" do

      it "should add javascript to initialize other stage runs microcontent" do
        assign(:show_stage_status_bar,true)
        assign(:stage_history_page,last_stage_history_page(1))
        assign(:stage,stage_with_three_runs)
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to have_selector("script[type='text/javascript']", :text=>/new\sMicroContentPopup\(\$\('other_stage_runs'\),\snew\sMicroContentPopup\.NoOpHandler\(\)\)/, :visible=>false)
      end
    end

    it "should display pipline name and label" do
      render :inline => '<div>content</div>', :layout=>@layout_name
      expect(response.body).to have_selector(".entity_status_wrapper .entity_title .name a[href='/tab/pipeline/history/pipeline-name']", :text=>"pipeline-name")
      expect(response.body).to have_selector(".entity_status_wrapper .entity_title li", :text=>"1")
      expect(response.body).to have_selector(".entity_status_wrapper .entity_title .last h1", :text=>"stage-0")
    end

    it "should show the rss feed link" do
      render :inline => '<div>content</div>', :layout=>@layout_name
      expect(response.body).to have_selector("a[href='/api/pipelines/pipeline-name/stages.xml'] .feed")
    end

    it "should display stage links" do
      params[:stage_name] = "stage-1"
      render :inline => '<div>content</div>', :layout=>@layout_name
      expect(response.body).to have_selector(".pipeline .stages .stage a.stage_bar[title='stage-0 (Cancelled)'][href='/pipelines/pipeline/1/stage-0/1/overview']")
      expect(response.body).to have_selector(".pipeline .stages .selected a.stage_bar[title='stage-1 (Cancelled)'][href='/pipelines/pipeline/1/stage-1/1/overview']")
    end

    it "should not display stage links for stages not run" do
      params[:stage_name] = "stage-1"
      @pim.getStageHistory().add(NullStageHistoryItem.new('blah-stage'))
      render :inline => '<div>content</div>', :layout=>@layout_name

      expect(response.body).to have_selector(".pipeline .stages .stage_bar[title='stage-0 (Cancelled)'][href='/pipelines/pipeline/1/stage-0/1/overview']")
      expect(response.body).to have_selector(".pipeline .stages .selected .stage_bar[title='stage-1 (Cancelled)'][href='/pipelines/pipeline/1/stage-1/1/overview']")
      expect(response.body).to have_selector(".pipeline .stages div .stage_bar[title='blah-stage (Unknown)']")
    end

    it "should not show rerun link when user does not have operate permission" do
      render :inline => '<div>content</div>', :layout=>@layout_name
      expect(response.body).to_not have_selector("#stage_bar_1 .operate a")
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
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to have_selector("#stage_bar_0 .action_rerun a", :text=>"")
      end

      it "should show trigger when not scheduled but can run" do
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to have_selector("a#stage_bar_trigger_blah-stage", :text=>"")
      end

      it "should show up only when can run" do
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to_not have_selector("#stage_bar_1 .operate a", :text=>/Trigger/)
        expect(response.body).to_not have_selector("#stage_bar_1 .operate a", :text=>/Return/)
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
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to have_selector("#stage_bar_0 .action_cancel a", :text=>"")
      end

      it "should not show up when stage is not running" do
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to_not have_selector("#stage_bar_1 .operate a", :text=>/Cancel/)
      end
    end

    describe "lock link" do

      it "should show unlock action if the current pipeline is the locked one" do
        assign(:pipeline,@pim)
        assign(:lockedPipeline,@pim.latestStage().getIdentifier())
        @pim.setCanUnlock(true)
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to have_selector(".locked .locked_instance a", :text=>"Click to unlock")
      end

      it "should show LOCKED if the current pipeline is the locked one and cannot be unlocked" do
        assign(:pipeline,@pim)
        assign(:lockedPipeline,@pim.latestStage().getIdentifier())
        @pim.setCanUnlock(false)
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to have_selector(".locked .locked_instance span", :text=>"LOCKED")
      end

      it "should show UNLOCKED if the current pipeline is lockable but no instance is currently locked" do
        assign(:pipeline,@pim)
        assign(:lockedPipeline,nil)
        @pim.setIsLockable(true)
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to_not have_selector(".locked")
        expect(response.body).to have_selector(".locked_instance span", :text=>"UNLOCKED")
      end

      it "should show link to locked pipeline if current one is not locked" do
        assign(:pipeline,@pim)
        assign(:lockedPipeline,StageIdentifier.new("blah", 1, "cool-bug", "stage", "2"))
        @pim.setCanUnlock(true)

        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to have_selector(".locked .locked_instance a[href='/pipelines/blah/1/stage/2/pipeline']", :text=>"Locked by cool-bug")
      end

      it "should not show link when no instance is locked" do
        render :inline => '<div>content</div>', :layout=>@layout_name
        expect(response.body).to_not have_selector(".locked .locked_instance")
      end

    end

    describe "graphs tab" do
      it "should not render stage history widget pane" do
        in_params(:action => "stats")

        render :inline => '<div>content</div>', :layout=>@layout_name

        Capybara.string(response.body).find("div.rail").tap do |div|
          expect(div).to_not have_selector("div#stage_history")
        end
      end
    end
  end
end
