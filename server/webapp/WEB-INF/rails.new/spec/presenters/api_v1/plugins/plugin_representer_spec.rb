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

describe ApiV1::Plugins::PluginRepresenter do

  describe :scm do

    it 'renders a plugin of SCM type with hal representation' do

      scm_view_model = SCMPluginViewModel.new('plugin-id', 'version', get_scm_configurations)

      presenter   = ApiV1::Plugins::PluginRepresenter.new(scm_view_model)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :doc)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugins/scm/plugin-id')
      actual_json.delete(:_links)
      expect(actual_json).to eq(expected_plugin_hash)
    end

    private

    def expected_plugin_hash
      {
        plugin_id:      "plugin-id",
        version:        "version",
        type:           "scm",
        configurations: {
          properties: [
                        {
                          key:           "k1",
                          required:      true,
                          secure:        false,
                          display_name:  "",
                          display_order: 0
                        }
                      ]
        }
      }

    end

    def get_scm_configurations
      c1 = SCMConfiguration.new("k1").with(SCMConfiguration::REQUIRED, true);
      scm_configurations = SCMConfigurations.new();
      scm_configurations.add(c1);
      scm_configurations
    end
  end
  describe :package_repository do

    it 'renders a plugin of Package Repository type with hal representation' do

      package_repository_view_model = PackageRepositoryPluginViewModel.new('plugin-id', 'version', get_package_configurations, get_repo_configurations)
      presenter                     = ApiV1::Plugins::PluginRepresenter.new(package_repository_view_model)
      actual_json                   = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :doc)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugins/package-repository/plugin-id')
      actual_json.delete(:_links)
      expect(actual_json).to eq(expected_plugin_hash)

    end

    private

    def expected_plugin_hash
      {
        plugin_id:      "plugin-id",
        version:        "version",
        type:           "package-repository",
        configurations: {
          repository_properties: [
                                   {
                                     key:           "REPO-KEY1",
                                     required:      true,
                                     secure:        false,
                                     display_name:  "",
                                     display_order: 0
                                   },
                                   {
                                     key:           "REPO-KEY2",
                                     required:      false,
                                     secure:        false,
                                     display_name:  "",
                                     display_order: 0
                                   }
                                 ],
          package_properties:    [
                                   {
                                     key:           "PACKAGE_KEY",
                                     required:      true,
                                     secure:        false,
                                     display_name:  "",
                                     display_order: 0
                                   }
                                 ]
        }
      }
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
  end

  describe :task do
    it 'renders all plugins of Task type with hal representation' do
      task_view_model = TaskPluginViewModel.new('plugin-id', 'version', get_task_configurations)
      presenter       = ApiV1::Plugins::PluginRepresenter.new(task_view_model)
      actual_json     = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :doc)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugins/task/plugin-id')
      actual_json.delete(:_links)
      expect(actual_json).to eq(expected_plugin_hash)
    end

    private
    def get_task_configurations
      task_config = com.thoughtworks.go.plugin.api.task.TaskConfig.new
      task_config.addProperty("K1").withDefault("V1");
      task_config.addProperty("K2").withDefault("V2");
      task_config
    end

    def expected_plugin_hash
      {
        plugin_id:      "plugin-id",
        version:        "version",
        type:           "task",
        configurations: {
          properties: [
                        {
                          key:           "K1",
                          required:      false,
                          secure:        false,
                          display_name:  "K1",
                          display_order: 0
                        },
                        {
                          key:           "K2",
                          required:      false,
                          secure:        false,
                          display_name:  "K2",
                          display_order: 0
                        }
                      ]
        }
      }
    end
  end
  describe :disabled do
    it 'renders a plugin of Disabled type with hal representation' do
      disabled_plugin_view_model = DisabledPluginViewModel.new('plugin-id', 'version', 'Invalid plugin')
      presenter                  = ApiV1::Plugins::PluginRepresenter.new(disabled_plugin_view_model)
      actual_json                = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:doc)
      actual_json.delete(:_links)
      expect(actual_json).to eq(expected_plugin_hash)
    end

    private
    def get_task_configurations
      task_config = com.thoughtworks.go.plugin.api.task.TaskConfig.new
      task_config.addProperty("K1").withDefault("V1");
      task_config.addProperty("K2").withDefault("V2");
      task_config
    end

    def expected_plugin_hash
      {
        plugin_id: "plugin-id",
        version:   "version",
        type:      "disabled",
        message:   "Invalid plugin"
      }
    end
  end
end
