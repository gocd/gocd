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

describe ApiV1::VersionRepresenter do

  it 'renders the current gocd server version details' do
    actual_json = ApiV1::VersionRepresenter.new(OpenStruct.new({version: '16.6.0',
                                                                build_number: '235',
                                                                git_sha: '69ef4921709a84831913d9fa7e750fbf840f213c',
                                                                full_version: '16.6.0 (235-69ef4921709a84831913d9fa7e750fbf840f213c)',
                                                                commit_url: 'https://github.com/gocd/gocd/commit/69ef4921709a84831913d9fa7e750fbf840f213c'
                                                               })).to_hash(url_builder: UrlBuilder.new)
    expect(actual_json[:version]).to eq('16.6.0')
    expect(actual_json[:build_number]).to eq('235')
    expect(actual_json[:git_sha]).to eq('69ef4921709a84831913d9fa7e750fbf840f213c')
    expect(actual_json[:full_version]).to eq('16.6.0 (235-69ef4921709a84831913d9fa7e750fbf840f213c)')
    expect(actual_json[:commit_url]).to eq('https://github.com/gocd/gocd/commit/69ef4921709a84831913d9fa7e750fbf840f213c')

    expect(actual_json).to have_links(:self, :doc)

    expect(actual_json).to have_link(:self).with_url('http://test.host/api/version')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#version')
  end

end
