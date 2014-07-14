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

describe "_stage_history.html.erb" do
  include StageModelMother, GoUtil

  before(:each) do
    stages = StageInstanceModels.new
    stages.add(stage_model("dev", "10"))
    @pipeline = PipelineInstanceModel.createPipeline("cruise", -1, "${COUNT}", BuildCause.createManualForced(), stages)
    @pipeline.setCounter(5)

    in_params :pipeline_name => 'cruise', :stage_name => "dev", :pipeline_counter => '5', :stage_counter => '1'

    template.stub(:is_user_an_admin?).and_return(true)

    @stage_history_page = stage_history_page(10)
    template.stub(:stage_detail_tab_path).with(instance_of(Hash)).and_return do |params|
      "url_for_#{params[:pipeline_counter]}_run_#{params[:stage_counter]}"
    end
  end

  it "should show title" do
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    response.should have_tag("h3", "Stage History")
  end

  it "should not show next link if next is nil" do
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    response.should_not have_tag ".stage_history .next"
  end

  it "should show compare link against each run" do
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}


    (1..10).each do |i|
      response.should have_tag("span.compare_pipeline a[href=?]", compare_pipelines_path(:pipeline_name => "cruise", :to_counter => i, :from_counter => 5))
    end
  end

  it "should mark stages having job-rerun differently" do
    @stage_history_page.getStages().each_with_index do |entry, i|
      (i%4 == 0) && entry.setRerunOfCounter(10)
    end
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    (1..10).each do |i|
      response.should have_tag(".stage a[href=url_for_#{i}_run_1]") do |link|
        link.should(((i-2)%4 == 0) ? have_tag("div.color_code_stage.has_rerun_jobs") : have_tag("div.color_code_stage.has_no_rerun_jobs"))
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
      response.should have_tag("span.compare_pipeline a[href=?]", compare_pipelines_path(:pipeline_name => "cruise", :to_counter => i, :from_counter => 8))
    end
  end

  it "should render stage links" do
    template.stub(:tab_aware_path_for_stage) do |id, tab|
      id.getStageLocator() + "/" +tab
    end
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    (1..10).each do |i|
      response.should have_tag ".stage a[href='cruise/#{i}/dev/1/jobs']"
    end
  end

  it "should show current page" do
    page = stage_history_page(55)
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    response.should have_tag ".stage_history .pagination" do
      with_tag "a#stage_history_prev", "prev"
      with_tag "a#stage_history_1", "1"
      with_tag "span", "..."
      with_tag "a#stage_history_4", "4"
      with_tag "a#stage_history_5", "5"
      with_tag "span", "6"
      with_tag "a#stage_history_7", "7"
      with_tag "a#stage_history_8", "8"
      with_tag "span", "..."
      with_tag "a#stage_history_10", "10"
      with_tag "a#stage_history_next", "next"
    end
  end

  it "should wireup page change handlers" do
    page = stage_history_page(55)
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    response.should have_tag("script[type='text/javascript']", /StageHistory\.bindHistoryLink\('#stage_history_prev', '\/history\/stage\/cruise\/5\/dev\/1\?page=5&amp;tab=jobs', 5\);/)
    response.should have_tag("script[type='text/javascript']", /StageHistory\.bindHistoryLink\('#stage_history_5', '\/history\/stage\/cruise\/5\/dev\/1\?page=5&amp;tab=jobs', 5\);/)

    response.should have_tag("script[type='text/javascript']", /StageHistory\.bindHistoryLink\('#stage_history_next', '\/history\/stage\/cruise\/5\/dev\/1\?page=7&amp;tab=jobs', 7\);/)
    response.should have_tag("script[type='text/javascript']", /StageHistory\.bindHistoryLink\('#stage_history_7', '\/history\/stage\/cruise\/5\/dev\/1\?page=7&amp;tab=jobs', 7\);/)

    response.should_not have_tag("script[type='text/javascript']", /StageHistory\.bindHistoryLink\(.*?page=6.*/)
  end

  it "should show the current stage as the first" do
    identifier = @stage_history_page.getStages()[9].getIdentifier()
    @stage_history_page.getStages()[9].setIdentifier(StageIdentifier.new(identifier.getPipelineName(), identifier.getPipelineCounter(), identifier.getPipelineLabel(), identifier.getStageName(), "2"))

    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    response.should have_tag ".stage_history .stage" do
      with_tag ".stage a[href=?].selected.alert .color_code_stage.Passed", 'url_for_5_run_1'
    end

    response.should have_tag ".stage_history .stage" do
      with_tag "a[href=?] .color_code_stage.Passed", 'url_for_10_run_1'
    end
    response.should have_tag ".stage_history .stage .label_counter_wrapper" do
      with_tag ".pipeline_label", "LABEL-10"
    end
    response.should have_tag ".stage_history .stage .label_counter_wrapper" do
      with_tag ".pipeline_label", "LABEL-9"
    end
    response.should have_tag ".stage_history .stage .label_counter_wrapper" do
      with_tag ".pipeline_label", "LABEL-8"
    end
    response.should have_tag ".stage_history .stage" do
      with_tag "a[href=?]", "url_for_1_run_2"
      with_tag ".stage_counter", "(run 2)"
    end
  end

  it "should wrap long pipeline labels" do
    identifier = @stage_history_page.getStages()[9].getIdentifier()
    longPipelineLabel = "a"*20
    @stage_history_page.getStages()[0].setIdentifier(StageIdentifier.new(identifier.getPipelineName(), identifier.getPipelineCounter(), longPipelineLabel, identifier.getStageName(), "2"))
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    response.should have_tag ".stage_history .stage .label_counter_wrapper" do
      with_tag ".pipeline_label" do |label|
        label.should have_tag "wbr"
      end
    end
  end

  it "should not divide stage history instances when config has not changed" do
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => @stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    response.body.should have_tag ".stage_history" do
      without_tag("div.config_change span", "Config Changed")
    end
  end

  it "should divide stage history instances when config has changed and show plain text indication for non-admins" do
    template.stub(:is_user_an_admin?).and_return(false)
    stage_history = stage_history_page(5)
    stage = StageMother.createPassedStage("cruise", 6, "dev", 1, "rspec", org.joda.time.DateTime.new().plus_minutes(10).toDate())
    stage.setConfigVersion("changed-md5-test")
    stage_history.getStages() << StageHistoryEntry.new(stage, 6, nil)
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    response.body.should have_tag ".stage_history" do
      with_tag("div.config_change.counter_6_1 span", "Config Changed")
      without_tag ("div.config_change.counter_6_1 span a")
      (1..5).each do |i|
        without_tag("div.config_change.counter_#{i}_1 span")
      end
    end
  end

  it "should show config changed as a link to open diff only for admin user" do
    template.stub(:is_user_an_admin?).and_return(true)
    stage_history = stage_history_page(5)
    stage = StageMother.createPassedStage("cruise", 6, "dev", 1, "rspec", org.joda.time.DateTime.new().plus_minutes(10).toDate())
    stage.setConfigVersion("changed-md5-test")
    stage_history.getStages() << StageHistoryEntry.new(stage, 6, nil)
    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}
    response.body.should have_tag ".stage_history" do
      with_tag("div.config_change.counter_6_1 span a", "Config Changed")
      with_tag("div.config_change.counter_6_1 span a[onclick=?]", "Modalbox.show('/config_change/between/md5-test/and/changed-md5-test', {overlayClose: false, title: 'Config Changes'})")
      (1..5).each do |i|
        without_tag("div.config_change.counter_#{i}_1 span")
      end
    end
  end

  it "should not show config changed as a link when the stage does not have corresponding md5 associated with it" do
    template.stub(:is_user_an_admin?).and_return(true)
    stage_history = stage_history_page(1)
    stage_history.getStages().last().setConfigVersion(nil)
    stage_history.getStages().first().setConfigVersion("changed-md5")

    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "md5-test"}}

    response.body.should have_tag ".stage_history" do
      with_tag("div.config_change.counter_1_1 span", "Config Changed")
      without_tag ("div.config_change.counter_1_1 span a")
      with_tag("div.config_change.counter_10_1 span a", "Config Changed")
      with_tag("div.config_change.counter_10_1 span a[onclick=?]", "Modalbox.show('/config_change/between/md5-test/and/changed-md5', {overlayClose: false, title: 'Config Changes'})")
    end
  end

  it "should show config changed as a link when config has changed between top of this page and bottom of previous page" do
    template.stub(:is_user_an_admin?).and_return(true)
    stage_history_page = mock('stage_history_page')
    bottom_of_previous_page = mock('bottom_of_last_page')
    bottom_of_previous_page.should_receive(:getConfigVersion).and_return('some-old-config-version-older-than-current-but-newer-than-top-of-this-page-config-version')

    top_of_this_page = mock('top_of_this_page')
    top_of_this_page.should_receive(:getConfigVersion).exactly(3).times.and_return('some-old-config-version')
    top_of_this_page.should_receive(:getIdentifier).exactly(7).times.and_return(StageIdentifier.new('p1', java.lang.Integer.new(1), 'label1', 'stage', '1'))
    top_of_this_page.should_receive(:getState).twice.and_return(StageState::Building)
    top_of_this_page.should_receive(:hasRerunJobs).twice.and_return(false)

    stage_history_page.should_receive(:getImmediateChronologicallyForwardStageHistoryEntry).and_return(bottom_of_previous_page)
    stage_history_page.should_receive(:getStages).and_return([top_of_this_page])
    stage_history_page.should_receive(:getPagination).and_return(Pagination::ONE_ITEM)

    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "current-config-version"}}

    response.body.should have_tag ".stage_history" do
      with_tag("span a", "Config Changed")
    end
  end

  it "should not show config changed as a link when config has not changed between top of this page and bottom of previous page" do
    template.stub(:is_user_an_admin?).and_return(true)
    stage_history_page = mock('stage_history_page')
    bottom_of_previous_page = mock('bottom_of_last_page')
    bottom_of_previous_page.should_receive(:getConfigVersion).and_return('some-old-config-version')

    top_of_this_page = mock('top_of_this_page')
    top_of_this_page.should_receive(:getConfigVersion).twice.and_return('some-old-config-version')
    top_of_this_page.should_receive(:getIdentifier).exactly(5).times.and_return(StageIdentifier.new('p1', java.lang.Integer.new(1), 'label1', 'stage', '1'))
    top_of_this_page.should_receive(:getState).twice.and_return(StageState::Building)
    top_of_this_page.should_receive(:hasRerunJobs).twice.and_return(false)

    stage_history_page.should_receive(:getImmediateChronologicallyForwardStageHistoryEntry).and_return(bottom_of_previous_page)
    stage_history_page.should_receive(:getStages).and_return([top_of_this_page])
    stage_history_page.should_receive(:getPagination).and_return(Pagination::ONE_ITEM)

    render :partial => "stages/stage_history", :locals => {:scope => {:stage_history_page => stage_history_page, :tab => 'jobs', :current_stage_pipeline => @pipeline, :current_config_version => "current-config-version"}}

    response.body.should have_tag ".stage_history" do
      without_tag("span a", "Config Changed")
    end
  end
end