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

describe ApiV1::Admin::MergedEnvironments::MergedEnvironmentsConfigRepresenter do

  describe :serialize do
    it 'renders a list of environments' do
      environment_config_one = EnvironmentConfigMother.environment("dev")
      environment_config_two = EnvironmentConfigMother.environment("testing")

      presenter = ApiV1::Admin::MergedEnvironments::MergedEnvironmentsConfigRepresenter.new([environment_config_one, environment_config_two])
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to have_links(:self, :doc)

      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/environments/merged')
      expect(actual_json).to have_link(:doc).with_url('https://api.gocd.io/#merged-environment-config')

      actual_json.fetch(:_embedded).should == {
        :environments => [ApiV1::Admin::MergedEnvironments::MergedEnvironmentConfigRepresenter.new(environment_config_one).to_hash(url_builder: UrlBuilder.new),
                          ApiV1::Admin::MergedEnvironments::MergedEnvironmentConfigRepresenter.new(environment_config_two).to_hash(url_builder: UrlBuilder.new)]
      }

    end
  end
end