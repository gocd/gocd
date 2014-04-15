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

require File.expand_path(File.dirname(__FILE__) + '/../../../spec_helper')

describe "/api/stages" do
  include GoUtil

  before(:each) do
    @stage = StageMother.create_passed_stage("pipeline_name", 100, "blah-stage", 12, "dev", java.util.Date.new())
    @stage.setApprovedBy("blahUser")
    @stage.setPipelineId(100)
    @stage_id = @stage.getIdentifier()
    @context = XmlWriterContext.new("http://localhost:8153/go", nil, nil, nil, nil)
    assigns[:doc] = StageXmlViewModel.new(@stage).toXml(@context)
    template.stub(:api_pipeline_instance_url) do |options|
      "url-#{options.map {|key, value| "#{key}=#{value}"}.join(",")}".gsub(/>/, '&gt;').gsub(/</, '&lt;')
    end
  end

  it "should contain stage details" do
    render '/api/stages/index.xml'
    response.body.should have_tag("stage[name='blah-stage'][counter='12']") do
      with_tag "pipeline[name='pipeline_name'][counter='100'][label='LABEL-100'][href='http://localhost:8153/go/api/pipelines/pipeline_name/100.xml']"
      with_tag "updated", @last_updated
      with_tag "result", "Passed"
      with_tag "state", "Completed"
      with_tag "approvedBy", "blahUser"
      with_tag "jobs" do
        with_tag "job[href='http://localhost:8153/go/api/jobs/-1.xml']"
      end
    end
    response.body.should =~ cdata_wraped_regexp_for("blahUser")
  end

  it "should have a self referencing link" do
    render '/api/stages/index.xml'
    response.body.should have_tag "link[rel='self'][href='http://localhost:8153/go/api/stages/#{@stage.getId()}.xml']"
    response.body.should have_tag "id", "urn:x-go.studios.thoughtworks.com:stage-id:pipeline_name:100:blah-stage:12"
  end

  it "should escape xml sensitive characters" do
    @stage_id = StageIdentifier.new("foo<", @stage_id.getPipelineCounter(), "foo<bar", @stage_id.getStageName(), @stage_id.getStageCounter())
    @stage.setName("<bar")
    @stage.setApprovedBy("blah\"User")
    @stage.setIdentifier(@stage_id)
    assigns[:doc] = StageXmlViewModel.new(@stage).toXml(@context)

    render "/api/stages/index.xml"

    root = dom4j_root_for(response.body)
    root.valueOf("//stage//@name").should == "<bar"
    root.valueOf("//pipeline//@name").should == "foo<"
    root.valueOf("//pipeline//@label").should == "foo<bar"
    root.valueOf("//approvedBy/.").should == "blah\"User"
  end
end