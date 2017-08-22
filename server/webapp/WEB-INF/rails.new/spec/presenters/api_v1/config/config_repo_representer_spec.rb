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

describe ApiV1::Config::ConfigRepoRepresenter do
  describe :serialize do
    it 'should render config repo with hal representation' do
      config_repo_config = ConfigRepoConfig.new(GitMaterialConfig.new('git://foo', 'master'), 'config-repo-json-plugin', 'repo-1')
      actual_json = ApiV1::Config::ConfigRepoRepresenter.new(config_repo_config).to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/config_repos/repo-1')
      actual_json.delete(:_links)
      expect(actual_json).to eq(get_config_repo_json)
    end

    protected

    def get_config_repo_json
      {
        :id => 'repo-1',
        :plugin_id => 'config-repo-json-plugin',
        :material => {
          :type => 'git',
          :attributes => {
            :url => 'git://foo',
            :destination => nil,
            :filter => nil,
            :invert_filter => false,
            :name => nil,
            :auto_update => true,
            :branch => 'master',
            :submodule_folder => nil,
            :shallow_clone => false
          }
        },
        :configuration => []
      }
    end
  end
end