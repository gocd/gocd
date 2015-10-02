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

describe "/comparison/_pipeline_autocomplete_list_entry.html.erb" do

  include StageModelMother

  before :each do
    @hg_revisions = ModificationsMother.createHgMaterialRevisions()
    @pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithModifications(@hg_revisions, "user"), stage_history_for("dev", "prod"))
    view.stub(:go_config_service).and_return(@go_config_service = double("go config service"))
  end

  it "should display pipeline counter and its details" do
    @go_config_service.should_receive(:getCommentRendererFor).with("some_pipeline").exactly(2).times.and_return(TrackingTool.new("http://foo/${ID}", "\\d+"))

    render :partial => "pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => @pipeline}}

    Capybara.string(response.body).find("div.pipeline").tap do |pipeline|
      expect(pipeline.find("div.pipeline_counter")).to have_selector("h3", "some-label")
      expect(pipeline).to have_selector("div[class='stage_bar Passed'][title='dev (Passed)']")
      expect(pipeline).to have_selector("div[class='stage_bar Passed'][title='prod (Passed)']")
      pipeline.find("div.pipeline_details table").tap do |table|
        expect(table).to have_selector("tr td.label", :text => "Revision:")
        expect(table).to have_selector("tr td", :text => "9fdcf27f16eadc362733328dd481d8a2c29915e1")
        expect(table).to have_selector("tr td.label", :text => "Comment:")
        expect(table).to have_selector("tr td", :text => "comment2")
        table.all("tr").tap do |all_rows|
          expect(all_rows[2]).to have_selector("td.label", :text => Nokogiri::HTML("Modified&nbsp;by:").text)
          expect(all_rows[6]).to have_selector("td.label", :text => Nokogiri::HTML("Modified&nbsp;by:").text)
        end
        expect(table).to have_selector("tr td", :text => /user2/)
      end
    end
  end

  it "should display pipeline counter and its details for package material with trackback url" do
    package_revision = ModificationsMother.createPackageMaterialRevision("1234", nil, '{"TYPE":"PACKAGE_MATERIAL","COMMENT":"Built on blrstdgobgr03.","TRACKBACK_URL":"/go/tab/build/detail/go-packages/244/dist/2/rpm"}')
    package_revisions = MaterialRevisions.new([package_revision].to_java(com.thoughtworks.go.domain.MaterialRevision))
    pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithModifications(package_revisions, "user"), stage_history_for("dev", "prod"))

    render :partial => "pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => pipeline}}

    Capybara.string(response.body).find("div.pipeline").tap do |pipeline|
      expect(pipeline.find("div.pipeline_counter")).to have_selector("h3", "some-label")
      expect(pipeline).to have_selector("div[class='stage_bar Passed'][title='dev (Passed)']")
      expect(pipeline).to have_selector("div[class='stage_bar Passed'][title='prod (Passed)']")
      pipeline.find("div.pipeline_details table").tap do |table|
        expect(table).to have_selector("tr td.label", :text => "Revision:")
        expect(table).to have_selector("tr td", :text => "1234")
        expect(table).to have_selector("tr td.label", :text => "Comment:")
        expect(table).to have_selector("tr td.comment", :text => "Built on blrstdgobgr03.Trackback: /go/tab/build/detail/go-packages/244/dist/2/rpm")
        expect(table).to have_selector("tr td.label", :text => Nokogiri::HTML("Modified&nbsp;by:").text)
      end
    end
  end

  it "should display pipeline counter and its details for package material without trackback and no comment" do
    package_revision = ModificationsMother.createPackageMaterialRevision("1234", nil, '{"TYPE":"PACKAGE_MATERIAL"}')
    package_revisions = MaterialRevisions.new([package_revision].to_java(com.thoughtworks.go.domain.MaterialRevision))
    pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithModifications(package_revisions, "user"), stage_history_for("dev", "prod"))

    render :partial => "pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => pipeline}}

    Capybara.string(response.body).find("div.pipeline").tap do |pipeline|
      expect(pipeline.find("div.pipeline_counter")).to have_selector("h3", "some-label")
      expect(pipeline).to have_selector("div[class='stage_bar Passed'][title='dev (Passed)']")
      expect(pipeline).to have_selector("div[class='stage_bar Passed'][title='prod (Passed)']")
      pipeline.find("div.pipeline_details table").tap do |table|
        expect(table).to have_selector("tr td.label", :text => "Revision:")
        expect(table).to have_selector("tr td", :text => "1234")
        expect(table).to have_selector("tr td.label", :text => "Comment:")
        expect(table).to have_selector("tr td.comment", :text => "Trackback: Not Provided")
        expect(table).to have_selector("tr td.label", :text => Nokogiri::HTML("Modified&nbsp;by:").text)
      end
    end
  end

  it "should skip modifications that do not match" do
    params[:q] = "foo"

    render :partial => "pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => @pipeline}}

    Capybara.string(response.body).find("div.pipeline").tap do |pipeline|
      expect(pipeline.find("div.pipeline_counter")).to have_selector("h3", "some-label")
      expect(pipeline).to have_selector("div[class='stage_bar Passed'][title='dev (Passed)']")
      expect(pipeline).to have_selector("div[class='stage_bar Passed'][title='prod (Passed)']")
    end

    expect(Capybara.string(response.body).all(:xpath, "div.modifications").count).to eq(0)
  end

  it "should render the stage bar" do
    params[:q] = "915"
    modified_time = java.util.Date.new
    revisions = MaterialRevisions.new(
        [MaterialRevision.new(MaterialsMother.filteredHgMaterial("foo"),
                              [Modification.new("user-915-1", "comment-915", "some email", modified_time, "revision-9151")])
        ].to_java(MaterialRevision))

    stages = stage_history_for("dev", "prod")
    stages.add(stage_model_with_unknown_state("new_stage"))
    @pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 19151, "some-915-label", BuildCause.createManualForced(revisions, Username.new(CaseInsensitiveString.new("user-915"))), stages)
    @go_config_service.should_receive(:getCommentRendererFor).with("some_pipeline").once.times.and_return(TrackingTool.new("http://foo/${ID}", "\\d+"))

    render :partial => "pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => @pipeline}}

    capybara_string = Capybara.string(response.body)

    capybara_string.find("a[href='#{stage_detail_tab_path(:pipeline_name => 'some_pipeline',
                                                      :pipeline_counter => 19151,
                                                      :stage_name => 'dev',
                                                      :stage_counter => 35)}']").tap do |link|
      expect(link).to have_selector("div[class='stage_bar Passed'][title='dev (Passed)']")
    end
    capybara_string.find("a[href='#{stage_detail_tab_path(:pipeline_name => 'some_pipeline',
                                                      :pipeline_counter => 19151,
                                                      :stage_name => 'prod',
                                                      :stage_counter => 35)}']").tap do |link|
      expect(link).to have_selector("div[class='stage_bar Passed'][title='prod (Passed)']")
    end
    capybara_string.all("div.stage").tap do |stages|
      expect(stages[1]).to have_selector("div[class='stage_bar Passed'][title='prod (Passed)']")
    end
  end

  it "should highlight matches" do
    params[:q] = "915"
    modified_time = java.util.Date.new
    revisions = MaterialRevisions.new(
        [MaterialRevision.new(MaterialsMother.filteredHgMaterial("foo"),
                              [Modification.new("user-915-1", "comment-915", "some email", modified_time, "revision-9151")])
        ].to_java(MaterialRevision))
    @pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 19151, "some-915-label", BuildCause.createManualForced(revisions, Username.new(CaseInsensitiveString.new("user-915"))), stage_history_for("dev", "prod"))
    @go_config_service.should_receive(:getCommentRendererFor).with("some_pipeline").once.times.and_return(TrackingTool.new("http://foo/${ID}", "\\d+"))

    render :partial => "pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => @pipeline}}

    Capybara.string(response.body).find("div.pipeline").tap do |pipeline|
      expect(pipeline.find("div.pipeline_counter")).to have_selector("h3", "some-915-label")
      pipeline.find("div.pipeline_details table").tap do |table|
        expect(table).to have_selector("tr td.label", :text => "Revision:")
        expect(table).to have_selector("tr td", :text => "revision-9151")
        expect(table).to have_selector("tr td mark", :text => "915")
        expect(table).to have_selector("tr td.label", :text => "Comment:")
        expect(table).to have_selector("tr td", :text => "comment-915")
        expect(table).to have_selector("tr td.comment mark", :text => "915")
        expect(table).to have_selector("tr td.label", :text => Nokogiri::HTML("Modified&nbsp;by:").text)
        expect(table).to have_selector("tr td", :text => "user-915-1 on #{modified_time.to_long_display_date_time()}")
        expect(table).to have_selector("tr td mark", :text => "915")
      end
    end
  end

  it "should render stage_bar html" do
    @go_config_service.stub(:getCommentRendererFor).with("some_pipeline").and_return(com.thoughtworks.go.config.TrackingTool.new())
    stub_template "_stage_bar.html.erb" => "STAGE BAR"

    render :partial => "pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => @pipeline, :disable_stage_bar_href => true}}

    assert_template partial: "_stage_bar.html.erb", locals: {scope: {pipeline: @pipeline, disable_stage_bar_href: true}}
    expect(rendered).to match("STAGE BAR")
  end
end
