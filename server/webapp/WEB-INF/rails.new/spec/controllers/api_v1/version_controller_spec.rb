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

require 'rails_helper'

describe ApiV1::VersionController do

  include ApiV1::ApiVersionHelper

  describe "show" do
    it 'HEAD should should render the current gocd server version for admins' do
      actual_json = {go_version: '16.6.0', go_build_number: '235', git_sha: '69ef4921709a84831913d9fa7e750fbf840f213c'}
      allow(ApiV1::VersionRepresenter).to receive(:version).and_return(OpenStruct.new(actual_json))

      head_with_api_header :show
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(OpenStruct.new(actual_json), ApiV1::VersionRepresenter))
    end

    it 'GET should should render the current gocd server version for admins' do
      actual_json = {go_version: '16.6.0', go_build_number: '235', git_sha: '69ef4921709a84831913d9fa7e750fbf840f213c'}
      allow(ApiV1::VersionRepresenter).to receive(:version).and_return(OpenStruct.new(actual_json))

      get_with_api_header :show
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(OpenStruct.new(actual_json), ApiV1::VersionRepresenter))
    end
  end
end
