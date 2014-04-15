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

describe "/api/jobs" do

  before(:each) do
    @properties = com.thoughtworks.go.domain.Properties.new
    @properties.add(com.thoughtworks.go.domain.Property.new("foo", "value_of_property_foo"))

    @plans = ArtifactPlans.new
    @plans.add(ArtifactPlan.new(ArtifactType::file, "artifact", "blahartifact/path"))
    @plans.add(ArtifactPlan.new(ArtifactType::file, "logs/log-artifact", "log-path"))
    @plans.add(ArtifactPlan.new(ArtifactType::unit, "test.xml", ""))

    @resources = Resources.new("linux, teapot")

    @variables = EnvironmentVariablesConfig.new
    @variables.add("VARIABLE_NAME", "variable-value")

    @job = JobInstanceMother::completed("job-name")
    @job.setStageId(666)
    @job.setAgentUuid("UUID")

    @job_properties_reader = mock("job_properties_reader")
    @job_properties_reader.stub(:getPropertiesForJob).with(1).and_return(@properties)

    @artifacts_url_reader = mock("artifacts_url_reader")
    @artifacts_url_reader.stub(:findArtifactRoot).with(@job.getIdentifier()).and_return("/artifacts-path")
    @artifacts_url_reader.stub(:findArtifactUrl).with(@job.getIdentifier()).and_return("/artifacts-url")

    @job_plan_loader = mock("job_plan_loader")
    @job_plan_loader.stub(:loadOriginalJobPlan).with(@job.getIdentifier()).and_return(DefaultJobPlan.new(@resources, @plans, nil, 1, @job.getIdentifier, 'UUID', @variables, @variables))

    @context = XmlWriterContext.new("http://test.host", @job_properties_reader, @artifacts_url_reader, @job_plan_loader, nil)
    assigns[:doc] = JobXmlViewModel.new(@job).toXml(@context)
    class << template
      include ApplicationHelper
      include Api::FeedsHelper
    end
  end

  it "should have a self referencing link" do
    render '/api/jobs/index.xml'
    response.body.should have_tag "link[rel='self'][href='http://test.host/api/jobs/1.xml']"
    response.body.should have_tag "id", "urn:x-go.studios.thoughtworks.com:job-id:pipeline:1:stage:1:job-name"
  end

  it "should contain link to stage" do
    render '/api/jobs/index.xml'
    response.body.should have_tag "stage[name='stage'][counter='1'][href='http://test.host/api/stages/666.xml']"
  end

  it "should contain job details" do
    render '/api/jobs/index.xml'

    response.body.should have_tag("job[name='job-name']") do
      with_tag "pipeline[name='pipeline'][counter='1'][label='label-1']"

      with_tag "state", "Completed"
      with_tag "result", "Passed"
      with_tag "properties" do
        with_tag "property[name='foo']", "value_of_property_foo"
      end
      with_tag "artifacts[baseUri='http://test.host/artifacts-url'][pathFromArtifactRoot='/artifacts-path']" do
        with_tag "artifact[dest='blahartifact/path'][src='artifact'][type='file']"
        with_tag "artifact[dest='log-path'][src='logs/log-artifact'][type='file']"
        with_tag "artifact[dest=''][src='test.xml'][type='unit']"

      end
      with_tag "agent[uuid='UUID']"
      with_tag "resources" do
        with_tag "resource", "linux"
        with_tag "resource", "teapot"
      end
      with_tag "environmentvariables" do
        with_tag "variable[name='VARIABLE_NAME']", "variable-value"
      end
    end
    response.body.should =~ cdata_wraped_regexp_for("value_of_property_foo")
    response.body.should =~ cdata_wraped_regexp_for("variable-value")
  end

  describe "xml sensitive characters" do
    include GoUtil

    before(:each) do
      properties = com.thoughtworks.go.domain.Properties.new
      properties.add(com.thoughtworks.go.domain.Property.new("prop<er\"ty", "val<ue_of_prop\"erty_foo"))

      plans = ArtifactPlans.new
      plans.add(ArtifactPlan.new(ArtifactType::file, "artifact", "blah<artif\"act/path"))
      plans.add(ArtifactPlan.new(ArtifactType::file, "logs/log-arti\"fact", "log-path"))
      plans.add(ArtifactPlan.new(ArtifactType::unit, "te<s\"t.xml", ""))

      variables = EnvironmentVariablesConfig.new
      variables.add("VARIA<BLE_NA\"ME", "varia<ble-val\"ue")

      @job = JobInstanceMother::completed("job<na\"me")
      @job.setStageId(666)
      @job_properties_reader.stub(:getPropertiesForJob).with(1).and_return(properties)
      @artifacts_url_reader.stub(:findArtifactUrl).with(@job.getIdentifier()).and_return("/artifacts-url")
      @artifacts_url_reader.stub(:findArtifactRoot).with(@job.getIdentifier()).and_return("/artifacts-path")
      @job_plan_loader.stub(:loadOriginalJobPlan).with(@job.getIdentifier()).and_return(DefaultJobPlan.new(@resources, plans, nil, 1, @job.getIdentifier, 'UUID', variables, variables))

      assigns[:doc] = JobXmlViewModel.new(@job).toXml(@context)
    end

    it "should be escaped" do
      render '/api/jobs/index.xml'
      root = dom4j_root_for(response.body)
      root.valueOf("//job/@name").should == "job<na\"me"
      root.valueOf("//stage/@name").should == "stage"
      root.valueOf("//pipeline/@name").should == "pipeline"
      root.valueOf("//property/@name").should == "prop<er\"ty"
      root.valueOf("//property/.").should == "val<ue_of_prop\"erty_foo"
      root.valueOf("//agent/@uuid").should == "1234"
      root.valueOf("//artifacts/@pathFromArtifactRoot").should == "/artifacts-path"
      root.valueOf("//artifact[1]/@dest").should == "blah<artif\"act/path"
      root.valueOf("//artifact[2]/@dest").should == "log-path"
      root.valueOf("//artifact[3]/@dest").should == ""
      root.valueOf("//artifact[1]/@src").should == "artifact"
      root.valueOf("//artifact[2]/@src").should == "logs/log-arti\"fact"
      root.valueOf("//artifact[3]/@src").should == "te<s\"t.xml"
      root.valueOf("//resource[1]/.").should == "linux"
      root.valueOf("//resource[2]/.").should == "teapot"
      root.valueOf("//variable/@name").should == "VARIA<BLE_NA\"ME"
      root.valueOf("//variable/.").should == "varia<ble-val\"ue"
    end
  end
end