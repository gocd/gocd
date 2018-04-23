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

describe ApiV4::AgentsController do
  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to index action of the agents controller' do
        expect(:get => 'api/agents').to route_to(action: 'index', controller: 'api_v4/agents')
      end
    end
    describe "without_header" do
      it 'should not route to index action of the agents controller without header' do
        expect(:get => 'api/agents').to_not route_to(action: 'index', controller: 'api_v4/agents')
        expect(:get => 'api/agents').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of the agents controller for uuid with hyphen' do
        expect(:get => 'api/agents/uuid-123').to route_to(action: 'show', controller: 'api_v4/agents', uuid: 'uuid-123')
      end

      it 'should route to show action of the agents controller for uuid with underscore' do
        expect(:get => 'api/agents/uuid_123').to route_to(action: 'show', controller: 'api_v4/agents', uuid: 'uuid_123')
      end

      it 'should route to show action of the agents controller for uuid with dots' do
        expect(:get => 'api/agents/uuid.123').to route_to(action: 'show', controller: 'api_v4/agents', uuid: 'uuid.123')
      end
    end
    describe "without_header" do
      it 'should not route to show action of the agents controller without header' do
        expect(:get => 'api/agents/uuid').to_not route_to(action: 'show', controller: 'api_v4/agents')
        expect(:get => 'api/agents/uuid').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents/uuid')
      end
    end
  end

  describe "delete" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to destoy action of the agents controller for uuid with hyphen' do
        expect(:delete => 'api/agents/uuid-123').to route_to(action: 'destroy', controller: 'api_v4/agents', uuid: 'uuid-123')
      end

      it 'should route to destroy action of the agents controller for uuid with underscore' do
        expect(:delete => 'api/agents/uuid_123').to route_to(action: 'destroy', controller: 'api_v4/agents', uuid: 'uuid_123')
      end

      it 'should route to destroy action of the agents controller for uuid with dots' do
        expect(:delete => 'api/agents/uuid.123').to route_to(action: 'destroy', controller: 'api_v4/agents', uuid: 'uuid.123')
      end
    end
    describe "without_header" do
      it 'should not route to destroy action of the agents controller without header' do
        expect(:delete => 'api/agents/uuid').to_not route_to(action: 'destroy', controller: 'api_v4/agents')
        expect(:delete => 'api/agents/uuid').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents/uuid')
      end
    end
  end

  describe "update" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to update action of the agents controller for uuid with hyphen' do
        expect(:patch => 'api/agents/uuid-123').to route_to(action: 'update', controller: 'api_v4/agents', uuid: 'uuid-123')
      end

      it 'should route to update action of the agents controller for uuid with underscore' do
        expect(:patch => 'api/agents/uuid_123').to route_to(action: 'update', controller: 'api_v4/agents', uuid: 'uuid_123')
      end

      it 'should route to update action of the agents controller for uuid with dots' do
        expect(:patch => 'api/agents/uuid.123').to route_to(action: 'update', controller: 'api_v4/agents', uuid: 'uuid.123')
      end
    end
    describe "without_header" do
      it 'should not route to update action of the agents controller without header' do
        expect(:patch => 'api/agents/uuid').to_not route_to(action: 'update', controller: 'api_v4/agents')
        expect(:patch => 'api/agents/uuid').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents/uuid')
      end
    end
  end

  describe "bulk_delete" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to bulk_destroy action of the agents controller' do
        expect(:delete => 'api/agents').to route_to(action: 'bulk_destroy', controller: 'api_v4/agents')
      end
    end
    describe "without_header" do
      it 'should not route to bulk_destroy action of the agents controller without header' do
        expect(:delete => 'api/agents').to_not route_to(action: 'bulk_destroy', controller: 'api_v4/agents')
        expect(:delete => 'api/agents').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents')
      end
    end
  end

  describe "bulk_update" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      
      it 'should route to bulk_update action of the agents controller' do
        expect(:patch => 'api/agents').to route_to(action: 'bulk_update', controller: 'api_v4/agents')
      end
    end
    describe "without_header" do
      it 'should not route to bulk_update action of the agents controller without header' do
        expect(:patch => 'api/agents').to_not route_to(action: 'bulk_update', controller: 'api_v4/agents')
        expect(:patch => 'api/agents').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents')
      end
    end
  end

end
