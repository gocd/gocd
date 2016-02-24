##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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
##########################################################################

require 'spec_helper'

describe "/api/jobs" do

  before :each do
    @properties = com.thoughtworks.go.domain.Properties.new
    @properties.add(com.thoughtworks.go.domain.Property.new("foo", "value_of_property_foo"))

    @plans = ArtifactPlans.new
    @plans.add(ArtifactPlan.new("artifact", "blahartifact/path"))
    @plans.add(ArtifactPlan.new("logs/log-artifact", "log-path"))
    @plans.add(TestArtifactPlan.new("test.xml", ""))

    @resources = Resources.new("linux, teapot")

    @variables = EnvironmentVariablesConfig.new
    @variables.add("VARIABLE_NAME", "variable-value")

    @job = JobInstanceMother::completed("job-name")
    @job.setStageId(666)
    @job.setAgentUuid("UUID")

    @job_properties_reader = double("job_properties_reader")
    @job_properties_reader.stub(:getPropertiesForJob).with(1).and_return(@properties)

    @artifacts_url_reader = double("artifacts_url_reader")
    @artifacts_url_reader.stub(:findArtifactRoot).with(@job.getIdentifier()).and_return("/artifacts-path")
    @artifacts_url_reader.stub(:findArtifactUrl).with(@job.getIdentifier()).and_return("/artifacts-url")

    @job_plan_loader = double("job_plan_loader")
    @job_plan_loader.stub(:loadOriginalJobPlan).with(@job.getIdentifier()).and_return(DefaultJobPlan.new(@resources, @plans, nil, 1, @job.getIdentifier, 'UUID', @variables, @variables, nil))

    @context = XmlWriterContext.new("http://test.host", @job_properties_reader, @artifacts_url_reader, @job_plan_loader, nil)
    assign(:doc, JobXmlViewModel.new(@job).toXml(@context))
    class << view
      include ApplicationHelper
      include Api::FeedsHelper
    end
  end

  it "should have a self referencing link" do
    render :template => '/api/jobs/index.xml.erb'

    job = Nokogiri::XML(response.body).xpath("//job")
    expect(job.xpath("link[@rel='self'][@href='http://test.host/api/jobs/1.xml']")).to_not be_nil_or_empty
    expect(job.xpath("id").text).to eq("urn:x-go.studios.thoughtworks.com:job-id:pipeline:1:stage:1:job-name")
  end

  it "should contain link to stage" do
    render :template => '/api/jobs/index.xml.erb'
    expect(Nokogiri::XML(response.body).xpath("//job/stage[@name='stage'][@counter='1'][@href='http://test.host/api/stages/666.xml']")).to_not be_nil_or_empty
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

      properties = entry.xpath("properties")
      expect(properties).to_not be_nil_or_empty
      properties.tap do |node|
        expect(node.xpath("property[@name='foo']").text).to eq("value_of_property_foo")
      end

      artifacts = entry.xpath("artifacts[@baseUri='http://test.host/artifacts-url'][@pathFromArtifactRoot='/artifacts-path']")
      expect(artifacts).to_not be_nil_or_empty
      artifacts.tap do |node|
        expect(node.xpath("artifact[@dest='blahartifact/path'][@src='artifact'][@type='file']")).to_not be_nil_or_empty
        expect(node.xpath("artifact[@dest='log-path'][@src='logs/log-artifact'][@type='file']")).to_not be_nil_or_empty
        expect(node.xpath("artifact[@dest=''][@src='test.xml'][@type='unit']")).to_not be_nil_or_empty
      end
      expect(entry.xpath("agent[@uuid='UUID']")).to_not be_nil_or_empty

      resources = entry.xpath("resources")
      expect(resources).to_not be_nil_or_empty
      resources.tap do |node|
        expect(node.xpath("resource")[0].text).to eq("linux")
        expect(node.xpath("resource")[1].text).to eq("teapot")
      end

      environment_variables = entry.xpath("environmentvariables")
      expect(environment_variables).to_not be_nil_or_empty
      environment_variables.tap do |node|
        expect(node.xpath("variable[@name='VARIABLE_NAME']").text).to eq("variable-value")
      end
    end

    expect(response.body).to match(/#{cdata_wraped_regexp_for("value_of_property_foo")}/)
    expect(response.body).to match(/#{cdata_wraped_regexp_for("variable-value")}/)
  end

  describe "xml sensitive characters" do
    include GoUtil

    before :each do
      properties = com.thoughtworks.go.domain.Properties.new
      properties.add(com.thoughtworks.go.domain.Property.new("prop<er\"ty", "val<ue_of_prop\"erty_foo"))

      plans = ArtifactPlans.new
      plans.add(ArtifactPlan.new("artifact", "blah<artif\"act/path"))
      plans.add(ArtifactPlan.new("logs/log-arti\"fact", "log-path"))
      plans.add(TestArtifactPlan.new("te<s\"t.xml", ""))

      variables = EnvironmentVariablesConfig.new
      variables.add("VARIA<BLE_NA\"ME", "varia<ble-val\"ue")

      @job = JobInstanceMother::completed("job<na\"me")
      @job.setStageId(666)
      @job_properties_reader.stub(:getPropertiesForJob).with(1).and_return(properties)
      @artifacts_url_reader.stub(:findArtifactUrl).with(@job.getIdentifier()).and_return("/artifacts-url")
      @artifacts_url_reader.stub(:findArtifactRoot).with(@job.getIdentifier()).and_return("/artifacts-path")
      @job_plan_loader.stub(:loadOriginalJobPlan).with(@job.getIdentifier()).and_return(DefaultJobPlan.new(@resources, plans, nil, 1, @job.getIdentifier, 'UUID', variables, variables, nil))

      assign(:doc, JobXmlViewModel.new(@job).toXml(@context))
    end

    it "should be escaped" do
      render :template => '/api/jobs/index.xml.erb'
      root = dom4j_root_for(response.body)
      expect(root.valueOf("//job/@name")).to eq("job<na\"me")
      expect(root.valueOf("//stage/@name")).to eq("stage")
      expect(root.valueOf("//pipeline/@name")).to eq("pipeline")
      expect(root.valueOf("//property/@name")).to eq("prop<er\"ty")
      expect(root.valueOf("//property/.")).to eq("val<ue_of_prop\"erty_foo")
      expect(root.valueOf("//agent/@uuid")).to eq("1234")
      expect(root.valueOf("//artifacts/@pathFromArtifactRoot")).to eq("/artifacts-path")
      expect(root.valueOf("//artifact[1]/@dest")).to eq("blah<artif\"act/path")
      expect(root.valueOf("//artifact[2]/@dest")).to eq("log-path")
      expect(root.valueOf("//artifact[3]/@dest")).to eq("")
      expect(root.valueOf("//artifact[1]/@src")).to eq("artifact")
      expect(root.valueOf("//artifact[2]/@src")).to eq("logs/log-arti\"fact")
      expect(root.valueOf("//artifact[3]/@src")).to eq("te<s\"t.xml")
      expect(root.valueOf("//resource[1]/.")).to eq("linux")
      expect(root.valueOf("//resource[2]/.")).to eq("teapot")
      expect(root.valueOf("//variable/@name")).to eq("VARIA<BLE_NA\"ME")
      expect(root.valueOf("//variable/.")).to eq("varia<ble-val\"ue")
    end
  end
end
