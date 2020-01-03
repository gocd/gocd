#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe "/api/jobs" do

  before :each do
    @plans = ArrayList.new
    @plans.add(com.thoughtworks.go.domain.ArtifactPlan.new(com.thoughtworks.go.domain.ArtifactPlanType::file, "artifact", "blahartifact/path"))
    @plans.add(com.thoughtworks.go.domain.ArtifactPlan.new(com.thoughtworks.go.domain.ArtifactPlanType::file, "logs/log-artifact", "log-path"))
    @plans.add(com.thoughtworks.go.domain.ArtifactPlan.new(com.thoughtworks.go.domain.ArtifactPlanType::unit, "test.xml", ""))

    @resources = com.thoughtworks.go.domain.Resources.new("linux, teapot")

    @variables = com.thoughtworks.go.domain.EnvironmentVariables.new
    @variables.add("VARIABLE_NAME", "variable-value")

    @job = JobInstanceMother::completed("job-name")
    @job.setStageId(666)
    @job.setAgentUuid("UUID")

    @artifacts_url_reader = double("artifacts_url_reader")
    allow(@artifacts_url_reader).to receive(:findArtifactRoot).with(@job.getIdentifier()).and_return("/artifacts-path")
    allow(@artifacts_url_reader).to receive(:findArtifactUrl).with(@job.getIdentifier()).and_return("/artifacts-url")

    @job_plan_loader = double("job_plan_loader")
    allow(@job_plan_loader).to receive(:loadOriginalJobPlan).with(@job.getIdentifier()).and_return(DefaultJobPlan.new(@resources, @plans, 1, @job.getIdentifier, 'UUID', @variables, @variables, nil, nil))

    @context = XmlWriterContext.new("http://test.host/go", @artifacts_url_reader, @job_plan_loader, nil, SystemEnvironment.new)
    assign(:doc, JobXmlViewModel.new(@job).toXml(@context))

    view.extend Api::FeedsHelper
  end

  it "should have a self referencing link" do
    render :template => '/api/jobs/index.xml.erb'

    job = Nokogiri::XML(response.body).xpath("//job")
    expect(job.xpath("link[@rel='self'][@href='http://test.host/go/api/jobs/1.xml']")).to_not be_nil_or_empty
    expect(job.xpath("id").text).to eq("urn:x-go.studios.thoughtworks.com:job-id:pipeline:1:stage:1:job-name")
  end

  it "should contain link to stage" do
    render :template => '/api/jobs/index.xml.erb'
    expect(Nokogiri::XML(response.body).xpath("//job/stage[@name='stage'][@counter='1'][@href='http://test.host/go/api/stages/666.xml']")).to_not be_nil_or_empty
  end

  it "should contain job details" do
    render :template => '/api/jobs/index.xml.erb'

    doc = Nokogiri::XML(response.body)
    job = doc.xpath("//job[@name='job-name']")

    expect(job).to_not be_nil_or_empty
    job.tap do |entry|
      expect(entry.xpath("pipeline[@name='pipeline'][@counter='1'][@label='label-1']"))
      expect(entry.xpath("state").text).to eq("Completed")
      expect(entry.xpath("result").text).to eq("Passed")

      artifacts = entry.xpath("artifacts[@baseUri='http://test.host/go/artifacts-url'][@pathFromArtifactRoot='/artifacts-path']")
      expect(artifacts).to_not be_nil_or_empty
      artifacts.tap do |node|
        expect(node.xpath("artifact[@dest=''][@src='test.xml'][@type='unit']")).to_not be_nil_or_empty
      end
      expect(entry.xpath("agent[@uuid='UUID']")).to_not be_nil_or_empty

      resources = entry.xpath("resources")
      expect(resources).to_not be_nil_or_empty

      environment_variables = entry.xpath("environmentvariables")
      expect(environment_variables).to_not be_nil_or_empty
    end
  end

  describe "xml sensitive characters" do
    include GoUtil

    before :each do
      plans = ArrayList.new
      plans.add(com.thoughtworks.go.domain.ArtifactPlan.new(com.thoughtworks.go.domain.ArtifactPlanType::file, "artifact", "blah<artif\"act/path"))
      plans.add(com.thoughtworks.go.domain.ArtifactPlan.new(com.thoughtworks.go.domain.ArtifactPlanType::file, "logs/log-arti\"fact", "log-path"))
      plans.add(com.thoughtworks.go.domain.ArtifactPlan.new(com.thoughtworks.go.domain.ArtifactPlanType::unit, "te<s\"t.xml", ""))

      variables = com.thoughtworks.go.domain.EnvironmentVariables.new
      variables.add("VARIA<BLE_NA\"ME", "varia<ble-val\"ue")

      @job = JobInstanceMother::completed("job<na\"me")
      @job.setStageId(666)
      allow(@artifacts_url_reader).to receive(:findArtifactUrl).with(@job.getIdentifier()).and_return("/artifacts-url")
      allow(@artifacts_url_reader).to receive(:findArtifactRoot).with(@job.getIdentifier()).and_return("/artifacts-path")
      allow(@job_plan_loader).to receive(:loadOriginalJobPlan).with(@job.getIdentifier()).and_return(DefaultJobPlan.new(@resources, plans, 1, @job.getIdentifier, 'UUID', variables, variables, nil, nil))

      assign(:doc, JobXmlViewModel.new(@job).toXml(@context))
    end

    it "should be escaped" do
      render :template => '/api/jobs/index.xml.erb'
      root = dom4j_root_for(response.body)
      expect(root.valueOf("//job/@name")).to eq("job<na\"me")
      expect(root.valueOf("//stage/@name")).to eq("stage")
      expect(root.valueOf("//pipeline/@name")).to eq("pipeline")
      expect(root.valueOf("//agent/@uuid")).to eq("1234")
      expect(root.valueOf("//artifacts/@pathFromArtifactRoot")).to eq("/artifacts-path")
      expect(root.valueOf("//artifact[1]/@dest")).to eq("")
      expect(root.valueOf("//artifact[1]/@src")).to eq("te<s\"t.xml")
    end
  end
end
