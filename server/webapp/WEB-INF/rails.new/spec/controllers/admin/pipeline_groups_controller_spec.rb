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

describe Admin::PipelineGroupsController do
  include MockRegistryModule
  before do
    controller.stub(:populate_health_messages)
    controller.stub(:set_current_user)
  end
  include ConfigSaveStubbing

  describe "routes" do
    it "should resolve route to the pipeline groups listing page" do
      {:get => "/admin/pipelines"}.should route_to(:controller => "admin/pipeline_groups", :action => "index")
    end

    it "should generate listing route" do
      pipeline_groups_url.should == "http://test.host/admin/pipelines"
    end

    it "should resolve route to move" do
      {:put => "/admin/pipelines/move/pipeline.name"}.should route_to(:controller => "admin/pipeline_groups", :action => "move", :pipeline_name => "pipeline.name")
    end

    it "should generate move route" do
      move_pipeline_to_group_url(:pipeline_name => "pipeline.name").should == "http://test.host/admin/pipelines/move/pipeline.name"
    end

    it "should resolve route to delete of pipeline" do
      {:delete => "/admin/pipelines/pipeline.name"}.should route_to(:controller => "admin/pipeline_groups", :action => "destroy", :pipeline_name => "pipeline.name")
    end

    it "should generate group edit route" do
      pipeline_group_edit_path(:group_name => "foo.group").should == "/admin/pipeline_group/foo.group/edit"
    end

    it "should resolve route to edit pipeline group" do
      {:get => "/admin/pipeline_group/foo.group/edit"}.should route_to(:controller => "admin/pipeline_groups", :action => "edit", :group_name => "foo.group")
    end

    it "should resolve route to show pipeline group" do
      {:get => "/admin/pipeline_group/foo.group"}.should route_to(:controller => "admin/pipeline_groups", :action => "show", :group_name => "foo.group")
    end

    it "should resolve route to new pipeline group" do
      {:get => "/admin/pipeline_group/new"}.should route_to(:controller => "admin/pipeline_groups", :action => "new")
    end

    it "should resolve /possible_groups" do
      {:get => "/admin/pipelines/possible_groups/my_pipeline/my_md5"}.should route_to(:controller => "admin/pipeline_groups", :action => "possible_groups", :pipeline_name => "my_pipeline", :config_md5 =>"my_md5")
      possible_groups_path(:pipeline_name => "my_pipeline", :config_md5=>"my_md5").should == "/admin/pipelines/possible_groups/my_pipeline/my_md5"
    end

    it "should generate group update route" do
      pipeline_group_update_path(:group_name => "foo.group").should == "/admin/pipeline_group/foo.group"
    end

    it "should resolve route to update pipeline group" do
      {:put => "/admin/pipeline_group/foo.group"}.should route_to(:controller => "admin/pipeline_groups", :action => "update", :group_name => "foo.group")
    end

    it "should generate delete pipeline route" do
      delete_pipeline_url(:pipeline_name => "pipeline.name").should == "http://test.host/admin/pipelines/pipeline.name"
    end

    it "should generate new pipeline group route" do
      pipeline_group_new_url.should == "http://test.host/admin/pipeline_group/new"
    end

    it "should generate new pipeline group route" do
      pipeline_group_create_url.should == "http://test.host/admin/pipeline_group"
    end

    it "should generate route for destroy of group" do
      pipeline_group_delete_path(:group_name => "group.foo").should == "/admin/pipeline_group/group.foo"
      {:delete => "/admin/pipeline_group/foo.group"}.should route_to(:controller => "admin/pipeline_groups", :action => "destroy_group", :group_name => "foo.group")
    end
  end

  describe :actions do
    before(:each) do
      @go_config_service = stub_service(:go_config_service)
      @pipeline_config_service = stub_service(:pipeline_config_service)
      @go_config_service.stub(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      @security_service = stub_service(:security_service)
      @security_service.stub(:isUserAdminOfGroup).and_return(true)
      @user = current_user
      @groups = PipelineConfigMother.createGroups(["group1", "group2", "group3"].to_java(java.lang.String))
      @config = BasicCruiseConfig.new(@groups.to_a.to_java(PipelineConfigs))
      group_for_edit = ConfigForEdit.new(@groups.get(0), @config, @config)
      @go_config_service.stub(:loadGroupForEditing).and_return(group_for_edit)
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    after(:each) do
      @config = nil
    end

    describe :new do
      it "should return a new pipeline group" do
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)

        get :new

        assigns[:group].should == BasicPipelineConfigs.new
        assert_template layout: false
      end
    end

    describe :create do

      before do
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
      end

      it "should create a new pipeline group with the given name" do
        stub_save_for_success(@config)
        group = BasicPipelineConfigs.new("name", Authorization.new(), [].to_java(PipelineConfig))

        post :create, :config_md5 => "1234abcd", :group => { :group => "name"}

        @config.getGroups().get(@groups.length).should == group
      end

      it "should show an error when new pipeline group could not be created" do
        stub_save_for_validation_error @config do |result, config, node|
          result.badRequest(LocalizedMessage.string("PIPELINE_NOT_FOUND", ["foo"].to_java(java.lang.String)))
        end

        post :create, :config_md5 => "1234abcd", :group => { :group => "name"}

        response.status.should == 400
        assert_template layout: false
      end

    end

    describe :index do
      before(:each) do
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
        @pipeline_config_service.should_receive(:canDeletePipelines).and_return({
                "pipeline_1" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_2" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_3" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_4" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_5" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_6" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
        })
      end

      it "should load all groups" do
        get :index

        assigns[:groups].should == @groups.to_a
        assigns[:pipeline_to_can_delete].should == {
                "pipeline_1" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_2" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_3" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_4" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_5" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_6" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
        }
        assert_template layout: "admin"
      end

      it "should load cruise_config" do
        get :index

        assigns[:cruise_config].should == @config
      end

      it "should load groups visible to admin" do
        @security_service.should_receive(:isUserAdminOfGroup).with(@user.getUsername(), "group1").and_return(true)
        @security_service.should_receive(:isUserAdminOfGroup).with(@user.getUsername(), "group2").and_return(false)
        @security_service.should_receive(:isUserAdminOfGroup).with(@user.getUsername(), "group3").and_return(true)

        get :index

        assigns[:groups].should == [@groups.get(0), @groups.get(2)]
      end
    end

    describe :destroy do

      before :each do
        @pipeline = @groups.get(0).get(0)
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
        @pipeline_config_service.should_receive(:canDeletePipelines).and_return({
                "pipeline_1" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_2" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_3" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_4" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_5" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_6" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
        })
      end

      it "should delete pipeline" do
        stub_save_for_success(@config)

        delete :destroy, :pipeline_name => @pipeline.name().to_s, :group_name => "group1", :config_md5 => "1234abcd"

        assigns[:groups].should == @groups.to_a
        assigns[:pipeline_to_can_delete].should == {
                "pipeline_1" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_2" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_3" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_4" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_5" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_6" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
        }
      end

      it "should return 400 when the pipeline is not found" do
        stub_save_for_validation_error(@config) do |result, config, node|
          result.badRequest(LocalizedMessage.string("PIPELINE_NOT_FOUND", []))
        end

        delete :destroy, :pipeline_name => "pipeline_1", :group_name => "group1", :config_md5 => "1234abcd"

        response.status.should == 400
        assigns[:groups].should == @groups.to_a
        assigns[:pipeline_to_can_delete].should == {
                "pipeline_1" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_2" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_3" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_4" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_5" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_6" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
        }
        assert_template layout: "admin"
      end
    end

    describe :edit do
      before do
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
        @group = @groups.get(0)
        @user_service = stub_service(:user_service)
        @user_service.stub(:allUsernames).and_return(["foo", "bar", "baz"])
        @user_service.stub(:allRoleNames).and_return(["foo_role", "bar_role", "baz_role"])
      end

      it "should load a pipeline group for editing" do
        get :edit, :group_name => "group1"

        assigns[:group].should == @group
        assigns[:cruise_config].should == @config
        assert_template layout: "admin"
      end

      it "should assign users and roles for autocomplete" do
        get :edit, :group_name => "group1"

        assigns[:autocomplete_users].should == ["foo", "bar", "baz"].to_json
        assigns[:autocomplete_roles].should == ["foo_role", "bar_role", "baz_role"].to_json
      end
    end

    describe :show do
      before do
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
        @group = @groups.get(0)
        @user_service = stub_service(:user_service)
        @user_service.stub(:allUsernames).and_return(["foo", "bar", "baz"])
        @user_service.stub(:allRoleNames).and_return(["foo_role", "bar_role", "baz_role"])
      end

      it "should load a pipeline group for editing" do
        get :show, :group_name => "group1"

        assigns[:group].should == @group
        assigns[:cruise_config].should == @config
        response.should render_template :edit
      end

      it "should assign users and roles for autocomplete" do
        get :show, :group_name => "group1"

        assigns[:autocomplete_users].should == ["foo", "bar", "baz"].to_json
        assigns[:autocomplete_roles].should == ["foo_role", "bar_role", "baz_role"].to_json
      end
    end

    describe :update do
      before(:each) do
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
        @group = @groups.get(0)
      end

      it "should save a pipeline_group" do
        stub_save_for_success(@config)
        stub_service(:flash_message_service).should_receive(:add).with(FlashMessageModel.new("Saved successfully.", "success")).and_return("random-message-uuid")

        put :update, :group_name => "group1", :config_md5 => "1234abcd", :group => {PipelineConfigs::GROUP => "new_group_name"}

        response.status.should == 302
        response.should redirect_to("http://test.host/admin/pipeline_group/new_group_name/edit?fm=random-message-uuid")
      end

      it "should error out if group is not found with new config object attributes assigned" do
        stub_save_for_validation_error(@config) do |result, config, node|
          result.notFound(LocalizedMessage.string("DELETE_TEMPLATE"), HealthStateType.general(HealthStateScope::GLOBAL))
        end

        put :update, :group_name => "group1", :config_md5 => "1234abcd", :group => {PipelineConfigs::GROUP => "new_group_name"}

        assigns[:cruise_config].should == @config
        assigns[:group].should == @group

        assigns[:group].getGroup().should == "new_group_name"
        assert_template "edit"
        response.status.should == 404
        assert_template layout: "admin"
      end
    end

    describe :move do
      before do
        @result = stub_localized_result
        @pipeline = @groups.get(0).get(0)
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
        @pipeline_config_service = stub_service(:pipeline_config_service)
        @pipeline_config_service.should_receive(:canDeletePipelines).and_return({
                "pipeline_1" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_2" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_3" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_4" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_5" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_6" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
        })
      end

      it "should move a pipeline" do
        stub_save_for_success(@config)

        put :move, :pipeline_name => "pipeline_1", :group_name => "group1", :config_md5 => "1234abcd"

        assigns[:groups].should == @groups.to_a
        assigns[:pipeline_to_can_delete].should == {
                "pipeline_1" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_2" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_3" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_4" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_5" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_6" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
        }
      end
    end

    describe :destroy_group do

      before :each do
        @empty_group = PipelineConfigMother.createGroup("empty_group", [].to_java(java.lang.String))
        @destroy_group_config = BasicCruiseConfig.new(@empty_group.to_a.to_java(PipelineConfigs))
        @go_config_service.should_receive(:getConfigForEditing).and_return(@destroy_group_config)
        @pipeline_config_service.should_receive(:canDeletePipelines).and_return({
                "pipeline_1" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_2" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_3" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_4" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_5" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                "pipeline_6" => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
        })
      end

      it "should delete an empty pipeline group" do
        stub_save_for_success(@destroy_group_config)

        delete :destroy_group, :group_name => "empty_group", :config_md5 => "1234abcd"

        assigns[:groups].size().should == 0
      end

    end

    describe :possible_groups do
      it "should render possible groups for given pipeline" do
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
        @go_config_service.should_receive(:doesMd5Match).with("my_md5").and_return(true)

        get :possible_groups, :pipeline_name => "pipeline_1", :config_md5 => "my_md5"

        assigns[:possible_groups].should == ["group2", "group3"]
        assigns[:pipeline_name].should == "pipeline_1"
        assigns[:md5_match].should == true
        assert_template "possible_groups"
        assert_template layout: false
      end
    end

  end
end
