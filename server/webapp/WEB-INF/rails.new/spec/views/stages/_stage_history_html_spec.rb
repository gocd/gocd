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

require 'spec_helper'

describe "_stage_history.html.erb" do
  include StageModelMother, GoUtil

  before(:each) do
    stages = StageInstanceModels.new
    stages.add(stage_model("dev", "10"))
    @pipeline = PipelineInstanceModel.createPipeline("cruise", -1, "${COUNT}", BuildCause.createManualForced(), stages)
    @pipeline.setCounter(5)

    in_params :pipeline_name => 'cruise', :stage_name => "dev", :pipeline_counter => '5', :stage_counter => '1'

    view.stub(:is_user_an_admin?).and_return(true)

    @stage_history_page = stage_history_page(10)
    view.stub(:stage_detail_tab_path).with(instance_of(Hash)).and_return do |params|
      "url_for_#{params[:pipeline_counter]}_run_#{params[:stage_counter]}"
    end
  end

  it "should show title" do
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    expect(response).to have_selector("h3", :text => "Stage History")
  end

  it "should not show next link if next is nil" do
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    expect(response).to_not have_selector ".stage_history .next"
  end

  it "should show compare link against each run" do
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}


    (1..10).each do |i|
      expect(response).to have_selector("span.compare_pipeline a[href='#{compare_pipelines_path(:pipeline_name => "cruise", :to_counter => i, :from_counter => 5)}']")
    end
  end

  it "should mark stages having job-rerun differently" do
    @stage_history_page.getStages().each_with_index do |entry, i|
      (i%4 == 0) && entry.setRerunOfCounter(10)
    end
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    (1..10).each do |i|
      Capybara.string(response.body).find(".stage a[href='url_for_#{i}_run_1']").tap do |link|
        expect(link).to(((i-2)%4 == 0) ? have_selector("div.color_code_stage.has_rerun_jobs") : have_selector("div.color_code_stage.has_no_rerun_jobs"))
      end
    end
  end


  it "should show compare link even if pipeline is a bisect" do
    stages = StageInstanceModels.new
    stages.add(stage_model("dev", "1"))
    pipeline = PipelineInstanceModel.createPipeline("cruise", -1, "${COUNT}", BuildCause.createManualForced(), stages)
    pipeline.setCounter(8)
    pipeline.setNaturalOrder(1.5) #This implies the pipeline is a bisect

    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :current_stage_pipeline => pipeline, :tab => "jobs", :current_config_version => "md5-test"}}

    (1..10).each do |i|
      expect(response).to have_selector("span.compare_pipeline a[href='#{compare_pipelines_path(:pipeline_name => "cruise", :to_counter => i, :from_counter => 8)}']")
    end
  end

  it "should render stage links" do
    view.stub(:tab_aware_path_for_stage) do |id, tab|
      id.getStageLocator() + "/" +tab
    end
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    (1..10).each do |i|
      expect(response).to have_selector ".stage a[href='cruise/#{i}/dev/1/jobs']"
    end
  end

  it "should show current page" do
    page = stage_history_page(55)
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}


    Capybara.string(response.body).find(".stage_history .pagination").tap do |f|
      expect(f).to have_selector "a#stage_history_prev", :text => "prev"
      expect(f).to have_selector "a#stage_history_1", :text => "1"
      expect(f).to have_selector "span", :text => "..."
      expect(f).to have_selector "a#stage_history_4", :text => "4"
      expect(f).to have_selector "a#stage_history_5", :text => "5"
      expect(f).to have_selector "span", "6"
      expect(f).to have_selector "a#stage_history_7", :text => "7"
      expect(f).to have_selector "a#stage_history_8", :text => "8"
      expect(f).to have_selector "span", :text => "..."
      expect(f).to have_selector "a#stage_history_10", :text => "10"
      expect(f).to have_selector "a#stage_history_next", :text => "next"
    end
  end

  it "should wireup page change handlers" do
    page = stage_history_page(55)
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    Capybara.string(response.body).all("script[type='text/javascript']", :visible => false).tap do |script_tag|
      expect(script_tag[0].text).to include "StageHistory.bindHistoryLink('#stage_history_prev', '/history/stage/cruise/5/dev/1?page=5&tab=jobs', 5);"
      expect(script_tag[3].text).to include "StageHistory.bindHistoryLink('#stage_history_5', '/history/stage/cruise/5/dev/1?page=5&tab=jobs', 5);"
      expect(script_tag[7].text).to include "StageHistory.bindHistoryLink('#stage_history_next', '/history/stage/cruise/5/dev/1?page=7&tab=jobs', 7);"
      expect(script_tag[4].text).to include "StageHistory.bindHistoryLink('#stage_history_7', '/history/stage/cruise/5/dev/1?page=7&tab=jobs', 7);"
      (0..script_tag.count()-1).each do |i|
        expect(script_tag[i].text).to_not include "StageHistory.bindHistoryLink(.*?page=6.*"
      end
    end
  end

  it "should show the current stage as the first" do
    identifier = @stage_history_page.getStages()[9].getIdentifier()
    @stage_history_page.getStages()[9].setIdentifier(StageIdentifier.new(identifier.getPipelineName(), identifier.getPipelineCounter(), identifier.getPipelineLabel(), identifier.getStageName(), "2"))

    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    Capybara.string(response.body).find(".stage_history .stage a[href='url_for_5_run_1'].selected.alert").tap do |f|
      expect(f).to have_selector  ".color_code_stage.Passed"
      expect(f).to have_selector  ".pipeline_label", :text => "LABEL-5"
    end

    Capybara.string(response.body).find(".stage_history .stage a[href='url_for_10_run_1']").tap do |f|
      expect(f).to have_selector  ".color_code_stage.Passed"
      expect(f).to have_selector  ".pipeline_label", :text => "LABEL-10"
    end

    Capybara.string(response.body).find(".stage_history .stage a[href='url_for_1_run_2']").tap do |f|
      expect(f).to have_selector  ".color_code_stage.Passed"
      expect(f).to have_selector  ".pipeline_label", :text => "LABEL-1"
      expect(f).to have_selector ".stage_counter", :text => "(run 2)"
    end
  end

  it "should wrap long pipeline labels" do
    identifier = @stage_history_page.getStages()[9].getIdentifier()
    longPipelineLabel = "a"*20
    @stage_history_page.getStages()[0].setIdentifier(StageIdentifier.new(identifier.getPipelineName(), identifier.getPipelineCounter(), longPipelineLabel, identifier.getStageName(), "2"))
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    Capybara.string(response.body).all(".stage_history .stage .label_counter_wrapper .pipeline_label").tap do |labels|
      (0..labels.count()-1).each do |i|
        expect(labels[i]).to have_selector "wbr"
      end
    end
  end

  it "should not divide stage history instances when config has not changed" do
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    Capybara.string(response.body).find(".stage_history").tap do |f|
      expect(f).to_not have_selector "div.config_change span", :text =>  "Config Changed"
    end
  end

  it "should divide stage history instances when config has changed and show plain text indication for non-admins" do
    view.stub(:is_user_an_admin?).and_return(false)
    stage_history = stage_history_page(5)
    stage = StageMother.createPassedStage("cruise", 6, "dev", 1, "rspec", org.joda.time.DateTime.new().plus_minutes(10).toDate())
    stage.setConfigVersion("changed-md5-test")
    stage_history.getStages() << StageHistoryEntry.new(stage, 6, nil)
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    Capybara.string(response.body).find(".stage_history").tap do |f|
      expect(f).to have_selector("div.config_change.counter_6_1 span", :text => "Config Changed")
      expect(f).to_not have_selector("div.config_change.counter_6_1 span a")
      (1..5).each do |i|
        expect(f).to_not have_selector("div.config_change.counter_#{i}_1 span")
      end
    end
  end

  it "should show config changed as a link to open diff only for admin user" do
    view.stub(:is_user_an_admin?).and_return(true)
    stage_history = stage_history_page(5)
    stage = StageMother.createPassedStage("cruise", 6, "dev", 1, "rspec", org.joda.time.DateTime.new().plus_minutes(10).toDate())
    stage.setConfigVersion("changed-md5-test")
    stage_history.getStages() << StageHistoryEntry.new(stage, 6, nil)
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    Capybara.string(response.body).find(".stage_history").tap do |f|
      expect(f).to have_selector("div.config_change.counter_6_1 span a", :text => "Config Changed")
      expect(f).to have_selector("div.config_change.counter_6_1 span a[onclick=\"Modalbox.show('/config_change/between/md5-test/and/changed-md5-test', {overlayClose: false, title: 'Config Changes'})\"]")
      (1..5).each do |i|
        expect(f).to_not have_selector("div.config_change.counter_#{i}_1 span")
      end
    end
  end

  it "should not show config changed as a link when the stage does not have corresponding md5 associated with it" do
    view.stub(:is_user_an_admin?).and_return(true)
    stage_history = stage_history_page(1)
    stage_history.getStages().last().setConfigVersion(nil)
    stage_history.getStages().first().setConfigVersion("changed-md5")

    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    Capybara.string(response.body).find(".stage_history").tap do |f|
      expect(f).to have_selector("div.config_change.counter_1_1 span", :text => "Config Changed")
      expect(f).to_not have_selector ("div.config_change.counter_1_1 span a")
      expect(f).to have_selector("div.config_change.counter_10_1 span a", :text => "Config Changed")
      expect(f).to have_selector("div.config_change.counter_10_1 span a[onclick=\"Modalbox.show('/config_change/between/md5-test/and/changed-md5', {overlayClose: false, title: 'Config Changes'})\"]")
    end
  end

  it "should show config changed as a link when config has changed between top of this page and bottom of previous page" do
    view.stub(:is_user_an_admin?).and_return(true)
    stage_history_page = double('stage_history_page')
    bottom_of_previous_page = double('bottom_of_last_page')
    bottom_of_previous_page.should_receive(:getConfigVersion).and_return('some-old-config-version-older-than-current-but-newer-than-top-of-this-page-config-version')

    top_of_this_page = double('top_of_this_page')
    top_of_this_page.should_receive(:getConfigVersion).exactly(3).times.and_return('some-old-config-version')
    top_of_this_page.should_receive(:getIdentifier).exactly(7).times.and_return(StageIdentifier.new('p1', java.lang.Integer.new(1), 'label1', 'stage', '1'))
    top_of_this_page.should_receive(:getState).twice.and_return(StageState::Building)
    top_of_this_page.should_receive(:hasRerunJobs).twice.and_return(false)

    stage_history_page.should_receive(:getImmediateChronologicallyForwardStageHistoryEntry).and_return(bottom_of_previous_page)
    stage_history_page.should_receive(:getStages).and_return([top_of_this_page])
    stage_history_page.should_receive(:getPagination).and_return(Pagination::ONE_ITEM)

    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "current-config-version"}}

    Capybara.string(response.body).find(".stage_history").tap do |f|
      expect(f).to have_selector("span a", :text => "Config Changed")
    end
  end

  it "should not show config changed as a link when config has not changed between top of this page and bottom of previous page" do
    view.stub(:is_user_an_admin?).and_return(true)
    stage_history_page = double('stage_history_page')
    bottom_of_previous_page = double('bottom_of_last_page')
    bottom_of_previous_page.should_receive(:getConfigVersion).and_return('some-old-config-version')

    top_of_this_page = double('top_of_this_page')
    top_of_this_page.should_receive(:getConfigVersion).twice.and_return('some-old-config-version')
    top_of_this_page.should_receive(:getIdentifier).exactly(5).times.and_return(StageIdentifier.new('p1', java.lang.Integer.new(1), 'label1', 'stage', '1'))
    top_of_this_page.should_receive(:getState).twice.and_return(StageState::Building)
    top_of_this_page.should_receive(:hasRerunJobs).twice.and_return(false)

    stage_history_page.should_receive(:getImmediateChronologicallyForwardStageHistoryEntry).and_return(bottom_of_previous_page)
    stage_history_page.should_receive(:getStages).and_return([top_of_this_page])
    stage_history_page.should_receive(:getPagination).and_return(Pagination::ONE_ITEM)

    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "current-config-version"}}

    Capybara.string(response.body).find(".stage_history").tap do |f|
      expect(f).to_not have_selector("span a", :text => "Config Changed")
    end
  end
end
