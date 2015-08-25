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

def schedule_options(specified_revisions, variables, secure_variables = {})
  ScheduleOptions.new(HashMap.new(specified_revisions), HashMap.new(variables), HashMap.new(secure_variables))
end

describe Api::PipelinesController do
  include StageModelMother
  include GoUtil
  include APIModelMother

  before :each do
    controller.stub(:populate_health_messages)
    @pipeline_service = Object.new
    @pipeline_history_service = double('pipeline_history_service')
    @pipeline_unlock_api_service = double('pipeline_unlock_api_service')
    @go_config_service = double('go_config_service')
    @changeset_service = double("changeset_service")
    @pipeline_pause_service = double("pipeline_pause_service")
    @status="status"
    controller.stub(:changeset_service).and_return(@changeset_service)
    controller.stub(:pipeline_scheduler).and_return(@pipeline_service)
    controller.stub(:pipeline_unlock_api_service).and_return(@pipeline_unlock_api_service)
    controller.stub(:pipeline_history_service).and_return(@pipeline_history_service)
    controller.stub(:go_config_service).and_return(@go_config_service)
    controller.stub(:current_user).and_return(@user = "user")
    controller.stub(:pipeline_service).and_return(@pipeline_service)
    controller.stub(:pipeline_pause_service).and_return(@pipeline_pause_service)

    @material_config = double('material_config')
    @fingerprint = '123456'
    @material_config.stub(:getPipelineUniqueFingerprint).and_return(@fingerprint)
    controller.stub(:populate_config_validity)
    setup_base_urls
  end

  it "should return only the path to a pipeline api" do
    api_pipeline_action_path(:pipeline_name => "pipeline", :action => "schedule").should == "/api/pipelines/pipeline/schedule"
  end

  describe :history do
    it "should route to history" do
      expect(:get => '/api/pipelines/up42/history').to route_to(:controller => "api/pipelines", :action => "history", :pipeline_name => 'up42', :offset => '0', :no_layout => true)
      expect(:get => '/api/pipelines/up42/history/1').to route_to(:controller => "api/pipelines", :action => "history", :pipeline_name => 'up42', :offset => '1', :no_layout => true)
    end

    it "should render history json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @pipeline_history_service.should_receive(:totalCount).and_return(10)
      @pipeline_history_service.should_receive(:loadMinimalData).with('up42', anything, "loser", anything).and_return(create_pipeline_history_model)

      get :history, :pipeline_name => 'up42', :offset => '5', :no_layout => true

      expect(response.body).to eq(PipelineHistoryAPIModel.new(Pagination.pageStartingAt(5, 10, 10), create_pipeline_history_model).to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @pipeline_history_service.should_receive(:totalCount).and_return(10)
      @pipeline_history_service.should_receive(:loadMinimalData).with('up42', anything, "loser", anything) do |pipeline_name, pagination, username, result|
        result.notAcceptable("Not Acceptable", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      get :history, :pipeline_name => 'up42', :no_layout => true

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end
  end

  describe :instance_by_counter do
    it "should route to instance_by_counter" do
      expect(:get => '/api/pipelines/up42/instance/1').to route_to(:controller => "api/pipelines", :action => "instance_by_counter", :pipeline_name => 'up42', :pipeline_counter => '1', :no_layout => true)
    end

    it "should render instance json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @pipeline_history_service.should_receive(:findPipelineInstance).with('up42', 1, loser, anything).and_return(create_pipeline_model)

      get :instance_by_counter, :pipeline_name => 'up42', :pipeline_counter => '1', :no_layout => true

      expect(response.body).to eq(PipelineInstanceAPIModel.new(create_pipeline_model).to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @pipeline_history_service.should_receive(:findPipelineInstance).with('up42', 1, loser, anything) do |pipeline_name, pipeline_counter, username, result|
        result.notAcceptable("Not Acceptable", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      get :instance_by_counter, :pipeline_name => 'up42', :pipeline_counter => '1', :no_layout => true

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end
  end

  describe :status do
    it "should route to history" do
      expect(:get => '/api/pipelines/up42/status').to route_to(:controller => "api/pipelines", :action => "status", :pipeline_name => 'up42', :no_layout => true)
    end

    it "should render status json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @pipeline_history_service.should_receive(:getPipelineStatus).with('up42', "loser", anything).and_return(create_pipeline_status_model)

      get :status, :pipeline_name => 'up42', :no_layout => true

      expect(response.body).to eq(PipelineStatusAPIModel.new(create_pipeline_status_model).to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @pipeline_history_service.should_receive(:getPipelineStatus).with('up42', "loser", anything) do |pipeline_name, username, result|
        result.notAcceptable("Not Acceptable", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      get :status, :pipeline_name => 'up42', :no_layout => true

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end
  end

  describe :card_activity do
    it "should generate the route" do
      expect(:get => '/api/card_activity/foo.bar/10/to/15').to route_to(:controller => "api/pipelines", :action => "card_activity", :format=>"xml", :pipeline_name => 'foo.bar', :from_pipeline_counter => "10", :to_pipeline_counter => "15", "no_layout" => true)
    end

    it "should generate the route with show_bisect" do
      expect(:get => '/api/card_activity/foo.bar/10/to/15?show_bisect=true').to route_to(:controller => "api/pipelines", :action => "card_activity", :format=>"xml", :pipeline_name => 'foo.bar', :from_pipeline_counter => "10", :to_pipeline_counter => "15", "no_layout" => true, :show_bisect => 'true')
    end

    it "should resolve route to itself" do
      expect(:get => '/api/card_activity/foo.bar/10/to/15?show_bisect=true').to route_to(:controller => "api/pipelines", :pipeline_name=>"foo.bar", :action => "card_activity", :no_layout=>true, :from_pipeline_counter => "10", :to_pipeline_counter => "15", :format => "xml", :show_bisect => 'true')
    end

    it "should show_bisect when requested" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @changeset_service.should_receive(:getCardNumbersBetween).with('foo', 10, 20, loser, an_instance_of(HttpLocalizedOperationResult), true).and_return([3,5,10])
      get :card_activity, :pipeline_name => 'foo', :from_pipeline_counter => "10", :to_pipeline_counter => "20", :no_layout => true, :show_bisect => 'true'
      expect(response.body).to eq("3,5,10")
    end

    it "should return a list of cards between two pipelines" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @changeset_service.should_receive(:getCardNumbersBetween).with('foo', 10, 20, loser, an_instance_of(HttpLocalizedOperationResult), false).and_return([3,5,10])
      get :card_activity, :pipeline_name => 'foo', :from_pipeline_counter => "10", :to_pipeline_counter => "20", :no_layout => true
      expect(response.body).to eq("3,5,10")
    end

    it "should render error when there is an error" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @changeset_service.should_receive(:getCardNumbersBetween).with('foo', 10, 20, loser, an_instance_of(HttpLocalizedOperationResult), false) do |name, from, to, user, result, show_bisect|
        result.notFound(LocalizedMessage.string("PIPELINE_NOT_FOUND", ['foo']), HealthStateType.general(HealthStateScope.forPipeline('foo')))
      end
      get :card_activity, :pipeline_name => 'foo', :from_pipeline_counter => "10", :to_pipeline_counter => "20", :no_layout => true
      expect(response.body).to eq("Pipeline 'foo' not found.\n")
    end
  end

  describe :schedule do
    before(:each) do
      com.thoughtworks.go.server.service.result.HttpOperationResult.stub(:new).and_return(@status)
    end

    it "should return 404 when I try to trigger a pipeline that does not exist" do
      message = "pipeline idonotexist does not exist"
      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('idonotexist',anything(), schedule_options({}, {}), @status)
      @status.stub(:httpCode).and_return(404)
      @status.stub(:detailedMessage).and_return(message)
      controller.should_receive(:render_if_error).with(message, 404).and_return(true)
      fake_template_presence "api/pipelines/schedule.erb", "dummy"

      post 'schedule', :pipeline_name => 'idonotexist', :no_layout => true
    end

    it "should return 404 when material does not exist" do
      message = "material does not exist"
      @go_config_service.should_receive(:findMaterialWithName).with(CaseInsensitiveString.new('pipeline'), CaseInsensitiveString.new('material_does_not_exist')).and_return(nil)
      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('pipeline', anything(), schedule_options({"material_does_not_exist" => "foo"}, {}), @status)
      @status.stub(:httpCode).and_return(404)
      @status.stub(:detailedMessage).and_return(message)

      controller.should_receive(:render_if_error).with(message, 404).and_return(true)
      fake_template_presence "api/pipelines/schedule.erb", "dummy"

      post 'schedule', :pipeline_name => 'pipeline', "materials" => {'material_does_not_exist' => "foo"}, :no_layout => true
    end

    it "should answer for /api/pipelines/blahpipeline/schedule" do
      expect(:post => '/api/pipelines/blahpipeline/schedule').to route_to(:controller => "api/pipelines", :pipeline_name=>"blahpipeline", :action => "schedule", :no_layout=>true)
    end

    it "should be able to specify a particular revision from a upstream pipeline" do
      @go_config_service.should_receive(:findMaterialWithName).with(CaseInsensitiveString.new('downstream'), CaseInsensitiveString.new('downstream')).and_return(@material_config)
      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('downstream',anything(), schedule_options({@fingerprint => "downstream/10/blah-stage/2"}, {}), @status)
      @status.stub(:httpCode).and_return(202)
      @status.stub(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post 'schedule', :pipeline_name => 'downstream', "materials" => {"downstream" => "downstream/10/blah-stage/2"}, :no_layout => true
    end

    it "should return 202 when I trigger a pipeline successfully" do
      message = "accepted request to schedule pipeline badger"
      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('badger',anything(),schedule_options({}, {}), @status)
      @status.stub(:httpCode).and_return(202)
      @status.stub(:detailedMessage).and_return(message)
      post 'schedule', :pipeline_name => 'badger', :no_layout => true
      expect(response.response_code).to eq(202)
      expect(response.body).to eq(message + "\n")
    end

    it "should fix empty revisions if necessary" do
      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('downstream',anything(),schedule_options({@fingerprint => "downstream/10/blah-stage/2"}, {}), @status)
      @status.stub(:httpCode).and_return(202)
      @status.stub(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post 'schedule', :pipeline_name => 'downstream', "materials" => {"downstream" => ""}, "original_fingerprint" => {@fingerprint => "downstream/10/blah-stage/2"}, :no_layout => true
    end

    it "should respect materials name material map over originalMaterials" do
      svn_material_config = double('svn_material_config')
      svn_fingerprint = 'svn_123456'
      svn_material_config.stub(:getPipelineUniqueFingerprint).and_return(svn_fingerprint)

      svn2_fingerprint = 'svn2_123456'

      @go_config_service.should_receive(:findMaterialWithName).with(CaseInsensitiveString.new('downstream'), CaseInsensitiveString.new('downstream')).and_return(@material_config)
      @go_config_service.should_receive(:findMaterialWithName).with(CaseInsensitiveString.new('downstream'), CaseInsensitiveString.new('svn')).and_return(svn_material_config)

      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('downstream',anything(),schedule_options({@fingerprint => "downstream/10/blah-stage/5", svn_fingerprint => "20", svn2_fingerprint => "45"}, {}), @status)
      @status.stub(:httpCode).and_return(202)
      @status.stub(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post 'schedule', :pipeline_name => 'downstream', "materials" => {"downstream" => "downstream/10/blah-stage/5", "svn" => "20"}, "original_fingerprint" => {@fingerprint => "downstream/10/blah-stage/2", svn_fingerprint => "30", svn2_fingerprint => "45"}, :no_layout => true
    end

    it "should support using pipeline unique fingerprint to select material" do
      downstream = "0942094a"
      svn = "875c3261"
      svn_2 = "98143abd"
      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('downstream',anything(),schedule_options({downstream => "downstream/10/blah-stage/5", svn => "20", svn_2 => "45"}, {}), @status)
      @status.stub(:httpCode).and_return(202)
      @status.stub(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post 'schedule', :pipeline_name => 'downstream', "material_fingerprint" => {downstream => "downstream/10/blah-stage/5", svn => "20"}, "original_fingerprint" => {downstream => "downstream/10/blah-stage/2", svn => "30", svn_2 => "45"}, :no_layout => true
    end

    it "should support using schedule time environment variable values while scheduling a pipeline" do
      downstream = "0942094a"
      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('downstream',anything(),schedule_options({downstream => "downstream/10/blah-stage/5"}, {'foo' => 'foo_value', 'bar' => 'bar_value'}), @status)
      @status.stub(:httpCode).and_return(202)
      @status.stub(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post 'schedule', :pipeline_name => 'downstream', "material_fingerprint" => {downstream => "downstream/10/blah-stage/5"}, 'variables' => {'foo' => 'foo_value', 'bar' => 'bar_value'}, :no_layout => true
    end

    it "should support using schedule time environment variable values while scheduling a pipeline" do
      downstream = "0942094a"
      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('downstream', anything(), schedule_options({downstream => "downstream/10/blah-stage/5"}, {'foo' => 'foo_value', 'bar' => 'bar_value'}, {'secure_name' => 'secure_value'}), @status)
      @status.stub(:httpCode).and_return(202)
      @status.stub(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post 'schedule', :pipeline_name => 'downstream', "material_fingerprint" => {downstream => "downstream/10/blah-stage/5"}, 'variables' => {'foo' => 'foo_value', 'bar' => 'bar_value'}, 'secure_variables' => {'secure_name' => 'secure_value'}, :no_layout => true
    end

    it "should support empty material_fingerprints" do
      svn = "875c3261"
      @pipeline_service.should_receive(:manualProduceBuildCauseAndSave).with('downstream',anything(),schedule_options({svn => "30"}, {}), @status)
      @status.stub(:httpCode).and_return(202)
      @status.stub(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post 'schedule', :pipeline_name => 'downstream', "material_fingerprint" => {svn => ""}, "original_fingerprint" => {svn => "30"}, :no_layout => true
    end
  end

  describe :pipeline_instance do
    it "should load pipeline by id" do
      pipeline = PipelineInstanceModel.createPipeline("pipeline", 1, "label", BuildCause.createWithEmptyModifications(), stage_history_for("blah-stage"))
      @pipeline_history_service.should_receive(:load).with(10, "user", anything).and_return(pipeline)
      get :pipeline_instance, :id => '10', :name => "pipeline", :format => "xml", :no_layout => true
      context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, nil)
      expect(assigns[:doc].asXML()).to eq(PipelineXmlViewModel.new(pipeline).toXml(context).asXML())
    end

    it "should respond with 404 when pipeline not found" do
      @pipeline_history_service.should_receive(:load).with(10, "user", anything).and_return(nil) do |id, user, result|
        result.notFound("Not Found", "", nil)
      end
      get :pipeline_instance, :id => '10', :name => "pipeline", :format => "xml", :no_layout => true
      expect(response.status).to eq(404)
    end

    it "should respond with 401 when user does not have view permission" do
      @pipeline_history_service.should_receive(:load).with(10, "user", anything).and_return(nil) do |id, user, result|
        result.unauthorized("Unauthorized", "", nil)
      end
      get :pipeline_instance, :id => '10', :format => "xml", :name => "pipeline", :no_layout => true
      expect(response.status).to eq(401)
    end

    it "should resolve url to action" do
      expect(:get => "/api/pipelines/pipeline.com/10.xml?foo=bar").to route_to(:controller => 'api/pipelines', :action => 'pipeline_instance', :name => "pipeline.com", :id => "10", :format => "xml", :foo  => "bar", :no_layout => true)
    end
  end

  describe :pipelines do
    it "should assign pipeline_configs and latest instance of each pipeline configured" do
      @pipeline_history_service.should_receive(:latestInstancesForConfiguredPipelines).with("user").and_return(:pipeline_instance)
      get :pipelines, :format => "xml", :no_layout => true
      expect(assigns[:pipelines]).to eq(:pipeline_instance)
    end
  end

  describe :stage_feed do
    before :each do
      controller.go_cache.clear
      controller.stub(:set_locale)
    end

    it "should return the url to the feed" do
      params = {:name => 'cruise'}
      expect(controller.url(params)).to eq("http://test.host/api/pipelines/cruise/stages.xml")
    end

    it "should return the url to the page given a stage locator" do
      expect(controller.page_url("cruise/1/dev/1")).to eq("http://test.host/pipelines/cruise/1/dev/1")
    end

    it "should answer for /api/pipelines/foo/stages.xml" do
      expect(:get => '/api/pipelines/foo/stages.xml').to route_to(:controller => "api/pipelines", :action => "stage_feed", :format=>"xml", :name => 'foo', :no_layout => true)
    end

    it "should set the stage feed from the java side" do
      Feed.should_receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed)
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', :format => "xml", :no_layout => true, :name => 'pipeline'
      expect(assigns[:feed]).to eq(:stage_feed)
    end

    it "should set content type as application/atom+xml" do
      Feed.should_receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed)
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', :format => "xml", :no_layout => true, :name => 'pipeline'
      expect(response.content_type).to eq("application/atom+xml")
    end

    it "should honor after if present" do
      Feed.should_receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:after)).and_return(:stage_feed)
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', :after => 10, :format => "xml", :no_layout => true, :name => 'pipeline'
      expect(assigns[:feed]).to eq(:stage_feed)
    end

    it "should honor before if present" do
      Feed.should_receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:before)).and_return(:stage_feed)
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', :before => 10, :format => "xml", :no_layout => true, :name => 'pipeline'
      expect(assigns[:feed]).to eq(:stage_feed)
    end

    it "should assign title"do
      Feed.should_receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed)
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', :format => "xml", :no_layout => true, :name => 'pipeline'
      expect(assigns[:title]).to eq("pipeline")
    end

    it "should return stages url with before and after params" do
      expected_routing_params = {:controller => "api/pipelines", :action => "stage_feed", :format => "xml", :name => 'cruise', :no_layout => true, :after => "2", :before => "bar"}
      expect(:get => api_pipeline_stage_feed_path(:after => 2, :before => "bar", :name => "cruise")).to route_to(expected_routing_params)
    end

    it "should return 404 if the pipeline does not exist" do
      controller.should_receive(:render_error_response).with("Pipeline not found", 404, true)
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new('does_not_exist')).and_return(false)
      get 'stage_feed', :format => "xml", :no_layout => true, :name => 'does_not_exist'
    end

    it "should render the error if there is any" do
      Feed.should_receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed) do |a, b, c, d|
        c.notFound(LocalizedMessage.string('Screwed'), HealthStateType.invalidConfig())
      end
      controller.should_receive(:render_localized_operation_result).with(an_instance_of(HttpLocalizedOperationResult))
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new('does_not_exist')).and_return(true)
      get 'stage_feed', :format => "xml", :no_layout => true, :name => 'does_not_exist'
    end
  end

  describe :releaseLock do
    it "should call service and render operation result" do
      @pipeline_unlock_api_service.should_receive(:unlock).with('pipeline-name', @user, anything) do |name, user, operation_result|
        operation_result.notAcceptable("done", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      fake_template_presence 'api/pipelines/releaseLock.erb', 'dummy'
      controller.should_receive(:render_if_error).with("done\n", 406).and_return(true)

      post :releaseLock, :pipeline_name => 'pipeline-name', :no_layout => true
    end

    it "should map /api/pipelines/:pipeline_name/releaseLock to :release_lock" do
      expect(:post => "/api/pipelines/cruise-1.3.2/releaseLock").to route_to(:controller => "api/pipelines", :pipeline_name => "cruise-1.3.2", :action => "releaseLock", :no_layout => true)
    end
  end

  describe :pause do
    it "should resolve route to pause" do
      expect(:post => "/api/pipelines/foo.bar/pause").to route_to(:controller => "api/pipelines", :pipeline_name => "foo.bar", :action => "pause", :no_layout => true)
    end

    it "should pause the pipeline" do
      @pipeline_pause_service.should_receive(:pause).with("foo.bar", "wait for next checkin", Username.new(CaseInsensitiveString.new("someuser"), "Some User"), an_instance_of(HttpLocalizedOperationResult))
      @controller.stub(:current_user).and_return(Username.new(CaseInsensitiveString.new("someuser"), "Some User"))
      post :pause, {:pipeline_name => "foo.bar", :no_layout => true, :pauseCause => "wait for next checkin"}
    end
  end

  describe :unpause do
    it "should resolve route to unpause" do
      expect(:post => "/api/pipelines/foo.bar/unpause").to route_to(:controller => "api/pipelines", :pipeline_name => "foo.bar", :action => "unpause", :no_layout => true)
    end

    it "should pause the pipeline" do
      @pipeline_pause_service.should_receive(:unpause).with("foo.bar", Username.new(CaseInsensitiveString.new("someuser"), "Some User"), an_instance_of(HttpLocalizedOperationResult))
      @controller.stub(:current_user).and_return(Username.new(CaseInsensitiveString.new("someuser"), "Some User"))
      post :unpause, {:pipeline_name => "foo.bar", :no_layout => true}
    end
  end
end
