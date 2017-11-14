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

describe ApiV1::VersionInfosController do
  include ApiHeaderSetupForRouting

  describe "with_header" do
    before(:each) do
      setup_header
    end

    it 'should route to stale action of version_infos controller' do
      expect(:get => 'api/version_infos/stale').to route_to(action: 'stale', controller: 'api_v1/version_infos')
    end

    it 'should route to update_server action of the version_infos controller' do
      expect(:patch => 'api/version_infos/go_server').to route_to(action: 'update_server', controller: 'api_v1/version_infos')
    end
  end

  describe "without_header" do
    it 'should not route to stale action of version_infos controller without header' do
      expect(:get => 'api/version_infos/stale').to_not route_to(action: 'stale', controller: 'api_v1/version_infos')
      expect(:get => 'api/version_infos/stale').to route_to(controller: 'application', action: 'unresolved', url: 'api/version_infos/stale')
    end

    it 'should not route to update_server action of version_infos controller without header' do
      expect(:patch => 'api/version_infos/go_server').to_not route_to(action: 'update_server', controller: 'api_v1/version_infos')
      expect(:patch => 'api/version_infos/go_server').to route_to(controller: 'application', action: 'unresolved', url: 'api/version_infos/go_server')
    end
  end
end
