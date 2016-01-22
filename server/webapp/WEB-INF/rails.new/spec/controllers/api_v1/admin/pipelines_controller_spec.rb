##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

require 'spec_helper'

describe ApiV1::Admin::PipelinesController do
  before(:each) do
    @pipeline_config_service = double("pipeline_config_service")
    controller.stub("pipeline_config_service").and_return(@pipeline_config_service)
    @pipeline_pause_service = double("pipeline_pause_service")
    controller.stub("pipeline_pause_service").and_return(@pipeline_pause_service)
    @go_config_service = double("go_config_service")
    controller.stub("go_config_service").and_return(@go_config_service)
    go_config = BasicCruiseConfig.new
    @repo = PackageRepositoryMother.create("repoid")
    @scm= SCMMother.create("scm-id")
    go_config.getPackageRepositories().add(@repo)
    go_config.getSCMs().add(@scm)
    @go_config_service.stub(:getCurrentConfig).and_return(go_config)
    @go_config_service.stub(:checkConfigFileValid).and_return(GoConfigValidity::valid())
  end

  after(:each) do
    controller.send(:go_cache).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE")
  end

  describe :security do
    describe :show do
      before(:each) do
        @pipeline_config_service.stub(:getPipelineConfig).with(anything()).and_return(PipelineConfig.new)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to disallow_action(:get, :show, { :name => "pipeline1" }).with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show)
      end
    end

    describe :update do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :update)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to disallow_action(:put, :update, { :name => "pipeline1" }).with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:put, :update)
      end
    end

    describe :create do
      it 'should allow anyone, with security disabled' do
        disable_security
        @pipeline_config_service.should_receive(:getPipelineConfig).with(anything()).and_return(nil)
        expect(controller).to allow_action(:post, :create, :group => "grp")
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to disallow_action(:post, :create, :pipeline => {:name => "pipeline1"}, :group => "grp").with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        @pipeline_config_service.should_receive(:getPipelineConfig).with(anything()).and_return(nil)
        expect(controller).to allow_action(:post, :create, :group => "grp")
      end
    end
  end

  describe :action do
    before :each do
      enable_security
    end

    describe :show do
      it "should show pipeline config for an admin" do
        login_as_admin
        pipeline_name = "pipeline1"
        pipeline      = PipelineConfigMother.pipelineConfig(pipeline_name)
        @pipeline_config_service.should_receive(:getPipelineConfig).with(pipeline_name).and_return(pipeline)

        get_with_api_header :show, :name => pipeline_name
        expect(response).to be_ok
        expected_response = expected_response(pipeline, ApiV1::Config::PipelineConfigRepresenter)
        expect(actual_response).to eq(expected_response)
        cached_etag = Digest::MD5.hexdigest(JSON.generate(expected_response))
        expect(response.etag).to eq("\"#{Digest::MD5.hexdigest(cached_etag)}\"")
        expect(response.headers["ETag"]).to eq("\"#{Digest::MD5.hexdigest(cached_etag)}\"")
        expect(controller.send(:go_cache).get("GO_PIPELINE_CONFIGS_ETAGS_CACHE", pipeline_name)).to eq(cached_etag)
      end

      it "should return 304 for show pipeline config if etag sent in request is fresh" do
        login_as_admin
        pipeline_name = "pipeline1"
        pipeline      = PipelineConfigMother.pipelineConfig(pipeline_name)
        @pipeline_config_service.should_receive(:getPipelineConfig).with(pipeline_name).and_return(pipeline)
        controller.send(:go_cache).put("GO_PIPELINE_CONFIGS_ETAGS_CACHE", pipeline_name, "latest-etag")

        controller.request.env['HTTP_IF_NONE_MATCH'] = Digest::MD5.hexdigest("latest-etag")

        get_with_api_header :show, { :name => pipeline_name }
        expect(response.code).to eq("304")
        expect(response.body).to be_empty
      end

      it "should return 404 for show pipeline config if pipeline is not found" do
        login_as_admin
        pipeline_name = "pipeline1"
        @pipeline_config_service.should_receive(:getPipelineConfig).with(pipeline_name).and_return(nil)
        get_with_api_header :show, :name => pipeline_name
        expect(response.code).to eq("404")
        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("Either the resource you requested was not found, or you are not authorized to perform this action.")
      end

      it "should show pipeline config if etag sent in request is stale" do
        login_as_admin
        pipeline_name = "pipeline1"
        pipeline      = PipelineConfigMother.pipelineConfig(pipeline_name)
        @pipeline_config_service.should_receive(:getPipelineConfig).with(pipeline_name).and_return(pipeline)
        controller.send(:go_cache).put("GO_PIPELINE_CONFIGS_ETAGS_CACHE", pipeline_name, "latest-etag")

        controller.request.env['HTTP_IF_NONE_MATCH'] = "old-etag"

        get_with_api_header :show, { name: pipeline_name }
        expect(response).to be_ok
        expect(response.body).to_not be_empty
      end
    end

    describe :update do
      before(:each) do
        login_as_admin
        @pipeline_name = "pipeline1"
        @pipeline      = PipelineConfigMother.pipelineConfig(@pipeline_name)
        controller.send(:go_cache).put("GO_PIPELINE_CONFIGS_ETAGS_CACHE", @pipeline_name, "latest-etag")
      end

      it "should update pipeline config for an admin" do
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        @pipeline_config_service.should_receive(:updatePipelineConfig).with(anything(), anything(), anything())
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("latest-etag")}\""

        put_with_api_header :update, name: @pipeline_name, :pipeline => pipeline

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@pipeline, ApiV1::Config::PipelineConfigRepresenter))
      end

      it "should not update pipeline config if etag passed does not match the one on server" do
        controller.request.env['HTTP_IF_MATCH'] = "old-etag"

        put_with_api_header :update, name: @pipeline_name, :pipeline => pipeline

        expect(response.code).to eq("412")
        expect(actual_response).to eq({ :message => "Someone has modified the configuration for pipeline 'pipeline1'. Please update your copy of the config with the changes." })
      end


      it "should not update pipeline config if no etag is passed" do
        put_with_api_header :update, name: @pipeline_name, :pipeline => pipeline

        expect(response.code).to eq("412")
        expect(actual_response).to eq({ :message => "Someone has modified the configuration for pipeline 'pipeline1'. Please update your copy of the config with the changes." })
      end

      it "should handle server validation errors" do
        result = double('HttpLocalizedOperationResult')
        result.stub(:isSuccessful).and_return(false)
        result.stub(:message).with(anything()).and_return("message from server")
        result.stub(:httpCode).and_return(406)
        HttpLocalizedOperationResult.stub(:new).and_return(result)

        @pipeline.addError("labelTemplate", PipelineConfig::LABEL_TEMPLATE_ERROR_MESSAGE)
        controller.stub(:get_pipeline_from_request) do
          controller.instance_variable_set(:@pipeline_config_from_request, @pipeline)
        end
        @pipeline_config_service.should_not_receive(:getPipelineConfig).with(@pipeline_name)
        @pipeline_config_service.should_receive(:updatePipelineConfig).with(anything(), anything(), result)
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("latest-etag")}\""

        put_with_api_header :update, name: @pipeline_name, :pipeline => invalid_pipeline

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
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("latest-etag")}\""

        put_with_api_header :update, name: @pipeline_name, :pipeline => pipeline("renamed_pipeline")

        expect(response.code).to eq("406")
        expect(actual_response).to eq({ :message => "Renaming of pipeline is not supported by this API." })
      end

      it "should set package definition on to package material before save" do
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        pipeline_being_saved = nil
        allow(@pipeline_config_service).to receive(:updatePipelineConfig) do |user, pipeline, result|
          pipeline_being_saved = pipeline
        end
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("latest-etag")}\""

        put_with_api_header :update, name: @pipeline_name, :pipeline => pipeline_with_pluggable_material("pipeline1", "package", "package-name")

        expect(response).to be_ok
        expect(pipeline_being_saved.materialConfigs().first().getPackageDefinition()).to eq(@repo.findPackage("package-name"))
      end

      it "should set scm config on to pluggable scm material before save" do
        pipeline_being_saved = nil
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)

        allow(@pipeline_config_service).to receive(:updatePipelineConfig) do |user, pipeline, result|
          pipeline_being_saved = pipeline
        end
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("latest-etag")}\""

        put_with_api_header :update, name: @pipeline_name, :pipeline => pipeline_with_pluggable_material("pipeline1", "plugin", "scm-id")
        expect(response).to be_ok
        expect(pipeline_being_saved.materialConfigs().first().getSCMConfig()).to eq(@scm)
      end


    end

    describe :create do
      before(:each) do
        login_as_admin
        @pipeline_name = "pipeline1"
        @pipeline      = PipelineConfigMother.pipelineConfig(@pipeline_name)
      end

      it "should create a new pipeline config" do
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        @pipeline_config_service.should_receive(:createPipelineConfig).with(anything(), anything(), anything(), "new_grp")
        expect(@pipeline_pause_service).to receive(:pause).with("pipeline1", "Under construction", @user)

        post_with_api_header :create, name: @pipeline_name, :pipeline => pipeline, :group => "new_grp"

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@pipeline, ApiV1::Config::PipelineConfigRepresenter))
      end

      it "should handle server validation errors" do
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        result = double('HttpLocalizedOperationResult')
        result.stub(:isSuccessful).and_return(false)
        result.stub(:message).with(anything()).and_return("message from server")
        result.stub(:httpCode).and_return(406)
        HttpLocalizedOperationResult.stub(:new).and_return(result)

        @pipeline.addError("labelTemplate", PipelineConfig::LABEL_TEMPLATE_ERROR_MESSAGE)
        controller.stub(:get_pipeline_from_request) do
          controller.instance_variable_set(:@pipeline_config_from_request, @pipeline)
        end
        @pipeline_config_service.should_not_receive(:getPipelineConfig).with(@pipeline_name)
        @pipeline_config_service.should_receive(:createPipelineConfig).with(anything(), anything(), result, "group")
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("latest-etag")}\""

        post_with_api_header :create, name: @pipeline_name, :pipeline => invalid_pipeline, :group => "group"

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
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        @pipeline_config_service.should_not_receive(:createPipelineConfig).with(anything(), anything(), anything(), "new_grp")

        post_with_api_header :create, name: @pipeline_name, :pipeline => pipeline, :group => "new_grp"

        expect(response.code).to eq("422")

        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("Failed to create pipeline '#{@pipeline_name}' as another pipeline by the same name already exists.")
      end

      it "should fail if group is blank" do
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        @pipeline_config_service.should_not_receive(:createPipelineConfig).with(anything(), anything(), anything(), anything())

        post_with_api_header :create, name: @pipeline_name, :pipeline => pipeline, :group => ""

        expect(response.code).to eq("422")

        json = JSON.parse(response.body).deep_symbolize_keys
        expect(json[:message]).to eq("Pipeline group must be specified for creating a pipeline.")
      end

      it "should set package definition on to package material before save" do
        pipeline_being_saved = nil
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        expect(@pipeline_pause_service).to receive(:pause).with("pipeline1", "Under construction", @user)

        allow(@pipeline_config_service).to receive(:createPipelineConfig) do |user, pipeline, result, group|
          pipeline_being_saved = pipeline
        end

        post_with_api_header :create, name: @pipeline_name, :pipeline => pipeline_with_pluggable_material("pipeline1", "package", "package-name"), :group => "group"
        expect(response).to be_ok
        expect(pipeline_being_saved.materialConfigs().first().getPackageDefinition()).to eq(@repo.findPackage("package-name"))
      end

      it "should set scm config on to pluggable scm material before save" do
        pipeline_being_saved = nil
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(nil)
        @pipeline_config_service.should_receive(:getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
        expect(@pipeline_pause_service).to receive(:pause).with("pipeline1", "Under construction", @user)

        allow(@pipeline_config_service).to receive(:createPipelineConfig) do |user, pipeline, result, group|
          pipeline_being_saved = pipeline
        end

        post_with_api_header :create, name: @pipeline_name, :pipeline => pipeline_with_pluggable_material("pipeline1", "plugin", "scm-id"), :group => "group"
        expect(response).to be_ok
        expect(pipeline_being_saved.materialConfigs().first().getSCMConfig()).to eq(@scm)
      end
    end

    def expected_data_with_validation_errors
      {
        enable_pipeline_locking: false,
        errors:                  { label_template: ["Invalid label. Label should be composed of alphanumeric text, it should contain the builder number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as \#{<param-name>}."] },
        label_template:          "${COUNT}",
        materials:               [{ type: "svn", attributes: { url: "http://some/svn/url", destination: "svnDir", filter: nil, name: "http___some_svn_url", auto_update: true, check_externals: false, username: nil } }],
        name:                    "pipeline1",
        environment_variables:   [],
        parameters:              [],
        stages:                  [{ name: "mingle", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: { :type => "success", :authorization => { :roles => [], :users => [] } }, environment_variables: [], jobs: [] }],
        template:                nil,
        timer:                   nil,
        tracking_tool:           nil
      }
    end

    def invalid_pipeline
      {
        label_template:          "${COUNT}",
        enable_pipeline_locking: false,
        name:                    "pipeline1",
        materials:               [
                                   {
                                     type:       "SvnMaterial",
                                     attributes: {
                                       name:            "http___some_svn_url",
                                       auto_update:     true,
                                       url:             "http://some/svn/url",
                                       destination:     "svnDir",
                                       filter:          nil,
                                       check_externals: false,
                                       username:        nil,
                                       password:        nil
                                     }
                                   }
                                 ],
        stages:                  [{ name: "mingle", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: { type: "success", authorization: {} }, jobs: [] }],
        errors:                  { label_template: ["Invalid label. Label should be composed of alphanumeric text, it should contain the builder number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as \#{<param-name>}."] }
      }
    end

    def pipeline (pipeline_name="pipeline1", material_type="hg", task_type="exec")
      { label_template: "Jyoti-${COUNT}", enable_pipeline_locking: false, name: pipeline_name, template_name: nil, parameters: [], environment_variables: [], materials: [{ type: material_type, attributes: { url: "../manual-testing/ant_hg/dummy", destination: "dest_dir", filter: { ignore: [] } }, name: "dummyhg", auto_update: true }], stages: [{ name: "up42_stage", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: { type: "success", authorization: { roles: [], users: [] } }, environment_variables: [], jobs: [{ name: "up42_job", run_on_all_agents: false, environment_variables: [], resources: [], tasks: [{ type: task_type, attributes: { command: "ls", working_dir: nil }, run_if: [] }], tabs: [], artifacts: [], properties: [] }] }], mingle: { base_url: nil, project_identifier: nil, mql_grouping_conditions: nil } }
    end

    def pipeline_with_pluggable_material (pipeline_name, material_type, ref)
      { label_template: "${COUNT}", name: pipeline_name, materials: [{ type: material_type, attributes: { ref: ref}}], stages: [{ name: "up42_stage", jobs: [{ name: "up42_job", tasks: [{ type: "exec", attributes: { command: "ls"} }] }] }] }
    end
  end

end
