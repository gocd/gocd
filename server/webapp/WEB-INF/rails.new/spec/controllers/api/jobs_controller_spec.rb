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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe Api::JobsController do

  include JobMother

  before do
    controller.stub(:set_locale)
    controller.stub(:licensed_agent_limit)
    controller.stub(:populate_config_validity)
  end

  it "should return a 404 HTTP response when id is not a number" do
    get 'index', :id => "does-not-exist", :format => "xml", :no_layout => true
    expect(response.status).to eq(404)
  end

  it "should return a 404 HTTP response when job cannot be loaded" do
    job_instance_service = double()
    job_instance_service.should_receive(:buildById).with(99).and_throw(Exception.new("foo"))
    controller.stub(:job_instance_service).and_return(job_instance_service)
    get 'index', :id => "99", :format => "xml", :no_layout => true
    expect(response.status).to eq(404)
  end

  it "should answer to /api/jobs/id.xml" do
    expect(:post => "/api/jobs/blah_id.xml").to route_to(:id => "blah_id", :action => "index", :controller => 'api/jobs', :format=>"xml", :no_layout => true)
  end

  it "should load job and properties based on passed on id param" do
    job = job_instance('job')

    controller.stub(:xml_api_service).and_return(xml_api_service = double(":xml_api_service"))
    xml_api_service.stub(:write).with(JobXmlViewModel.new(job), "http://test.host/go").and_return(:dom)

    controller.stub(:job_instance_service).and_return(job_instance_service = double(":job_api_service"))
    job_instance_service.should_receive(:buildById).with(1).and_return(job)
    fake_template_presence 'api/jobs/index', 'some data'

    get 'index', :id => "1", :format => "xml", :no_layout => true

    expect(assigns[:doc]).to eq(:dom)
  end

  it "should answer to /api/jobs/scheduled.xml" do
    expect(:get => "/api/jobs/scheduled.xml").to route_to(:action => "scheduled", :controller => 'api/jobs', :format=>"xml", :no_layout => true)
  end

  it "should return ordered builds with environment names when scheduled is called" do

    jobPlan1 = JobInstanceMother.jobPlan("job-1", 1)
    jobPlan2 = JobInstanceMother.jobPlan("job-2", 2)
    jobPlan3 = JobInstanceMother.jobPlan("job-3", 3)

    waitingJobPlans =  java.util.ArrayList.new
    waitingJobPlans.add(WaitingJobPlan.new(jobPlan1, "env1"))
    waitingJobPlans.add(WaitingJobPlan.new(jobPlan2, nil))
    waitingJobPlans.add(WaitingJobPlan.new(jobPlan3, "env1"))

    controller.stub(:job_instance_service).and_return(job_instance_service = double(":job_instance_service"))
    job_instance_service.stub(:waitingJobPlans).and_return(waitingJobPlans)
    fake_template_presence 'api/jobs/scheduled', 'some data'

    get :scheduled, :format => "xml", :no_layout => true

    context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, nil)
    expect(assigns[:doc].asXML()).to eq(JobPlanXmlViewModel.new(waitingJobPlans).toXml(context).asXML())
  end
end
