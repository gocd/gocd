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

require 'rails_helper'

describe Api::JobsController do

  include JobMother

  before do
    @job_instance_service = double('job_instance_service')
    allow(controller).to receive(:job_instance_service).and_return(@job_instance_service)
    allow(controller).to receive(:xml_api_service).and_return(@xml_api_service = double(":xml_api_service"))

    allow(controller).to receive(:populate_config_validity)
  end

  it "should return a 404 HTTP response when id is not a number" do
    get :index, params: {:id => "does-not-exist", :no_layout => true}, format: :xml
    expect(response.status).to eq(404)
  end

  it "should return a 404 HTTP response when job cannot be loaded" do
    job_instance_service = double()
    expect(job_instance_service).to receive(:buildById).with(99).and_throw(Exception.new("foo"))
    allow(controller).to receive(:job_instance_service).and_return(job_instance_service)
    get :index, params: {:id => "99", :no_layout => true}, format: :xml
    expect(response.status).to eq(404)
  end

  it "should load job and properties based on passed on id param" do
    job = job_instance('job')

    allow(@xml_api_service).to receive(:write).with(JobXmlViewModel.new(job), "http://test.host/go").and_return(:dom)

    expect(@job_instance_service).to receive(:buildById).with(1).and_return(job)
    fake_template_presence 'api/jobs/index', 'some data'

    get :index, params: {:id => "1", :no_layout => true}, format: :xml

    expect(assigns[:doc]).to eq(:dom)
  end

  it "should return ordered builds with environment names when scheduled is called" do
    job = [job_instance('job')]

    jobPlan1 = JobInstanceMother.jobPlan("job-1", 1)
    jobPlan2 = JobInstanceMother.jobPlan("job-2", 2)
    jobPlan3 = JobInstanceMother.jobPlan("job-3", 3)

    waitingJobPlans =  java.util.ArrayList.new
    waitingJobPlans.add(WaitingJobPlan.new(jobPlan1, "env1"))
    waitingJobPlans.add(WaitingJobPlan.new(jobPlan2, nil))
    waitingJobPlans.add(WaitingJobPlan.new(jobPlan3, "env1"))
    allow(@xml_api_service).to receive(:write).with(anything, anything).and_return(:dom)

    allow(@job_instance_service).to receive(:waitingJobPlans).and_return(waitingJobPlans)
    fake_template_presence 'api/jobs/scheduled', 'some data'

    get :scheduled, params: { :no_layout => true }, format: :xml

    context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, nil)
    expect(assigns[:doc]).to eq(:dom)
  end

  describe "history" do
    include APIModelMother

    it "should render history json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@job_instance_service).to receive(:getJobHistoryCount).and_return(10)
      expect(@job_instance_service).to receive(:findJobHistoryPage).with('pipeline', 'stage', 'job', anything, "loser", anything).and_return([create_job_model])

      get :history, params: { :pipeline_name => 'pipeline', :stage_name => 'stage', :job_name => 'job', :offset => '5', :no_layout => true }

      expect(response.body).to eq(JobHistoryAPIModel.new(Pagination.pageStartingAt(5, 10, 10), [create_job_model]).to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@job_instance_service).to receive(:getJobHistoryCount).and_return(10)
      expect(@job_instance_service).to receive(:findJobHistoryPage).with('pipeline', 'stage', 'job', anything, "loser", anything) do |pipeline_name, stage_name, job_name, pagination, username, result|
        result.notAcceptable("Not Acceptable", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      get :history, params: { :pipeline_name => 'pipeline', :stage_name => 'stage', :job_name => 'job', :no_layout => true }

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end
  end
end
