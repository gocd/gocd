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

    doc = Nokogiri::XML(response.body)
    stage = doc.xpath("//stage[@name='blah-stage'][@counter='12']")

    expect(stage).to_not be_nil_or_empty
    expect(stage.xpath("pipeline[@name='pipeline_name'][@counter='100'][@label='LABEL-100'][@href='http://localhost:8153/go/api/pipelines/pipeline_name/100.xml']")).to_not be_nil_or_empty
    expect(stage.xpath("updated").text).to eq(DateUtils.formatISO8601(@last_updated))
    expect(stage.xpath("result").text).to eq("Passed")
    expect(stage.xpath("state").text).to eq("Completed")
    expect(stage.xpath("state").text).to eq("Completed")
    expect(stage.xpath("approvedBy").text).to eq("blahUser")

    jobs = stage.xpath("jobs/job")
    expect(jobs.count).to eq(1)
    expect(jobs[0].attr('href')).to eq("http://localhost:8153/go/api/jobs/-1.xml")

    expect(response.body).to match(/#{cdata_wraped_regexp_for("blahUser")}/)
  end

  it "should have a self referncing link" do
    render :template => '/api/stages/index.xml.erb'

    doc = Nokogiri::XML(response.body)
    stage = doc.xpath("//stage[@name='blah-stage'][@counter='12']")

    expect(stage.xpath("link[@href='http://localhost:8153/go/api/stages/#{@stage.getId()}.xml']")).to_not be_nil_or_empty
    expect(stage.xpath("link[@rel='self']")).to_not be_nil_or_empty
    expect(stage.xpath("id").text).to eq("urn:x-go.studios.thoughtworks.com:stage-id:pipeline_name:100:blah-stage:12")
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
