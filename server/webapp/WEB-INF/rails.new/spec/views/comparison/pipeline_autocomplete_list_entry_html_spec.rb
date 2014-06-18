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

require File.join(File.dirname(__FILE__), "/../../spec_helper")

describe "pipeline_autocomplete_list_entry.html.erb" do

  include StageModelMother

  before :each do
    @hg_revisions = ModificationsMother.createHgMaterialRevisions()
    @pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithModifications(@hg_revisions, "user"), stage_history_for("dev", "prod"))
    template.stub(:go_config_service).and_return(@go_config_service = mock("go config service"))
  end

  it "should display pipeline counter and its details" do
    @go_config_service.should_receive(:getCommentRendererFor).with("some_pipeline").exactly(2).times.and_return(TrackingTool.new("http://foo/${ID}", "\\d+"))

    render :partial => "comparison/pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => @pipeline}}

    response.body.should have_tag("div.pipeline_counter h3", "some-label")
    response.body.should have_tag("div[class='stage_bar Passed'][title='dev (Passed)']")
    response.body.should have_tag("div[class='stage_bar Passed'][title='prod (Passed)']")
    response.body.should have_tag("div.pipeline_details table") do
      with_tag("tr") do
        with_tag("td.label", "Revision:")
        with_tag("td", "9fdcf27f16eadc362733328dd481d8a2c29915e1")
      end
      with_tag("tr") do
        with_tag("td.label", "Comment:")
        with_tag("td", "comment2")
      end
      with_tag("tr") do
        with_tag("td.label", "Modified&nbsp;by:")
        with_tag("td", /user2/)
      end
    end
  end

  it "should display pipeline counter and its details for package material with trackback url" do
    package_revision = ModificationsMother.createPackageMaterialRevision("1234", nil, '{"TYPE":"PACKAGE_MATERIAL","COMMENT":"Built on blrstdgobgr03.","TRACKBACK_URL":"/go/tab/build/detail/go-packages/244/dist/2/rpm"}')
    package_revisions = MaterialRevisions.new([package_revision].to_java(com.thoughtworks.go.domain.MaterialRevision))
    pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithModifications(package_revisions, "user"), stage_history_for("dev", "prod"))

    render :partial => "comparison/pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => pipeline}}

    response.body.should have_tag("div.pipeline_counter h3", "some-label")
    response.body.should have_tag("div[class='stage_bar Passed'][title='dev (Passed)']")
    response.body.should have_tag("div[class='stage_bar Passed'][title='prod (Passed)']")
    response.body.should have_tag("div.pipeline_details table") do
      with_tag("tr") do
        with_tag("td.label", "Revision:")
        with_tag("td", "1234")
      end
      with_tag("tr") do
        with_tag("td.label", "Comment:")
        with_tag("td.comment", "Built on blrstdgobgr03.Trackback: /go/tab/build/detail/go-packages/244/dist/2/rpm")
      end
      with_tag("tr") do
        with_tag("td.label", "Modified&nbsp;by:")
      end
    end
  end

  it "should display pipeline counter and its details for package material without trackback and no comment" do
    package_revision = ModificationsMother.createPackageMaterialRevision("1234", nil, '{"TYPE":"PACKAGE_MATERIAL"}')
    package_revisions = MaterialRevisions.new([package_revision].to_java(com.thoughtworks.go.domain.MaterialRevision))
    pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithModifications(package_revisions, "user"), stage_history_for("dev", "prod"))

    render :partial => "comparison/pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => pipeline}}
    response.body.should have_tag("div.pipeline_counter h3", "some-label")
    response.body.should have_tag("div[class='stage_bar Passed'][title='dev (Passed)']")
    response.body.should have_tag("div[class='stage_bar Passed'][title='prod (Passed)']")
    response.body.should have_tag("div.pipeline_details table") do
      with_tag("tr") do
        with_tag("td.label", "Revision:")
        with_tag("td", "1234")
      end
      with_tag("tr") do
        with_tag("td.label", "Comment:")
        with_tag("td.comment", "Trackback: Not Provided")
      end
      with_tag("tr") do
        with_tag("td.label", "Modified&nbsp;by:")
      end
    end
  end

  it "should skip modifications that do not match" do
    params[:q] = "foo"

    render :partial => "comparison/pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => @pipeline}}

    response.body.should have_tag("div.pipeline_counter h3", "some-label")
    response.body.should have_tag("div[class='stage_bar Passed'][title='dev (Passed)']")
    response.body.should have_tag("div[class='stage_bar Passed'][title='prod (Passed)']")
    response.body.should_not have_tag("div.modifications")
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

    render :partial => "comparison/pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => @pipeline}}

    response.body.should have_tag("a[href='#{stage_detail_path(:pipeline_name => 'some_pipeline',
                                                               :pipeline_counter => 19151,
                                                               :stage_name => 'dev',
                                                               :stage_counter => 35)}']") do
      with_tag("div[class='stage_bar Passed'][title='dev (Passed)']")
    end
    response.body.should have_tag("a[href='#{stage_detail_path(:pipeline_name => 'some_pipeline',
                                                               :pipeline_counter => 19151,
                                                               :stage_name => 'prod',
                                                               :stage_counter => 35)}']") do
      with_tag("div[class='stage_bar Passed'][title='prod (Passed)']")
    end
    response.body.should have_tag("div.stage") do
      with_tag("div[class='stage_bar Passed'][title='prod (Passed)']")
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

    render :partial => "comparison/pipeline_autocomplete_list_entry", :locals => {:scope => {:pipeline => @pipeline}}

    response.body.should have_tag("div.pipeline_counter h3", "some-915-label")
    response.body.should have_tag("div.pipeline_details table") do
      with_tag("tr") do
        with_tag("td.label", "Revision:")
        with_tag("td", "revision-9151") do
          with_tag("strong[class='highlight']", "915")
        end
      end
      with_tag("tr") do
        with_tag("td.label", "Comment:")
        with_tag("td", "comment-915") do
          with_tag("strong[class='highlight']", "915")
        end
      end
      with_tag("tr") do
        with_tag("td.label", "Modified&nbsp;by:")
        with_tag("td", "user-915-1 on #{modified_time.to_long_display_date_time()}") do
          with_tag("strong[class='highlight']", "915")
        end
      end
    end
  end

end
