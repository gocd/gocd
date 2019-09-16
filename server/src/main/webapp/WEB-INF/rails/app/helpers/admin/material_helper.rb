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

module Admin
  module MaterialHelper
    include JavaImports

    def material_options
      {'Git' => GitMaterialConfig::TYPE,
       'Subversion' => SvnMaterialConfig::TYPE,
       'Mercurial' => HgMaterialConfig::TYPE,
       'Perforce' => P4MaterialConfig::TYPE,
       'Team Foundation Server' => com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE,
       'Pipeline' => DependencyMaterialConfig::TYPE,
       'Package' => PackageMaterialConfig::TYPE
      }
    end

    def repository_packages_map_from_config
      return @repository_packages_map if @repository_packages_map
      @repository_packages_map = {}
      @original_cruise_config.getPackageRepositories().each do |repo|
        metadata = RepositoryMetadataStore.getInstance().getMetadata(repo.getPluginConfiguration().getId())
        hash = {:name => repo.getName(), :packages => [], :is_plugin_missing => metadata.nil?, :plugin_id => repo.getPluginConfiguration().getId()}
        repo.getPackages().each do |package|
          hash[:packages] << {:name => package.getName(), :id => package.getId()}
        end
        @repository_packages_map[repo.getId()] = hash
      end
      @repository_packages_map
    end

    def package_material_plugins
      plugins = RepositoryMetadataStore.getInstance().getPlugins()
      [["[Select]", ""]] + plugins.to_a
    end
  end
end