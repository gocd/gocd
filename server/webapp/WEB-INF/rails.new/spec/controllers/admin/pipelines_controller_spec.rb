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

describe Admin::PipelinesController do
  before do
    controller.stub(:pipeline_pause_service).with().and_return(@pipeline_pause_service = double('Pipeline Pause Service'))
  end

  include ConfigSaveStubbing
  include TaskMother

  before(:each) do
    @user = Username.new(CaseInsensitiveString.new("loser"))
    controller.stub(:current_user).and_return(@user)
  end

  describe "routes" do
    it "should match /edit" do
      {:get => "/admin/pipelines/foo.bar/general"}.should route_to(:controller => "admin/pipelines", :action => "edit", :pipeline_name => 'foo.bar', :current_tab => 'general', :stage_parent => "pipelines")
    end

    it "should match /update" do
      {:put => "/admin/pipelines/foo.baz/general"}.should route_to(:controller => "admin/pipelines", :action => "update", :pipeline_name => 'foo.baz', :current_tab => 'general', :stage_parent => "pipelines")
    end

    it "should match /pause_info" do
      {:get => "/admin/pipelines/foo.baz/pause_info.json"}.should route_to(:controller => "admin/pipelines", :action => "pause_info", :pipeline_name => 'foo.baz', :format => "json")
      pause_info_refresh_path(:pipeline_name => 'foo.baz').should == "/admin/pipelines/foo.baz/pause_info.json"
    end

    it "should match /new" do
      {:get => "/admin/pipeline/new"}.should route_to(:controller => "admin/pipelines", :action => "new")
      pipeline_new_path.should == "/admin/pipeline/new"
      pipeline_new_path(:group => "foo.bar").should == "/admin/pipeline/new?group=foo.bar"
    end

    it "should match /create" do
      {:post => "/admin/pipelines"}.should route_to(:controller => "admin/pipelines", :action => "create")
      pipeline_create_path.should == "/admin/pipelines"
    end

    it "should match /clone" do
      {:get => "/admin/pipeline/foo.bar/clone"}.should route_to(:controller => "admin/pipelines", :action => "clone", :pipeline_name => 'foo.bar')
      pipeline_clone_path(:pipeline_name => "foo.bar").should == "/admin/pipeline/foo.bar/clone"
    end

    it "should match /save_clone" do
      {:post => "/admin/pipeline/save_clone"}.should route_to(:controller => "admin/pipelines", :action => "save_clone")
      pipeline_save_clone_path.should == "/admin/pipeline/save_clone"
    end

  end

  describe "pause_info" do
    before(:each) do
      pipeline_config = PipelineConfigMother.pipelineConfigWithMingleConfiguration("HelloWorld", "http://mingleurl.com:7823", "go", "'status' > 'In Dev'")

      pipeline_config_for_edit = ConfigForEdit.new(pipeline_config, BasicCruiseConfig.new, BasicCruiseConfig.new)

      controller.stub(:go_config_service).with().and_return(@go_config_service = double('Go Config Service'))
      @result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(@result)

      @go_config_service.should_receive(:loadForEdit).with('HelloWorld', @user, @result).and_return(pipeline_config_for_edit)
      @go_config_service.stub(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      @go_config_service.stub(:registry)

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("HelloWorld").and_return(@pause_info)
    end

    it "should load pause_info for json" do
      get :pause_info, :pipeline_name => "HelloWorld", :format => "json"

      assigns[:pipeline].should_not be_nil
      assigns[:pause_info].should == @pause_info
    end
  end

  describe "edit" do
    before(:each) do
      pipeline_config = PipelineConfigMother.pipelineConfigWithMingleConfiguration("HelloWorld", "http://mingleurl.com:7823", "go", "'status' > 'In Dev'")
      pipeline_config.setLabelTemplate("some_label_template")
      @pipeline_config_for_edit = ConfigForEdit.new(pipeline_config, BasicCruiseConfig.new, BasicCruiseConfig.new)

      controller.stub(:go_config_service).with().and_return(@go_config_service = double('Go Config Service'))

      @result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(@result)

      @go_config_service.stub(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      @go_config_service.stub(:registry)
    end

    describe "for authorized user" do
      before(:each) do
        @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("HelloWorld").and_return(@pause_info)
        @go_config_service.should_receive(:loadForEdit).with('HelloWorld', @user, @result).and_return(@pipeline_config_for_edit)
      end

      describe "GET general" do
        it "should load pipeline name, pipeline label template" do
          get :edit, {:pipeline_name => "HelloWorld", :current_tab => 'general', :stage_parent => "pipelines"}

          assigns[:pipeline].name().should == CaseInsensitiveString.new("HelloWorld")
          assigns[:pipeline].getLabelTemplate().should == "some_label_template"
          assigns[:pause_info].should == @pause_info
          assert_template layout: "pipelines/details"
        end
      end

      describe "GET project_management" do
        it "should load mingle gadget config" do
          get :edit, {:pipeline_name => "HelloWorld", :current_tab => 'project_management', :stage_parent=>"pipelines"}

          assigns[:pipeline].getMingleConfig().getProjectIdentifier() == "go"
          assigns[:pipeline].getMingleConfig().getQuotedMql() == "'status' > 'In Dev'"
          assigns[:pause_info].should == @pause_info
        end
      end
    end

    describe "with view" do
      render_views

      before do
        @go_config_service.stub(:isSecurityEnabled).and_return(false)
      end

      it "should error out when user is unauthorized" do
        expect(@go_config_service).to receive(:loadForEdit).with('HelloWorld', anything(), anything()) do |_, _, result|
          result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["HelloWorld"].to_java), HealthStateType.unauthorised_for_pipeline("HelloWorld"))
          nil
        end

        get :edit, :pipeline_name => "HelloWorld", :current_tab => 'general', :stage_parent=>"pipelines"

        assigns[:pipeline].should be_nil
        expect(response.status).to eq(401)
        expect(response.body).to have_selector("h3", :text => "Unauthorized to edit HelloWorld pipeline.")
      end
    end
  end

  describe "update" do
    before(:each) do
      controller.stub(:populate_config_validity)

      @cruise_config = BasicCruiseConfig.new()
      cruise_config_mother = GoConfigMother.new
      @pipeline = cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      @go_config_service = stub_service(:go_config_service)
      @go_config_service.stub(:registry)
    end

    describe "for authorized user" do
      before(:each) do
        @pause_info = PipelinePauseInfo.paused("just for fun", "loser")

        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").at_least(1).and_return(@pause_info)
      end

      it "should set config attributes on pipeline when updating" do
        stub_save_for_success

        put :update, :pipeline_name => "pipeline-name", :current_tab => 'general', :pipeline => {"labelTemplate" => "${COUNT}-something"}, :config_md5 => "md5", :stage_parent=>"pipelines"

        assigns[:pipeline].should == @pipeline
        @pipeline.getLabelTemplate().should == "${COUNT}-something"
        assigns[:pause_info].should == @pause_info
      end

      it "should set variables and parameters to empty if not sent in params" do
        stub_save_for_success
        @pipeline.variables().add("key1", "value1")

        put :update, :pipeline_name => "pipeline-name", :current_tab => 'general', :config_md5 => "md5", :default_as_empty_list => ["pipeline>variables"], :stage_parent=>"pipelines"

        assigns[:pipeline].variables().isEmpty().should == true

        @pipeline.addParam(ParamConfig.new("param1", "value1"))

        stub_save_for_success

        put :update, :pipeline_name => "pipeline-name", :current_tab => 'general', :config_md5 => "md5", :default_as_empty_list => ["pipeline>params"], :stage_parent=>"pipelines"

        assigns[:pipeline].getParams().isEmpty().should == true
        assigns[:pause_info].should == @pause_info
      end

      it "should update only if configuration is valid" do
        stub_save_for_validation_error do |result, _, node|
          node.addError("labelTemplate", "invalid-label")
          result.badRequest(LocalizedMessage.string("FAILED_TO_UPDATE_PIPELINE", ["pipeline-name"]))
        end

        put :update, :pipeline_name => "pipeline-name", :current_tab => 'general', :pipeline => {"labelTemplate" => "${COUNT}-#junk"}, :config_md5 => "md5", :stage_parent=>"pipelines"

        assigns[:errors].size.should == 0
        assigns[:pause_info].should == @pause_info
        assert_template layout: "pipelines/details"
      end

      describe "params" do
        it "should report errors on deleted params that are referenced elsewhere" do
          stub_save_for_validation_error do |result, _, node|
            node.addError("labelTemplate", ParamSubstitutionHandler::NO_PARAM_FOUND_MSG.gsub("'%s'", "'to-be-deleted'"))
            result.badRequest(LocalizedMessage.string("FAILED_TO_UPDATE_PIPELINE", ["pipeline-name"]))
          end

          @pipeline.addParam(ParamConfig.new("to-be-deleted", "original-deleted-value"))
          @pipeline.addParam(ParamConfig.new("to-be-modified", "original-value"))

          put :update, :pipeline_name => "pipeline-name", :current_tab => 'parameters', :config_md5 => "md5", :stage_parent=>"pipelines", :default_as_empty_list => ["pipeline>params"],
              :pipeline => {:params => [{:name => "to-be-modified", :valueForDisplay => "modified-value"},
                                        {:name => "added", :valueForDisplay => "added-value"}]}
          assigns[:pause_info].should == @pause_info
          params = assigns[:pipeline].getParams()
          params.size().should == 3

          deleted = params.get(0)
          deleted.getName().should == "to-be-deleted"
          deleted.getValue().should == "original-deleted-value"
          deleted.errors().on(ParamConfig::NAME).should == "Parameter cannot be deleted because it is referenced by other elements"

          mod = params.get(1)
          mod.getName().should == "to-be-modified"
          mod.getValue().should == "modified-value"
          mod.getValueForDisplay().should == "modified-value"

          added = params.get(2)
          added.getName().should == "added"
          added.getValue().should == "added-value"
          added.getValueForDisplay().should == "added-value"
        end

        it "should report errors on renamed params that are referenced elsewhere" do
          stub_save_for_validation_error do |result, _, node|
            node.addError("labelTemplate", ParamSubstitutionHandler::NO_PARAM_FOUND_MSG.gsub("'%s'", "'to-be-deleted'"))
            result.badRequest(LocalizedMessage.string("FAILED_TO_UPDATE_PIPELINE", ["pipeline-name"]))
          end

          @pipeline.addParam(ParamConfig.new("to-be-deleted", "original-deleted-value"))
          @pipeline.addParam(ParamConfig.new("to-be-modified", "original-value"))

          put :update, :pipeline_name => "pipeline-name", :current_tab => 'parameters', :config_md5 => "md5", :stage_parent=>"pipelines", :default_as_empty_list => ["pipeline>params"], :pipeline => {:params => [{:name => "renamed", :valueForDisplay => "renamed-value", :original_name => "to-be-deleted"},
                                                                                                                                                                                                                   {:name => "to-be-modified", :valueForDisplay => "modified-value"}]}
          params = assigns[:pipeline].getParams()
          params.size().should == 2

          renamed = params.get(0)
          renamed.getName().should == "renamed"
          renamed.getValue().should == "renamed-value"
          renamed.errors().on(ParamConfig::NAME).should == "Parameter 'to-be-deleted' cannot be renamed because it is referenced by other elements"

          mod = params.get(1)
          mod.getName().should == "to-be-modified"
          mod.getValue().should == "modified-value"
        end
      end
    end

  end

  describe "new" do

    before(:each) do
      controller.stub(:go_config_service).with().and_return(@go_config_service = double('Go Config Service'))
      @go_config_service.stub(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      @go_config_service.stub(:registry)
      @cruise_config = BasicCruiseConfig.new
      @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
      @cruise_config_mother = GoConfigMother.new
    end

    it "should have a new pipeline group with a pipeline in it" do
      @go_config_service.should_receive(:getCurrentConfig).and_return(@cruise_config)

      get :new

      job_configs = JobConfigs.new([JobConfig.new(CaseInsensitiveString.new("defaultJob"), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))].to_java(JobConfig))
      pipeline = PipelineConfig.new(CaseInsensitiveString.new(""), MaterialConfigs.new, [StageConfig.new(CaseInsensitiveString.new("defaultStage"), job_configs)].to_java(StageConfig))
      assigns[:pipeline_group].should == BasicPipelineConfigs.new([pipeline].to_java(PipelineConfig))
      assigns[:pipeline].should == pipeline
      assigns[:all_pipelines].should == java.util.ArrayList.new
      assigns[:cruise_config].should == @cruise_config
      assigns[:original_cruise_config].should == @cruise_config
      assert_template layout: "application"
    end

    it "should populate group name if adding to an existing group and get all existing pipelines as list of string" do
      cruise_config_mother = GoConfigMother.new
      cruise_config_mother.addPipeline(@cruise_config, "new_pipeline", "stageName", ["jobname"].to_java(java.lang.String))
      @go_config_service.should_receive(:getCurrentConfig).and_return(@cruise_config)

      get :new, :group => "foo.bar"

      assigns[:group_name].should == "foo.bar"
      list_of_pipelines = java.util.ArrayList.new
      list_of_pipelines.add(CaseInsensitiveString.new("new_pipeline"))
      assigns[:all_pipelines].should == list_of_pipelines
    end

    it "should have template list assigned" do
      @go_config_service.should_receive(:getCurrentConfig).and_return(@cruise_config)
      template_name = "someTemplateName"
      GoConfigMother.new.addPipelineWithTemplate(@cruise_config, "someTemplatePipeline", template_name, "stageName", ["jobName"].to_java(java.lang.String))

      get :new

      assigns[:template_list].should == TemplatesConfig.new([@cruise_config.getTemplateByName(CaseInsensitiveString.new(template_name))].to_java(PipelineTemplateConfig))
    end

    it "should have pipelines using templates listed in the pipelineStageJsontemplate list assigned" do
      template_name = "someTemplateName"
      cruise_config_interpolated = BasicCruiseConfig.new
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      @cruise_config_mother.addPipelineWithTemplate(@cruise_config, "someTemplatePipeline", template_name, "stageName", ["jobName"].to_java(java.lang.String))

      @cruise_config_mother.addPipeline(cruise_config_interpolated, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(cruise_config_interpolated, "someTemplatePipeline", "templateStage", ["templateJob"].to_java(java.lang.String))

      @go_config_service.should_receive(:getCurrentConfig).and_return(cruise_config_interpolated)

      get :new

      assigns[:pipeline_stages_json].should == "[{\"pipeline\":\"pipeline2\",\"stage\":\"stage-2\"},{\"pipeline\":\"someTemplatePipeline\",\"stage\":\"templateStage\"}]"
    end

    it "should have modifiable group and pipelineStageJson for DependencyMaterial is assigned" do
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "a", "b", ["job-1"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline1", "stage-1", ["job-1"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "Ab", "stage-1", ["job-1"].to_java(java.lang.String))

      controller.stub(:security_service).and_return(@security_service = double('Security Service'))
      @security_service.should_receive(:modifiableGroupsForUser).with(@user).and_return(["group1", "group2"])
      @security_service.stub(:hasViewOrOperatePermissionForPipeline).and_return(true)

      @go_config_service.should_receive(:getCurrentConfig).and_return(@cruise_config)

      get :new

      assigns[:groups_json].should == [{"group" => "group1"}, {"group" => "group2"}].to_json
      assigns[:pipeline_stages_json].should == "[{\"pipeline\":\"a\",\"stage\":\"b\"},{\"pipeline\":\"Ab\",\"stage\":\"stage-1\"},{\"pipeline\":\"pipeline1\",\"stage\":\"stage-1\"},{\"pipeline\":\"pipeline2\",\"stage\":\"stage-2\"}]"
    end
  end

  describe "create" do
    before :all do
      set_up_registry
      task_preference = com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference.new(TaskMother::ApiTaskForTest.new)
      PluggableTaskConfigStore.store().setPreferenceFor("curl.plugin", task_preference)
    end

    after :all do
      unload_all_from_registry
      PluggableTaskConfigStore.store().removePreferenceFor("curl.plugin")
    end

    before(:each) do
      PackageMetadataStore.getInstance().removeMetadata("pluginid")

      @cruise_config_mother = GoConfigMother.new
      @result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(@result)

      controller.stub(:go_config_service).with().and_return(@go_config_service = double('Go Config Service'))
      @go_config_service.stub(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      @cruise_config = BasicCruiseConfig.new
      @repository1 = PackageRepositoryMother.create("repo-id", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
      @pkg = PackageDefinitionMother.create("pkg-id", "package3-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "p3v2")].to_java(ConfigurationProperty)), @repository1)
      @repository1.setPackages(Packages.new([@pkg].to_java(PackageDefinition)))
      repos = PackageRepositories.new
      repos.add(@repository1)
      @cruise_config.setPackageRepositories(repos)
      @go_config_service.should_receive(:getConfigForEditing).and_return(Cloner.new().deepClone(@cruise_config))
      @go_config_service.stub(:registry)
      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @pipeline_pause_service.stub(:pipelinePauseInfo).with("new-pip").and_return(@pause_info)
      @pluggable_task_service = double('Pluggable_task_service')

      controller.stub(:pluggable_task_service).and_return(@pluggable_task_service)

      allow(@go_config_service).to receive(:updateUserPipelineSelections)
      allow(controller).to receive(:cookies).and_return({})
    end

    after(:each) do
      PackageMetadataStore.getInstance().removeMetadata("pluginid")
    end

    it "should populate group name from the submitted value if it is present" do
      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))

      stub_save_for_success
      @pipeline_pause_service.should_receive(:pause).with("new-pip", "Under construction", @user)

      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip"}}

      assigns[:group_name].should == "new-group"
    end

    it "should create a new pipeline in a new pipeline group and pause the pipeline" do
      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))

      stub_save_for_success
      @pipeline_pause_service.should_receive(:pause).with("new-pip", "Under construction", @user)

      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip"}}

      @cruise_config.getAllErrors().size.should == 0
      assigns[:pause_info].should == @pause_info
      assert_save_arguments
      assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
      response.should redirect_to anything
    end

    it "should update a users pipeline selections when that user successfully creates a new pipeline" do
      pipeline_name = "new-pip"

      current_user_entity_id = 9999
      controller.stub(:current_user_entity_id).and_return(current_user_entity_id)

      selected_pipeline_id = "456"
      controller.stub(:cookies).and_return(cookiejar={:selected_pipelines => selected_pipeline_id})

      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))
      @pipeline_pause_service.should_receive(:pause).with("new-pip", "Under construction", @user)
      @go_config_service.should_receive(:updateUserPipelineSelections).with(selected_pipeline_id, current_user_entity_id, CaseInsensitiveString.new(pipeline_name))

      stub_save_for_success
      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => pipeline_name}}
    end

    it "should NOT update a users pipeline selections when that user does not successfully creates a new pipeline" do
      pipeline_name = "new-pip"

      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))
      @go_config_service.should_not_receive(:updateUserPipelineSelections)

      stub_save_for_validation_error do |result, _, _|
        result.unauthorized(com.thoughtworks.go.i18n.LocalizedMessage.string("UNAUTHORIZED_TO_CREATE_PIPELINE"), nil)
      end
      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => pipeline_name}}
    end

    it "should create a new pipeline based on a template" do
      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))

      stub_save_for_success
      @pipeline_pause_service.should_receive(:pause).with("new-pip", "Under construction", @user)

      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip", :configurationType => PipelineConfig::CONFIGURATION_TYPE_TEMPLATE, :templateName => "some _template"}}

      @cruise_config.getAllErrors().size.should == 0
      assigns[:pause_info].should == @pause_info
      assert_save_arguments
      assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
      response.should redirect_to anything
    end

    it "should throw up if pipeline name is empty and populate all states required for new action" do
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      controller.stub(:security_service).and_return(@security_service = double('Security Service'))
      controller.stub(:task_view_service).and_return(task_view_service = double('Task View Service'))
      task_view_models = Object.new()
      task_view_service.should_receive(:getTaskViewModels).and_return(task_view_models)
      task_view_service.should_receive(:getViewModel).with(anything, anything).and_return(TaskViewModel.new(nil, nil, nil))
      task_view_service.should_receive(:getModelOfType).with(anything, anything).and_return(TaskViewModel.new(nil, nil, nil))
      @security_service.should_receive(:modifiableGroupsForUser).with(@user).and_return(["group1", "group2"])
      @security_service.stub(:hasViewOrOperatePermissionForPipeline).and_return(true)

      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("").and_return(@pause_info)

      stub_save_for_validation_error do |result, cruise_config, pipeline|
        pipeline.addError("name", "empty pipeline name")
        result.badRequest(LocalizedMessage.string("SAVE_FAILED"));
      end

      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => ""}}

      assigns[:errors].size.should == 1
      assigns[:errors][0].should == "empty pipeline name"
      assigns[:groups_json].should == [{"group" => "group1"}, {"group" => "group2"}].to_json
      assigns[:pipeline_stages_json].should == "[{\"pipeline\":\"pipeline2\",\"stage\":\"stage-2\"}]"
      job_configs = JobConfigs.new([JobConfig.new(CaseInsensitiveString.new("defaultJob"), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))].to_java(JobConfig))
      stage_config = StageConfig.new(CaseInsensitiveString.new("defaultStage"), job_configs)
      pipeline_config = PipelineConfig.new(CaseInsensitiveString.new(""), com.thoughtworks.go.config.materials.MaterialConfigs.new, [stage_config].to_java(StageConfig))
      assigns[:pipeline].should == pipeline_config
      assigns[:pipeline_group].should == BasicPipelineConfigs.new("new-group", Authorization.new, [pipeline_config].to_java(PipelineConfig))
      assigns[:group_name].should == "new-group"
      assigns[:task_view_models].should == task_view_models
      list_of_pipelines = java.util.ArrayList.new
      list_of_pipelines.add(CaseInsensitiveString.new("pipeline2"))
      assigns[:all_pipelines].should == list_of_pipelines
      assert_save_arguments
      assert_template "new"
      assert_template layout: "application"
      response.status.should == 400
    end

    it "should handle validation errors for a pipeline based on a template" do
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      controller.stub(:security_service).and_return(@security_service = double('Security Service'))
      controller.stub(:task_view_service).and_return(task_view_service = double('Task View Service'))
      task_view_models = Object.new()
      task_view_service.should_receive(:getTaskViewModels).and_return(task_view_models)
      @security_service.should_receive(:modifiableGroupsForUser).with(@user).and_return(["group1", "group2"])
      @security_service.stub(:hasViewOrOperatePermissionForPipeline).and_return(true)

      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("").and_return(@pause_info)

      stub_save_for_validation_error do |result, cruise_config, pipeline|
        pipeline.addError("name", "empty pipeline name")
        result.badRequest(LocalizedMessage.string("SAVE_FAILED"));
      end

      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "", :configurationType => PipelineConfig::CONFIGURATION_TYPE_TEMPLATE, :templateName => "some _template"}}

      assigns[:errors].size.should == 1
      assigns[:errors][0].should == "empty pipeline name"
      assigns[:group_name].should == "new-group"
      assigns[:task_view_models].should == task_view_models
      assert_save_arguments
      assert_template "new"
      assert_template layout: "application"
      response.status.should == 400
    end

    it "should load group name if user does not have permission for that group" do
      task_view_service = double('Task View Service')
      controller.stub(:task_view_service).and_return(task_view_service)
      task_view_service.should_receive(:taskInstanceFor).with("ant").and_return(AntTask.new)
      task_view_models = Object.new()
      task_view_service.should_receive(:getTaskViewModels).and_return(task_view_models)
      task_view_service.should_receive(:getViewModel).with(anything, anything).and_return(TaskViewModel.new(nil, nil, nil))
      task_view_service.should_receive(:getModelOfType).with(anything, anything).and_return(TaskViewModel.new(nil, nil, nil))
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))

      stub_save_for_validation_error do |result, cruise_config, node|
        result.unauthorized(com.thoughtworks.go.i18n.LocalizedMessage.string("UNAUTHORIZED_TO_CREATE_PIPELINE"), nil)
      end

      job = {:name => "job", :tasks => {:taskOptions => "ant", "ant" => {}}}
      stage = {:name => "stage", :jobs => [job]}
      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "defaultGroup", :pipeline => {:name => "new-pip", :stage => stage}}

      assigns[:group_name].should == "defaultGroup"
    end

    it "should populate new package material with submitted value if it is present" do
      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))

      stub_save_for_success
      @pipeline_pause_service.should_receive(:pause).with("new-pip", "Under construction", @user)

      pkg_params = {:create_or_associate_pkg_def => "associate", :package_definition => {:repositoryId => "repo-id"}, :packageId => "pkg-id"}
      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip", :materials => {:materialType => PackageMaterialConfig::TYPE}}}, :material => pkg_params

      assigns[:group_name].should == "new-group"
      new_pipeline = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new("new-pip"))
      new_pipeline.material_configs.get(0).type.should == PackageMaterialConfig::TYPE
      new_pipeline.material_configs.get(0).getPackageId().should == "pkg-id"
    end

    it "should create new package material with submitted value if it is present" do
      controller.stub(:package_definition_service).with().and_return(StubPackageDefinitionService.new)

      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))

      stub_save_for_success
      @pipeline_pause_service.should_receive(:pause).with("new-pip", "Under construction", @user)

      pkg_params = {:create_or_associate_pkg_def => "create", :package_definition => {:repositoryId => "repo-id", :name => "pkg-name"}}
      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => "new-pip", :materials => {:materialType => PackageMaterialConfig::TYPE}}}, :material => pkg_params

      assigns[:group_name].should == "new-group"
      new_pipeline = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new("new-pip"))
      new_pipeline.material_configs.get(0).type.should == PackageMaterialConfig::TYPE
      new_pipeline.material_configs.get(0).getPackageDefinition().getName().should == "pkg-name"
    end

    it "should load package material data if pipeline save fails" do
      package_material_config = PackageMaterialConfig.new()
      package_material_config.setPackageDefinition(PackageDefinitionMother.create("pkg-id", "package3-name", nil, @repository1))
      @pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", MaterialConfigs.new([package_material_config].to_java(com.thoughtworks.go.domain.materials.MaterialConfig)), ["job-2"].to_java(java.lang.String))
      @subject = @pipeline
      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))

      stub_save_for_validation_error do |result, cruise_config, node|
        result.unauthorized(com.thoughtworks.go.i18n.LocalizedMessage.string("UNAUTHORIZED_TO_CREATE_PIPELINE"), nil)
      end

      @pipeline_pause_service.stub(:pipelinePauseInfo).with("pipeline2").and_return(@pause_info)
      PackageMetadataStore.getInstance().addMetadataFor("pluginid", PackageConfigurations.new())

      # params do not matter since we have stubbed save for error & @pipeline is the object that is worked upon
      pkg_params = {:create_or_associate_pkg_def => "associate", :package_definition => {:repositoryId => "repo-id"}, :packageId => "pkg-id"}
      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "defaultGroup", :pipeline => {:name => "new-pip", :materials => {:materialType => PackageMaterialConfig::TYPE}}}, :material => pkg_params

      assigns[:group_name].should == "defaultGroup"
      assigns[:package_configuration].name.should == "package3-name"
    end

    it "should be able to create a pipeline with a pluggable task" do
      @pluggable_task_service.stub(:validate)
      task_view_service = double('Task View Service')
      controller.stub(:task_view_service).and_return(task_view_service)
      @new_task = PluggableTask.new( PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("Url", false, nil)].to_java(ConfigurationProperty)))
      task_view_service.should_receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))
      stub_save_for_success
      pipeline_name = "new-pip"
      @pipeline_pause_service.should_receive(:pause).with(pipeline_name, "Under construction", @user)

      job = {:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:foo => "bar"}}}
      stage = {:name => "stage", :jobs => [job]}
      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => pipeline_name, :stage => stage}}

      @cruise_config.getAllErrors().size.should == 0
      assigns[:pause_info].should == @pause_info
      assert_save_arguments
      assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
      pipeline_config = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new(pipeline_name))
      pipeline_config.getFirstStageConfig().getJobs().first().getTasks().first().instance_of?(PluggableTask).should == true
      response.should redirect_to anything
    end

    it "should validate pluggable tasks before create" do
      task_view_service = double('Task View Service')
      controller.stub(:task_view_service).and_return(task_view_service)
      @pluggable_task_service.stub(:validate) do |task|
        task.getConfiguration().getProperty("key").addError("key", "some error")
      end
      @new_task = PluggableTask.new( PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("key", false, nil)].to_java(ConfigurationProperty)))
      task_view_service.should_receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
      task_view_service.should_receive(:getViewModel).with(@new_task, "new").and_return(TaskViewModel.new(nil, nil, nil))
      task_view_service.should_receive(:getModelOfType).with(anything, anything).and_return(TaskViewModel.new(nil, nil, nil))
      @go_config_service.should_receive(:getCurrentConfig).twice.and_return(Cloner.new().deepClone(@cruise_config))
      stub_save_for_validation_error do |result, cruise_config, pipeline|
        result.badRequest(LocalizedMessage.string("SAVE_FAILED"))
      end
      task_view_service.should_receive(:getTaskViewModels).and_return(Object.new())
      pipeline_name = "new-pip"

      job = {:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:key => "value"}}}
      stage = {:name => "stage", :jobs => [job]}
      post :create, :config_md5 => "1234abcd", :pipeline_group => {:group => "new-group", :pipeline => {:name => pipeline_name, :stage => stage}}

      task_to_be_saved = assigns[:pipeline].getFirstStageConfig().getJobs().first().getTasks().first()
      task_to_be_saved.instance_of?(PluggableTask).should == true
      task_to_be_saved.getConfiguration().getProperty("key").errors().getAll().size().should > 0
      task_to_be_saved.getConfiguration().getProperty("key").errors().getAllOn("key").get(0).should == "some error"
      assert_template "new"
      assert_template layout: "application"
      response.status.should == 400
    end
  end

  describe "clone" do

    before :each do
      controller.stub(:go_config_service).with().and_return(@go_config_service = double('Go Config Service'))
      @go_config_service.stub(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      controller.stub(:security_service).and_return(@security_service = double('Security Service'))
      @cruise_config = BasicCruiseConfig.new
      @pipeline = PipelineConfigMother.pipelineConfig("foo.bar")
      @cruise_config.addPipeline("group1", @pipeline)
      @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
      @security_service.should_receive(:modifiableGroupsForUser).with(@user).and_return(["group1", "group2"])
      @go_config_service.stub(:registry)
    end

    describe "clone:get" do
      it "should populate variables for cloning pipeline with pipeline name and group" do
        get :clone, :pipeline_name => "foo.bar", :config_md5 => "1234abcd", :group => "group1"

        clonedPipeline = @pipeline.duplicate()
        assigns[:pipeline].should == clonedPipeline
        assigns[:pipeline_group].should == BasicPipelineConfigs.new([clonedPipeline].to_java(PipelineConfig))
        assigns[:group_name].should == "group1"
        assigns[:groups_list].should == ["group1", "group2"]
        assigns[:groups_json].should == [{"group" => "group1"}, {"group" => "group2"}].to_json
        assert_template layout: false
      end
    end

    describe "save_clone" do
      before :each do
        allow(@pipeline_pause_service).to receive(:pause)
        allow(@go_config_service).to receive(:updateUserPipelineSelections)
      end

      it "should save cloned pipeline successfully" do
        @cruise_config.addPipeline("foo.bar", @pipeline)
        stub_save_for_success
        @pipeline_pause_service.should_receive(:pause).with("new-pip", "Under construction", @user)

        post :save_clone, :config_md5 => "1234abcd", :pipeline_group => {:group => "group1", :pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s

        @cruise_config.getAllErrors().size.should == 0
        assigns[:pause_info].should == @pause_info

        response.location.should =~ /\/admin\/pipelines\/new-pip\/general/
        response.status.should == 200
        response.body.should == "Saved successfully"
      end


      it "should save cloned pipeline successfully when group is not set" do
        @cruise_config.addPipeline("foo.bar", @pipeline)
        stub_save_for_success
        @pipeline_pause_service.should_receive(:pause).with("new-pip", "Under construction", @user)

        post :save_clone, :config_md5 => "1234abcd", :pipeline_group => {:pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s

        @cruise_config.getAllErrors().size.should == 0
        assigns[:pause_info].should == @pause_info

        response.location.should =~ /\/admin\/pipelines\/new-pip\/general/
        response.status.should == 200
        response.body.should == "Saved successfully"
      end


      it "should show validation errors" do
        @cruise_config.addPipeline("foo.bar", @pipeline)
        stub_save_for_validation_error do |result, _, node|
           node.addError("name", "Pipeline name is not unique")
           result.badRequest(LocalizedMessage.string("FAILED_TO_UPDATE_PIPELINE", ["pipeline-name"]))
        end

        post :save_clone, :config_md5 => "1234abcd", :pipeline_group => {:group => "group1", :pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s

        @cruise_config.getAllErrors().size.should == 1
        assigns[:errors].size.should == 1
        response.status.should == 400
        response.location.should be_nil
      end

      it "should update the user's pipeline selections when save clone is successful" do
        @cruise_config.addPipeline("foo.bar", @pipeline)
        stub_save_for_success

        current_user_entity_id = 9999
        controller.stub(:current_user_entity_id).and_return(current_user_entity_id)

        selected_pipeline_id = "456"
        controller.stub(:cookies).and_return(cookiejar={:selected_pipelines => selected_pipeline_id})

        expect(@go_config_service).to receive(:updateUserPipelineSelections).with(selected_pipeline_id, current_user_entity_id, CaseInsensitiveString.new("new-pip"))

        post :save_clone, :config_md5 => "1234abcd", :pipeline_group => {:group => "group1", :pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s
      end

      it "should not update the user's pipeline selections when save clone is not successful" do
        stub_save_for_validation_error do |result, _, _|
          result.badRequest(LocalizedMessage.string("FAILED_TO_UPDATE_PIPELINE", ["pipeline-name"]))
        end

        post :save_clone, :config_md5 => "1234abcd", :pipeline_group => {:group => "group1", :pipeline => {:name => "new-pip"}}, :pipeline_name => @pipeline.name().to_s

        expect(@go_config_service).to_not receive(:updateUserPipelineSelections)
      end
    end
  end

  describe "clone with error" do
    it "should render error if pipeline to be cloned does not exist" do
      get :clone, :pipeline_name => "doesNotExist", :config_md5 => "1234abcd", :group => "group1"

      response.response_code.should == 404
      assert_template layout: "application"
    end
  end
end
