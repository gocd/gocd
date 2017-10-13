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

require 'spec_helper'

describe ApiV4::Shared::ConfigOrigin::ConfigRepoOriginRepresenter do
  it 'should render remote config origin' do
    git_material = GitMaterialConfig.new('https://github.com/config-repos/repo', 'master')
    config_repo = RepoConfigOrigin.new(ConfigRepoConfig.new(git_material, 'json-plugin'), 'revision1')

    actual_json = ApiV4::Shared::ConfigOrigin::ConfigRepoOriginRepresenter.new(config_repo).to_hash(url_builder: UrlBuilder.new)
    material_json = ApiV4::Admin::Pipelines::Materials::GitMaterialRepresenter.new(git_material).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq(expected_json)
  end

  def expected_json
    {
      type: 'config repo',
      repo: {
        type: 'git',
        attributes: {
          url: 'https://github.com/config-repos/repo',
          destination: nil,
          filter: nil,
          invert_filter: false,
          name: nil,
          auto_update: true,
          branch: 'master',
          submodule_folder: nil,
          shallow_clone: false
        },
        revision: 'revision1'
      }
    }
  end
end
