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

describe Api::JobsController do

  include JobMother

  before do
    @job_instance_service = double('job_instance_service')
    allow(controller).to receive(:job_instance_service).and_return(@job_instance_service)
    allow(controller).to receive(:xml_api_service).and_return(@xml_api_service = double(":xml_api_service"))

    allow(controller).to receive(:populate_config_validity)
  end

  it "should answer to /api/jobs/scheduled.xml" do
    expect(:get => "/api/jobs/scheduled.xml").to route_to(:action => "scheduled", :controller => 'api/jobs', :format => "xml", :no_layout => true)
  end

  it "should return ordered builds with environment names when scheduled is called" do
    job = [job_instance('job')]

    jobPlan1 = JobInstanceMother.jobPlan("job-1", 1)
    jobPlan2 = JobInstanceMother.jobPlan("job-2", 2)
    jobPlan3 = JobInstanceMother.jobPlan("job-3", 3)

    waitingJobPlans = java.util.ArrayList.new
    waitingJobPlans.add(WaitingJobPlan.new(jobPlan1, "env1"))
    waitingJobPlans.add(WaitingJobPlan.new(jobPlan2, nil))
    waitingJobPlans.add(WaitingJobPlan.new(jobPlan3, "env1"))
    allow(@xml_api_service).to receive(:write).with(anything, anything).and_return(:dom)

    allow(@job_instance_service).to receive(:waitingJobPlans).and_return(waitingJobPlans)
    fake_template_presence 'api/jobs/scheduled', 'some data'

    get :scheduled, params: {:format => "xml", :no_layout => true}

    context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, SystemEnvironment.new)
    expect(assigns[:doc]).to eq(:dom)
  end
end
