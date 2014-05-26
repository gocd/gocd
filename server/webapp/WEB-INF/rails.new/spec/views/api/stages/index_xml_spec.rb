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
    @last_updated = java.util.Date.new()
    @stage = StageMother.create_passed_stage("pipeline_name", 100, "blah-stage", 12, "dev", @last_updated)
    @stage.setApprovedBy("blahUser")
    @stage.setPipelineId(100)
    @stage_id = @stage.getIdentifier()
    @context = XmlWriterContext.new("http://localhost:8153/go", nil, nil, nil, nil)
    assign(:doc, StageXmlViewModel.new(@stage).toXml(@context))
    allow(view).to receive(:api_pipeline_instance_url) do |options|
      "url-#{options.map {|key, value| "#{key}=#{value}"}.join(",")}".gsub(/>/, '&gt;').gsub(/</, '&lt;')
    end
  end

  it "should contain stage details" do
    render :template => '/api/stages/index.xml.erb'

    doc = Nokogiri::Slop(response.body)

    expect(doc.stage["name"]).to eq("blah-stage")
    expect(doc.stage["counter"]).to eq("12")
    expect(doc.stage.pipeline["name"]).to eq("pipeline_name")
    expect(doc.stage.pipeline["counter"]).to eq("100")
    expect(doc.stage.pipeline["label"]).to eq("LABEL-100")
    expect(doc.stage.pipeline["href"]).to eq("http://localhost:8153/go/api/pipelines/pipeline_name/100.xml")
    expect(doc.stage.updated.content).to eq(@last_updated.iso8601())
    expect(doc.stage.result.content).to eq("Passed")
    expect(doc.stage.state.content).to eq("Completed")
    expect(doc.stage.approvedBy.content).to eq("blahUser")
    expect(doc.stage.jobs.job["href"]).to eq("http://localhost:8153/go/api/jobs/-1.xml")

    expect(response.body).to match(/#{cdata_wraped_regexp_for("blahUser")}/)
  end

  it "should have a self referncing link" do
    render :template => '/api/stages/index.xml.erb'

    doc = Nokogiri::Slop(response.body)
    expect(doc.stage.link["href"]).to eq("http://localhost:8153/go/api/stages/#{@stage.getId()}.xml")
    expect(doc.stage.link["rel"]).to eq("self")
    expect(doc.stage.id.content).to eq("urn:x-go.studios.thoughtworks.com:stage-id:pipeline_name:100:blah-stage:12")
  end

  it "should escape xml sensitive characters" do
    @stage_id = StageIdentifier.new("foo<", @stage_id.getPipelineCounter(), "foo<bar", @stage_id.getStageName(), @stage_id.getStageCounter())
    @stage.setName("<bar")
    @stage.setApprovedBy("blah\"User")
    @stage.setIdentifier(@stage_id)
    assign(:doc, StageXmlViewModel.new(@stage).toXml(@context))

    render :template => '/api/stages/index.xml.erb'

    root = dom4j_root_for(response.body)
    expect(root.valueOf("//stage//@name")).to eq("<bar")
    expect(root.valueOf("//pipeline//@name")).to eq("foo<")
    expect(root.valueOf("//pipeline//@label")).to eq("foo<bar")
    expect(root.valueOf("//approvedBy/.")).to eq("blah\"User")
  end
end