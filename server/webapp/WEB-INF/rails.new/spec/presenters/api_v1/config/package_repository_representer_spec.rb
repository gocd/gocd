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

describe ApiV1::Config::PackageRepositoryRepresenter do

  describe :serialize do
    it 'renders a package repository with hal representation' do
      presenter = ApiV1::Config::PackageRepositoryRepresenter.new(get_package_repository)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :find, :doc)
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/repositories/:repo_id')
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/repositories/npm.org')
      expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#package-repository')

      actual_json.delete(:_links)
      expect(actual_json).to eq(get_package_repository_json)
    end
  end

  describe :deserialize do
    it 'renders a package repository with hal representation' do
      package_repo = PackageRepository.new
      ApiV1::Config::PackageRepositoryRepresenter.new(package_repo).from_hash(get_package_repository_json)
      expected_repo = get_package_repository
      expect(expected_repo.getId).to eq(package_repo.getId)
      expect(expected_repo.getName).to eq(package_repo.getName)
      expect(expected_repo.getConfiguration).to eq(package_repo.getConfiguration)
      expect(expected_repo.getPluginConfiguration).to eq(package_repo.getPluginConfiguration)

      expect(package_repo.getPackages.isEmpty).to eq(true)
    end
  end

  def get_package_repository
    pkg = PackageDefinition.new
    pkg.setId('prettyjson')
    pkg.setName('prettyjson')
    packages = Packages.new(pkg)
    config_property = ConfigurationProperty.new(ConfigurationKey.new('foo'), ConfigurationValue.new('bar'))
    config = Configuration.new(config_property)
    repo = PackageRepository.new('npm.org', 'NodeJS', PluginConfiguration.new('npm', '1'), config)
    repo.setPackages(packages)
    repo
  end

  def get_package_repository_json
    {
      name: 'NodeJS',
      repo_id: 'npm.org',
      plugin_metadata: {
        id: "npm",
        version: "1"
      },
      configuration: [
        {
          key: 'foo',
          value: 'bar'
        }
      ],
      _embedded: {
        packages: [
          {
            _links: {
              # TODO: Ganesh Patil did this
              # self: {
              #   href: "http://test.host/api/admin/repositories/prettyjson"
              # },
              doc: {
                href: "https://api.go.cd/#packages"
              }
              # find: {
              #   href: "http://test.host/api/admin/repositories/prettyjson"
              # }
            },
            name: "prettyjson",
            id: "prettyjson"
          }
        ]
      }
    }
  end
end
