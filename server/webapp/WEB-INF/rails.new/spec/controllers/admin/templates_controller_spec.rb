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

require 'rails_helper'

describe Admin::TemplatesController do
  include MockRegistryModule
  include ConfigSaveStubbing
  include GoUtil

  before :each do
    @template_config_service = stub_service(:template_config_service)
    @go_config_service = stub_service(:go_config_service)
  end

  describe "action" do
    before :each do
      login_as_admin
      @pipeline = PipelineTemplateConfig.new(CaseInsensitiveString.new("some_template"), [StageConfigMother.stageConfig("defaultStage")].to_java(StageConfig))
      @cruise_config = BasicCruiseConfig.new
      @cruise_config.addTemplate(@pipeline)
      @user = current_user
      @result = stub_localized_result

      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    describe "index" do
      before(:each) do
        expect(@template_config_service).to receive(:templatesWithPipelinesForUser).with(@user.getUsername).and_return(@template_to_pipelines = {"template1" => to_list([]), "template2" => to_list(["first", "second"])})
        expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
      end

      it "should populate all the templates and the associated pipelines" do
        get :index

        expect(assigns[:template_to_pipelines]).to eq(@template_to_pipelines)
        assert_template layout: "admin"
      end
    end

    describe "edit" do
      before(:each) do
        expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
      end

      it "should assign template with name" do
        get :edit, params: { :stage_parent => "templates", :pipeline_name => "some_template", :current_tab => "general" }

        expect(assigns[:pipeline]).to eq(@pipeline)
        assert_template "general"
        assert_template layout: "templates/details"
      end
    end

    describe "edit_permissions" do
      before(:each) do
        expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
        @user_service = stub_service(:user_service)
        allow(@user_service).to receive(:allUsernames).and_return(["foo", "bar", "baz"])
        allow(@user_service).to receive(:allRoleNames).and_return(["role1", "other"])
      end

      it "should assign template" do
        get :edit_permissions, params: { :template_name => "some_template" }

        expect(assigns[:pipeline]).to eq(@pipeline)
        expect(response).to render_template("edit_permissions")
        assert_template layout: "admin"
      end

      it "should assign users for autocomplete" do
        get :edit_permissions, params: { :template_name => "template1" }

        expect(assigns[:autocomplete_users]).to eq(["foo", "bar", "baz"].to_json)
      end

      it "should assign roles for autocomplete" do
        get :edit_permissions, params: { :template_name => "template1" }

        expect(assigns[:autocomplete_roles]).to eq(["role1", "other"].to_json)
      end

      it "should set tab name to templates" do
        get :edit_permissions, params: { :template_name => "template1" }

        expect(assigns[:tab_name]).to eq("templates")
      end
    end

    describe "update_permissions" do
      before(:each) do
        @user_service = stub_service(:user_service)
        allow(@user_service).to receive(:allUsernames).and_return(["foo", "bar", "baz"])
        allow(@user_service).to receive(:allRoleNames).and_return(["role1", "other"])
      end

      it "should update permissions for template successfully" do
        stub_save_for_success(@cruise_config)
        expect(stub_service(:flash_message_service)).to receive(:add).with(FlashMessageModel.new("Saved successfully.", "success")).and_return("random-message-uuid")

        put :update_permissions, params: { :config_md5 => "1234abcd", :template_name => "some_template", :template => {:name => "some_template", :authorization => [{:name => "new-admin", :type => "USER", :privileges => [{:admin => "ON"}]}]} }

        admins = @cruise_config.getTemplateByName(CaseInsensitiveString.new('some_template')).getAuthorization().getAdminsConfig()
        expect(admins.size()).to eq(1)
        expect(admins.get(0).getName().toString()).to eq("new-admin")
        expect(response.status).to eq(302)
        expect(response).to redirect_to("http://test.host/admin/templates/some_template/permissions?fm=random-message-uuid")
      end

      it "should assign users for autocomplete on error" do
        stub_save_for_validation_error(@cruise_config) do |result, config, node|
          result.badRequest(LocalizedMessage.string("SAVE_FAILED"))
        end

        put :update_permissions, params: { :config_md5 => "1234abcd", :template_name => "some_template", :template => {:name => "some_template"} }

        expect(response.status).to eq(400)
        expect(assigns[:pipeline]).to eq(@cruise_config.getTemplateByName(CaseInsensitiveString.new('some_template')))
        expect(assigns[:autocomplete_users]).to eq(["foo", "bar", "baz"].to_json)
        assert_template layout: "admin"
      end

      it "should assign roles for autocomplete on error" do
        stub_save_for_validation_error(@cruise_config) do |result, config, node|
          result.badRequest(LocalizedMessage.string("SAVE_FAILED"))
        end

        put :update_permissions, params: { :config_md5 => "1234abcd", :template_name => "some_template", :template => {:name => "some_template"} }

        expect(response.status).to eq(400)
        expect(assigns[:pipeline]).to eq(@cruise_config.getTemplateByName(CaseInsensitiveString.new('some_template')))
        expect(assigns[:autocomplete_roles]).to eq(["role1", "other"].to_json)
        assert_template layout: "admin"
      end
    end

    describe "destroy" do
      before(:each) do
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
        expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
      end

      it "should delete a template" do
        first_set = java.util.HashMap.new()
        first_set.put(CaseInsensitiveString.new("some_template"), java.util.ArrayList.new())
        first_set.put(CaseInsensitiveString.new("some_template_2"), java.util.ArrayList.new())
        templates_after_delete = java.util.HashMap.new()
        templates_after_delete.put(CaseInsensitiveString.new("some_template_2"), java.util.ArrayList.new())
        allow(@template_config_service).to receive(:templatesWithPipelinesForUser).and_return(first_set, templates_after_delete)

        @pipeline_2 = PipelineTemplateConfig.new(CaseInsensitiveString.new("some_template_2"), [StageConfigMother.stageConfig("defaultStage")].to_java(StageConfig))
        @cruise_config.addTemplate(@pipeline_2)
        expect(@cruise_config.getTemplates().size()).to eq(2)
        stub_save_for_success

        delete :destroy, params: { :pipeline_name => "some_template", :config_md5 => "abcd1234" }

        expect(assigns[:cruise_config].getTemplates().size()).to eq(1)

        assert_save_arguments "abcd1234"
        assert_update_command ::ConfigUpdate::SaveAsTemplateAdmin, ConfigUpdate::TemplatesNode, ConfigUpdate::TemplatesTemplateSubject

        expect(assigns[:template_to_pipelines]).to eq(templates_after_delete)
      end

      it "should return error if there are dependent pipelines for the template" do
        template_with_dependent_pipelines = java.util.HashMap.new()
        list_of_pipelines = java.util.ArrayList.new()
        list_of_pipelines.add("some_pipeline")
        template_with_dependent_pipelines.put(CaseInsensitiveString.new("Template1"),list_of_pipelines)
        allow(@template_config_service).to receive(:templatesWithPipelinesForUser).and_return(template_with_dependent_pipelines)

        allow(controller).to receive(:set_error_flash).and_return("Error!")

        delete :destroy, params: { :pipeline_name => "Template1", :config_md5 => "abcd1234" }
        expect(response).to redirect_to templates_path(:fm => "Error!")
        expect(@go_config_service).not_to receive(:updateConfigFromUI)
      end
    end

    describe "new" do
      it "should create an empty template" do
        expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
        template_config_service = stub_service(:template_config_service)

        expected = java.util.Arrays.asList([PipelineConfigMother.pipeline_config("pipeline1"), PipelineConfigMother.pipeline_config("pipeline.2"), PipelineConfigMother.pipeline_config("FOO_BAR")].to_java(PipelineConfig))
        expect(template_config_service).to receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        get :new

        expect(assigns[:pipeline]).to eq(PipelineTemplateConfigViewModel.new(PipelineTemplateConfig.new, "", expected))
        assert_template "new"
        assert_template layout: false
      end

      it "should create an empty template when pipelineToExtractFrom is set" do
        in_params(:pipelineToExtractFrom => 'pipeline1')
        expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)

        template_config_service = stub_service(:template_config_service)

        expected = java.util.Arrays.asList([PipelineConfigMother.pipeline_config("pipeline1"), PipelineConfigMother.pipeline_config("pipeline.2"), PipelineConfigMother.pipeline_config("FOO_BAR")].to_java(PipelineConfig))
        expect(template_config_service).to receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        get :new

        expect(assigns[:pipeline]).to eq(PipelineTemplateConfigViewModel.new(PipelineTemplateConfig.new, "pipeline1", expected))
        assert_template "new"
        assert_template layout: false
      end

    end

    describe "create" do
      before :each do
        allow(controller).to receive(:check_admin_user_or_group_admin_user_and_401).and_return(nil)
        @security_service = stub_service(:security_service)
        expect(@security_service).to receive(:isUserGroupAdmin).and_return(false)
      end
      it "should create a new template given a name" do
        stub_save_for_success
        template_config_service = stub_service(:template_config_service)
        expect(template_config_service).to receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return([])

        post :create, params: { :pipeline => {:template => {:name => "template_foo"}, :useExistingPipeline => "0"}, :config_md5 => "1234abcd" }

        templates = assigns[:cruise_config].getTemplates()
        expect(templates.size()).to eq(2)
        expect(templates.get(0).name()).to eq(CaseInsensitiveString.new("some_template"))
        expect(templates.get(1).name()).to eq(CaseInsensitiveString.new("template_foo"))
        assigns[:pipeline].class == PipelineTemplateConfigViewModel
        expect(assigns[:pipeline].templateConfig().name().toString()).to eq("template_foo")
        expect(assigns[:pipeline].useExistingPipeline()).to be_falsey
        expect(assigns[:pipeline].selectedPipelineName()).to eq(nil)

        assert_save_arguments
      end

      it "should extract a new template from a pipeline config" do
        stub_save_for_success
        template_config_service = stub_service(:template_config_service)

        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline1", ["stage1", "stage2"].to_java(java.lang.String))
        @cruise_config.addPipeline('default', pipeline)

        expected = java.util.Arrays.asList([pipeline, PipelineConfigMother.pipeline_config("pipeline.2"), PipelineConfigMother.pipeline_config("FOO_BAR")].to_java(PipelineConfig))
        expect(template_config_service).to receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        post :create, params: { :pipeline => {:template => {:name => "new_template"}, :useExistingPipeline => "1", :selectedPipelineName => "pipeline1"}, :config_md5 => "1234abcd" }

        templates = assigns[:cruise_config].getTemplates()
        expect(templates.size()).to eq(2)
        expect(templates.get(0).name()).to eq(CaseInsensitiveString.new("some_template"))
        new_template = templates.get(1)
        expect(new_template.name()).to eq(CaseInsensitiveString.new("new_template"))
        expect(new_template.get(0).name.toString).to eq("stage1")
        expect(new_template.get(1).name.toString).to eq("stage2")

        assigns[:pipeline].class == PipelineTemplateConfigViewModel
        expect(assigns[:pipeline].templateConfig().name().toString()).to eq("new_template")
        expect(assigns[:pipeline].useExistingPipeline()).to be_truthy
        expect(assigns[:pipeline].selectedPipelineName()).to eq("pipeline1")

        assert_save_arguments
      end

      it "should modify pipeline defintion to use template when a template is extracted" do
        stub_save_for_success
        template_config_service = stub_service(:template_config_service)

        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline1", ["stage1", "stage2"].to_java(java.lang.String))
        @cruise_config.addPipeline('default', pipeline)

        expected = java.util.Arrays.asList([pipeline, PipelineConfigMother.pipeline_config("pipeline.2"), PipelineConfigMother.pipeline_config("FOO_BAR")].to_java(PipelineConfig))
        expect(template_config_service).to receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        post :create, params: { :pipeline => {:template => {:name => "new_template"}, :useExistingPipeline => "1", :selectedPipelineName => "pipeline1"}, :config_md5 => "1234abcd" }

        modified_pipeline = @cruise_config.pipelineConfigByName(CaseInsensitiveString.new('pipeline1'))
        expect(modified_pipeline.getTemplateName).to eq(CaseInsensitiveString.new('new_template'))
        expect(modified_pipeline.isEmpty).to be_truthy
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
        expect(template_config_service).to receive(:allPipelinesNotUsingTemplates).with(@user, @result).and_return(expected)

        post :create, params: { :pipeline => {:template => {:name => "some_template"}, :useExistingPipeline => "1", :selectedPipelineName => "pipeline.2"}, :config_md5 => "abcd1234" }

        expect(assigns[:pipeline].useExistingPipeline()).to be_truthy
        expect(assigns[:pipeline].pipelineNames()).to eq(java.util.Arrays.asList(["pipeline1", "pipeline.2", "FOO_BAR"].to_java :string))
        expect(assigns[:pipeline].selectedPipelineName()).to eq("pipeline.2")
        expect(assigns[:errors].size).to eq(1)
        assert_save_arguments "abcd1234"
        assert_template "new"
        expect(response.status).to eq(400)
        assert_template layout: false
      end
    end
  end
end
