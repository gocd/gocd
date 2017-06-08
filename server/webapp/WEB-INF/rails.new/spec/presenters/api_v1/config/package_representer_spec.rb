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

describe ApiV1::Config::PackageRepresenter do

  describe :serialize do
    it 'renders a package with hal representation' do
      presenter = ApiV1::Config::PackageRepresenter.new({package: get_package})
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :find, :doc)
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/packages/:package_id')
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/packages/uuid')
      expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#packages')

      actual_json.delete(:_links)
      expect(actual_json).to eq(get_expected_json)
    end
  end

  describe :deserialize do
    it 'should convert from minimal json to package' do
      package = PackageDefinition.new

      ApiV1::Config::PackageRepresenter.new({package: package, repository: nil}).from_hash(package_hash)
      pkg = get_package
      pkg.setRepository(nil)
      expect(package).to eq(pkg)
    end
  end

  def get_package
    config = Configuration.new(ConfigurationProperty.new(ConfigurationKey.new("PACKAGE_ID"), ConfigurationValue.new("id")))
    pkg = PackageDefinition.new("uuid", "prettyjson", config)
    repo = PackageRepository.new
    repo.setName("npm")
    repo.setId("repo-id")
    pkg.setRepository(repo)
    return pkg
  end

  def get_expected_json
    {
      configuration: [
        {
          key: "PACKAGE_ID",
          value: "id"
        }
      ],
      id: "uuid",
      name: "prettyjson",
      auto_update: true,
      package_repo: {
        _links: {
          self: {
            href: "http://test.host/api/admin/repositories/repo-id"
          },
          doc: {
            href: "https://api.gocd.org/#package-repositories"
          },
          find: {
            href: "http://test.host/api/admin/repositories/:repo_id"
          }
        },
        name: "npm",
        id: "repo-id"
      }
    }
  end

    def package_hash
      {
        configuration: [
          {
            key: "PACKAGE_ID",
            value: "id"
          }
        ],
        id: "uuid",
        name: "prettyjson"
      }
    end

end
