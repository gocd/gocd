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

describe ApiV1::Admin::Internal::EnvironmentsController do

  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to index action of the internal environments controller' do
        expect(:get => 'api/admin/internal/environments').to route_to(action: 'index', controller: 'api_v1/admin/internal/environments')
      end
    end
    describe "without_header" do

      it 'should not route to index action of internal environments controller without header' do
        expect(:get => 'api/admin/internal/environments').to_not route_to(action: 'index', controller: 'api_v1/admin/internal/environments')
        expect(:get => 'api/admin/internal/environments').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/internal/environments')
      end
    end
  end
end
