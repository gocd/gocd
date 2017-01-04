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

describe ApiV1::Config::Materials::CheckConnectionRepresenter do
  describe 'deserialize' do
    it 'renders a check repository with hal representation' do
      package_repo = PackageRepository.new
      ApiV1::Config::Materials::CheckConnectionRepresenter.new(package_repo).from_hash(get_repository_json)
      expected_repo = get_package_repository
      expect(expected_repo.getConfiguration).to eq(package_repo.getConfiguration)
      expect(expected_repo.getPluginConfiguration).to eq(package_repo.getPluginConfiguration)
    end

    def get_repository_json
      {
        plugin: "yum",
        configuration: [
          {
            key: "REPO_URL",
            value: "https://download.go.cd/experimental/"
          }
        ]
      }
    end

    def get_package_repository
      configuration_property = ConfigurationProperty.new(ConfigurationKey.new("REPO_URL"), ConfigurationValue.new("https://download.go.cd/experimental/"))
      configuration = Configuration.new(configuration_property)
      expected_package_repository = PackageRepository.new
      expected_package_repository.setPluginConfiguration(PluginConfiguration.new("yum", nil))
      expected_package_repository.setConfiguration(configuration)

      expected_package_repository
    end
  end
end
