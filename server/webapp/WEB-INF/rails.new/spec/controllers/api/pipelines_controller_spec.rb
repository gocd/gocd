##########################GO-LICENSE-START################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe Api::PipelinesController do
  include StageModelMother
  include GoUtil
  include APIModelMother

  before :each do
    @pipeline_service = double('pipeline_service')
    @pipeline_history_service = double('pipeline_history_service')
    @pipeline_unlock_api_service = double('pipeline_unlock_api_service')
    @go_config_service = double('go_config_service')
    @changeset_service = double("changeset_service")
    @pipeline_pause_service = double("pipeline_pause_service")
    @status = double()
    allow(HttpOperationResult).to receive(:new).and_return(@status)
    allow(controller).to receive(:changeset_service).and_return(@changeset_service)
    allow(controller).to receive(:pipeline_scheduler).and_return(@pipeline_service)
    allow(controller).to receive(:pipeline_unlock_api_service).and_return(@pipeline_unlock_api_service)
    allow(controller).to receive(:pipeline_history_service).and_return(@pipeline_history_service)
    allow(controller).to receive(:go_config_service).and_return(@go_config_service)
    allow(controller).to receive(:current_user).and_return(@user = "user")
    allow(controller).to receive(:pipeline_service).and_return(@pipeline_service)
    allow(controller).to receive(:pipeline_pause_service).and_return(@pipeline_pause_service)

    @material_config = double('material_config')
    @fingerprint = '123456'
    allow(@material_config).to receive(:getPipelineUniqueFingerprint).and_return(@fingerprint)
    allow(controller).to receive(:populate_config_validity)
    setup_base_urls
    allow(@status).to receive(:canContinue).and_return(true)
    allow(@status).to receive(:httpCode).and_return(200)
  end

  it "should return only the path to a pipeline api" do
    expect(api_pipeline_schedule_path(:pipeline_name => "pipeline")).to eq("/api/pipelines/pipeline/schedule")
  end

  describe "history" do

    it "should render history json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:totalCount).and_return(10)
      expect(@pipeline_history_service).to receive(:loadMinimalData).with('up42', anything, loser, anything).and_return(create_pipeline_history_model)

      get :history, params: { :pipeline_name => 'up42', :offset => '5', :no_layout => true }

      expect(response.body).to eq(PipelineHistoryAPIModel.new(Pagination.pageStartingAt(5, 10, 10), create_pipeline_history_model).to_json)
    end

    it "should render error correctly" do
      expect(@status).to receive(:canContinue).and_return(false)
      expect(@status).to receive(:detailedMessage).and_return("Not Acceptable")
      expect(@status).to receive(:httpCode).and_return(406)

      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:totalCount).and_return(10)
      expect(@pipeline_history_service).to receive(:loadMinimalData).with('up42', anything, loser, @status)

      get :history, params: { :pipeline_name => 'up42', :no_layout => true }

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end
  end

  describe "instance_by_counter" do

    it "should render instance json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:findPipelineInstance).with('up42', 1, loser, anything).and_return(create_pipeline_model)

      get :instance_by_counter, params: { :pipeline_name => 'up42', :pipeline_counter => '1', :no_layout => true }

      expect(response.body).to eq(PipelineInstanceAPIModel.new(create_pipeline_model).to_json)
    end

    it "should render error correctly" do
      expect(@status).to receive(:canContinue).and_return(false)
      expect(@status).to receive(:detailedMessage).and_return("Not Acceptable")
      expect(@status).to receive(:httpCode).and_return(406)
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:findPipelineInstance).with('up42', 1, loser, @status)
      get :instance_by_counter, params: { :pipeline_name => 'up42', :pipeline_counter => '1', :no_layout => true }

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end
  end

  describe "status" do

    it "should render status json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:getPipelineStatus).with('up42', "loser", anything).and_return(create_pipeline_status_model)

      get :status, params: { :pipeline_name => 'up42', :no_layout => true }

      expect(response.body).to eq(PipelineStatusAPIModel.new(create_pipeline_status_model).to_json)
    end

    it "should render error correctly" do
      expect(@status).to receive(:canContinue).and_return(false)
      expect(@status).to receive(:detailedMessage).and_return("Not Acceptable")
      expect(@status).to receive(:httpCode).and_return(406)
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:getPipelineStatus).with('up42', "loser", @status)

      get :status, params: { :pipeline_name => 'up42', :no_layout => true }

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end
  end

  describe "schedule" do
    before(:each) do
      allow(com.thoughtworks.go.server.service.result.HttpOperationResult).to receive(:new).and_return(@status)
    end

    it "should return 404 when I try to trigger a pipeline that does not exist" do
      message = "pipeline idonotexist does not exist"
      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with('idonotexist',anything(), schedule_options({}, {}), @status)
      allow(@status).to receive(:httpCode).and_return(404)
      allow(@status).to receive(:detailedMessage).and_return(message)
      expect(controller).to receive(:render_if_error).with(message, 404).and_return(true)
      fake_template_presence "api/pipelines/schedule.erb", "dummy"

      post :schedule, params: { :pipeline_name => 'idonotexist', :no_layout => true}
    end

    it "should return 404 when material does not exist" do
      message = "material does not exist"
      expect(@go_config_service).to receive(:findMaterialWithName).with(CaseInsensitiveString.new('pipeline'), CaseInsensitiveString.new('material_does_not_exist')).and_return(nil)
      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with('pipeline', anything(), schedule_options({"material_does_not_exist" => "foo"}, {}), @status)
      allow(@status).to receive(:httpCode).and_return(404)
      allow(@status).to receive(:detailedMessage).and_return(message)

      expect(controller).to receive(:render_if_error).with(message, 404).and_return(true)
      fake_template_presence "api/pipelines/schedule.erb", "dummy"

      post :schedule, params: { :pipeline_name => 'pipeline', "materials" => {'material_does_not_exist' => "foo"}, :no_layout => true}
    end

    it "should be able to specify a particular revision from a upstream pipeline" do
      expect(@go_config_service).to receive(:findMaterialWithName).with(CaseInsensitiveString.new('downstream'), CaseInsensitiveString.new('downstream')).and_return(@material_config)
      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with('downstream',anything(), schedule_options({@fingerprint => "downstream/10/blah-stage/2"}, {}), @status)
      allow(@status).to receive(:httpCode).and_return(202)
      allow(@status).to receive(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post :schedule, params: { :pipeline_name => 'downstream', "materials" => {"downstream" => "downstream/10/blah-stage/2"}, :no_layout => true}
    end

    it "should return 202 when I trigger a pipeline successfully" do
      message = "accepted request to schedule pipeline badger"
      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with('badger',anything(),schedule_options({}, {}), @status)
      allow(@status).to receive(:httpCode).and_return(202)
      allow(@status).to receive(:detailedMessage).and_return(message)
      post :schedule, params: { :pipeline_name => 'badger', :no_layout => true}
      expect(response.response_code).to eq(202)
      expect(response.body).to eq(message + "\n")
    end

    it "should fix empty revisions if necessary" do
      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with('downstream',anything(),schedule_options({@fingerprint => "downstream/10/blah-stage/2"}, {}), @status)
      allow(@status).to receive(:httpCode).and_return(202)
      allow(@status).to receive(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post :schedule, params: { :pipeline_name => 'downstream', "materials" => {"downstream" => ""}, "original_fingerprint" => {@fingerprint => "downstream/10/blah-stage/2"}, :no_layout => true}
    end

    it "should respect materials name material map over originalMaterials" do
      svn_material_config = double('svn_material_config')
      svn_fingerprint = 'svn_123456'
      allow(svn_material_config).to receive(:getPipelineUniqueFingerprint).and_return(svn_fingerprint)

      svn2_fingerprint = 'svn2_123456'

      expect(@go_config_service).to receive(:findMaterialWithName).with(CaseInsensitiveString.new('downstream'), CaseInsensitiveString.new('downstream')).and_return(@material_config)
      expect(@go_config_service).to receive(:findMaterialWithName).with(CaseInsensitiveString.new('downstream'), CaseInsensitiveString.new('svn')).and_return(svn_material_config)

      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with('downstream',anything(),schedule_options({@fingerprint => "downstream/10/blah-stage/5", svn_fingerprint => "20", svn2_fingerprint => "45"}, {}), @status)
      allow(@status).to receive(:httpCode).and_return(202)
      allow(@status).to receive(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post :schedule, params: { :pipeline_name => 'downstream', "materials" => {"downstream" => "downstream/10/blah-stage/5", "svn" => "20"}, "original_fingerprint" => {@fingerprint => "downstream/10/blah-stage/2", svn_fingerprint => "30", svn2_fingerprint => "45"}, :no_layout => true}
    end

    it "should support using pipeline unique fingerprint to select material" do
      downstream = "0942094a"
      svn = "875c3261"
      svn_2 = "98143abd"
      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with(eq('downstream'),anything(),schedule_options({downstream => "downstream/10/blah-stage/5", svn => "20", svn_2 => "45"}, {}), @status)
      allow(@status).to receive(:httpCode).and_return(202)
      allow(@status).to receive(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post :schedule, params: { :pipeline_name => 'downstream', "material_fingerprint" => {downstream => "downstream/10/blah-stage/5", svn => "20"}, "original_fingerprint" => {downstream => "downstream/10/blah-stage/2", svn => "30", svn_2 => "45"}, :no_layout => true}
    end

    it "should support using schedule time environment variable values while scheduling a pipeline" do
      downstream = "0942094a"
      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with('downstream', anything(),schedule_options({downstream => "downstream/10/blah-stage/5"}, {'foo' => 'foo_value', 'bar' => 'bar_value'}), @status)
      allow(@status).to receive(:httpCode).and_return(202)
      allow(@status).to receive(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post :schedule, params: { :pipeline_name => 'downstream', "material_fingerprint" => {downstream => "downstream/10/blah-stage/5"}, 'variables' => {'foo' => 'foo_value', 'bar' => 'bar_value'}, :no_layout => true}
    end

    it "should support using schedule time environment variable values while scheduling a pipeline" do
      downstream = "0942094a"
      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with('downstream', anything(), schedule_options({downstream => "downstream/10/blah-stage/5"}, {'foo' => 'foo_value', 'bar' => 'bar_value'}, {'secure_name' => 'secure_value'}), @status)
      allow(@status).to receive(:httpCode).and_return(202)
      allow(@status).to receive(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      warn "spec"
      warn({'foo' => 'foo_value', 'bar' => 'bar_value'}.symbolize_keys.to_param)
      post :schedule, params: { :pipeline_name => 'downstream', "material_fingerprint" => {downstream => "downstream/10/blah-stage/5"}, 'variables' => {'foo' => 'foo_value', 'bar' => 'bar_value'}, 'secure_variables' => {'secure_name' => 'secure_value'}, :no_layout => true}
    end

    it "should support empty material_fingerprints" do
      svn = "875c3261"
      expect(@pipeline_service).to receive(:manualProduceBuildCauseAndSave).with('downstream', anything(),schedule_options({svn => "30"}, {}), @status)
      allow(@status).to receive(:httpCode).and_return(202)
      allow(@status).to receive(:detailedMessage).and_return("accepted request to schedule pipeline badger")
      post :schedule, params: { :pipeline_name => 'downstream', "material_fingerprint" => {svn => ""}, "original_fingerprint" => {svn => "30"}, :no_layout => true}
    end
  end

  describe "pipeline_instance" do
    it "should load pipeline by id" do
      pipeline = PipelineInstanceModel.createPipeline("pipeline", 1, "label", BuildCause.createWithEmptyModifications(), stage_history_for("blah-stage"))
      expect(@pipeline_history_service).to receive(:load).with(10, "user", anything).and_return(pipeline)
      get :pipeline_instance, params: { :id => '10', :name => "pipeline", :no_layout => true}, format: :xml
      context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, nil)
      expect(assigns[:doc].asXML()).to eq(PipelineXmlViewModel.new(pipeline).toXml(context).asXML())
    end

    it "should respond with 404 when pipeline not found" do
      expect(@status).to receive(:canContinue).and_return(false)
      expect(@status).to receive(:detailedMessage).and_return("Not Found")
      allow(@status).to receive(:httpCode).and_return(404)
      expect(@pipeline_history_service).to receive(:load).with(10, "user", anything).and_return(nil)
      get :pipeline_instance, params: { :id => '10', :name => "pipeline", :no_layout => true }, format: :xml
      expect(response.status).to eq(404)
    end

    it "should respond with 401 when user does not have view permission" do
      expect(@status).to receive(:canContinue).and_return(false)
      expect(@status).to receive(:detailedMessage).and_return("Unauthorized")
      allow(@status).to receive(:httpCode).and_return(401)
      expect(@pipeline_history_service).to receive(:load).with(10, "user", anything).and_return(nil)
      get :pipeline_instance, params: { :id => '10', :name => "pipeline", :no_layout => true  }, format: :xml
      expect(response.status).to eq(401)
    end
  end

  describe "pipelines" do
    it "should assign pipeline_configs and latest instance of each pipeline configured" do
      expect(@pipeline_history_service).to receive(:latestInstancesForConfiguredPipelines).with("user").and_return(:pipeline_instance)
      get :pipelines, format: :xml, params: { :no_layout => true }
      expect(assigns[:pipelines]).to eq(:pipeline_instance)
    end
  end

  describe "stage_feed" do
    before :each do
      controller.go_cache.clear
    end

    it "should return the url to the feed" do
      params = {:name => 'cruise'}
      expect(controller.url(params.merge(UrlBuilder.default_url_options))).to eq("http://test.host/api/pipelines/cruise/stages.xml")
    end

    it "should return the url to the page given a stage locator" do
      expect(controller.page_url("cruise/1/dev/1", UrlBuilder.default_url_options)).to eq("http://test.host/pipelines/cruise/1/dev/1")
    end

    it "should set the stage feed from the java side" do
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get :stage_feed, params: { :name => 'pipeline', :no_layout => true }, format: :xml
      expect(assigns[:feed]).to eq(:stage_feed)
    end

    it "should set content type as application/atom+xml" do
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get :stage_feed, params: { :name => 'pipeline', :no_layout => true }, format: :xml
      expect(response.content_type).to eq("application/atom+xml")
    end

    it "should honor after if present" do
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:after)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get :stage_feed, params: { :after => 10, :name => 'pipeline', :no_layout => true }, format: :xml
      expect(assigns[:feed]).to eq(:stage_feed)
    end

    it "should honor before if present" do
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:before)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get :stage_feed, params: {:before => 10, :name => 'pipeline', :no_layout => true }, format: :xml
      expect(assigns[:feed]).to eq(:stage_feed)
    end

    it "should assign title"do
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get :stage_feed, params: {:name => 'pipeline', :no_layout => true}, format: :xml
      expect(assigns[:title]).to eq("pipeline")
    end

    it "should return 404 if the pipeline does not exist" do
      expect(controller).to receive(:render_error_response).with("Pipeline not found", 404, true)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('does_not_exist')).and_return(false)
      get :stage_feed, params: { :name => 'does_not_exist', :no_layout => true}, format: :xml
    end

    it "should render the error if there is any" do
      http_localized_operation_result = double(HttpLocalizedOperationResult)
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(http_localized_operation_result)

      allow(http_localized_operation_result).to receive(:message).and_return("Screwed")
      allow(http_localized_operation_result).to receive(:isSuccessful).and_return(false)
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), http_localized_operation_result, have_key(:controller)).and_return(:stage_feed)
      expect(controller).to receive(:render_localized_operation_result).with(http_localized_operation_result)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('does_not_exist')).and_return(true)
      get :stage_feed, params: { :name => 'does_not_exist', :no_layout => true }, format: :xml
    end
  end

  describe "releaseLock" do
    it "should call service and render operation result" do
      expect(@status).to receive(:detailedMessage).and_return("done")
      expect(@status).to receive(:httpCode).and_return(406)
      expect(@pipeline_unlock_api_service).to receive(:unlock).with('pipeline-name', @user, @status)

      post :releaseLock, params: { :pipeline_name => 'pipeline-name', :no_layout => true }
      expect(response.status).to eq(406)
      expect(response.body).to eq("done\n")
    end
  end

  describe "pause" do
    it "should pause the pipeline" do
      expect(@pipeline_pause_service).to receive(:pause).with("foo.bar", "wait for next checkin", Username.new(CaseInsensitiveString.new("someuser"), "Some User"), an_instance_of(HttpLocalizedOperationResult))
      allow(@controller).to receive(:current_user).and_return(Username.new(CaseInsensitiveString.new("someuser"), "Some User"))
      post :pause, params: { :pipeline_name => "foo.bar", :pauseCause => "wait for next checkin", :no_layout => true }
    end
  end

  describe "unpause" do
    it "should pause the pipeline" do
      expect(@pipeline_pause_service).to receive(:unpause).with("foo.bar", Username.new(CaseInsensitiveString.new("someuser"), "Some User"), an_instance_of(HttpLocalizedOperationResult))
      allow(@controller).to receive(:current_user).and_return(Username.new(CaseInsensitiveString.new("someuser"), "Some User"))
      post :unpause, params: { :pipeline_name => "foo.bar", :no_layout => true }
    end
  end

  def schedule_options(specified_revisions, variables, secure_variables = {})
    ScheduleOptions.new(specified_revisions.dup, variables.dup, secure_variables.dup)
  end

end
