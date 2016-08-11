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

describe Api::JobsController do

  include JobMother

  before do
    @job_instance_service = double('job_instance_service')
    controller.stub(:job_instance_service).and_return(@job_instance_service)
    controller.stub(:set_locale)
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
    expect(:get => "/api/jobs/blah_id.xml").to route_to(:id => "blah_id", :action => "index", :controller => 'api/jobs', :format=>"xml", :no_layout => true)
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

  describe :history do
    include APIModelMother

    it "should render history json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @job_instance_service.should_receive(:getJobHistoryCount).and_return(10)
      @job_instance_service.should_receive(:findJobHistoryPage).with('pipeline', 'stage', 'job', anything, "loser", anything).and_return([create_job_model])

      get :history, :pipeline_name => 'pipeline', :stage_name => 'stage', :job_name => 'job', :offset => '5', :no_layout => true

      expect(response.body).to eq(JobHistoryAPIModel.new(Pagination.pageStartingAt(5, 10, 10), [create_job_model]).to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @job_instance_service.should_receive(:getJobHistoryCount).and_return(10)
      @job_instance_service.should_receive(:findJobHistoryPage).with('pipeline', 'stage', 'job', anything, "loser", anything) do |pipeline_name, stage_name, job_name, pagination, username, result|
        result.notAcceptable("Not Acceptable", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      get :history, :pipeline_name => 'pipeline', :stage_name => 'stage', :job_name => 'job', :no_layout => true

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end

    describe :route do
      it "should route to history" do
        expect(:get => "/api/jobs/pipeline/stage/job/history").to route_to(:controller => 'api/jobs', :action => "history", :pipeline_name => "pipeline", :stage_name => "stage", :job_name => "job", :offset => "0", :no_layout => true)
        expect(:get => "/api/jobs/pipeline/stage/job/history/1").to route_to(:controller => 'api/jobs', :action => "history", :pipeline_name => "pipeline", :stage_name => "stage", :job_name => "job", :offset => "1", :no_layout => true)
      end

      describe :with_pipeline_name_contraint do
        it 'should route to history action of stages controller having dots in pipeline name' do
          expect(:get => 'api/jobs/some.thing/bar/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'some.thing', stage_name: 'bar', job_name: 'jobName', offset: '0')
        end

        it 'should route to history action of stages controller having hyphen in pipeline name' do
          expect(:get => 'api/jobs/some-thing/bar/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'some-thing', stage_name: 'bar', job_name: 'jobName', offset: '0')
        end

        it 'should route to history action of stages controller having underscore in pipeline name' do
          expect(:get => 'api/jobs/some_thing/bar/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'some_thing', stage_name: 'bar', job_name: 'jobName', offset: '0')
        end

        it 'should route to history action of stages controller having alphanumeric pipeline name' do
          expect(:get => 'api/jobs/123foo/bar/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: '123foo', stage_name: 'bar', job_name: 'jobName', offset: '0')
        end

        it 'should route to history action of stages controller having capitalized pipeline name' do
          expect(:get => 'api/jobs/FOO/bar/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'FOO', stage_name: 'bar', job_name: 'jobName', offset: '0')
        end

        it 'should not route to history action of stages controller for invalid pipeline name' do
          expect(:get => 'api/jobs/fo$%#@6/bar/jobName/history').to_not be_routable
        end
      end

      describe :with_stage_name_constraint do
        it 'should route to history action of stages controller having dots in stage name' do
          expect(:get => 'api/jobs/foo/some.thing/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'foo', stage_name: 'some.thing', job_name: 'jobName', offset: '0')
        end

        it 'should route to history action of stages controller having hyphen in stage name' do
          expect(:get => 'api/jobs/foo/some-thing/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'foo', stage_name: 'some-thing', job_name: 'jobName', offset: '0')
        end

        it 'should route to history action of stages controller having underscore in stage name' do
          expect(:get => 'api/jobs/foo/some_thing/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'foo', stage_name: 'some_thing', job_name: 'jobName', offset: '0')
        end

        it 'should route to history action of stages controller having alphanumeric stage name' do
          expect(:get => 'api/jobs/123foo/bar123/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: '123foo', stage_name: 'bar123', job_name: 'jobName', offset: '0')
        end

        it 'should route to history action of stages controller having capitalized stage name' do
          expect(:get => 'api/jobs/foo/BAR/jobName/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'foo', stage_name: 'BAR', job_name: 'jobName', offset: '0')
        end

        it 'should not route to history action of stages controller for invalid stage name' do
          expect(:get => 'api/jobs/some_thing/fo$%#@6/jobName/history').to_not be_routable
        end
      end

      describe :with_job_name_constraint do
        it 'should route to history action of stages controller having dots in stage name' do
          expect(:get => 'api/jobs/foo/bar/some.thing/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'foo', stage_name: 'bar', job_name: 'some.thing',  offset: '0')
        end

        it 'should route to history action of stages controller having hyphen in stage name' do
          expect(:get => 'api/jobs/foo/bar/some-thing/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'foo', stage_name: 'bar', job_name: 'some-thing',  offset: '0')
        end

        it 'should route to history action of stages controller having underscore in stage name' do
          expect(:get => 'api/jobs/foo/bar/some_thing/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'foo', stage_name: 'bar', job_name: 'some_thing',  offset: '0')
        end

        it 'should route to history action of stages controller having alphanumeric stage name' do
          expect(:get => 'api/jobs/123foo/bar/bar123/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: '123foo', stage_name: 'bar', job_name: 'bar123',  offset: '0')
        end

        it 'should route to history action of stages controller having capitalized stage name' do
          expect(:get => 'api/jobs/foo/bar/BAR/history').to route_to(no_layout: true, controller: 'api/jobs', action: 'history', pipeline_name: 'foo', stage_name: 'bar', job_name: 'BAR',  offset: '0')
        end

        it 'should not route to history action of stages controller for invalid stage name' do
          expect(:get => 'api/jobs/some_thing/stage_name/fo$%#@6/history').to_not be_routable
        end
      end
    end
  end
end
