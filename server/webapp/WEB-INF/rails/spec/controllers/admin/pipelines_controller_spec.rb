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

    allow(controller).to receive(:pluggable_task_service).and_return(@pluggable_task_service)
    allow(controller).to receive(:task_view_service).and_return(@task_view_service = double('Task View Service'))
    allow(controller).to receive(:package_definition_service).with(no_args).and_return(@package_definition_service = StubPackageDefinitionService.new)
    allow(controller).to receive(:pipeline_selections_service).and_return(@pipeline_selections_service = double('pipeline selections service'))
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

    it "should match /clone" do
      expect({:get => "/admin/pipeline/foo.bar/clone"}).to route_to(:controller => "admin/pipelines", :action => "clone", :pipeline_name => 'foo.bar')
      expect(pipeline_clone_path(:pipeline_name => "foo.bar")).to eq("/admin/pipeline/foo.bar/clone")
    end

    it "should match /save_clone" do
      expect({:post => "/admin/pipeline/save_clone"}).to route_to(:controller => "admin/pipelines", :action => "save_clone")
      expect(pipeline_save_clone_path).to eq("/admin/pipeline/save_clone")
    end

  end

  describe "pause_info" do
    before(:each) do
      pipeline_config = PipelineConfigMother.pipelineConfigWithMingleConfiguration("HelloWorld", "http://mingleurl.com:7823", "go", "'status' > 'In Dev'")

      pipeline_config_for_edit = ConfigForEdit.new(pipeline_config, BasicCruiseConfig.new, BasicCruiseConfig.new)

      @result = HttpLocalizedOperationResult.new
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)

      expect(@go_config_service).to receive(:loadForEdit).with('HelloWorld', @user, @result).and_return(pipeline_config_for_edit)
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
      pipeline_config = PipelineConfigMother.pipelineConfigWithMingleConfiguration("HelloWorld", "http://mingleurl.com:7823", "go", "'status' > 'In Dev'")
      pipeline_config.setLabelTemplate("some_label_template")
      @pipeline_config_for_edit = ConfigForEdit.new(pipeline_config, BasicCruiseConfig.new, BasicCruiseConfig.new)

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
      end

      describe "GET general" do
        it "should load pipeline name, pipeline label template" do
          get :edit, params:{:pipeline_name => "HelloWorld", :current_tab => 'general', :stage_parent => "pipelines"}

          expect(assigns[:pipeline].name()).to eq(CaseInsensitiveString.new("HelloWorld"))
          expect(assigns[:pipeline].getLabelTemplate()).to eq("some_label_template")
          expect(assigns[:pause_info]).to eq(@pause_info)
          assert_template layout: "pipelines/details"
        end
      end

      describe "GET project_management" do
        it "should load mingle gadget config" do
          get :edit, params:{:pipeline_name => "HelloWorld", :current_tab => 'project_management', :stage_parent=>"pipelines"}

          assigns[:pipeline].getMingleConfig().getProjectIdentifier() == "go"
          assigns[:pipeline].getMingleConfig().getQuotedMql() == "'status' > 'In Dev'"
          expect(assigns[:pause_info]).to eq(@pause_info)
        end
      end
    end

    describe "with view" do
      render_views

      before do
        allow(@go_config_service).to receive(:isSecurityEnabled).and_return(false)
      end

      it "should error out when user is unauthorized" do
        expect(@go_config_service).to receive(:loadForEdit).with('HelloWorld', anything(), anything()) do |_, _, result|
          result.forbidden('Unauthorized to edit HelloWorld pipeline.', HealthStateType.forbidden_for_pipeline("HelloWorld"))
          nil
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
    end

    describe "for authorized user" do
      before(:each) do
        @pause_info = PipelinePauseInfo.paused("just for fun", "loser")

        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").at_least(1).and_return(@pause_info)
      end

      it "should set config attributes on pipeline when updating" do
        stub_save_for_success

        put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'general', :pipeline => {"labelTemplate" => "${COUNT}-something"}, :config_md5 => "md5", :stage_parent=>"pipelines"}

        expect(assigns[:pipeline]).to eq(@pipeline)
        expect(@pipeline.getLabelTemplate()).to eq("${COUNT}-something")
        expect(assigns[:pause_info]).to eq(@pause_info)
      end

      it "should set variables and parameters to empty if not sent in params" do
        stub_save_for_success
        @pipeline.variables().add("key1", "value1")

        put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'general', :config_md5 => "md5", :default_as_empty_list => ["pipeline>variables"], :stage_parent=>"pipelines"}

        expect(assigns[:pipeline].variables().isEmpty()).to eq(true)

        @pipeline.addParam(ParamConfig.new("param1", "value1"))

        stub_save_for_success

        put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'general', :config_md5 => "md5", :default_as_empty_list => ["pipeline>params"], :stage_parent=>"pipelines"}

        expect(assigns[:pipeline].getParams().isEmpty()).to eq(true)
        expect(assigns[:pause_info]).to eq(@pause_info)
      end

      it "should update only if configuration is valid" do
        stub_save_for_validation_error do |result, _, node|
          node.addError("labelTemplate", "invalid-label")
          result.badRequest("Failed to update pipeline 'pipeline-name'.")
        end

        put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'general', :pipeline => {"labelTemplate" => "${COUNT}-#junk"}, :config_md5 => "md5", :stage_parent=>"pipelines"}

        expect(assigns[:errors].size).to eq(0)
        expect(assigns[:pause_info]).to eq(@pause_info)
        assert_template layout: "pipelines/details"
      end

      describe "params" do
        it "should report errors on deleted params that are referenced elsewhere" do
          stub_save_for_validation_error do |result, _, node|
            node.addError("labelTemplate", ParamSubstitutionHandler::NO_PARAM_FOUND_MSG.gsub("'%s'", "'to-be-deleted'"))
            result.badRequest("Failed to update pipeline 'pipeline-name'.")
          end

          @pipeline.addParam(ParamConfig.new("to-be-deleted", "original-deleted-value"))
          @pipeline.addParam(ParamConfig.new("to-be-modified", "original-value"))

          put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'parameters', :config_md5 => "md5", :stage_parent=>"pipelines", :default_as_empty_list => ["pipeline>params"],
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
          stub_save_for_validation_error do |result, _, node|
            node.addError("labelTemplate", ParamSubstitutionHandler::NO_PARAM_FOUND_MSG.gsub("'%s'", "'to-be-deleted'"))
            result.badRequest("Failed to update pipeline 'pipeline-name'.")
          end

          @pipeline.addParam(ParamConfig.new("to-be-deleted", "original-deleted-value"))
          @pipeline.addParam(ParamConfig.new("to-be-modified", "original-value"))

          put :update, params:{:pipeline_name => "pipeline-name", :current_tab => 'parameters', :config_md5 => "md5", :stage_parent=>"pipelines", :default_as_empty_list => ["pipeline>params"], :pipeline => {:params => [{:name => "renamed", :valueForDisplay => "renamed-value", :original_name => "to-be-deleted"},
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

  describe "clone" do

    before :each do
      allow(controller).to receive(:go_config_service).with(no_args).and_return(@go_config_service = double('Go Config Service'))
      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      allow(controller).to receive(:security_service).and_return(@security_service = double('Security Service'))
      @cruise_config = BasicCruiseConfig.new
      @pipeline = PipelineConfigMother.pipelineConfig("foo.bar")
      @cruise_config.addPipeline("group1", @pipeline)
      expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
      expect(@security_service).to receive(:modifiableGroupsForUser).with(@user).and_return(["dev", "group1", "Docs", "group2", "ApiDocs", "api"])
      allow(@go_config_service).to receive(:registry)
    end

    describe "clone:get" do
      it "should populate variables for cloning pipeline with pipeline name and group" do
        get :clone, params:{:pipeline_name => "foo.bar", :config_md5 => "1234abcd", :group => "group1"}

        clonedPipeline = @pipeline.duplicate()
        expect(assigns[:pipeline]).to eq(clonedPipeline)
        expect(assigns[:pipeline_group]).to eq(BasicPipelineConfigs.new([clonedPipeline].to_java(PipelineConfig)))
        expect(assigns[:group_name]).to eq("group1")
        expect(assigns[:groups_list]).to eq(["api", "ApiDocs", "dev", "Docs", "group1", "group2"])
        expect(assigns[:groups_json]).to eq([{"group" => "api"}, {"group" => "ApiDocs"}, {"group" => "dev"}, {"group" => "Docs"}, {"group" => "group1"}, {"group" => "group2"}].to_json)
        assert_template layout: false
      end
    end

    describe "save_clone" do
      before :each do
        allow(@pipeline_pause_service).to receive(:pause)
        allow(@pipeline_selections_service).to receive(:update)
      end

      it "should save cloned pipeline successfully" do
        @cruise_config.addPipeline("foo.bar", @pipeline)
        stub_save_for_success
        expect(@pipeline_pause_service).to receive(:pause).with("new-pip", "Under construction", @user)

        post :save_clone, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "group1", :pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s}

        expect(@cruise_config.getAllErrors().size).to eq(0)
        expect(assigns[:pause_info]).to eq(@pause_info)

        expect(response.location).to match(/\/admin\/pipelines\/new-pip\/general/)
        expect(response.status).to eq(200)
        expect(response.body).to eq("Saved successfully")
      end


      it "should save cloned pipeline successfully when group is not set" do
        @cruise_config.addPipeline("foo.bar", @pipeline)
        stub_save_for_success
        expect(@pipeline_pause_service).to receive(:pause).with("new-pip", "Under construction", @user)

        post :save_clone, params:{:config_md5 => "1234abcd", :pipeline_group => {:pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s}

        expect(@cruise_config.getAllErrors().size).to eq(0)
        expect(assigns[:pause_info]).to eq(@pause_info)

        expect(response.location).to match(/\/admin\/pipelines\/new-pip\/general/)
        expect(response.status).to eq(200)
        expect(response.body).to eq("Saved successfully")
      end


      it "should show validation errors" do
        @cruise_config.addPipeline("foo.bar", @pipeline)
        stub_save_for_validation_error do |result, _, node|
           node.addError("name", "Pipeline name is not unique")
           result.badRequest("Failed to update pipeline 'pipeline-name'.")
        end

        post :save_clone, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "group1", :pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s}

        expect(@cruise_config.getAllErrors().size).to eq(1)
        expect(assigns[:errors].size).to eq(1)
        expect(response.status).to eq(400)
        expect(response.location).to be_nil
      end

      it "should update the user's pipeline selections when save clone is successful" do
        @cruise_config.addPipeline("foo.bar", @pipeline)
        stub_save_for_success

        current_user_entity_id = 9999
        allow(controller).to receive(:current_user_entity_id).and_return(current_user_entity_id)

        selected_pipeline_id = "456"
        allow(controller).to receive(:cookies).and_return(cookiejar={:selected_pipelines => selected_pipeline_id})

        expect(@pipeline_selections_service).to receive(:update).with(selected_pipeline_id, current_user_entity_id, CaseInsensitiveString.new("new-pip"))

        post :save_clone, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "group1", :pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s}
      end

      it "should not update the user's pipeline selections when save clone is not successful" do
        stub_save_for_validation_error do |result, _, _|
          result.badRequest("Failed to update pipeline 'pipeline-name'.")
        end

        post :save_clone, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "group1", :pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s}

        expect(@pipeline_selections_service).to_not receive(:update)
      end
    end
  end

  describe "clone with error" do
    before :each do
      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      @cruise_config = BasicCruiseConfig.new
      expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
      allow(@go_config_service).to receive(:registry)
    end
    it "should render error if pipeline to be cloned does not exist" do
      get :clone, params:{:pipeline_name => "doesNotExist", :config_md5 => "1234abcd", :group => "group1"}

      expect(response.response_code).to eq(404)
      assert_template layout: "application"
    end
  end
end
