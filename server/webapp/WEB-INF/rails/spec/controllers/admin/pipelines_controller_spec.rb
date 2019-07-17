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

    it "should match /new" do
      expect({:get => "/admin/pipeline/new"}).to route_to(:controller => "admin/pipelines", :action => "new")
      expect(pipeline_new_path).to eq("/admin/pipeline/new")
      expect(pipeline_new_path(:group => "foo.bar")).to eq("/admin/pipeline/new?group=foo.bar")
    end

    it "should match /create" do
      expect({:post => "/admin/pipelines"}).to route_to(:controller => "admin/pipelines", :action => "create")
      expect(pipeline_create_path).to eq("/admin/pipelines")
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

  describe "new" do

    before(:each) do
      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      allow(@go_config_service).to receive(:registry)
      allow(@go_config_service).to receive(:rolesForUser).and_return(nil)
      @cruise_config = BasicCruiseConfig.new
      expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
      @cruise_config_mother = GoConfigMother.new
      expect(@task_view_service).to receive(:getTaskViewModels).and_return(Object.new())
      expect(@security_service).to receive(:modifiableGroupsForUser).with(@user).and_return(["group1", "group2"])
    end

    it "should have a new pipeline group with a pipeline in it" do
      expect(@go_config_service).to receive(:getCurrentConfig).and_return(@cruise_config)

      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])

      get :new

      job_configs = JobConfigs.new([JobConfig.new(CaseInsensitiveString.new("defaultJob"), ResourceConfigs.new, ArtifactConfigs.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))].to_java(JobConfig))
      pipeline = PipelineConfig.new(CaseInsensitiveString.new(""), MaterialConfigs.new, [StageConfig.new(CaseInsensitiveString.new("defaultStage"), job_configs)].to_java(StageConfig))
      expect(assigns[:pipeline_group]).to eq(BasicPipelineConfigs.new([pipeline].to_java(PipelineConfig)))
      expect(assigns[:pipeline]).to eq(pipeline)
      expect(assigns[:all_pipelines]).to eq(java.util.ArrayList.new)
      expect(assigns[:cruise_config]).to eq(@cruise_config)
      expect(assigns[:original_cruise_config]).to eq(@cruise_config)
      assert_template layout: "application"
    end

    it "should populate group name if adding to an existing group and get all existing pipelines as list of string" do
      cruise_config_mother = GoConfigMother.new
      cruise_config_mother.addPipeline(@cruise_config, "new_pipeline", "stageName", ["jobname"].to_java(java.lang.String))
      expect(@go_config_service).to receive(:getCurrentConfig).and_return(@cruise_config)
      allow(@security_service).to receive(:hasViewOrOperatePermissionForPipeline).and_return(true)
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])

      get :new, params:{:group => "foo.bar"}

      expect(assigns[:group_name]).to eq("foo.bar")
      list_of_pipelines = java.util.ArrayList.new
      list_of_pipelines.add(CaseInsensitiveString.new("new_pipeline"))
      expect(assigns[:all_pipelines]).to eq(list_of_pipelines)
    end

    it "should have template list assigned" do
      expect(@go_config_service).to receive(:getCurrentConfig).and_return(@cruise_config)
      template_name = "someTemplateName"
      GoConfigMother.new.addPipelineWithTemplate(@cruise_config, "someTemplatePipeline", template_name, "stageName", ["jobName"].to_java(java.lang.String))

      list_of_templates = [TemplatesViewModel.new(@cruise_config.getTemplateByName(CaseInsensitiveString.new(template_name)), true, true)]
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return(list_of_templates)

      get :new

      expect(assigns[:template_list]).to eq(list_of_templates)
    end

    it "should have pipelines using templates listed in the pipelineStageJsontemplate list assigned" do
      template_name = "someTemplateName"
      cruise_config_interpolated = BasicCruiseConfig.new
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      @cruise_config_mother.addPipelineWithTemplate(@cruise_config, "someTemplatePipeline", template_name, "stageName", ["jobName"].to_java(java.lang.String))

      @cruise_config_mother.addPipeline(cruise_config_interpolated, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(cruise_config_interpolated, "someTemplatePipeline", "templateStage", ["templateJob"].to_java(java.lang.String))

      list_of_templates = [TemplatesViewModel.new(@cruise_config.getTemplateByName(CaseInsensitiveString.new(template_name)), true, true)]
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return(list_of_templates)

      allow(@security_service).to receive(:hasViewOrOperatePermissionForPipeline).and_return(true)

      expect(@go_config_service).to receive(:getCurrentConfig).and_return(cruise_config_interpolated)

      get :new

      expect(assigns[:pipeline_stages_json]).to eq("[{\"pipeline\":\"pipeline2\",\"stage\":\"stage-2\"},{\"pipeline\":\"someTemplatePipeline\",\"stage\":\"templateStage\"}]")
    end

    it "should have modifiable group and pipelineStageJson for DependencyMaterial is assigned" do
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "a", "b", ["job-1"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline1", "stage-1", ["job-1"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "Ab", "stage-1", ["job-1"].to_java(java.lang.String))

      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      allow(@security_service).to receive(:hasViewOrOperatePermissionForPipeline).and_return(true)

      expect(@go_config_service).to receive(:getCurrentConfig).and_return(@cruise_config)

      get :new

      expect(assigns[:groups_json]).to eq([{"group" => "group1"}, {"group" => "group2"}].to_json)
      expect(assigns[:pipeline_stages_json]).to eq("[{\"pipeline\":\"a\",\"stage\":\"b\"},{\"pipeline\":\"Ab\",\"stage\":\"stage-1\"},{\"pipeline\":\"pipeline1\",\"stage\":\"stage-1\"},{\"pipeline\":\"pipeline2\",\"stage\":\"stage-2\"}]")
    end
  end

  describe "create" do
    before :each do
      set_up_registry
      task_preference = com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference.new(TaskMother::ApiTaskForTest.new)
      PluggableTaskConfigStore.store().setPreferenceFor("curl.plugin", task_preference)
    end

    after :each do
      unload_all_from_registry
      PluggableTaskConfigStore.store().removePreferenceFor("curl.plugin")
    end

    before(:each) do
      PackageMetadataStore.getInstance().removeMetadata("pluginid")

      @cruise_config_mother = GoConfigMother.new
      @result = HttpLocalizedOperationResult.new
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)

      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      allow(@go_config_service).to receive(:rolesForUser).and_return(nil)
      @cruise_config = BasicCruiseConfig.new
      @repository1 = PackageRepositoryMother.create("repo-id", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
      @pkg = PackageDefinitionMother.create("pkg-id", "package3-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "p3v2")].to_java(ConfigurationProperty)), @repository1)
      @repository1.setPackages(Packages.new([@pkg].to_java(PackageDefinition)))
      repos = PackageRepositories.new
      repos.add(@repository1)
      @cruise_config.setPackageRepositories(repos)
      expect(@go_config_service).to receive(:getConfigForEditing).and_return(GoConfigCloner.new().deepClone(@cruise_config))
      allow(@go_config_service).to receive(:registry)
      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      allow(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("new-pip").and_return(@pause_info)

      allow(@pipeline_selections_service).to receive(:update)
      allow(controller).to receive(:cookies).and_return({})
      allow(@security_service).to receive(:modifiableGroupsForUser).with(@user).and_return(["group1", "group2"])
    end

    after(:each) do
      PackageMetadataStore.getInstance().removeMetadata("pluginid")
    end

    it "should populate group name from the submitted value if it is present" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))

      stub_save_for_success
      expect(@pipeline_pause_service).to receive(:pause).with("new-pip", "Under construction", @user)

      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip"}}}

      expect(assigns[:group_name]).to eq("new-group")
    end

    it "should create a new pipeline in a new pipeline group and pause the pipeline" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))

      stub_save_for_success
      expect(@pipeline_pause_service).to receive(:pause).with("new-pip", "Under construction", @user)

      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip"}}}

      expect(@cruise_config.getAllErrors().size).to eq(0)
      expect(assigns[:pause_info]).to eq(@pause_info)
      assert_save_arguments
      assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
      expect(response).to redirect_to anything
    end

    it "should update a users pipeline selections when that user successfully creates a new pipeline" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      pipeline_name = "new-pip"

      current_user_entity_id = 9999
      allow(controller).to receive(:current_user_entity_id).and_return(current_user_entity_id)

      selected_pipeline_id = "456"
      allow(controller).to receive(:cookies).and_return(cookiejar={:selected_pipelines => selected_pipeline_id})

      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))
      expect(@pipeline_pause_service).to receive(:pause).with("new-pip", "Under construction", @user)
      expect(@pipeline_selections_service).to receive(:update).with(selected_pipeline_id, current_user_entity_id, CaseInsensitiveString.new(pipeline_name))

      stub_save_for_success
      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => pipeline_name}}}
    end

    it "should NOT update a users pipeline selections when that user does not successfully creates a new pipeline" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      expect(@task_view_service).to receive(:getTaskViewModels).and_return(Object.new)
      expect(@task_view_service).to receive(:getViewModel).with(anything, anything).and_return(TaskViewModel.new(nil, nil))
      expect(@task_view_service).to receive(:getModelOfType).with(anything, anything).and_return(TaskViewModel.new(nil, nil))
      allow(@security_service).to receive(:hasViewOrOperatePermissionForPipeline).and_return(true)

      pipeline_name = "new-pip"

      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))
      expect(@pipeline_selections_service).not_to receive(:update)

      stub_save_for_validation_error do |result, _, _|
        result.forbidden("unauthorized", nil)
      end
      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => pipeline_name}}}
    end

    it "should create a new pipeline based on a template" do
      template_name = "some_template"
      @cruise_config.addTemplate(PipelineTemplateConfigMother.createTemplate(template_name))
      list_of_templates = [TemplatesViewModel.new(@cruise_config.getTemplateByName(CaseInsensitiveString.new(template_name)), true, true)]
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return(list_of_templates)

      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))

      stub_save_for_success
      expect(@pipeline_pause_service).to receive(:pause).with("new-pip", "Under construction", @user)

      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip", :configurationType => PipelineConfig::CONFIGURATION_TYPE_TEMPLATE, :templateName => template_name}}}

      expect(@cruise_config.getAllErrors().size).to eq(0)
      expect(assigns[:pause_info]).to eq(@pause_info)
      assert_save_arguments
      assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
      expect(response).to redirect_to anything
    end

    it "should throw up if pipeline name is empty and populate all states required for new action" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))

      task_view_models = Object.new()
      expect(@task_view_service).to receive(:getTaskViewModels).and_return(task_view_models)
      expect(@task_view_service).to receive(:getViewModel).with(anything, anything).and_return(TaskViewModel.new(nil, nil))
      expect(@task_view_service).to receive(:getModelOfType).with(anything, anything).and_return(TaskViewModel.new(nil, nil))
      allow(@security_service).to receive(:hasViewOrOperatePermissionForPipeline).and_return(true)

      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("").and_return(@pause_info)

      stub_save_for_validation_error do |result, cruise_config, pipeline|
        pipeline.addError("name", "empty pipeline name")
        result.badRequest("Save failed, see errors below");
      end

      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => ""}}}

      expect(assigns[:errors].size).to eq(1)
      expect(assigns[:errors][0]).to eq("empty pipeline name")
      expect(assigns[:groups_json]).to eq([{"group" => "group1"}, {"group" => "group2"}].to_json)
      expect(assigns[:pipeline_stages_json]).to eq("[{\"pipeline\":\"pipeline2\",\"stage\":\"stage-2\"}]")
      job_configs = JobConfigs.new([JobConfig.new(CaseInsensitiveString.new("defaultJob"), ResourceConfigs.new, ArtifactConfigs.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))].to_java(JobConfig))
      stage_config = StageConfig.new(CaseInsensitiveString.new("defaultStage"), job_configs)
      pipeline_config = PipelineConfig.new(CaseInsensitiveString.new(""), com.thoughtworks.go.config.materials.MaterialConfigs.new, [stage_config].to_java(StageConfig))
      expect(assigns[:pipeline]).to eq(pipeline_config)
      expect(assigns[:pipeline_group]).to eq(BasicPipelineConfigs.new("new-group", Authorization.new, [pipeline_config].to_java(PipelineConfig)))
      expect(assigns[:group_name]).to eq("new-group")
      expect(assigns[:task_view_models]).to eq(task_view_models)
      list_of_pipelines = java.util.ArrayList.new
      list_of_pipelines.add(CaseInsensitiveString.new("pipeline2"))
      expect(assigns[:all_pipelines]).to eq(list_of_pipelines)
      assert_save_arguments
      assert_template "new"
      assert_template layout: "application"
      expect(response.status).to eq(400)
    end

    it "should handle validation errors for a pipeline based on a template" do
      template_name = "some_template"
      @cruise_config.addTemplate(PipelineTemplateConfigMother.createTemplate(template_name))
      list_of_templates = [TemplatesViewModel.new(@cruise_config.getTemplateByName(CaseInsensitiveString.new(template_name)), true, true)]
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return(list_of_templates)

      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      task_view_models = Object.new()
      expect(@task_view_service).to receive(:getTaskViewModels).and_return(task_view_models)
      expect(@security_service).to receive(:modifiableGroupsForUser).with(@user).and_return(["group1", "group2"])
      allow(@security_service).to receive(:hasViewOrOperatePermissionForPipeline).and_return(true)

      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("").and_return(@pause_info)

      stub_save_for_validation_error do |result, cruise_config, pipeline|
        pipeline.addError("name", "empty pipeline name")
        result.badRequest("Save failed, see errors below");
      end

      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "", :configurationType => PipelineConfig::CONFIGURATION_TYPE_TEMPLATE, :templateName => "some_template"}}}

      expect(assigns[:errors].size).to eq(1)
      expect(assigns[:errors][0]).to eq("empty pipeline name")
      expect(assigns[:group_name]).to eq("new-group")
      expect(assigns[:task_view_models]).to eq(task_view_models)
      assert_save_arguments
      assert_template "new"
      assert_template layout: "application"
      expect(response.status).to eq(400)
    end

    it "should load group name if user does not have permission for that group" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      allow(@security_service).to receive(:hasViewOrOperatePermissionForPipeline).and_return(true)
      expect(@task_view_service).to receive(:taskInstanceFor).with("ant").and_return(AntTask.new)
      task_view_models = Object.new()
      expect(@task_view_service).to receive(:getTaskViewModels).and_return(task_view_models)
      expect(@task_view_service).to receive(:getViewModel).with(anything, anything).and_return(TaskViewModel.new(nil, nil))
      expect(@task_view_service).to receive(:getModelOfType).with(anything, anything).and_return(TaskViewModel.new(nil, nil))
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))

      stub_save_for_validation_error do |result, cruise_config, node|
        result.forbidden("unauthorized", nil)
      end

      job = {:name => "job", :tasks => {:taskOptions => "ant", "ant" => {}}}
      stage = {:name => "stage", :jobs => [job]}
      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "defaultGroup", :pipeline => {:name => "new-pip", :stage => stage}}}

      expect(assigns[:group_name]).to eq("defaultGroup")
    end

    it "should populate new package material with submitted value if it is present" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))

      stub_save_for_success
      expect(@pipeline_pause_service).to receive(:pause).with("new-pip", "Under construction", @user)

      pkg_params = {:create_or_associate_pkg_def => "associate", :package_definition => {:repositoryId => "repo-id"}, :packageId => "pkg-id"}
      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip", :materials => {:materialType => PackageMaterialConfig::TYPE}}}, :material => pkg_params}

      expect(assigns[:group_name]).to eq("new-group")
      new_pipeline = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new("new-pip"))
      expect(new_pipeline.material_configs.get(0).type).to eq(PackageMaterialConfig::TYPE)
      expect(new_pipeline.material_configs.get(0).getPackageId()).to eq("pkg-id")
    end

    it "should create new package material with submitted value if it is present" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))

      stub_save_for_success
      expect(@pipeline_pause_service).to receive(:pause).with("new-pip", "Under construction", @user)

      pkg_params = {:create_or_associate_pkg_def => "create", :package_definition => {:repositoryId => "repo-id", :name => "pkg-name"}}
      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip", :materials => {:materialType => PackageMaterialConfig::TYPE}}}, :material => pkg_params}

      expect(assigns[:group_name]).to eq("new-group")
      new_pipeline = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new("new-pip"))
      expect(new_pipeline.material_configs.get(0).type).to eq(PackageMaterialConfig::TYPE)
      expect(new_pipeline.material_configs.get(0).getPackageDefinition().getName()).to eq("pkg-name")
    end

    it "should load package material data if pipeline save fails" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      expect(@task_view_service).to receive(:getTaskViewModels).and_return(Object.new())
      expect(@task_view_service).to receive(:getViewModel).with(anything, anything).and_return(TaskViewModel.new(nil, nil))
      expect(@task_view_service).to receive(:getModelOfType).with(anything, anything).and_return(TaskViewModel.new(nil, nil))
      allow(@security_service).to receive(:hasViewOrOperatePermissionForPipeline).and_return(true)
      package_material_config = PackageMaterialConfig.new()
      package_material_config.setPackageDefinition(PackageDefinitionMother.create("pkg-id", "package3-name", nil, @repository1))
      @pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", MaterialConfigs.new([package_material_config].to_java(com.thoughtworks.go.domain.materials.MaterialConfig)), ["job-2"].to_java(java.lang.String))
      @subject = @pipeline
      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))

      stub_save_for_validation_error do |result, cruise_config, node|
        result.forbidden("unauthorized", nil)
      end

      allow(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline2").and_return(@pause_info)
      PackageMetadataStore.getInstance().addMetadataFor("pluginid", PackageConfigurations.new())

      # params do not matter since we have stubbed save for error & @pipeline is the object that is worked upon
      pkg_params = {:create_or_associate_pkg_def => "associate", :package_definition => {:repositoryId => "repo-id"}, :packageId => "pkg-id"}
      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "defaultGroup", :pipeline => {:name => "new-pip", :materials => {:materialType => PackageMaterialConfig::TYPE}}}, :material => pkg_params}

      expect(assigns[:group_name]).to eq("defaultGroup")
      expect(assigns[:package_configuration].name).to eq("package3-name")
    end

    it "should be able to create a pipeline with a pluggable task" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      allow(@pluggable_task_service).to receive(:validate)
      @new_task = PluggableTask.new( PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("Url", false, nil)].to_java(ConfigurationProperty)))
      expect(@task_view_service).to receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))
      stub_save_for_success
      pipeline_name = "new-pip"
      expect(@pipeline_pause_service).to receive(:pause).with(pipeline_name, "Under construction", @user)

      job = {:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:foo => "bar"}}}
      stage = {:name => "stage", :jobs => [job]}
      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => pipeline_name, :stage => stage}}}

      expect(@cruise_config.getAllErrors().size).to eq(0)
      expect(assigns[:pause_info]).to eq(@pause_info)
      assert_save_arguments
      assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
      pipeline_config = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new(pipeline_name))
      expect(pipeline_config.getFirstStageConfig().getJobs().first().getTasks().first().instance_of?(PluggableTask)).to eq(true)
      expect(response).to redirect_to anything
    end

    it "should validate pluggable tasks before create" do
      expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return([])
      allow(@pluggable_task_service).to receive(:validate) do |task|
        task.getConfiguration().getProperty("key").addError("key", "some error")
      end
      @new_task = PluggableTask.new( PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("key", false, nil)].to_java(ConfigurationProperty)))
      expect(@task_view_service).to receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
      expect(@task_view_service).to receive(:getViewModel).with(@new_task, "new").and_return(TaskViewModel.new(nil, nil))
      expect(@task_view_service).to receive(:getModelOfType).with(anything, anything).and_return(TaskViewModel.new(nil, nil))
      expect(@go_config_service).to receive(:getCurrentConfig).twice.and_return(GoConfigCloner.new().deepClone(@cruise_config))
      stub_save_for_validation_error do |result, cruise_config, pipeline|
        result.badRequest("Save failed, see errors below")
      end
      expect(@task_view_service).to receive(:getTaskViewModels).and_return(Object.new())
      pipeline_name = "new-pip"

      job = {:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:key => "value"}}}
      stage = {:name => "stage", :jobs => [job]}
      post :create, params:{:config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => pipeline_name, :stage => stage}}}

      task_to_be_saved = assigns[:pipeline].getFirstStageConfig().getJobs().first().getTasks().first()
      expect(task_to_be_saved.instance_of?(PluggableTask)).to eq(true)
      expect(task_to_be_saved.getConfiguration().getProperty("key").errors().getAll().size()).to be > 0
      expect(task_to_be_saved.getConfiguration().getProperty("key").errors().getAllOn("key").get(0)).to eq("some error")
      assert_template "new"
      assert_template layout: "application"
      expect(response.status).to eq(400)
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
