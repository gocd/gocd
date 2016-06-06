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

describe Admin::TemplatesController do
  include MockRegistryModule
  include ConfigSaveStubbing
  include GoUtil

  describe "routes" do
    it "should resolve route to the templates listing page" do
      {:get => "/admin/templates"}.should route_to(:controller => "admin/templates", :action => "index")
    end

    it "should generate listing route" do
      templates_url.should == "http://test.host/admin/templates"
    end

    it "should resolve route to the template delete" do
      {:delete => "/admin/templates/template.name"}.should route_to(:controller => "admin/templates", :action => "destroy", :pipeline_name => "template.name")
      delete_template_path(:pipeline_name => "template.name").should == "/admin/templates/template.name"
    end

    it "should resolve & generate route to the template edit" do
      {:get => "/admin/templates/blah.blah/general"}.should route_to(:controller => "admin/templates", :action => "edit", :stage_parent => "templates", :pipeline_name => "blah.blah", :current_tab => 'general')
      template_edit_path(:pipeline_name => "blah.blah", :current_tab => 'general').should == "/admin/templates/blah.blah/general"
    end

    it "should resolve & generate route to the template update" do
      {:put => "/admin/templates/blah.blah/general"}.should route_to(:controller => "admin/templates", :action => "update", :stage_parent => "templates", :pipeline_name => "blah.blah", :current_tab => 'general')
      template_update_path(:pipeline_name => "blah.blah", :current_tab => 'general').should == "/admin/templates/blah.blah/general"
    end

    it "should resolve & generate route for new" do
      {:get => "/admin/templates/new"}.should route_to(:controller => "admin/templates", :action => "new")
      template_new_path.should == "/admin/templates/new"
    end

    it "should resolve & generate route for create" do
      {:post => "/admin/templates/create"}.should route_to(:controller => "admin/templates", :action => "create")
      template_create_path.should == "/admin/templates/create"
    end

    it "should resolve & generate route for edit permissions" do
      {:get => "/admin/templates/template_name/permissions"}.should route_to(:controller => "admin/templates", :action => "edit_permissions", :template_name => "template_name")
      edit_template_permissions_path(:template_name => "foo").should == "/admin/templates/foo/permissions"
    end

    it "should resolve & generate route for update permissions" do
      {:post => "/admin/templates/template_name/permissions"}.should route_to(:controller => "admin/templates", :action => "update_permissions", :template_name => "template_name")
      update_template_permissions_path(:template_name => "foo").should == "/admin/templates/foo/permissions"
    end
  end

  describe :action do
    before :each do
      @pipeline = PipelineTemplateConfig.new(CaseInsensitiveString.new("some_template"), [StageConfigMother.stageConfig("defaultStage")].to_java(StageConfig))
      @cruise_config = BasicCruiseConfig.new
      @cruise_config.addTemplate(@pipeline)
      @user = current_user
      @result = stub_localized_result

      @template_config_service = stub_service(:template_config_service)
      @go_config_service = stub_service(:go_config_service)

      @go_config_service.stub(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    describe "index" do
      before(:each) do
        @template_config_service.should_receive(:templatesWithPipelinesForUser).with(@user.getUsername.toString).and_return(@template_to_pipelines = {"template1" => to_list([]), "template2" => to_list(["first", "second"])})
        @go_config_service.should_receive(:loadCruiseConfigForEdit).with(@user, @result).and_return(@cruise_config)
      end

      it "should populate all the templates and the associated pipelines" do
        get :index

        assigns[:template_to_pipelines].should == @template_to_pipelines
        assert_template layout: "admin"
      end
    end

    describe "edit" do
      before(:each) do
        @go_config_service.should_receive(:loadCruiseConfigForEdit).with(@user, @result).and_return(@cruise_config)
      end

      it "should assign template with name" do
        get :edit, :stage_parent => "templates", :pipeline_name => "some_template", :current_tab => "general"

        assigns[:pipeline].should == @pipeline
        assert_template "general"
        assert_template layout: "templates/details"
      end
    end

    describe :edit_permissions do
      before(:each) do
        @go_config_service.should_receive(:loadCruiseConfigForEdit).with(@user, @result).and_return(@cruise_config)
        @user_service = stub_service(:user_service)
        @user_service.stub(:allUsernames).and_return(["foo", "bar", "baz"])
      end

      it "should assign template" do
        get :edit_permissions, :template_name => "some_template"

        assigns[:pipeline].should == @pipeline
        response.should render_template("edit_permissions")
        assert_template layout: "admin"
      end

      it "should assign users for autocomplete" do
        get :edit_permissions, :template_name => "template1"

        assigns[:autocomplete_users].should == ["foo", "bar", "baz"].to_json
      end

      it "should set tab name to templates" do
        get :edit_permissions, :template_name => "template1"

        assigns[:tab_name].should == "templates"
      end
    end

    describe :update_permissions do
      before(:each) do
        @user_service = stub_service(:user_service)
        @user_service.stub(:allUsernames).and_return(["foo", "bar", "baz"])
      end

      it "should update permissions for template successfully" do
        stub_save_for_success(@cruise_config)
        stub_service(:flash_message_service).should_receive(:add).with(FlashMessageModel.new("Saved successfully.", "success")).and_return("random-message-uuid")

        put :update_permissions, :config_md5 => "1234abcd", :template_name => "some_template", :template => {:name => "some_template", :authorization => [{:name => "new-admin", :type => "USER", :privileges => [{:admin => "ON"}]}]}

        admins = @cruise_config.getTemplateByName(CaseInsensitiveString.new('some_template')).getAuthorization().getAdminsConfig()
        admins.size().should == 1
        admins.get(0).getName().toString().should == "new-admin"
        response.status.should == 302
        response.should redirect_to("http://test.host/admin/templates/some_template/permissions?fm=random-message-uuid")
      end

      it "should assign users for autocomplete on error" do
        stub_save_for_validation_error(@cruise_config) do |result, config, node|
          result.badRequest(LocalizedMessage.string("SAVE_FAILED"))
        end

        put :update_permissions, :config_md5 => "1234abcd", :template_name => "some_template", :template => {:name => "some_template"}

        response.status.should == 400
        assigns[:pipeline].should == @cruise_config.getTemplateByName(CaseInsensitiveString.new('some_template'))
        assigns[:autocomplete_users].should == ["foo", "bar", "baz"].to_json
        assert_template layout: "admin"
      end
    end

    describe "destroy" do
      before(:each) do
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
        @go_config_service.should_receive(:loadCruiseConfigForEdit).with(@user, @result).and_return(@cruise_config)
      end

      it "should delete a template" do
        @pipeline_2 = PipelineTemplateConfig.new(CaseInsensitiveString.new("some_template_2"), [StageConfigMother.stageConfig("defaultStage")].to_java(StageConfig))
        @cruise_config.addTemplate(@pipeline_2)
        @cruise_config.getTemplates().size().should == 2
        stub_save_for_success

        delete :destroy, :pipeline_name => "some_template", :config_md5 => "abcd1234"

        assigns[:cruise_config].getTemplates().size().should == 1

        assert_save_arguments "abcd1234"
        assert_update_command ::ConfigUpdate::SaveAsSuperAdmin, ConfigUpdate::TemplatesNode, ConfigUpdate::TemplatesTemplateSubject
        h = java.util.HashMap.new()
        h.put(CaseInsensitiveString.new("some_template_2"), java.util.ArrayList.new())
        assigns[:template_to_pipelines].should == h
      end

      it "should return error if there are dependent pipelines for the template" do
        controller.stub(:set_error_flash).and_return("Error!")
        com.thoughtworks.go.helper.GoConfigMother.new().addPipelineWithTemplate(@cruise_config, "P1", "Template1", "S1", ["J1"].to_java(java.lang.String))

        delete :destroy, :pipeline_name => "Template1", :config_md5 => "abcd1234"
        response.should redirect_to templates_path(:fm => "Error!")
        @go_config_service.should_not_receive(:updateConfigFromUI)
      end
    end

    describe "new" do
      it "should create an empty template" do
        @go_config_service.should_receive(:loadCruiseConfigForEdit).with(@user, @result).and_return(@cruise_config)
        template_config_service = stub_service(:template_config_service)

        expected = java.util.Arrays.asList([PipelineConfigMother.pipeline_config("pipeline1"), PipelineConfigMother.pipeline_config("pipeline.2"), PipelineConfigMother.pipeline_config("FOO_BAR")].to_java(PipelineConfig))
        template_config_service.should_receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        get :new

        assigns[:pipeline].should == PipelineTemplateConfigViewModel.new(PipelineTemplateConfig.new, "", expected)
        assert_template "new"
        assert_template layout: false
      end

      it "should create an empty template when pipelineToExtractFrom is set" do
        in_params(:pipelineToExtractFrom => 'pipeline1')
        @go_config_service.should_receive(:loadCruiseConfigForEdit).with(@user, @result).and_return(@cruise_config)

        template_config_service = stub_service(:template_config_service)

        expected = java.util.Arrays.asList([PipelineConfigMother.pipeline_config("pipeline1"), PipelineConfigMother.pipeline_config("pipeline.2"), PipelineConfigMother.pipeline_config("FOO_BAR")].to_java(PipelineConfig))
        template_config_service.should_receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        get :new

        assigns[:pipeline].should == PipelineTemplateConfigViewModel.new(PipelineTemplateConfig.new, "pipeline1", expected)
        assert_template "new"
        assert_template layout: false
      end

    end

    describe "create" do
      it "should create a new template given a name" do
        stub_save_for_success
        template_config_service = stub_service(:template_config_service)
        template_config_service.should_receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return([])

        post :create, :pipeline => {:template => {:name => "template_foo"}, :useExistingPipeline => "0"}, :config_md5 => "1234abcd"

        templates = assigns[:cruise_config].getTemplates()
        templates.size().should == 2
        templates.get(0).name().should == CaseInsensitiveString.new("some_template")
        templates.get(1).name().should == CaseInsensitiveString.new("template_foo")
        assigns[:pipeline].class == PipelineTemplateConfigViewModel
        assigns[:pipeline].templateConfig().name().toString().should == "template_foo"
        assigns[:pipeline].useExistingPipeline().should be_false
        assigns[:pipeline].selectedPipelineName().should == nil

        assert_save_arguments
      end

      it "should extract a new template from a pipeline config" do
        stub_save_for_success
        template_config_service = stub_service(:template_config_service)

        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline1", ["stage1", "stage2"].to_java(java.lang.String))
        @cruise_config.addPipeline('default', pipeline)

        expected = java.util.Arrays.asList([pipeline, PipelineConfigMother.pipeline_config("pipeline.2"), PipelineConfigMother.pipeline_config("FOO_BAR")].to_java(PipelineConfig))
        template_config_service.should_receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        post :create, :pipeline => {:template => {:name => "new_template"}, :useExistingPipeline => "1", :selectedPipelineName => "pipeline1"}, :config_md5 => "1234abcd"

        templates = assigns[:cruise_config].getTemplates()
        templates.size().should == 2
        templates.get(0).name().should == CaseInsensitiveString.new("some_template")
        new_template = templates.get(1)
        new_template.name().should == CaseInsensitiveString.new("new_template")
        new_template.get(0).name.toString.should == "stage1"
        new_template.get(1).name.toString.should == "stage2"

        assigns[:pipeline].class == PipelineTemplateConfigViewModel
        assigns[:pipeline].templateConfig().name().toString().should == "new_template"
        assigns[:pipeline].useExistingPipeline().should be_true
        assigns[:pipeline].selectedPipelineName().should == "pipeline1"

        assert_save_arguments
      end

      it "should modify pipeline defintion to use template when a template is extracted" do
        stub_save_for_success
        template_config_service = stub_service(:template_config_service)

        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline1", ["stage1", "stage2"].to_java(java.lang.String))
        @cruise_config.addPipeline('default', pipeline)

        expected = java.util.Arrays.asList([pipeline, PipelineConfigMother.pipeline_config("pipeline.2"), PipelineConfigMother.pipeline_config("FOO_BAR")].to_java(PipelineConfig))
        template_config_service.should_receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        post :create, :pipeline => {:template => {:name => "new_template"}, :useExistingPipeline => "1", :selectedPipelineName => "pipeline1"}, :config_md5 => "1234abcd"

        modified_pipeline = @cruise_config.pipelineConfigByName(CaseInsensitiveString.new('pipeline1'))
        modified_pipeline.getTemplateName.should == CaseInsensitiveString.new('new_template')
        modified_pipeline.isEmpty.should be_true
        assert_save_arguments
      end

      it "should assign config_errors for display when template throws error" do
        stub_save_for_validation_error do |result, config, node|
          @cruise_config.errors().add("base", "someError")
          @cruise_config.getTemplates().get(0).addError("name", "foo")
          @cruise_config.getTemplates().get(1).addError("name", "foo")
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_ADMINISTER"))
        end
        pipeline2 = PipelineConfigMother.pipeline_config("pipeline.2")
        @cruise_config.addPipeline('default', pipeline2)

        template_config_service = stub_service(:template_config_service)
        expected = java.util.Arrays.asList([PipelineConfigMother.pipeline_config("pipeline1"), pipeline2, PipelineConfigMother.pipeline_config("FOO_BAR")].to_java(PipelineConfig))
        template_config_service.should_receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        post :create, :pipeline => {:template => {:name => "some_template"}, :useExistingPipeline => "1", :selectedPipelineName => "pipeline.2"}, :config_md5 => "abcd1234"

        assigns[:pipeline].useExistingPipeline().should be_true
        assigns[:pipeline].pipelineNames().should == java.util.Arrays.asList(["pipeline1", "pipeline.2", "FOO_BAR"].to_java :string)
        assigns[:pipeline].selectedPipelineName().should == "pipeline.2"
        assigns[:errors].size.should == 1
        assert_save_arguments "abcd1234"
        assert_template "new"
        response.status.should == 400
        assert_template layout: false
      end
    end
  end
end
