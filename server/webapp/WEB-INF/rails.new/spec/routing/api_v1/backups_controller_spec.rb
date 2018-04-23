##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

describe ApiV1::BackupsController do
  include ApiHeaderSetupForRouting

  describe "create" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to create action of the backups controller with custom header' do
        expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
        expect(:post => 'api/backups').to route_to(action: 'create', controller: 'api_v1/backups')
      end

      it 'should route to errors without custom header' do
        expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(false)
        expect(:post => 'api/backups').to route_to(controller: 'api_v1/errors', action: 'not_found', url: 'backups')
      end
    end
    describe "without_header" do
      it 'should not route to create action of backups controller without header' do
        expect(:post => 'api/backups').to_not route_to(action: 'backups', controller: 'api_v1/backups')
        expect(:post => 'api/backups').to route_to(controller: 'application', action: 'unresolved', url: 'api/backups')
      end
    end
  end
end
