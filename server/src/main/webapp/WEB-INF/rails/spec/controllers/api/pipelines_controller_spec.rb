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

  describe "history" do

    it "should render history json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:totalCount).and_return(10)
      expect(@pipeline_history_service).to receive(:loadMinimalData).with('up42', anything, loser, anything).and_return(create_pipeline_history_model)

      get :history, params:{:pipeline_name => 'up42', :offset => '5', :no_layout => true}

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

      get :history, params:{:pipeline_name => 'up42', :no_layout => true}

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end

    describe "route" do
      it "should route to history" do
        expect(:get => '/api/pipelines/up42/history').to route_to(:controller => "api/pipelines", :action => "history", :pipeline_name => 'up42', :offset => '0', :no_layout => true)
        expect(:get => '/api/pipelines/up42/history/1').to route_to(:controller => "api/pipelines", :action => "history", :pipeline_name => 'up42', :offset => '1', :no_layout => true)
      end

      describe "with_pipeline_name_contraint" do
        it 'should route to history action of pipelines controller having dots in pipeline name' do
          expect(:get => 'api/pipelines/some.thing/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: 'some.thing', :offset => '0')
        end

        it 'should route to history action of pipelines controller having hyphen in pipeline name' do
          expect(:get => 'api/pipelines/some-thing/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: 'some-thing', :offset => '0')
        end

        it 'should route to history action of pipelines controller having underscore in pipeline name' do
          expect(:get => 'api/pipelines/some_thing/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: 'some_thing', :offset => '0')
        end

        it 'should route to history action of pipelines controller having alphanumeric pipeline name' do
          expect(:get => 'api/pipelines/123foo/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: '123foo', :offset => '0')
        end

        it 'should route to history action of pipelines controller having capitalized pipeline name' do
          expect(:get => 'api/pipelines/FOO/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: 'FOO', :offset => '0')
        end

        it 'should not route to history action of pipelines controller for invalid pipeline name' do
          expect(:get => 'api/pipelines/fo$%#@6/history').to_not be_routable
        end
      end

    end
  end

  describe "instance_by_counter" do

    it "should render instance json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:findPipelineInstance).with('up42', 1, loser, anything).and_return(create_pipeline_model)

      get :instance_by_counter, params:{:pipeline_name => 'up42', :pipeline_counter => '1', :no_layout => true}

      expect(response.body).to eq(PipelineInstanceAPIModel.new(create_pipeline_model).to_json)
    end

    it "should render error correctly" do
      expect(@status).to receive(:canContinue).and_return(false)
      expect(@status).to receive(:detailedMessage).and_return("Not Acceptable")
      expect(@status).to receive(:httpCode).and_return(406)
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:findPipelineInstance).with('up42', 1, loser, @status)
      get :instance_by_counter, params:{:pipeline_name => 'up42', :pipeline_counter => '1', :no_layout => true}

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end

    describe "route" do
      describe "with_pipeline_name_contraint" do
        it 'should route to instance_by_counter action of pipelines controller having dots in pipeline name' do
          expect(:get => 'api/pipelines/some.thing/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: 'some.thing',  pipeline_counter: '1')
        end

        it 'should route to instance_by_counter action of pipelines controller having hyphen in pipeline name' do
          expect(:get => 'api/pipelines/some-thing/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: 'some-thing',  pipeline_counter: '1')
        end

        it 'should route to instance_by_counter action of pipelines controller having underscore in pipeline name' do
          expect(:get => 'api/pipelines/some_thing/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: 'some_thing',  pipeline_counter: '1')
        end

        it 'should route to instance_by_counter action of pipelines controller having alphanumeric pipeline name' do
          expect(:get => 'api/pipelines/123foo/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: '123foo',  pipeline_counter: '1')
        end

        it 'should route to instance_by_counter action of pipelines controller having capitalized pipeline name' do
          expect(:get => 'api/pipelines/FOO/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: 'FOO',  pipeline_counter: '1')
        end

        it 'should not route to instance_by_counter action of pipelines controller for invalid pipeline name' do
          expect(:get => 'api/pipelines/fo$%#@6/instance/1').to_not be_routable
        end
      end

      describe "with_pipeline_counter_constraint" do
        it 'should not route to instance_by_counter action of pipelines controller for invalid pipeline counter' do
          expect(:get => 'api/pipelines/some.thing/instance/fo$%#@6/2').to_not be_routable
        end
      end
    end
  end

  describe "status" do

    it "should render status json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:getPipelineStatus).with('up42', "loser", anything).and_return(create_pipeline_status_model)

      get :status, params:{:pipeline_name => 'up42', :no_layout => true}

      expect(response.body).to eq(PipelineStatusAPIModel.new(create_pipeline_status_model).to_json)
    end

    it "should render error correctly" do
      expect(@status).to receive(:canContinue).and_return(false)
      expect(@status).to receive(:detailedMessage).and_return("Not Acceptable")
      expect(@status).to receive(:httpCode).and_return(406)
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_history_service).to receive(:getPipelineStatus).with('up42', "loser", @status)

      get :status, params:{:pipeline_name => 'up42', :no_layout => true}

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end

    describe "route" do
      it "should route to status" do
        expect(:get => '/api/pipelines/up42/status').to route_to(:controller => "api/pipelines", :action => "status", :pipeline_name => 'up42', :no_layout => true)
      end

      describe "with_pipeline_name_contraint" do
        it 'should route to status action of pipelines controller having dots in pipeline name' do
          expect(:get => 'api/pipelines/some.thing/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: 'some.thing')
        end

        it 'should route to status action of pipelines controller having hyphen in pipeline name' do
          expect(:get => 'api/pipelines/some-thing/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: 'some-thing')
        end

        it 'should route to status action of pipelines controller having underscore in pipeline name' do
          expect(:get => 'api/pipelines/some_thing/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: 'some_thing')
        end

        it 'should route to status action of pipelines controller having alphanumeric pipeline name' do
          expect(:get => 'api/pipelines/123foo/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: '123foo')
        end

        it 'should route to status action of pipelines controller having capitalized pipeline name' do
          expect(:get => 'api/pipelines/FOO/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: 'FOO')
        end

        it 'should not route to status action of pipelines controller for invalid pipeline name' do
          expect(:get => 'api/pipelines/fo$%#@6/status').to_not be_routable
        end
      end

    end
  end

  describe "pipeline_instance" do
    it "should load pipeline by id" do
      pipeline = PipelineInstanceModel.createPipeline("pipeline", 1, "label", BuildCause.createWithEmptyModifications(), stage_history_for("blah-stage"))
      expect(@pipeline_history_service).to receive(:load).with(10, "user", anything).and_return(pipeline)
      get :pipeline_instance, params:{:id => '10', :name => "pipeline", :format => "xml", :no_layout => true}
      context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, SystemEnvironment.new)
      expect(assigns[:doc].asXML()).to eq(PipelineXmlViewModel.new(pipeline).toXml(context).asXML())
    end

    it "should respond with 404 when pipeline not found" do
      expect(@status).to receive(:canContinue).and_return(false)
      expect(@status).to receive(:detailedMessage).and_return("Not Found")
      allow(@status).to receive(:httpCode).and_return(404)
      expect(@pipeline_history_service).to receive(:load).with(10, "user", anything).and_return(nil)
      get :pipeline_instance, params:{:id => '10', :name => "pipeline", :format => "xml", :no_layout => true}
      expect(response.status).to eq(404)
    end

    it "should respond with 403 when user does not have view permission" do
      expect(@status).to receive(:canContinue).and_return(false)
      expect(@status).to receive(:detailedMessage).and_return("Unauthorized")
      allow(@status).to receive(:httpCode).and_return(403)
      expect(@pipeline_history_service).to receive(:load).with(10, "user", anything).and_return(nil)
      get :pipeline_instance, params:{:id => '10', :format => "xml", :name => "pipeline", :no_layout => true}
      expect(response.status).to eq(403)
    end

    it "should resolve url to action" do
      expect(:get => "/api/pipelines/pipeline.com/10.xml?foo=bar").to route_to(:controller => 'api/pipelines', :action => 'pipeline_instance', :name => "pipeline.com", :id => "10", :format => "xml", :foo  => "bar", :no_layout => true)
    end

    describe "route" do

      describe "with_pipeline_name_contraint" do
        it 'should route to pipeline_instance action of pipelines controller having dots in pipeline name' do
          expect(:get => 'api/pipelines/some.thing/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: 'some.thing', id: '1')
        end

        it 'should route to pipeline_instance action of pipelines controller having hyphen in pipeline name' do
          expect(:get => 'api/pipelines/some-thing/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: 'some-thing', id: '1')
        end

        it 'should route to pipeline_instance action of pipelines controller having underscore in pipeline name' do
          expect(:get => 'api/pipelines/some_thing/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: 'some_thing', id: '1')
        end

        it 'should route to pipeline_instance action of pipelines controller having alphanumeric pipeline name' do
          expect(:get => 'api/pipelines/123foo/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: '123foo', id: '1')
        end

        it 'should route to pipeline_instance action of pipelines controller having capitalized pipeline name' do
          expect(:get => 'api/pipelines/FOO/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: 'FOO', id: '1')
        end

        it 'should not route to pipeline_instance action of pipelines controller for invalid pipeline name' do
          expect(:get => 'api/pipelines/fo$%#@6/1.xml').to_not be_routable
        end
      end

    end
  end

  describe "pipelines" do
    it "should assign pipeline_configs and latest instance of each pipeline configured" do
      expect(@pipeline_history_service).to receive(:latestInstancesForConfiguredPipelines).with("user").and_return(:pipeline_instance)
      get :pipelines, params:{:format => "xml", :no_layout => true}
      expect(assigns[:pipelines]).to eq(:pipeline_instance)
    end

    describe "route" do
      it 'should resolve route to pipelines action' do
        expect(get: 'api/pipelines.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipelines')
      end
    end
  end

  describe "stage_feed" do
    before :each do
      controller.go_cache.clear
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
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', params:{:format => "xml", :no_layout => true, :name => 'pipeline'}
      expect(assigns[:feed]).to eq(:stage_feed)
    end

    it "should set content type as application/atom+xml" do
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', params:{:format => "xml", :no_layout => true, :name => 'pipeline'}
      expect(response.content_type).to eq("application/atom+xml")
    end

    it "should honor after if present" do
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:after)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', params:{:after => 10, :format => "xml", :no_layout => true, :name => 'pipeline'}
      expect(assigns[:feed]).to eq(:stage_feed)
    end

    it "should honor before if present" do
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:before)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', params:{:before => 10, :format => "xml", :no_layout => true, :name => 'pipeline'}
      expect(assigns[:feed]).to eq(:stage_feed)
    end

    it "should assign title"do
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), an_instance_of(HttpLocalizedOperationResult), have_key(:controller)).and_return(:stage_feed)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('pipeline')).and_return(true)
      get 'stage_feed', params:{:format => "xml", :no_layout => true, :name => 'pipeline'}
      expect(assigns[:title]).to eq("pipeline")
    end

    it "should return stages url with before and after params" do
      expected_routing_params = {:controller => "api/pipelines", :action => "stage_feed", :format => "xml", :name => 'cruise', :no_layout => true, :after => "2", :before => "bar"}
      expect(:get => api_pipeline_stage_feed_path(:after => 2, :before => "bar", :name => "cruise")).to route_to(expected_routing_params)
    end

    it "should return 404 if the pipeline does not exist" do
      expect(controller).to receive(:render_error_response).with("Pipeline not found", 404, true)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('does_not_exist')).and_return(false)
      get 'stage_feed', params:{:format => "xml", :no_layout => true, :name => 'does_not_exist'}
    end

    it "should render the error if there is any" do
      http_localized_operation_result = double(HttpLocalizedOperationResult)
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(http_localized_operation_result)

      allow(http_localized_operation_result).to receive(:message).and_return("Screwed")
      allow(http_localized_operation_result).to receive(:isSuccessful).and_return(false)
      expect(Feed).to receive(:new).with(@user, an_instance_of(PipelineStagesFeedService::PipelineStageFeedResolver), http_localized_operation_result, have_key(:controller)).and_return(:stage_feed)
      expect(controller).to receive(:render_localized_operation_result).with(http_localized_operation_result)
      expect(@go_config_service).to receive(:hasPipelineNamed).with(CaseInsensitiveString.new('does_not_exist')).and_return(true)
      get 'stage_feed', params:{:format => "xml", :no_layout => true, :name => 'does_not_exist'}
    end

    describe "route" do

      describe "with_pipeline_name_contraint" do
        it 'should route to stage_feed action of pipelines controller having dots in pipeline name' do
          expect(:get => 'api/pipelines/some.thing/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: 'some.thing')
        end

        it 'should route to stage_feed action of pipelines controller having hyphen in pipeline name' do
          expect(:get => 'api/pipelines/some-thing/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: 'some-thing')
        end

        it 'should route to stage_feed action of pipelines controller having underscore in pipeline name' do
          expect(:get => 'api/pipelines/some_thing/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: 'some_thing')
        end

        it 'should route to stage_feed action of pipelines controller having alphanumeric pipeline name' do
          expect(:get => 'api/pipelines/123foo/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: '123foo')
        end

        it 'should route to stage_feed action of pipelines controller having capitalized pipeline name' do
          expect(:get => 'api/pipelines/FOO/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: 'FOO')
        end

        it 'should not route to stage_feed action of pipelines controller for invalid pipeline name' do
          expect(:get => 'api/pipelines/fo$%#@6/stages.xml').to_not be_routable
        end
      end

    end
  end

  def schedule_options(specified_revisions, variables, secure_variables = {})
    ScheduleOptions.new(specified_revisions, variables, secure_variables)
  end

end
