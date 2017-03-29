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

describe ApiV4::Shared::ConfigOrigin::ConfigXmlOriginRepresenter do
  it 'should render local config origin' do
    presenter = ApiV4::Shared::ConfigOrigin::ConfigXmlOriginRepresenter.new(get_config_xml_origin)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(expected_json)
  end

  def get_config_xml_origin
    FileConfigOrigin.new
  end

  def expected_json
    {
      type: 'local',
      file: 'cruise-config.xml'
    }
  end
end
