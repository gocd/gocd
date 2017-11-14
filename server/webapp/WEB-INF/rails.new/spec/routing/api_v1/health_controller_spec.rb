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

require 'rails_helper'

describe ApiV1::HealthController do
  include ApiHeaderSetupForRouting

  describe 'show' do
    before(:each) do
      setup_header
    end

    it 'should route to show action of the health controller' do
      expect(:get => 'api/v1/health').to route_to(action: 'show', controller: 'api_v1/health')
    end

    it 'should route to errors without custom header' do
      expect(:get => 'api/v12/health').to route_to(controller: 'api_v1/errors', action: 'not_found', url: 'v12/health')
    end
  end
end