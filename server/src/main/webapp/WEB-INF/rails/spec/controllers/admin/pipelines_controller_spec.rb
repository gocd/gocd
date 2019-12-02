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

describe Admin::PipelinesController do
  before do
    allow(controller).to receive(:pipeline_pause_service).with(no_args).and_return(@pipeline_pause_service = double('Pipeline Pause Service'))
  end

  include ConfigSaveStubbing
  include TaskMother

  before(:each) do
    @user = Username.new(CaseInsensitiveString.new("loser"))
    allow(controller).to receive(:current_user).and_return(@user)
    allow(controller).to receive(:go_config_service).with(no_args).and_return(@go_config_service = double('Go Config Service'))
    @template_config_service = double('Template Config Service')
    allow(controller).to receive(:template_config_service).and_return(@template_config_service)
    allow(controller).to receive(:security_service).and_return(@security_service = double('Security Service'))
    @pluggable_task_service = double('Pluggable_task_service')
    @pipeline_config_service = double("Pipeline config service")
    allow(controller).to receive(:pipeline_config_service).and_return(@pipeline_config_service)

    allow(controller).to receive(:pluggable_task_service).and_return(@pluggable_task_service)
    allow(controller).to receive(:task_view_service).and_return(@task_view_service = double('Task View Service'))
    allow(controller).to receive(:package_definition_service).with(no_args).and_return(@package_definition_service = StubPackageDefinitionService.new)
    allow(controller).to receive(:pipeline_selections_service).and_return(@pipeline_selections_service = double('pipeline selections service'))
    allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service = double('entity hashing service'))
    allow(@entity_hashing_service).to receive(:md5ForEntity).and_return('pipeline-md5')
  end

  describe "routes" do
    it "should match /edit" do
      expect({:get => "/admin/pipelines/foo.bar/general"}).to route_to(:controller => "admin/pipelines", :action => "edit", :pipeline_name => 'foo.bar', :current_tab => 'general', :stage_parent => "pipelines")
    end

    it "should match /update" do
      expect({:put => "/admin/pipelines/foo.baz/general"}).to route_to(:controller => "admin/pipelines", :action => "update", :pipeline_name => 'foo.baz', :current_tab => 'general', :stage_parent => "pipelines")
    end

    it "should match /pause_info" do
      expect({:get => "/admin/pipelines/foo.baz/pause_info.json"}).to route_to(:controller => "admin/pipelines", :action => "pause_info", :pipeline_name => 'foo.baz', :format => "json")
      expect(pause_info_refresh_path(:pipeline_name => 'foo.baz')).to eq("/admin/pipelines/foo.baz/pause_info.json")
    end

    it "should match /save_clone" do
      expect({:post => "/admin/pipeline/save_clone"}).to route_to(:controller => "admin/pipelines", :action => "save_clone")
      expect(pipeline_save_clone_path).to eq("/admin/pipeline/save_clone")
    end

  end

  describe "pause_info" do
    before(:each) do
      go_config_mother = GoConfigMother.new
      cruise_config = BasicCruiseConfig.new
      pipeline_config = go_config_mother.addPipeline(cruise_config, "HelloWorld", "foo")

      pipeline_config_for_edit = ConfigForEdit.new(pipeline_config, cruise_config, cruise_config)

      @result = HttpLocalizedOperationResult.new
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)

      expect(@go_config_service).to receive(:loadForEdit).with('HelloWorld', @user, @result).and_return(pipeline_config_for_edit)
      expect(@go_config_service).to receive(:doesPipelineExist).and_return(true)
      expect(@go_config_service).to receive(:isPipelineDefinedInConfigRepository).and_return(false)
      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      allow(@go_config_service).to receive(:registry)

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("HelloWorld").and_return(@pause_info)
    end

    it "should load pause_info for json" do
      get :pause_info,params:{:pipeline_name => "HelloWorld", :format => "json"}

      expect(assigns[:pipeline]).not_to be_nil
      expect(assigns[:pause_info]).to eq(@pause_info)
    end
  end

  describe "edit" do
    before(:each) do
      go_config_mother = GoConfigMother.new
      cruise_config = BasicCruiseConfig.new
      pipeline_config = go_config_mother.addPipeline(cruise_config, "HelloWorld", "foo")
      pipeline_config.setLabelTemplate("some_label_template")

      @pipeline_config_for_edit = ConfigForEdit.new(pipeline_config, cruise_config, cruise_config)

      @result = HttpLocalizedOperationResult.new
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)

      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      allow(@go_config_service).to receive(:registry)
    end

    describe "for authorized user" do
      before(:each) do
        @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("HelloWorld").and_return(@pause_info)
        expect(@go_config_service).to receive(:loadForEdit).with('HelloWorld', @user, @result).and_return(@pipeline_config_for_edit)
        expect(@go_config_service).to receive(:doesPipelineExist).and_return(true)
        expect(@go_config_service).to receive(:isPipelineDefinedInConfigRepository).and_return(false)
      end

      describe "GET general" do
        it "should load pipeline name, pipeline label template" do
          get :edit, params:{:pipeline_name => "HelloWorld", :current_tab => 'general', :stage_parent => "pipelines"}

          expect(assigns[:pipeline].name()).to eq(CaseInsensitiveString.new("HelloWorld"))
          expect(assigns[:pipeline].getLabelTemplate()).to eq("some_label_template")
          expect(assigns[:pause_info]).to eq(@pause_info)
          expect(assigns[:pipeline_group_name]).to eq('defaultGroup')
          expect(assigns[:pipeline_md5]).to eq('pipeline-md5')
          assert_template layout: "pipelines/details"
        end
      end
    end

    describe "with view" do
      render_views

      before do
        allow(@go_config_service).to receive(:isSecurityEnabled).and_return(false)
      end

      it "should error out when user is unauthorized" do
        expect(@go_config_service).to receive(:doesPipelineExist).and_return(true)
        expect(@go_config_service).to receive(:isPipelineDefinedInConfigRepository).and_return(false)
        expect(@go_config_service).to receive(:loadForEdit).with('HelloWorld', anything(), anything()) do |_, _, result|
          result.forbidden('Unauthorized to edit HelloWorld pipeline.', HealthStateType.forbidden_for_pipeline("HelloWorld"))
          @pipeline_config_for_edit
        end

        get :edit, params:{:pipeline_name => "HelloWorld", :current_tab => 'general', :stage_parent=>"pipelines"}

        expect(assigns[:pipeline]).to be_nil
        expect(response.status).to eq(403)
        expect(response.body).to have_selector("h3", :text => "Unauthorized to edit HelloWorld pipeline.")
      end
    end
  end

  describe "update" do
    before(:each) do
      allow(controller).to receive(:populate_config_validity)

      @cruise_config = BasicCruiseConfig.new()
      cruise_config_mother = GoConfigMother.new
      @pipeline = cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      @go_config_service = stub_service(:go_config_service)
      allow(@go_config_service).to receive(:registry)
      allow(@go_config_service).to receive(:getCurrentConfig).and_return(@cruise_config)

      @pipeline_config_service = stub_service(:pipeline_config_service)
      allow(@pipeline_config_service).to receive(:getPipelineConfig).with(an_instance_of(String)).and_return(@pipeline)
    end

    describe "for authorized user" do
      before(:each) do
        @pause_info = PipelinePauseInfo.paused("just for fun", "loser")

        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").at_least(1).and_return(@pause_info)
      end

      it "should set config attributes on pipeline when updating" do
        result = HttpLocalizedOperationResult.new
        result.setMessage("saved successfully")
        expect(@pipeline_config_service).to receive(:updatePipelineConfig).and_return(result)
        request_params = {:pipeline_name => "pipeline-name", :current_tab => 'general',
                          :pipeline_md5 => "pipeline-md5", :pipeline_group_name => 'my-group',
                          :pipeline => {"labelTemplate" => "${COUNT}-something"}, :config_md5 => "md5", :stage_parent => "pipelines"}

        put :update, params: request_params

        expect(assigns[:pipeline].labelTemplate).to eq("${COUNT}-something")
        expect(assigns[:pause_info]).to eq(@pause_info)
        expect(assigns[:pipeline_group_name]).to eq('my-group')
        expect(assigns[:pipeline_md5]).to eq('pipeline-md5')
      end

      it "should set variables and parameters to empty if not sent in params" do
        result = HttpLocalizedOperationResult.new
        result.setMessage("saved successfully")
        expect(@pipeline_config_service).to receive(:updatePipelineConfig).and_return(result)
        @pipeline.variables().add("key1", "value1")

        put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'general', :config_md5 => "md5",
                             :pipeline_md5 => "pipeline-md5", :pipeline_group_name => 'my-group',
                             :default_as_empty_list => ["pipeline>variables"], :stage_parent=>"pipelines"}

        expect(assigns[:pipeline].variables().isEmpty()).to eq(true)

        expect(@pipeline_config_service).to receive(:updatePipelineConfig).and_return(result)
        @pipeline.addParam(ParamConfig.new("param1", "value1"))

        put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'general',
                             :pipeline_md5 => "pipeline-md5", :pipeline_group_name => 'my-group',
                             :config_md5 => "md5", :default_as_empty_list => ["pipeline>params"], :stage_parent=>"pipelines"}

        expect(assigns[:pipeline].getParams().isEmpty()).to eq(true)
        expect(assigns[:pause_info]).to eq(@pause_info)
        expect(assigns[:pipeline_group_name]).to eq('my-group')
        expect(assigns[:pipeline_md5]).to eq('pipeline-md5')
      end

      it "should update only if configuration is valid" do
        result = HttpLocalizedOperationResult.new
        result.badRequest("Failed to update pipeline 'pipeline-name'.")
        expect(@pipeline_config_service).to receive(:updatePipelineConfig).and_return(result)

        put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'general', :pipeline_group_name => 'my-group',
                             :pipeline_md5 => "pipeline-md5",
                             :pipeline => {"labelTemplate" => "${COUNT}-#junk"}, :config_md5 => "md5", :stage_parent=>"pipelines"}

        expect(assigns[:errors].size).to eq(0)
        expect(assigns[:pause_info]).to eq(@pause_info)
        expect(assigns[:pipeline_group_name]).to eq('my-group')
        expect(assigns[:pipeline_md5]).to eq('pipeline-md5')
        assert_template layout: "pipelines/details"
      end

      describe "params" do
        it "should report errors on deleted params that are referenced elsewhere" do
          result = HttpLocalizedOperationResult.new
          result.badRequest("Failed to update pipeline 'pipeline-name'.")
          expect(@pipeline_config_service).to receive(:updatePipelineConfig).and_return(result)
          @pipeline.addError("labelTemplate", ParamSubstitutionHandler::NO_PARAM_FOUND_MSG.gsub("'%s'", "'to-be-deleted'"))

          @pipeline.addParam(ParamConfig.new("to-be-deleted", "original-deleted-value"))
          @pipeline.addParam(ParamConfig.new("to-be-modified", "original-value"))

          put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'parameters', :config_md5 => "md5",
                               :stage_parent=>"pipelines", :default_as_empty_list => ["pipeline>params"],
                               :pipeline_md5 => "pipeline-md5", :pipeline_group_name => 'my-group',
              :pipeline => {:params => [{:name => "to-be-modified", :valueForDisplay => "modified-value"},
                                        {:name => "added", :valueForDisplay => "added-value"}]}}
          expect(assigns[:pause_info]).to eq(@pause_info)
          params = assigns[:pipeline].getParams()
          expect(params.size()).to eq(3)

          deleted = params.get(0)
          expect(deleted.getName()).to eq("to-be-deleted")
          expect(deleted.getValue()).to eq("original-deleted-value")
          expect(deleted.errors().on(ParamConfig::NAME)).to eq("Parameter cannot be deleted because it is referenced by other elements")

          mod = params.get(1)
          expect(mod.getName()).to eq("to-be-modified")
          expect(mod.getValue()).to eq("modified-value")
          expect(mod.getValueForDisplay()).to eq("modified-value")

          added = params.get(2)
          expect(added.getName()).to eq("added")
          expect(added.getValue()).to eq("added-value")
          expect(added.getValueForDisplay()).to eq("added-value")
        end

        it "should report errors on renamed params that are referenced elsewhere" do
          result = HttpLocalizedOperationResult.new
          result.badRequest("Failed to update pipeline 'pipeline-name'.")
          expect(@pipeline_config_service).to receive(:updatePipelineConfig).and_return(result)
          @pipeline.addError("labelTemplate", ParamSubstitutionHandler::NO_PARAM_FOUND_MSG.gsub("'%s'", "'to-be-deleted'"))

          @pipeline.addParam(ParamConfig.new("to-be-deleted", "original-deleted-value"))
          @pipeline.addParam(ParamConfig.new("to-be-modified", "original-value"))

          put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'parameters', :config_md5 => "md5",
                               :stage_parent=>"pipelines", :default_as_empty_list => ["pipeline>params"],
                               :pipeline_group_name => 'my-group', :pipeline_md5 => "pipeline-md5",
                               :pipeline => {:params => [{:name => "renamed", :valueForDisplay => "renamed-value", :original_name => "to-be-deleted"},
                                                                                                                                                                                                                   {:name => "to-be-modified", :valueForDisplay => "modified-value"}]}}
          params = assigns[:pipeline].getParams()
          expect(params.size()).to eq(2)

          renamed = params.get(0)
          expect(renamed.getName()).to eq("renamed")
          expect(renamed.getValue()).to eq("renamed-value")
          expect(renamed.errors().on(ParamConfig::NAME)).to eq("Parameter 'to-be-deleted' cannot be renamed because it is referenced by other elements")

          mod = params.get(1)
          expect(mod.getName()).to eq("to-be-modified")
          expect(mod.getValue()).to eq("modified-value")
        end
      end
    end

  end
end
