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

require File.expand_path(File.dirname(__FILE__) + '/../spec_helper')

describe MaterialsHelper do
  include MaterialsHelper
  include RailsLocalizer

  before do
    @material = SvnMaterial.new("http://foo:bar@sf.net", "user", "pass", false)
  end
  
  it "should return scrubbed value for url and literal values for others" do
    attributes_for_material(@material).should == ' type="SvnMaterial" url="http://foo:******@sf.net" username="user" checkExternals="false"'
  end

  it "should return xml safe values" do
    @material = SvnMaterial.new("file:///junk<foo/dir", "us\"er", "pass", false)
    attributes_for_material(@material).should == ' type="SvnMaterial" url="file:///junk&lt;foo/dir" username="us&quot;er" checkExternals="false"'
  end

  it "should understand current modification" do
    @deployed_revision = com.thoughtworks.go.domain.materials.svn.SubversionRevision.new("123")
    modification = com.thoughtworks.go.domain.materials.Modification.new(java.util.Date.new(), "123", "label-10", nil)
    another_modification = com.thoughtworks.go.domain.materials.Modification.new(java.util.Date.new(), "456", "label-10", nil)
    current_modification?(modification).should be_true
    current_modification?(another_modification).should be_false
  end
  
  it "should not consider current modification when deployed revision is unknown" do
    @deployed_revision = com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel::UNKNOWN_REVISION
    modification = com.thoughtworks.go.domain.materials.Modification.new(java.util.Date.new(), "No historical data", "label-20", nil)
    current_modification?(modification).should be_false
  end

  it "should understand latest modification" do
    date = java.util.Date.new()

    modification = com.thoughtworks.go.domain.materials.Modification.new(date, "abc", "label-10", nil)
    another_modification = com.thoughtworks.go.domain.materials.Modification.new(date, "def", "label-10", nil)

    @modifications = com.thoughtworks.go.domain.materials.Modifications.new([modification, another_modification])

    latest_modification?(modification).should be_true
    latest_modification?(another_modification).should be_false
  end

  it "should understand if material has any modifications" do
    updated_material = Object.new
    unupdated_material = Object.new

    mock_material_service = mock("material_service")
    stub!(:material_service).and_return(mock_material_service)

    mock_material_service.should_receive(:hasModificationFor).with(updated_material).and_return(true)
    has_modification?(updated_material).should == true

    mock_material_service.should_receive(:hasModificationFor).with(unupdated_material).and_return(false)
    has_modification?(unupdated_material).should == false
  end

  it "should ask StageService for stage_url_from_identifier" do
    mock_stage_service = mock("stage_service")
    stub!(:stage_service).and_return(mock_stage_service)

    mock_stage_service.should_receive(:findStageIdByLocator).with("p/1/s/1").and_return(10)
    
    stage_url_from_identifier("p/1/s/1").should == "http://test.host/api/stages/10.xml" 
  end

  it "should replace tracking tool text with a link" do
    stub!(:go_config_service).and_return(service = mock("go config service"))
    service.should_receive(:getCommentRendererFor).with("pipeline").and_return(TrackingTool.new("http://pavan/${ID}", "\\d+"))

    modification = ModificationsMother.oneModifiedFile("1")
    modification.setComment("#42 What is the question sir?")
    render_tracking_tool_link(modification, 'pipeline').should == "<p>#<a href=\"http://pavan/42\" target=\"story_tracker\">42</a> What is the question sir?</p>"
  end

  describe :render_comment_markup_for do
    it "should display comment and trackback url" do
      stub!(:go_config_service).and_return(service = mock("go config service"))
      comment_str = '{"TYPE":"PACKAGE_MATERIAL","COMMENT":"Built on blrstdgobgr03.","TRACKBACK_URL":"google.com"}'
      render_comment_markup_for(comment_str, 'pipeline').should == "Built on blrstdgobgr03.<br/>Trackback: <a href=\"google.com\">google.com</a>"
    end

    it "should display trackback url when comment is not provided" do
      stub!(:go_config_service).and_return(service = mock("go config service"))
      comment_str = '{"TYPE":"PACKAGE_MATERIAL", "TRACKBACK_URL":"google.com"}'
      render_comment_markup_for(comment_str, 'pipeline').should == "Trackback: <a href=\"google.com\">google.com</a>"
    end

    it "should display trackback url as not provided when trachback_url and comment are not there" do
      stub!(:go_config_service).and_return(service = mock("go config service"))
      comment_str = '{"TYPE":"PACKAGE_MATERIAL"}'
      render_comment_markup_for(comment_str, 'pipeline').should == "Trackback: Not Provided"
    end

  end

  describe :render_simple_comment do
    it "should display only comment when both comment and trackback url are provided" do
      stub!(:go_config_service).and_return(service = mock("go config service"))
      comment_str = '{"TYPE":"PACKAGE_MATERIAL","COMMENT":"Built on blrstdgobgr03.","TRACKBACK_URL":"google.com"}'
      render_simple_comment(comment_str).should == "Built on blrstdgobgr03."
    end

    it "should display only trackback url when comment is not provided" do
      stub!(:go_config_service).and_return(service = mock("go config service"))
      comment_str = '{"TYPE":"PACKAGE_MATERIAL", "TRACKBACK_URL":"google.com"}'
      render_simple_comment(comment_str).should == "Trackback: google.com"
    end

    it "should display only trackback url as not provided when comment and trackback url are not provided" do
      stub!(:go_config_service).and_return(service = mock("go config service"))
      comment_str = '{"TYPE":"PACKAGE_MATERIAL"}'
      render_simple_comment(comment_str).should == "Trackback: Not Provided"
    end

  end
end
