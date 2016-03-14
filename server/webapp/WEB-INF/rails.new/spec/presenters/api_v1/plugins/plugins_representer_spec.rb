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
    it 'renders all plugins of SCM type with hal representation' do
      scm_view_model         = SCMPluginViewModel.new('plugin-id', 'version', '', SCMConfigurations.new)
      scm_plugin_view_models = ArrayList.new
      scm_plugin_view_models.add(scm_view_model)
      presenter   = ApiV1::Plugins::PluginsRepresenter.new(scm_plugin_view_models, {type: 'scm'})
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :doc)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugins/scm')
      expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/#plugins')
      actual_json.delete(:_links)
      actual_json.fetch(:_embedded).should == {plugins: [ApiV1::Plugins::PluginRepresenter.new(scm_view_model).to_hash(url_builder: UrlBuilder.new)]}
    end
  end

  describe :package_repository do
    it 'renders all plugins of Package Repository type with hal representation' do
      package_repository_view_model  = PackageRepositoryPluginViewModel.new('plugin-id', 'version', '', PackageConfigurations.new, PackageConfigurations.new)
      package_repository_view_models = ArrayList.new
      package_repository_view_models.add(package_repository_view_model)
      presenter   = ApiV1::Plugins::PluginsRepresenter.new(package_repository_view_models, {type: 'package_repository'})
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :doc)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugins/package_repository')
      expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/#plugins')
      actual_json.delete(:_links)
      actual_json.fetch(:_embedded).should == {plugins: [ApiV1::Plugins::PluginRepresenter.new(package_repository_view_model).to_hash(url_builder: UrlBuilder.new)]}
    end

    describe :task do
      it 'renders all plugins of Task type with hal representation' do
        task_view_model  = TaskPluginViewModel.new('plugin-id', 'version', '', com.thoughtworks.go.plugin.api.task::TaskConfig.new)
        task_view_models = ArrayList.new
        task_view_models.add(task_view_model)
        presenter   = ApiV1::Plugins::PluginsRepresenter.new(task_view_models, {type: 'task'})
        actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

        expect(actual_json).to have_links(:self, :doc)
        expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugins/task')
        expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/#plugins')
        actual_json.delete(:_links)
        actual_json.fetch(:_embedded).should == {plugins: [ApiV1::Plugins::PluginRepresenter.new(task_view_model).to_hash(url_builder: UrlBuilder.new)]}
      end
    end
  end
  it 'renders all plugins  with hal representation' do
    view_models                   = ArrayList.new
    task_view_model               = TaskPluginViewModel.new('plugin-id', 'version','', com.thoughtworks.go.plugin.api.task::TaskConfig.new)
    package_repository_view_model = PackageRepositoryPluginViewModel.new('plugin-id', 'version', '', PackageConfigurations.new, PackageConfigurations.new)
    disabled_plugin_view_model    = DisabledPluginViewModel.new('plugin-id', 'version', 'Invalid plugin')

    view_models.add(task_view_model)
    view_models.add(package_repository_view_model)
    view_models.add(disabled_plugin_view_model)
    presenter   = ApiV1::Plugins::PluginsRepresenter.new(view_models)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugins')
    expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/#plugins')
    actual_json.delete(:_links)
    actual_json.fetch(:_embedded).should == {plugins: [
                                                        ApiV1::Plugins::PluginRepresenter.new(task_view_model).to_hash(url_builder: UrlBuilder.new),
                                                        ApiV1::Plugins::PluginRepresenter.new(package_repository_view_model).to_hash(url_builder: UrlBuilder.new),
                                                        ApiV1::Plugins::PluginRepresenter.new(disabled_plugin_view_model).to_hash(url_builder: UrlBuilder.new)
                                                      ]}
  end
end
