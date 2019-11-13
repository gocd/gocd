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

describe Admin::TemplatesController do
  include MockRegistryModule
  include ConfigSaveStubbing
  include GoUtil

  before :each do
    @template_config_service = stub_service(:template_config_service)
    @go_config_service = stub_service(:go_config_service)
  end

  describe "routes" do

    it "should resolve & generate route to the template edit" do
      expect({:get => "/admin/templates/blah.blah/general"}).to route_to(:controller => "admin/templates", :action => "edit", :stage_parent => "templates", :pipeline_name => "blah.blah", :current_tab => 'general')
      expect(template_edit_path(:pipeline_name => "blah.blah", :current_tab => 'general')).to eq("/admin/templates/blah.blah/general")
    end

    it "should resolve & generate route to the template update" do
      expect({:put => "/admin/templates/blah.blah/general"}).to route_to(:controller => "admin/templates", :action => "update", :stage_parent => "templates", :pipeline_name => "blah.blah", :current_tab => 'general')
      expect(template_update_path(:pipeline_name => "blah.blah", :current_tab => 'general')).to eq("/admin/templates/blah.blah/general")
    end

    it "should resolve & generate route for edit permissions" do
      expect({:get => "/admin/templates/template_name/permissions"}).to route_to(:controller => "admin/templates", :action => "edit_permissions", :template_name => "template_name")
      expect(edit_template_permissions_path(:template_name => "foo")).to eq("/admin/templates/foo/permissions")
    end

    it "should resolve & generate route for update permissions" do
      expect({:post => "/admin/templates/template_name/permissions"}).to route_to(:controller => "admin/templates", :action => "update_permissions", :template_name => "template_name")
      expect(update_template_permissions_path(:template_name => "foo")).to eq("/admin/templates/foo/permissions")
    end
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

    describe "edit" do
      before(:each) do
        expect(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
      end

      it "should assign template with name" do
        get :edit, params:{:stage_parent => "templates", :pipeline_name => "some_template", :current_tab => "general"}

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
        get :edit_permissions, params:{:template_name => "some_template"}

        expect(assigns[:pipeline]).to eq(@pipeline)
        expect(response).to render_template("edit_permissions")
        assert_template layout: "admin"
      end

      it "should assign users for autocomplete" do
        get :edit_permissions, params:{:template_name => "template1"}

        expect(assigns[:autocomplete_users]).to eq(["foo", "bar", "baz"].to_json)
      end

      it "should assign roles for autocomplete" do
        get :edit_permissions, params:{:template_name => "template1"}

        expect(assigns[:autocomplete_roles]).to eq(["role1", "other"].to_json)
      end

      it "should set tab name to templates" do
        get :edit_permissions, params:{:template_name => "template1"}

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

        put :update_permissions, params:{:config_md5 => "1234abcd", :template_name => "some_template", :template => {:name => "some_template", :authorization => [{:name => "new-admin", :type => "USER", :privileges => [{:admin => "ON"}]}]}}

        admins = @cruise_config.getTemplateByName(CaseInsensitiveString.new('some_template')).getAuthorization().getAdminsConfig()
        expect(admins.size()).to eq(1)
        expect(admins.get(0).getName().toString()).to eq("new-admin")
        expect(response.status).to eq(302)
        expect(response).to redirect_to("http://test.host/admin/templates/some_template/permissions?fm=random-message-uuid")
      end

      it "should assign users for autocomplete on error" do
        stub_save_for_validation_error(@cruise_config) do |result, config, node|
          result.badRequest("Save failed, see errors below")
        end

        put :update_permissions, params:{:config_md5 => "1234abcd", :template_name => "some_template", :template => {:name => "some_template"}}

        expect(response.status).to eq(400)
        expect(assigns[:pipeline]).to eq(@cruise_config.getTemplateByName(CaseInsensitiveString.new('some_template')))
        expect(assigns[:autocomplete_users]).to eq(["foo", "bar", "baz"].to_json)
        assert_template layout: "admin"
      end

      it "should assign roles for autocomplete on error" do
        stub_save_for_validation_error(@cruise_config) do |result, config, node|
          result.badRequest("Save failed, see errors below")
        end

        put :update_permissions, params:{:config_md5 => "1234abcd", :template_name => "some_template", :template => {:name => "some_template"}}

        expect(response.status).to eq(400)
        expect(assigns[:pipeline]).to eq(@cruise_config.getTemplateByName(CaseInsensitiveString.new('some_template')))
        expect(assigns[:autocomplete_roles]).to eq(["role1", "other"].to_json)
        assert_template layout: "admin"
      end
    end

  end
end
