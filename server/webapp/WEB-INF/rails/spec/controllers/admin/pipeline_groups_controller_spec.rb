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
    it "should resolve route to the pipeline groups listing page" do
      expect({:get => "/admin/pipelines"}).to route_to(:controller => "admin/pipeline_groups", :action => "index")
    end

    it "should generate listing route" do
      expect(pipeline_groups_url).to eq("http://test.host/admin/pipelines")
    end

    it "should resolve route to move" do
      expect({:put => "/admin/pipelines/move/pipeline.name"}).to route_to(:controller => "admin/pipeline_groups", :action => "move", :pipeline_name => "pipeline.name")
    end

    it "should generate move route" do
      expect(move_pipeline_to_group_url(:pipeline_name => "pipeline.name")).to eq("http://test.host/admin/pipelines/move/pipeline.name")
    end

    it "should resolve route to delete of pipeline" do
      expect({:delete => "/admin/pipelines/pipeline.name"}).to route_to(:controller => "admin/pipeline_groups", :action => "destroy", :pipeline_name => "pipeline.name")
    end

    it "should generate group edit route" do
      expect(pipeline_group_edit_path(:group_name => "foo.group")).to eq("/admin/pipeline_group/foo.group/edit")
    end

    it "should resolve route to edit pipeline group" do
      expect({:get => "/admin/pipeline_group/foo.group/edit"}).to route_to(:controller => "admin/pipeline_groups", :action => "edit", :group_name => "foo.group")
    end

    it "should resolve route to show pipeline group" do
      expect({:get => "/admin/pipeline_group/foo.group"}).to route_to(:controller => "admin/pipeline_groups", :action => "show", :group_name => "foo.group")
    end

    it "should resolve route to new pipeline group" do
      expect({:get => "/admin/pipeline_group/new"}).to route_to(:controller => "admin/pipeline_groups", :action => "new")
    end

    it "should resolve /possible_groups" do
      expect({:get => "/admin/pipelines/possible_groups/my_pipeline/my_md5"}).to route_to(:controller => "admin/pipeline_groups", :action => "possible_groups", :pipeline_name => "my_pipeline", :config_md5 =>"my_md5")
      expect(possible_groups_path(:pipeline_name => "my_pipeline", :config_md5=>"my_md5")).to eq("/admin/pipelines/possible_groups/my_pipeline/my_md5")
    end

    it "should generate group update route" do
      expect(pipeline_group_update_path(:group_name => "foo.group")).to eq("/admin/pipeline_group/foo.group")
    end

    it "should resolve route to update pipeline group" do
      expect({:put => "/admin/pipeline_group/foo.group"}).to route_to(:controller => "admin/pipeline_groups", :action => "update", :group_name => "foo.group")
    end

    it "should generate delete pipeline route" do
      expect(delete_pipeline_url(:pipeline_name => "pipeline.name")).to eq("http://test.host/admin/pipelines/pipeline.name")
    end

    it "should generate new pipeline group route" do
      expect(pipeline_group_new_url).to eq("http://test.host/admin/pipeline_group/new")
    end

    it "should generate new pipeline group route" do
      expect(pipeline_group_create_url).to eq("http://test.host/admin/pipeline_group")
    end

    it "should generate route for destroy of group" do
      expect(pipeline_group_delete_path(:group_name => "group.foo")).to eq("/admin/pipeline_group/group.foo")
      expect({:delete => "/admin/pipeline_group/foo.group"}).to route_to(:controller => "admin/pipeline_groups", :action => "destroy_group", :group_name => "foo.group")
    end
  end

  describe "actions" do
    before(:each) do
      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      allow(@security_service).to receive(:isUserAdminOfGroup).and_return(true)
      @user = current_user
      @groups = PipelineConfigMother.createGroups(["group1", "group2", "group3"].to_java(java.lang.String))
      @config = BasicCruiseConfig.new(@groups.to_a.to_java(PipelineConfigs))
      group_for_edit = ConfigForEdit.new(@groups.get(0), @config, @config)
      allow(@go_config_service).to receive(:loadGroupForEditing).and_return(group_for_edit)
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    after(:each) do
      @config = nil
    end

    describe "new" do
      it "should return a new pipeline group" do
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@config)

        get :new

        expect(assigns[:group]).to eq(BasicPipelineConfigs.new)
        assert_template layout: false
      end
    end

    describe "create" do

      before do
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@config)
      end

      it "should create a new pipeline group with the given name" do
        stub_save_for_success(@config)
        group = BasicPipelineConfigs.new("name", Authorization.new(), [].to_java(PipelineConfig))

        post :create, params:{:config_md5 => "1234abcd", :group => { :group => "name"}}

        expect(@config.getGroups().get(@groups.length)).to eq(group)
      end

      it "should show an error when new pipeline group could not be created" do
        stub_save_for_validation_error @config do |result, config, node|
          result.badRequest('some message')
        end

        post :create, params:{:config_md5 => "1234abcd", :group => { :group => "name"}}

        expect(response.status).to eq(400)
        assert_template layout: false
      end

    end

    describe "index" do
      before(:each) do
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@config)
        expect(@pipeline_config_service).to receive(:canDeletePipelines).and_return({
                "pipeline_1" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_2" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_3" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_4" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_5" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_6" => CanDeleteResult.new(true, 'Delete this pipeline')
        })
      end

      it "should load all groups" do
        get :index

        expect(assigns[:groups]).to eq(@groups.to_a)
        expect(assigns[:pipeline_to_can_delete]).to eq({
                "pipeline_1" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_2" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_3" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_4" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_5" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_6" => CanDeleteResult.new(true, 'Delete this pipeline')
        })
        assert_template layout: "admin"
      end

      it "should load cruise_config" do
        get :index

        expect(assigns[:cruise_config]).to eq(@config)
      end

      it "should load groups visible to admin" do
        expect(@security_service).to receive(:isUserAdminOfGroup).with(@user.getUsername(), "group1").and_return(true)
        expect(@security_service).to receive(:isUserAdminOfGroup).with(@user.getUsername(), "group2").and_return(false)
        expect(@security_service).to receive(:isUserAdminOfGroup).with(@user.getUsername(), "group3").and_return(true)

        get :index

        expect(assigns[:groups]).to eq([@groups.get(0), @groups.get(2)])
      end
    end

    describe "destroy" do

      before :each do
        @pipeline = @groups.get(0).get(0)
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@config)
        expect(@pipeline_config_service).to receive(:canDeletePipelines).and_return({
                "pipeline_1" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_2" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_3" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_4" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_5" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_6" => CanDeleteResult.new(true, 'Delete this pipeline')
        })
      end

      it "should delete pipeline" do
        stub_save_for_success(@config)

        delete :destroy, params:{:pipeline_name => @pipeline.name().to_s, :group_name => "group1", :config_md5 => "1234abcd"}

        expect(assigns[:groups]).to eq(@groups.to_a)
        expect(assigns[:pipeline_to_can_delete]).to eq({
                "pipeline_1" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_2" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_3" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_4" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_5" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_6" => CanDeleteResult.new(true, 'Delete this pipeline')
        })
      end

      it "should return 400 when the pipeline is not found" do
        stub_save_for_validation_error(@config) do |result, config, node|
          result.badRequest('some request')
        end

        delete :destroy, params:{:pipeline_name => "pipeline_1", :group_name => "group1", :config_md5 => "1234abcd"}

        expect(response.status).to eq(400)
        expect(assigns[:groups]).to eq(@groups.to_a)
        expect(assigns[:pipeline_to_can_delete]).to eq({
                "pipeline_1" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_2" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_3" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_4" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_5" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_6" => CanDeleteResult.new(true, 'Delete this pipeline')
        })
        assert_template layout: "admin"
      end
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

    describe "show" do
      before do
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@config)
        @group = @groups.get(0)
        allow(@user_service).to receive(:allUsernames).and_return(["foo", "bar", "baz"])
        allow(@user_service).to receive(:allRoleNames).and_return(["foo_role", "bar_role", "baz_role"])
      end

      it "should load a pipeline group for editing" do
        get :show, params:{:group_name => "group1"}

        expect(assigns[:group]).to eq(@group)
        expect(assigns[:cruise_config]).to eq(@config)
        expect(response).to render_template :edit
      end

      it "should assign users and roles for autocomplete" do
        get :show, params:{:group_name => "group1"}

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

    describe "move" do
      before do
        @result = stub_localized_result
        @pipeline = @groups.get(0).get(0)
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@config)
        expect(@pipeline_config_service).to receive(:canDeletePipelines).and_return({
                "pipeline_1" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_2" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_3" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_4" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_5" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_6" => CanDeleteResult.new(true, 'Delete this pipeline')
        })
      end

      it "should move a pipeline" do
        stub_save_for_success(@config)

        put :move, params:{:pipeline_name => "pipeline_1", :group_name => "group1", :config_md5 => "1234abcd"}

        expect(assigns[:groups]).to eq(@groups.to_a)
        expect(assigns[:pipeline_to_can_delete]).to eq({
                "pipeline_1" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_2" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_3" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_4" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_5" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_6" => CanDeleteResult.new(true, 'Delete this pipeline')
        })
      end
    end

    describe "destroy_group" do

      before :each do
        @empty_group = PipelineConfigMother.createGroup("empty_group", [].to_java(java.lang.String))
        @destroy_group_config = BasicCruiseConfig.new(@empty_group.to_a.to_java(PipelineConfigs))
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@destroy_group_config)
        expect(@pipeline_config_service).to receive(:canDeletePipelines).and_return({
                "pipeline_1" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_2" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_3" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_4" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_5" => CanDeleteResult.new(true, 'Delete this pipeline'),
                "pipeline_6" => CanDeleteResult.new(true, 'Delete this pipeline')
        })
      end

      it "should delete an empty pipeline group" do
        stub_save_for_success(@destroy_group_config)

        delete :destroy_group, params:{:group_name => "empty_group", :config_md5 => "1234abcd"}

        expect(assigns[:groups].size()).to eq(0)
      end

    end

    describe "possible_groups" do
      it "should render possible groups for given pipeline" do
        expect(@go_config_service).to receive(:getMergedConfigForEditing).and_return(@config)
        expect(@go_config_service).to receive(:doesMd5Match).with("my_md5").and_return(true)

        get :possible_groups, params:{:pipeline_name => "pipeline_1", :config_md5 => "my_md5"}

        expect(assigns[:possible_groups]).to eq(["group2", "group3"])
        expect(assigns[:pipeline_name]).to eq("pipeline_1")
        expect(assigns[:md5_match]).to eq(true)
        assert_template "possible_groups"
        assert_template layout: false
      end
    end

  end
end
