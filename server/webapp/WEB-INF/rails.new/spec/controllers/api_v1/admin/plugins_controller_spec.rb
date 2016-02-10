##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe ApiV1::Admin::PluginsController do

  describe :security do
    describe :index do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to disallow_action(:get, :index, {:type => "scm"}).with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :index)
      end
    end
    describe :show do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to disallow_action(:get, :show, type: 'scm').with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show)
      end
    end
  end

  describe :action do
    before :each do
      enable_security
      @plugin_service = double('plugin_service')
      controller.stub(:plugin_service).and_return(@plugin_service)
    end

    describe :index do
      it 'should render all plugins json' do
        view_models    = ArrayList.new
        scm_view_model = SCMPluginViewModel.new('plugin-id', 'version', get_scm_configurations)
        view_models.add(scm_view_model)

        package_repository_view_model = PackageRepositoryPluginViewModel.new('plugin-id', 'version', get_package_configurations, get_repo_configurations)
        view_models.add(package_repository_view_model)

        task_plugin_view_model = TaskPluginViewModel.new('plugin-id', 'version', get_task_configurations)
        view_models.add(task_plugin_view_model)

        invalid_plugin_view_model = com.thoughtworks.go.server.ui.DisabledPluginViewModel.new('plugin-id', 'version', 'Invalid Plugin')
        view_models.add(invalid_plugin_view_model)

        @plugin_service.stub(:populatePluginViewModels).and_return(view_models)
        @plugin_service.stub(:isValidPluginType).and_return(true)

        login_as_admin
        get_with_api_header :index
        expect(response.code).to eq("200")
        expect(actual_response).to eq(expected_response_with_options(view_models, ApiV1::Plugins::PluginsRepresenter))
      end
    end
    describe :show_with_type do
      describe :scm do
        it 'should render scm plugins json' do
          scm_view_model         = SCMPluginViewModel.new('plugin-id', 'version', get_scm_configurations)
          scm_plugin_view_models = ArrayList.new
          scm_plugin_view_models.add(scm_view_model)
          @plugin_service.stub(:populatePluginViewModelsOfType).and_return(scm_plugin_view_models)
          @plugin_service.stub(:isValidPluginType).and_return(true)

          login_as_admin
          get_with_api_header :show, type: 'scm'
          expect(response.code).to eq("200")
          expect(actual_response).to eq(expected_response_with_options(scm_plugin_view_models, {type: 'scm'}, ApiV1::Plugins::PluginsRepresenter))
        end

      end

      describe :package_configurations do
        it 'should render package repository plugins json' do
          package_repository_view_model  = PackageRepositoryPluginViewModel.new('plugin-id', 'version', get_package_configurations, get_repo_configurations)
          package_repository_view_models = ArrayList.new
          package_repository_view_models.add(package_repository_view_model)
          @plugin_service.stub(:populatePluginViewModelsOfType).and_return(package_repository_view_models)
          @plugin_service.stub(:isValidPluginType).and_return(true)

          login_as_admin
          get_with_api_header :show, type: 'package_repository'
          expect(response.code).to eq("200")
          expect(actual_response).to eq(expected_response_with_options(package_repository_view_models, {type: 'package_repository'}, ApiV1::Plugins::PluginsRepresenter))
        end

      end

      describe :task do
        it 'should render task plugins json' do
          task_plugin_view_model  = TaskPluginViewModel.new('plugin-id', 'version', get_task_configurations)
          task_plugin_view_models = ArrayList.new
          task_plugin_view_models.add(task_plugin_view_model)

          @plugin_service.stub(:populatePluginViewModelsOfType).and_return(task_plugin_view_models)
          @plugin_service.stub(:isValidPluginType).and_return(true)

          login_as_admin
          get_with_api_header :show, type: 'task'
          expect(response.code).to eq("200")
          expect(actual_response).to eq(expected_response_with_options(task_plugin_view_models, {type: 'task'}, ApiV1::Plugins::PluginsRepresenter))
        end
      end

      it 'should give error when invalid plugin type is passed' do
        @plugin_service.stub(:populatePluginViewModelsOfType).and_return(nil)
        @plugin_service.stub(:isValidPluginType).and_return(false)

        login_as_admin
        get_with_api_header :show, type: 'invalid_plugin'

        expect(response.code).to eq("422")
        expect(response.body).to include("Invalid plugin type")
      end
    end
    describe :show_with_type_and_plugin_id do
      describe :scm do
        it 'should render a scm plugin json' do
          scm_view_model = SCMPluginViewModel.new('plugin-id', 'version', get_scm_configurations)
          @plugin_service.stub(:populatePluginViewModel).and_return(scm_view_model)
          @plugin_service.stub(:isValidPluginType).and_return(true)

          login_as_admin
          get_with_api_header :show, type: 'scm', plugin_id: 'plugin-id'
          expect(response.code).to eq("200")
          expect(actual_response).to eq(expected_response(scm_view_model, ApiV1::Plugins::PluginRepresenter))
        end
      end
      describe :package_repository do
        it 'should render a package repository plugin json' do
          package_repository_view_model = PackageRepositoryPluginViewModel.new('plugin-id', 'version', get_package_configurations, get_repo_configurations)
          @plugin_service.stub(:populatePluginViewModel).and_return(package_repository_view_model)
          @plugin_service.stub(:isValidPluginType).and_return(true)

          login_as_admin
          get_with_api_header :show, type: 'package-repository', plugin_id: 'plugin-id'
          expect(response.code).to eq("200")
          expect(actual_response).to eq(expected_response(package_repository_view_model, ApiV1::Plugins::PluginRepresenter))
        end
      end

      describe :task do
        it 'should render a task plugin json' do
          task_plugin_view_model = TaskPluginViewModel.new('plugin-id', 'version', get_task_configurations)

          @plugin_service.stub(:populatePluginViewModel).and_return(task_plugin_view_model)
          @plugin_service.stub(:isValidPluginType).and_return(true)

          login_as_admin
          get_with_api_header :show, type: 'task', plugin_id: 'plugin-id'
          expect(response.code).to eq("200")
          expect(actual_response).to eq(expected_response(task_plugin_view_model, ApiV1::Plugins::PluginRepresenter))
        end
      end

      it 'should give error when invalid plugin type is passed' do
        @plugin_service.stub(:populatePluginViewModel).and_return(nil)
        @plugin_service.stub(:isValidPluginType).and_return(true)
        login_as_admin
        get_with_api_header :show, type: 'invalid_plugin', plugin_id: 'plugin-id'
        expect(response.code).to eq("422")
        expect(response.body).to include("Your request could not be processed. Invalid plugin id 'plugin-id' or invalid plugin type 'invalid_plugin'. Type has to be one of 'scm','package-repository'")

      end

      it 'should give error when invalid plugin id is passed' do
        @plugin_service.stub(:populatePluginViewModel).and_return(nil)
        @plugin_service.stub(:isValidPluginType).and_return(false)
        login_as_admin
        get_with_api_header :show, type: 'scm', plugin_id: 'invalid-plugin-id'
        expect(response.code).to eq("422")
        expect(response.body).to include("Your request could not be processed. Invalid plugin id 'invalid-plugin-id' or invalid plugin type 'scm'. Type has to be one of 'scm','package-repository'")
      end
    end

    private
    def get_scm_configurations
      c1 = SCMConfiguration.new("k1").with(SCMConfiguration::REQUIRED, true);
      c2 = SCMConfiguration.new("k2").with(SCMConfiguration::REQUIRED, false);
      scm_configurations = SCMConfigurations.new();
      scm_configurations.add(c1);
      scm_configurations.add(c2);
      scm_configurations
    end

    def get_repo_configurations
      repository_configuration = com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration.new
      repository_configuration.add(com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty.new("REPO-KEY1"));
      repository_configuration.add(com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty.new("REPO-KEY2").with(com.thoughtworks.go.plugin.api.config.Property::REQUIRED, false));

      PackageConfigurations.new(repository_configuration)

    end

    def get_package_configurations
      package_configuration = com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.new
      package_configuration.add(com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty.new("PACKAGE_KEY"));

      PackageConfigurations.new(package_configuration)
    end

    def get_task_configurations
      task_config = com.thoughtworks.go.plugin.api.task.TaskConfig.new
      task_config.addProperty("K1").withDefault("V1");
      task_config.addProperty("K2").withDefault("V2");
      task_config
    end

  end
end
