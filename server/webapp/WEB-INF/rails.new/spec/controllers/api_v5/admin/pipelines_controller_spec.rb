##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
##########################################################################

require 'rails_helper'

describe ApiV5::Admin::PipelinesController do
  include ApiHeaderSetupTeardown
  include ApiV5::ApiVersionHelper

  before(:each) do
    @pipeline_md5 = 'md5'
    @group = "group"
    @pipeline_config_service = double("pipeline_config_service")
    allow(controller).to receive("pipeline_config_service").and_return(@pipeline_config_service)
    @pipeline_pause_service = double("pipeline_pause_service")
    allow(controller).to receive("pipeline_pause_service").and_return(@pipeline_pause_service)
    @go_config_service = double("go_config_service")
    allow(controller).to receive("go_config_service").and_return(@go_config_service)
    @entity_hashing_service = double('entity_hashing_service')
    allow(controller).to receive('entity_hashing_service').and_return(@entity_hashing_service)
    go_config = BasicCruiseConfig.new
    @repo = PackageRepositoryMother.create("repoid")
    @scm= SCMMother.create("scm-id")
    go_config.getPackageRepositories().add(@repo)
    go_config.getSCMs().add(@scm)
    allow(@go_config_service).to receive(:getCurrentConfig).and_return(go_config)
    allow(@go_config_service).to receive(:checkConfigFileValid).and_return(GoConfigValidity::valid())
    allow(@go_config_service).to receive(:findGroupNameByPipeline).and_return(@group)
    @pipeline_groups = double(com.thoughtworks.go.domain.PipelineGroups)
    allow(@go_config_service).to receive(:groups).and_return(@pipeline_groups)
    allow(@pipeline_groups).to receive(:hasGroup).and_return(true)
    allow(@entity_hashing_service).to receive(:md5ForEntity).and_return(@pipeline_md5)
    @latest_etag = "\"#{Digest::MD5.hexdigest(@pipeline_md5)}\""
  end

  after(:each) do
    controller.send(:go_cache).remove("GO_ETAG_CACHE")
  end

  describe "security" do
    describe "show" do
      before(:each) do
        allow(@pipeline_config_service).to receive(:getPipelineConfig).with(anything()).and_return(PipelineConfig.new)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_pipeline_group_Non_Admin_user
        allow(@security_service).to receive(:hasViewPermissionForPipeline).and_return(false)
        expect(controller).to disallow_action(:get, :show, {:pipeline_name => "pipeline1"}).with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admin users, with security enabled' do
        login_as_pipeline_group_admin_user(@group)
        expect(controller).to allow_action(:get, :show)
      end
    end

    describe "update" do
      it 'should allow anyone, with security disabled' do
        @pipeline_config_service.stub(:getPipelineConfig).with(anything()).and_return(PipelineConfig.new)
        disable_security
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(controller).to receive(:check_for_attempted_pipeline_rename).and_return(nil)
        expect(controller).to allow_action(:put, :update)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_pipeline_group_Non_Admin_user
        expect(controller).to disallow_action(:put, :update, {:pipeline_name => "pipeline1"}).with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admin users, with security enabled' do
        @pipeline_config_service.stub(:getPipelineConfig).with(anything()).and_return(PipelineConfig.new)
        login_as_pipeline_group_admin_user(@group)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(controller).to receive(:check_for_attempted_pipeline_rename).and_return(nil)
        expect(controller).to allow_action(:put, :update)
      end
    end

    describe "create" do
      before :each do
        allow(controller).to receive(:check_if_pipeline_by_same_name_already_exists).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:post, :create, :group => @group)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_pipeline_group_Non_Admin_user
        expect(controller).to disallow_action(:post, :create, :pipeline => {:name => "pipeline1"}, :group => @group).with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admin users, with security enabled' do
        login_as_pipeline_group_admin_user(@group)
        expect(controller).to allow_action(:post, :create, :group => @group)
      end
    end

    describe "destroy" do
      before(:each) do
        allow(@pipeline_config_service).to receive(:getPipelineConfig).with(anything()).and_return(PipelineConfig.new)
      end
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        allow(@security_service).to receive(:isUserAdminOfGroup).and_return(false)
        expect(controller).to disallow_action(:delete, :destroy, :pipeline_name => "pipeline1").with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        allow(@security_service).to receive(:isUserAdminOfGroup).and_return(false)
        expect(controller).to disallow_action(:delete, :destroy, :pipeline_name => "pipeline1").with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        allow(@security_service).to receive(:isUserAdminOfGroup).and_return(true)
        expect(controller).to allow_action(:delete, :destroy)
      end
    end
  end

  describe "action" do
    before :each do
      enable_security
      allow(@security_service).to receive(:hasViewPermissionForPipeline).and_return(true)
      @pipeline_name = 'pipeline1'
    end

    describe "show" do

      it "should not show pipeline config for Non Admin users" do
        login_as_pipeline_group_Non_Admin_user
        pipeline = PipelineConfigMother.pipelineConfig(@pipeline_name)

        get_with_api_header :show, :pipeline_name => @pipeline_name

        expect(response.code).to eq("401")
        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("You are not authorized to perform this action.")
      end

      it 'should show pipeline config for an admin' do
        login_as_pipeline_group_admin_user(@group)
        pipeline = PipelineConfigMother.pipelineConfig(@pipeline_name)
        pipeline_md5 = 'md5_for_pipeline_config'

        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(pipeline)
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(pipeline).and_return(pipeline_md5)

        get_with_api_header :show, :pipeline_name => @pipeline_name

        expect(response).to be_ok
        expected_response = expected_response(pipeline, ApiV5::Admin::Pipelines::PipelineConfigRepresenter)
        expect(actual_response).to eq(expected_response)
        expect(response.headers['ETag']).to eq("\"#{Digest::MD5.hexdigest(pipeline_md5)}\"")
      end

      it "should return 304 for show pipeline config if etag sent in request is fresh" do
        login_as_pipeline_group_admin_user(@group)
        pipeline = PipelineConfigMother.pipelineConfig(@pipeline_name)
        pipeline_md5 = 'md5_for_pipeline_config'

        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(pipeline)
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(pipeline).and_return(pipeline_md5)

        controller.request.env['HTTP_IF_NONE_MATCH'] = Digest::MD5.hexdigest(pipeline_md5)

        get_with_api_header :show, {:pipeline_name => @pipeline_name}

        expect(response.code).to eq('304')
        expect(response.body).to be_empty
      end

      it "should return 404 for show pipeline config if pipeline is not found" do
        login_as_pipeline_group_admin_user(@group)
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)

        get_with_api_header :show, :pipeline_name => @pipeline_name

        expect(response.code).to eq("404")
        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("Either the resource you requested was not found, or you are not authorized to perform this action.")
      end

      it "should show pipeline config if etag sent in request is stale" do
        login_as_pipeline_group_admin_user(@group)
        pipeline = PipelineConfigMother.pipelineConfig(@pipeline_name)
        pipeline_md5 = 'md5_for_pipeline_config'

        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(pipeline)
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(pipeline).and_return(pipeline_md5)

        controller.request.env['HTTP_IF_NONE_MATCH'] = 'stale-etag'

        get_with_api_header :show, {pipeline_name: @pipeline_name}
        expect(response).to be_ok
        expect(response.body).to_not be_empty
      end

      describe "route" do
        describe "with_header" do
          it 'should route to show action of pipelines controller for alphanumeric pipeline name' do
            expect(:get => 'api/admin/pipelines/foo123').to route_to(action: 'show', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo123')
          end

          it 'should route to show action of pipelines controller for pipeline name with dots' do
            expect(:get => 'api/admin/pipelines/foo.123').to route_to(action: 'show', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo.123')
          end

          it 'should route to show action of pipelines controller for pipeline name with hyphen' do
            expect(:get => 'api/admin/pipelines/foo-123').to route_to(action: 'show', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo-123')
          end

          it 'should route to show action of pipelines controller for pipeline name with underscore' do
            expect(:get => 'api/admin/pipelines/foo_123').to route_to(action: 'show', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo_123')
          end

          it 'should route to show action of pipelines controller for capitalized pipeline name' do
            expect(:get => 'api/admin/pipelines/FOO').to route_to(action: 'show', controller: 'api_v5/admin/pipelines', pipeline_name: 'FOO')
          end
        end
        describe "without_header" do
          before :each do
            teardown_header
          end
          it 'should not route to show action of pipelines controller without header' do
            expect(:get => 'api/admin/pipelines/foo').to_not route_to(action: 'show', controller: 'api_v5/admin/pipelines')
            expect(:get => 'api/admin/pipelines/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/pipelines/foo')
          end
        end
      end
    end

    describe "update" do
      before(:each) do
        login_as_pipeline_group_admin_user(@group)
        @pipeline = PipelineConfigMother.pipelineConfig(@pipeline_name)
        @pipeline.setOrigin(FileConfigOrigin.new)
        controller.send(:go_cache).put("GO_PIPELINE_CONFIGS_ETAGS_CACHE", @pipeline_name, "latest-etag")
      end

      it "should not update pipeline config if the user is not admin or pipeline group admin" do
        allow(@pipeline_groups).to receive(:hasGroup).and_return(true)
        allow(@security_service).to receive(:isUserAdminOfGroup).and_return(false)
        put_with_api_header :update, pipeline_name: @pipeline_name, :pipeline => pipeline

        # expect(response.code).to eq("401")

        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("You are not authorized to perform this action.")
      end

      it "should update pipeline config for an admin" do
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(@pipeline).and_return(@pipeline_md5)
        expect(@pipeline_config_service).to receive(:getPipelineConfig).twice.with(@pipeline_name).and_return(@pipeline)
        expect(@pipeline_config_service).to receive(:updatePipelineConfig).with(anything(), anything(), @pipeline_md5, anything())

        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest(@pipeline_md5)}\""

        put_with_api_header :update, pipeline_name: @pipeline_name, :pipeline => pipeline

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@pipeline, ApiV5::Admin::Pipelines::PipelineConfigRepresenter))
      end

      it "should not update pipeline config if etag passed does not match the one on server" do
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        controller.request.env['HTTP_IF_MATCH'] = "old-etag"

        put_with_api_header :update, pipeline_name: @pipeline_name, :pipeline => pipeline

        expect(response.code).to eq("412")
        expect(actual_response).to eq({:message => "Someone has modified the configuration for pipeline 'pipeline1'. Please update your copy of the config with the changes."})
      end

      it "should not update pipeline config if no etag is passed" do
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        put_with_api_header :update, pipeline_name: @pipeline_name, :pipeline => pipeline

        expect(response.code).to eq("412")
        expect(actual_response).to eq({:message => "Someone has modified the configuration for pipeline 'pipeline1'. Please update your copy of the config with the changes."})
      end

      it "should not update pipeline config when the pipeline is defined remotely" do
        gitMaterial = GitMaterialConfig.new("https://github.com/config-repos/repo", "master")
        origin = RepoConfigOrigin.new(ConfigRepoConfig.new(gitMaterial, "json-plugib"), "revision1")
        @pipeline.setOrigin(origin)

        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        put_with_api_header :update, pipeline_name: @pipeline_name, :pipeline => pipeline

        expect(response.code).to eq("422")
        expect(actual_response).to eq({:message => "Can not operate on pipeline 'pipeline1' as it is defined remotely in 'https://github.com/config-repos/repo at revision1'."})
      end

      it "should handle server validation errors" do
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        result = double('HttpLocalizedOperationResult')
        allow(result).to receive(:isSuccessful).and_return(false)
        allow(result).to receive(:message).with(anything()).and_return("message from server")
        allow(result).to receive(:httpCode).and_return(406)
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)

        @pipeline.addError("labelTemplate", PipelineConfig::LABEL_TEMPLATE_ERROR_MESSAGE % 'foo bar')
        allow(controller).to receive(:get_pipeline_from_request) do
          controller.instance_variable_set(:@pipeline_config_from_request, @pipeline)
        end
        expect(@pipeline_config_service).to receive(:updatePipelineConfig).with(anything(), anything(), @pipeline_md5, result)
        controller.request.env['HTTP_IF_MATCH'] = @latest_etag

        put_with_api_header :update, pipeline_name: @pipeline_name, :pipeline => invalid_pipeline

        expect(response.code).to eq("406")
        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("message from server")
        data = json[:data]
        data.delete(:_links)
        data[:materials].first.deep_symbolize_keys!
        data[:stages].first.deep_symbolize_keys!
        expect(data).to eq(expected_data_with_validation_errors)
      end

      it "should not allow renaming a pipeline" do
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        controller.request.env['HTTP_IF_MATCH'] = @latest_etag

        put_with_api_header :update, pipeline_name: @pipeline_name, :pipeline => pipeline("renamed_pipeline")

        expect(response.code).to eq("406")
        expect(actual_response).to eq({:message => "Renaming the pipeline resource is not supported by this API."})
      end

      it "should set package definition on to package material before save" do
        expect(@pipeline_config_service).to receive(:getPipelineConfig).twice.with(@pipeline_name).and_return(@pipeline)
        pipeline_being_saved = nil
        allow(@pipeline_config_service).to receive(:updatePipelineConfig) do |user, pipeline, result|
          pipeline_being_saved = pipeline
        end
        controller.request.env['HTTP_IF_MATCH'] = @latest_etag

        put_with_api_header :update, pipeline_name: @pipeline_name, :pipeline => pipeline_with_pluggable_material("pipeline1", "package", "package-name")

        expect(response).to be_ok
        expect(pipeline_being_saved.materialConfigs().first().getPackageDefinition()).to eq(@repo.findPackage("package-name"))
      end

      it "should set scm config on to pluggable scm material before save" do
        pipeline_being_saved = nil
        expect(@pipeline_config_service).to receive(:getPipelineConfig).twice.with(@pipeline_name).and_return(@pipeline)

        allow(@pipeline_config_service).to receive(:updatePipelineConfig) do |user, pipeline, result|
          pipeline_being_saved = pipeline
        end
        controller.request.env['HTTP_IF_MATCH'] = @latest_etag

        put_with_api_header :update, pipeline_name: @pipeline_name, :pipeline => pipeline_with_pluggable_material("pipeline1", "plugin", "scm-id")
        expect(response).to be_ok
        expect(pipeline_being_saved.materialConfigs().first().getSCMConfig()).to eq(@scm)
      end

      describe "route" do
        describe "with_header" do
          it 'should route to update action of pipelines controller for alphanumeric pipeline name' do
            expect(:put => 'api/admin/pipelines/foo123').to route_to(action: 'update', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo123')
          end

          it 'should route to update action of pipelines controller for pipeline name with dots' do
            expect(:put => 'api/admin/pipelines/foo.123').to route_to(action: 'update', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo.123')
          end

          it 'should route to update action of pipelines controller for pipeline name with hyphen' do
            expect(:put => 'api/admin/pipelines/foo-123').to route_to(action: 'update', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo-123')
          end

          it 'should route to update action of pipelines controller for pipeline name with underscore' do
            expect(:put => 'api/admin/pipelines/foo_123').to route_to(action: 'update', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo_123')
          end

          it 'should route to update action of pipelines controller for capitalized pipeline name' do
            expect(:put => 'api/admin/pipelines/FOO').to route_to(action: 'update', controller: 'api_v5/admin/pipelines', pipeline_name: 'FOO')
          end
        end
        describe "without_header" do
          before :each do
            teardown_header
          end

          it 'should not route to update action of pipelines controller without header' do
            expect(:put => 'api/admin/pipelines/foo').to_not route_to(action: 'update', controller: 'api_v5/admin/pipelines')
            expect(:put => 'api/admin/pipelines/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/pipelines/foo')
          end
        end
      end
    end

    describe "create" do
      before(:each) do
        @pipeline = PipelineConfigMother.pipelineConfig(@pipeline_name)
        @pipeline.setOrigin(FileConfigOrigin.new)
      end

      it "should not allow non admin users to create a new pipeline config" do
        login_as_pipeline_group_Non_Admin_user
        post_with_api_header :create, :pipeline => pipeline, :group => "new_grp"

        expect(response.code).to eq("401")

        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("You are not authorized to perform this action.")
      end

      it "should not allow admin users of one pipeline group to create a new pipeline config in another group" do
        enable_security
        allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
        allow(@pipeline_groups).to receive(:hasGroup).and_return(true)
        allow(@security_service).to receive(:isUserAdminOfGroup).and_return(false) # pipeline group admin
        allow(@security_service).to receive(:isUserAdmin).and_return(false) # not an admin

        post_with_api_header :create, :pipeline => pipeline, :group => "another_group"

        expect(response.code).to eq("401")

        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("You are not authorized to perform this action.")
      end

      it "should allow admin users create a new pipeline config in any group" do
        enable_security
        allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
        allow(@pipeline_groups).to receive(:hasGroup).and_return(false)
        allow(@security_service).to receive(:isUserAdmin).and_return(true)

        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        expect(@pipeline_config_service).to receive(:createPipelineConfig).with(anything(), anything(), anything(), "new_grp")
        expect(@pipeline_pause_service).to receive(:pause).with("pipeline1", "Under construction", @user)

        post_with_api_header :create, :pipeline => pipeline, :group => "new_grp"

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@pipeline, ApiV5::Admin::Pipelines::PipelineConfigRepresenter))
      end

      it "should create a new pipeline config" do
        login_as_pipeline_group_admin_user("new_grp")
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        expect(@pipeline_config_service).to receive(:createPipelineConfig).with(anything(), anything(), anything(), "new_grp")
        expect(@pipeline_pause_service).to receive(:pause).with("pipeline1", "Under construction", @user)

        post_with_api_header :create, :pipeline => pipeline, :group => "new_grp"

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@pipeline, ApiV5::Admin::Pipelines::PipelineConfigRepresenter))
      end

      it "should handle server validation errors" do
        login_as_pipeline_group_admin_user("group")
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        result = double('HttpLocalizedOperationResult')
        allow(result).to receive(:isSuccessful).and_return(false)
        allow(result).to receive(:message).with(anything()).and_return("message from server")
        allow(result).to receive(:httpCode).and_return(406)
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)

        @pipeline.addError("labelTemplate", PipelineConfig::LABEL_TEMPLATE_ERROR_MESSAGE % 'foo bar')
        allow(controller).to receive(:get_pipeline_from_request) do
          controller.instance_variable_set(:@pipeline_config_from_request, @pipeline)
        end

        expect(@pipeline_config_service).to receive(:createPipelineConfig).with(anything(), anything(), result, "group")
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("latest-etag")}\""

        post_with_api_header :create, :pipeline => invalid_pipeline, :group => "group"

        expect(response.code).to eq("406")
        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("message from server")
        data = json[:data]
        data.delete(:_links)
        data[:materials].first.deep_symbolize_keys!
        data[:stages].first.deep_symbolize_keys!
        expect(data).to eq(expected_data_with_validation_errors)
      end

      it "should fail if a pipeline by same name already exists" do
        login_as_pipeline_group_admin_user("new_grp")
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        expect(@pipeline_config_service).not_to receive(:createPipelineConfig).with(anything(), anything(), anything(), "new_grp")

        post_with_api_header :create, :pipeline => pipeline, :group => "new_grp"

        expect(response.code).to eq("422")

        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("Failed to add pipeline. The pipeline '#{@pipeline_name}' already exists.")
      end

      it "should fail if group is blank" do
        allow(@security_service).to receive(:isUserAdminOfGroup).and_return(true)
        allow(@security_service).to receive(:isUserAdmin).and_return(true)
        allow(@pipeline_config_service).to receive(:getPipelineConfig).and_return(nil)

        post_with_api_header :create, :pipeline => pipeline, :group => ""

        expect(response.code).to eq("422")

        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("Pipeline group must be specified for creating a pipeline.")
      end

      it "should set package definition on to package material before save" do
        login_as_pipeline_group_admin_user("group")
        pipeline_being_saved = nil
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        expect(@pipeline_pause_service).to receive(:pause).with("pipeline1", "Under construction", @user)

        allow(@pipeline_config_service).to receive(:createPipelineConfig) do |user, pipeline, result, group|
          pipeline_being_saved = pipeline
        end

        post_with_api_header :create, :pipeline => pipeline_with_pluggable_material("pipeline1", "package", "package-name"), :group => "group"
        expect(response).to be_ok
        expect(pipeline_being_saved.materialConfigs().first().getPackageDefinition()).to eq(@repo.findPackage("package-name"))
      end

      it "should set scm config on to pluggable scm material before save" do
        login_as_pipeline_group_admin_user("group")
        pipeline_being_saved = nil
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        expect(@pipeline_pause_service).to receive(:pause).with("pipeline1", "Under construction", @user)

        allow(@pipeline_config_service).to receive(:createPipelineConfig) do |user, pipeline, result, group|
          pipeline_being_saved = pipeline
        end

        post_with_api_header :create, :pipeline => pipeline_with_pluggable_material("pipeline1", "plugin", "scm-id"), :group => "group"
        expect(response).to be_ok
        expect(pipeline_being_saved.materialConfigs().first().getSCMConfig()).to eq(@scm)
      end

      describe "route" do
        describe "with_header" do

          it 'should route to create action of pipelines controller' do
            expect(:post => 'api/admin/pipelines/').to route_to(action: 'create', controller: 'api_v5/admin/pipelines')
          end
        end
        describe "without_header" do
          before :each do
            teardown_header
          end

          it 'should not route to create action of pipelines controller without header' do
            expect(:post => 'api/admin/pipelines').to_not route_to(action: 'create', controller: 'api_v5/admin/pipelines')
            expect(:post => 'api/admin/pipelines').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/pipelines')
          end
        end
      end
    end

    describe "destroy" do
      before(:each) do
        login_as_admin
        @pipeline_name = "pipeline1"
        @pipeline = PipelineConfigMother.pipelineConfig(@pipeline_name)
        allow(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        allow(@security_service).to receive(:isUserAdminOfGroup).and_return(true)
      end

      it "should delete pipeline config for an admin" do
        expect(@pipeline_config_service).to receive(:deletePipelineConfig).with(anything(), @pipeline, an_instance_of(HttpLocalizedOperationResult)) do |username, pipeline, result|
          result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", 'pipeline', pipeline.name.to_s))
        end

        put_with_api_header :destroy, pipeline_name: @pipeline_name

        expect(response.code).to eq("200")
        expect(actual_response).to eq({:message => "The pipeline 'pipeline1' was deleted successfully."})
      end


      it "should render not found if the specified pipeline is absent" do
        allow(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        put_with_api_header :destroy, pipeline_name: @pipeline_name

        expect(response.code).to eq("404")
        expect(actual_response).to eq({:message => "Either the resource you requested was not found, or you are not authorized to perform this action."})
      end

      it "should not delete pipeline config when the pipeline is defined remotely" do
        gitMaterial = GitMaterialConfig.new("https://github.com/config-repos/repo", "master")
        origin = RepoConfigOrigin.new(ConfigRepoConfig.new(gitMaterial, "json-plugib"), "revision1")
        @pipeline.setOrigin(origin)

        expect(@pipeline_config_service).to receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        put_with_api_header :destroy, pipeline_name: @pipeline_name

        expect(response.code).to eq("422")
        expect(actual_response).to eq({:message => "Can not operate on pipeline 'pipeline1' as it is defined remotely in 'https://github.com/config-repos/repo at revision1'."})
      end

      describe "route" do
        describe "with_header" do

          it 'should route to destroy action of pipelines controller for alphanumeric pipeline name' do
            expect(:delete => 'api/admin/pipelines/foo123').to route_to(action: 'destroy', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo123')
          end

          it 'should route to destroy action of pipelines controller for pipeline name with dots' do
            expect(:delete => 'api/admin/pipelines/foo.123').to route_to(action: 'destroy', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo.123')
          end

          it 'should route to destroy action of pipelines controller for pipeline name with hyphen' do
            expect(:delete => 'api/admin/pipelines/foo-123').to route_to(action: 'destroy', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo-123')
          end

          it 'should route to destroy action of pipelines controller for pipeline name with underscore' do
            expect(:delete => 'api/admin/pipelines/foo_123').to route_to(action: 'destroy', controller: 'api_v5/admin/pipelines', pipeline_name: 'foo_123')
          end

          it 'should route to destroy action of pipelines controller for capitalized pipeline name' do
            expect(:delete => 'api/admin/pipelines/FOO').to route_to(action: 'destroy', controller: 'api_v5/admin/pipelines', pipeline_name: 'FOO')
          end
        end
        describe "without_header" do
          before :each do
            teardown_header
          end
          it 'should not route to destroy action of pipelines controller without header' do
            expect(:delete => 'api/admin/pipelines/foo').to_not route_to(action: 'destroy', controller: 'api_v5/admin/pipelines')
            expect(:delete => 'api/admin/pipelines/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/pipelines/foo')
          end
        end
      end
    end

    def expected_data_with_validation_errors
      {
        lock_behavior: "none",
        errors: {label_template: ["Invalid label 'foo bar'. Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as \#{<param-name>}."]},
        label_template: "${COUNT}",
        materials: [{type: "svn", attributes: {url: "http://some/svn/url", destination: "svnDir", filter: nil, invert_filter: false, name: "http___some_svn_url", auto_update: true, check_externals: false, username: nil}}],
        name: "pipeline1",
        origin: {
          _links: {
            self: {
              href: 'http://test.host/admin/config_xml'
            },
            doc: {
              href: 'https://api.gocd.org/#get-configuration'
            }
          },
          type: 'gocd'
        },
        environment_variables: [],
        parameters: [],
        stages: [{name: "mingle", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: {:type => "success", :authorization => {:roles => [], :users => []}}, environment_variables: [], jobs: []}],
        template: nil,
        timer: nil,
        tracking_tool: nil
      }
    end

    def invalid_pipeline
      {
        label_template: "${COUNT}",
        lock_behavior: "none",
        name: "pipeline1",
        materials: [
          {
            type: "SvnMaterial",
            attributes: {
              name: "http___some_svn_url",
              auto_update: true,
              url: "http://some/svn/url",
              destination: "svnDir",
              filter: nil,
              check_externals: false,
              username: nil,
              password: nil
            }
          }
        ],
        stages: [{name: "mingle", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: {type: "success", authorization: {}}, jobs: []}],
        errors: {label_template: ["Invalid label. Label should be composed of alphanumeric text, it should contain the builder number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as \#{<param-name>}."]}
      }
    end

    def pipeline (pipeline_name="pipeline1", material_type="hg", task_type="exec")
      {label_template: "Jyoti-${COUNT}", lock_behavior: "none", name: pipeline_name, template_name: nil, parameters: [], environment_variables: [], materials: [{type: material_type, attributes: {url: "../manual-testing/ant_hg/dummy", destination: "dest_dir", filter: {ignore: []}}, name: "dummyhg", auto_update: true}], stages: [{name: "up42_stage", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: {type: "success", authorization: {roles: [], users: []}}, environment_variables: [], jobs: [{name: "up42_job", run_on_all_agents: false, environment_variables: [], resources: [], tasks: [{type: task_type, attributes: {command: "ls", working_dir: nil}, run_if: []}], tabs: [], artifacts: [], properties: []}]}], mingle: {base_url: nil, project_identifier: nil, mql_grouping_conditions: nil}}
    end

    def pipeline_with_pluggable_material (pipeline_name, material_type, ref)
      {label_template: "${COUNT}", name: pipeline_name, materials: [{type: material_type, attributes: {ref: ref}}], stages: [{name: "up42_stage", jobs: [{name: "up42_job", tasks: [{type: "exec", attributes: {command: "ls"}}]}]}]}
    end
  end

end
