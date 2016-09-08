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

describe ApiV4::AgentsController do
  include AgentInstanceFactory

  before do
    controller.stub(:agent_service).and_return(@agent_service = double('agent-service'))
    controller.stub(:security_service).and_return(@security_service = double('security-service'))
    @current_user = Username.new(CaseInsensitiveString.new('user'))
    controller.stub(:job_instance_service).and_return(@job_instance_service = double('job instance service'))
  end

  describe :index do
    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :index).with(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should allow normal users, with security enabled' do
        login_as_user
        expect(controller).to allow_action(:get, :index)
      end
    end

    describe 'logged in' do
      before(:each) do
        login_as_user
      end

      it 'should get agents json' do
        two_agents = {idle_agent => %w(uat perf), missing_agent => %w()}

        @agent_service.should_receive(:agentEnvironmentMap).and_return(two_agents)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response_with_args(two_agents, ApiV4::AgentsRepresenter, @security_service, @current_user))
      end

      it 'should get empty json when there are no agents' do
        zero_agents = {}

        @agent_service.should_receive(:agentEnvironmentMap).and_return(zero_agents)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response_with_args(zero_agents, ApiV4::AgentsRepresenter, @security_service, @current_user))
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v4+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to index action of the agents controller' do
          expect(:get => 'api/agents').to route_to(action: 'index', controller: 'api_v4/agents')
        end
      end
      describe :without_header do
        it 'should not route to index action of the agents controller without header' do
          expect(:get => 'api/agents').to_not route_to(action: 'index', controller: 'api_v4/agents')
          expect(:get => 'api/agents').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents')
        end
      end
    end
  end

  describe :show do
    describe :security do
      before(:each) do
        @agent = AgentInstanceMother.idle()
        @agent_service.stub(:findAgent).and_return(@agent)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, uuid: @agent.getUuid())
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, uuid: @agent.getUuid()).with(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should allow normal users, with security enabled' do
        login_as_user
        expect(controller).to allow_action(:get, :show, uuid: @agent.getUuid())
      end
    end

    describe 'logged in' do
      before(:each) do
        login_as_user
      end

      it 'should get agents json' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).with(agent.getUuid()).and_return(agent)

        get_with_api_header :show, uuid: agent.getUuid()
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return 404 when agent is not found' do
        null_agent = NullAgentInstance.new('some-uuid')
        @agent_service.should_receive(:findAgent).with(null_agent.getUuid()).and_return(null_agent)

        get_with_api_header :show, uuid: null_agent.getUuid()
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v4+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to show action of the agents controller for uuid with hyphen' do
          expect(:get => 'api/agents/uuid-123').to route_to(action: 'show', controller: 'api_v4/agents', uuid: 'uuid-123')
        end

        it 'should route to show action of the agents controller for uuid with underscore' do
          expect(:get => 'api/agents/uuid_123').to route_to(action: 'show', controller: 'api_v4/agents', uuid: 'uuid_123')
        end

        it 'should not route to show action of the agents controller for uuid with dots' do
          expect(:get => 'api/agents/uuid.123').to_not route_to(action: 'show', controller: 'api_v4/agents', uuid: 'uuid.123')
        end
      end
      describe :without_header do
        it 'should not route to show action of the agents controller without header' do
          expect(:get => 'api/agents/uuid').to_not route_to(action: 'show', controller: 'api_v4/agents')
          expect(:get => 'api/agents/uuid').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents/uuid')
        end
      end
    end
  end

  describe :delete do
    describe :security do
      before(:each) do
        @agent = AgentInstanceMother.idle()
        @agent_service.stub(:findAgent).and_return(@agent)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, uuid: @agent.getUuid())
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, uuid: @agent.getUuid()).with(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should not allow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, uuid: @agent.getUuid()).with(401, 'You are not authorized to perform this action.')
      end
    end

    describe 'as admin user' do
      before(:each) do
        login_as_admin
      end

      it 'should render result in case of error' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).with(agent.getUuid()).and_return(agent)

        @agent_service.should_receive(:deleteAgents).with(@user, anything(), [agent.getUuid()]) do |user, result, uuid|
          result.notAcceptable('Not Acceptable', HealthStateType.general(HealthStateScope::GLOBAL))
        end

        delete_with_api_header :destroy, :uuid => agent.getUuid()
        expect(response).to have_api_message_response(406, 'Not Acceptable')
      end

      it 'should return 200 when delete completes' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).with(agent.getUuid()).and_return(agent)

        @agent_service.should_receive(:deleteAgents).with(@user, anything(), [agent.getUuid()]) do |user, result, uuid|
          result.ok('Deleted 1 agent(s).')
        end

        delete_with_api_header :destroy, :uuid => agent.getUuid()
        expect(response).to be_ok
        expect(response).to have_api_message_response(200, 'Deleted 1 agent(s).')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v4+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to destoy action of the agents controller for uuid with hyphen' do
          expect(:delete => 'api/agents/uuid-123').to route_to(action: 'destroy', controller: 'api_v4/agents', uuid: 'uuid-123')
        end

        it 'should route to destroy action of the agents controller for uuid with underscore' do
          expect(:delete => 'api/agents/uuid_123').to route_to(action: 'destroy', controller: 'api_v4/agents', uuid: 'uuid_123')
        end

        it 'should not route to destroy action of the agents controller for uuid with dots' do
          expect(:delete => 'api/agents/uuid.123').to_not route_to(action: 'destroy', controller: 'api_v4/agents', uuid: 'uuid.123')
        end
      end
      describe :without_header do
        it 'should not route to destroy action of the agents controller without header' do
          expect(:delete => 'api/agents/uuid').to_not route_to(action: 'destroy', controller: 'api_v4/agents')
          expect(:delete => 'api/agents/uuid').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents/uuid')
        end
      end
    end
  end

  describe :update do
    describe :security do
      before(:each) do
        @agent = AgentInstanceMother.idle()
        @agent_service.stub(:findAgent).and_return(@agent)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:patch, :update, uuid: @agent.getUuid(), hostname: 'some-hostname')
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:patch, :update, uuid: @agent.getUuid(), hostname: 'some-hostname').with(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should not allow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:patch, :update, uuid: @agent.getUuid(), hostname: 'some-hostname').with(401, 'You are not authorized to perform this action.')
      end
    end

    describe 'as admin user' do
      before(:each) do
        login_as_admin
      end

      it 'should return agent json when agent name update is successful' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', nil, nil, TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname'
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent resources update is successful by specifing a comma separated string' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", nil, TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', resources: "java,linux,firefox"
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent environments update is successful by specifing a comma separated string' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', nil, 'pre-prod,performance', TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', environments: "pre-prod,performance"
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent is enabled' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", nil, TriState.TRUE) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', resources: "java,linux,firefox", agent_config_state: 'enabled'
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent is disabled' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", nil, TriState.FALSE) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', resources: "java,linux,firefox", agent_config_state: "diSAbled"
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent resources update is successful by specifying a resource array' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", nil, TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', resources: ['java', 'linux', 'firefox']
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent environments update is successful by specifying an environment array' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', nil, 'pre-prod,staging', TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', environments: ['pre-prod', 'staging']
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return 404 when agent is not found' do
        null_agent = NullAgentInstance.new('some-uuid')
        @agent_service.should_receive(:findAgent).with(null_agent.getUuid()).and_return(null_agent)

        patch_with_api_header :update, uuid: null_agent.getUuid()
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should raise error when submitting a junk (non-blank) value for enabled boolean' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).with(agent.getUuid()).and_return(agent)

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', agent_config_state: 'foo'
        expect(response).to have_api_message_response(400, 'Your request could not be processed. The value of `agent_config_state` can be one of `Enabled`, `Disabled` or null.')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v4+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to update action of the agents controller for uuid with hyphen' do
          expect(:patch => 'api/agents/uuid-123').to route_to(action: 'update', controller: 'api_v4/agents', uuid: 'uuid-123')
        end

        it 'should route to update action of the agents controller for uuid with underscore' do
          expect(:patch => 'api/agents/uuid_123').to route_to(action: 'update', controller: 'api_v4/agents', uuid: 'uuid_123')
        end

        it 'should not route to update action of the agents controller for uuid with dots' do
          expect(:patch => 'api/agents/uuid.123').to_not route_to(action: 'update', controller: 'api_v4/agents', uuid: 'uuid.123')
        end
      end
      describe :without_header do
        it 'should not route to update action of the agents controller without header' do
          expect(:patch => 'api/agents/uuid').to_not route_to(action: 'update', controller: 'api_v4/agents')
          expect(:patch => 'api/agents/uuid').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents/uuid')
        end
      end
    end
  end

  describe :bulk_delete do
    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :bulk_destroy)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :bulk_destroy)
      end

      it 'should not allow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :bulk_destroy)
      end
    end

    describe 'as user' do
      it 'should not allow normal users to bulk_destroy the agents' do
        login_as_user

        delete_with_api_header :bulk_destroy, :uuids => ['foo']
        expect(response).to have_api_message_response(401, 'You are not authorized to perform this action.')
      end
    end

    describe 'as admin user' do
      before(:each) do
        login_as_admin
      end

      it 'should allow admin users to delete a group of agents' do
        agent1 = AgentInstanceMother.idle()
        agent2= AgentInstanceMother.idle()

        @agent_service.should_receive(:deleteAgents).with(@user, anything(), [agent1.getUuid(), agent2.getUuid()]) do |user, result, uuids|
          result.ok('Deleted 2 agent(s).')
        end

        delete_with_api_header :bulk_destroy, :uuids => [agent1.getUuid(), agent2.getUuid()]
        expect(response).to be_ok
        expect(response).to have_api_message_response(200, 'Deleted 2 agent(s).')
      end

      it 'should render result in case of error' do
        agent1 = AgentInstanceMother.idle()
        agent2 = AgentInstanceMother.idle()

        @agent_service.should_receive(:deleteAgents).with(@user, anything(), [agent1.getUuid(), agent2.getUuid()]) do |user, result, uuids|
          result.notAcceptable('Not Acceptable', HealthStateType.general(HealthStateScope::GLOBAL))
        end

        delete_with_api_header :bulk_destroy, :uuids => [agent1.getUuid(), agent2.getUuid()]
        expect(response).to have_api_message_response(406, 'Not Acceptable')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v4+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to bulk_destroy action of the agents controller' do
          expect(:delete => 'api/agents').to route_to(action: 'bulk_destroy', controller: 'api_v4/agents')
        end
      end
      describe :without_header do
        it 'should not route to bulk_destroy action of the agents controller without header' do
          expect(:delete => 'api/agents').to_not route_to(action: 'bulk_destroy', controller: 'api_v4/agents')
          expect(:delete => 'api/agents').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents')
        end
      end
    end

  end

  describe :bulk_update do
    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:patch, :bulk_update)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:patch, :bulk_update)
      end

      it 'should not allow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:patch, :bulk_update)
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:patch, :bulk_update)
      end
    end

    describe 'as user ' do
      it 'should not allow normal users to bulk_destroy the agents' do
        login_as_user

        patch_with_api_header :bulk_update, :uuids => ['foo']
        expect(response).to have_api_message_response(401, 'You are not authorized to perform this action.')
      end
    end

    describe 'as admin user' do
      before(:each) do
        login_as_admin
      end

      it 'should allow admin users to update a group of agents' do
        uuids = %w(agent-1 agent-2)
        @agent_service.should_receive(:bulkUpdateAgentAttributes).with(@user, anything(), uuids, anything(), anything(), anything(), anything(), anything()) do |user, result, uuids, r_add, r_remove, e_add, e_remove, state|
          result.setMessage(LocalizedMessage.string("BULK_AGENT_UPDATE_SUCESSFUL", uuids.join(', ')));
        end

        patch_with_api_header :bulk_update, :uuids => uuids
        expect(response).to be_ok
        expect(response).to have_api_message_response(200, 'Updated agent(s) with uuid(s): [agent-1, agent-2].')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v4+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to bulk_update action of the agents controller' do
          expect(:patch => 'api/agents').to route_to(action: 'bulk_update', controller: 'api_v4/agents')
        end
      end
      describe :without_header do
        it 'should not route to bulk_update action of the agents controller without header' do
          expect(:patch => 'api/agents').to_not route_to(action: 'bulk_update', controller: 'api_v4/agents')
          expect(:patch => 'api/agents').to route_to(controller: 'application', action: 'unresolved', url: 'api/agents')
        end
      end
    end
  end

end
