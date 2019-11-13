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

describe Admin::PipelineGroupsController do
  include MockRegistryModule
  before do
    allow(controller).to receive(:set_current_user)
    @go_config_service = stub_service(:go_config_service)
    @pipeline_config_service = stub_service(:pipeline_config_service)
    @security_service = stub_service(:security_service)
    @user_service = stub_service(:user_service)
  end
  include ConfigSaveStubbing

  describe "routes" do
    it "should generate group edit route" do
      expect(pipeline_group_edit_path(:group_name => "foo.group")).to eq("/admin/pipeline_group/foo.group/edit")
    end

    it "should resolve route to edit pipeline group" do
      expect({:get => "/admin/pipeline_group/foo.group/edit"}).to route_to(:controller => "admin/pipeline_groups", :action => "edit", :group_name => "foo.group")
    end

    it "should generate group update route" do
      expect(pipeline_group_update_path(:group_name => "foo.group")).to eq("/admin/pipeline_group/foo.group")
    end

    it "should resolve route to update pipeline group" do
      expect({:put => "/admin/pipeline_group/foo.group"}).to route_to(:controller => "admin/pipeline_groups", :action => "update", :group_name => "foo.group")
    end

  end

  describe "actions" do
    before(:each) do
      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      allow(@security_service).to receive(:isUserAdminOfGroup).and_return(true)
      @user = current_user
      @groups = PipelineConfigMother.createGroups(["group1", "dev", "Docs", "group2", "ApiDocs", "group3", "api"].to_java(java.lang.String))
      @config = BasicCruiseConfig.new(@groups.to_a.to_java(PipelineConfigs))
      group_for_edit = ConfigForEdit.new(@groups.get(0), @config, @config)
      allow(@go_config_service).to receive(:loadGroupForEditing).and_return(group_for_edit)
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    after(:each) do
      @config = nil
    end

    describe "edit" do
      before do
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@config)
        @group = @groups.get(0)

        allow(@user_service).to receive(:allUsernames).and_return(["foo", "bar", "baz"])
        allow(@user_service).to receive(:allRoleNames).and_return(["foo_role", "bar_role", "baz_role"])
      end

      it "should load a pipeline group for editing" do
        get :edit, params:{:group_name => "group1"}

        expect(assigns[:group]).to eq(@group)
        expect(assigns[:cruise_config]).to eq(@config)
        assert_template layout: "admin"
      end

      it "should assign users and roles for autocomplete" do
        get :edit, params:{:group_name => "group1"}

        expect(assigns[:autocomplete_users]).to eq(["foo", "bar", "baz"].to_json)
        expect(assigns[:autocomplete_roles]).to eq(["foo_role", "bar_role", "baz_role"].to_json)
      end
    end

    describe "update" do
      before(:each) do
        allow(controller).to receive(:autocomplete_for_permissions_and_tab).and_return(nil)
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@config)
        @group = @groups.get(0)
      end

      it "should save a pipeline_group" do
        stub_save_for_success(@config)
        expect(stub_service(:flash_message_service)).to receive(:add).with(FlashMessageModel.new("Saved successfully.", "success")).and_return("random-message-uuid")

        put :update, params:{:group_name => "group1", :config_md5 => "1234abcd", :group => {PipelineConfigs::GROUP => "new_group_name"}}

        expect(response.status).to eq(302)
        expect(response).to redirect_to("http://test.host/admin/pipeline_group/new_group_name/edit?fm=random-message-uuid")
      end

      it "should error out if group is not found with new config object attributes assigned" do
        stub_save_for_validation_error(@config) do |result, config, node|
          result.notFound("not found", HealthStateType.general(HealthStateScope::GLOBAL))
        end

        put :update, params:{:group_name => "group1", :config_md5 => "1234abcd", :group => {PipelineConfigs::GROUP => "new_group_name"}}

        expect(assigns[:cruise_config]).to eq(@config)
        expect(assigns[:group]).to eq(@group)

        expect(assigns[:group].getGroup()).to eq("new_group_name")
        assert_template "edit"
        expect(response.status).to eq(404)
        assert_template layout: "admin"
      end
    end

  end
end
