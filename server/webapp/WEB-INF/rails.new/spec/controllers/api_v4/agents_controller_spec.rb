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
  include AgentInstanceFactory

  include ApiV4::ApiVersionHelper

  before do
    allow(controller).to receive(:agent_service).and_return(@agent_service = double('agent-service'))
    allow(controller).to receive(:security_service).and_return(@security_service = double('security-service'))
    @current_user = Username.new(CaseInsensitiveString.new('user'))
    allow(controller).to receive(:job_instance_service).and_return(@job_instance_service = double('job instance service'))
  end

  describe "index" do
    describe "security" do
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

        expect(@agent_service).to receive(:agentEnvironmentMap).and_return(two_agents)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response_with_args(two_agents, ApiV4::AgentsRepresenter, @security_service, @current_user))
      end

      it 'should get empty json when there are no agents' do
        zero_agents = {}

        expect(@agent_service).to receive(:agentEnvironmentMap).and_return(zero_agents)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response_with_args(zero_agents, ApiV4::AgentsRepresenter, @security_service, @current_user))
      end
    end
  end

  describe "show" do
    describe "security" do
      before(:each) do
        @agent = AgentInstanceMother.idle()
        allow(@agent_service).to receive(:findAgent).and_return(@agent)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, params: { uuid: @agent.getUuid() })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, params: { uuid: @agent.getUuid() }).with(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should allow normal users, with security enabled' do
        login_as_user
        expect(controller).to allow_action(:get, :show, params: { uuid: @agent.getUuid() })
      end
    end

    describe 'logged in' do
      before(:each) do
        login_as_user
      end

      it 'should get agents json' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).with(agent.getUuid()).and_return(agent)

        get_with_api_header :show, params: { uuid: agent.getUuid() }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return 404 when agent is not found' do
        null_agent = NullAgentInstance.new('some-uuid')
        expect(@agent_service).to receive(:findAgent).with(null_agent.getUuid()).and_return(null_agent)

        get_with_api_header :show, params: { uuid: null_agent.getUuid() }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end
  end

  describe "delete" do
    describe "security" do
      before(:each) do
        @agent = AgentInstanceMother.idle()
        allow(@agent_service).to receive(:findAgent).and_return(@agent)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, params: { uuid: @agent.getUuid() })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, params: { uuid: @agent.getUuid() }).with(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should not allow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, params: { uuid: @agent.getUuid() }).with(401, 'You are not authorized to perform this action.')
      end
    end

    describe 'as admin user' do
      before(:each) do
        login_as_admin
      end

      it 'should render result in case of error' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).with(agent.getUuid()).and_return(agent)

        expect(@agent_service).to receive(:deleteAgents).with(@user, anything(), [agent.getUuid()]) do |user, result, uuid|
          result.notAcceptable('Not Acceptable', HealthStateType.general(HealthStateScope::GLOBAL))
        end

        delete_with_api_header :destroy, params: { :uuid => agent.getUuid() }
        expect(response).to have_api_message_response(406, 'Not Acceptable')
      end

      it 'should return 200 when delete completes' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).with(agent.getUuid()).and_return(agent)

        expect(@agent_service).to receive(:deleteAgents).with(@user, anything(), [agent.getUuid()]) do |user, result, uuid|
          result.ok('Deleted 1 agent(s).')
        end

        delete_with_api_header :destroy, params: { :uuid => agent.getUuid() }
        expect(response).to be_ok
        expect(response).to have_api_message_response(200, 'Deleted 1 agent(s).')
      end
    end
  end

  describe "update" do
    describe "security" do
      before(:each) do
        @agent = AgentInstanceMother.idle()
        allow(@agent_service).to receive(:findAgent).and_return(@agent)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:patch, :update, params: { uuid: @agent.getUuid(), hostname: 'some-hostname' })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:patch, :update, params: { uuid: @agent.getUuid(), hostname: 'some-hostname' }).with(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should not allow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:patch, :update, params: { uuid: @agent.getUuid(), hostname: 'some-hostname' }).with(401, 'You are not authorized to perform this action.')
      end
    end

    describe 'as admin user' do
      before(:each) do
        login_as_admin
      end

      it 'should return agent json when agent name update is successful' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        expect(@agent_service).to receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', nil, nil, TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, params: { uuid: agent.getUuid(), hostname: 'some-hostname' }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent resources update is successful by specifing a comma separated string' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        expect(@agent_service).to receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", nil, TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, params: { uuid: agent.getUuid(), hostname: 'some-hostname', resources: "java,linux,firefox" }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json with no resources when agent resources update is successful by specifying an empty array' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        expect(@agent_service).to receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "", nil, TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, params: { uuid: agent.getUuid(), hostname: 'some-hostname', resources: nil }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), resources: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent environments update is successful by specifing a comma separated string' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        expect(@agent_service).to receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', nil, 'pre-prod,performance', TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, params: { uuid: agent.getUuid(), hostname: 'some-hostname', environments: "pre-prod,performance" }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json with no environments when agent environments update is successful by specifying an empty array' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        expect(@agent_service).to receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', nil, "", TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, params: { uuid: agent.getUuid(), hostname: 'some-hostname', environments: nil }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), resources: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent is enabled' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        expect(@agent_service).to receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", nil, TriState.TRUE) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, params: { uuid: agent.getUuid(), hostname: 'some-hostname', resources: "java,linux,firefox", agent_config_state: 'enabled' }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent is disabled' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        expect(@agent_service).to receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", nil, TriState.FALSE) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, params: { uuid: agent.getUuid(), hostname: 'some-hostname', resources: "java,linux,firefox", agent_config_state: "diSAbled" }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent resources update is successful by specifying a resource array' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        expect(@agent_service).to receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", nil, TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, params: { uuid: agent.getUuid(), hostname: 'some-hostname', resources: ['java', 'linux', 'firefox'] }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return agent json when agent environments update is successful by specifying an environment array' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        expect(@agent_service).to receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', nil, 'pre-prod,staging', TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, params: { uuid: agent.getUuid(), hostname: 'some-hostname', environments: ['pre-prod', 'staging'] }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({agent: agent, environments: %w(), security_service: @security_service, current_user: @current_user}, ApiV4::AgentRepresenter))
      end

      it 'should return 404 when agent is not found' do
        null_agent = NullAgentInstance.new('some-uuid')
        expect(@agent_service).to receive(:findAgent).with(null_agent.getUuid()).and_return(null_agent)

        patch_with_api_header :update, params: { uuid: null_agent.getUuid() }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should raise error when submitting a junk (non-blank) value for enabled boolean' do
        agent = AgentInstanceMother.idle()
        expect(@agent_service).to receive(:findAgent).with(agent.getUuid()).and_return(agent)

        patch_with_api_header :update, params: {uuid: agent.getUuid(), hostname: 'some-hostname', agent_config_state: 'foo'}
        expect(response).to have_api_message_response(400, 'Your request could not be processed. The value of `agent_config_state` can be one of `Enabled`, `Disabled` or null.')
      end
    end
  end

  describe "bulk_delete" do
    describe "security" do
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

        delete_with_api_header :bulk_destroy, params: { :uuids => ['foo'] }
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

        expect(@agent_service).to receive(:deleteAgents).with(@user, anything(), [agent1.getUuid(), agent2.getUuid()]) do |user, result, uuids|
          result.ok('Deleted 2 agent(s).')
        end

        delete_with_api_header :bulk_destroy, params: { :uuids => [agent1.getUuid(), agent2.getUuid()] }
        expect(response).to be_ok
        expect(response).to have_api_message_response(200, 'Deleted 2 agent(s).')
      end

      it 'should render result in case of error' do
        agent1 = AgentInstanceMother.idle()
        agent2 = AgentInstanceMother.idle()

        expect(@agent_service).to receive(:deleteAgents).with(@user, anything(), [agent1.getUuid(), agent2.getUuid()]) do |user, result, uuids|
          result.notAcceptable('Not Acceptable', HealthStateType.general(HealthStateScope::GLOBAL))
        end

        delete_with_api_header :bulk_destroy, params: { :uuids => [agent1.getUuid(), agent2.getUuid()] }
        expect(response).to have_api_message_response(406, 'Not Acceptable')
      end
    end
  end
  
  describe "bulk_update" do
    describe "security" do
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

        patch_with_api_header :bulk_update, params: { :uuids => ['foo'] }
        expect(response).to have_api_message_response(401, 'You are not authorized to perform this action.')
      end
    end

    describe 'as admin user' do
      before(:each) do
        login_as_admin
      end

      it 'should allow admin users to update a group of agents' do
        uuids = %w(agent-1 agent-2)
        expect(@agent_service).to receive(:bulkUpdateAgentAttributes).with(@user, anything(), uuids, anything(), anything(), anything(), anything(), anything()) do |user, result, uuids, r_add, r_remove, e_add, e_remove, state|
          result.setMessage(LocalizedMessage.string("BULK_AGENT_UPDATE_SUCESSFUL", uuids.join(', ')));
        end

        patch_with_api_header :bulk_update, params: { :uuids => uuids }
        expect(response).to be_ok
        expect(response).to have_api_message_response(200, 'Updated agent(s) with uuid(s): [agent-1, agent-2].')
      end
    end
  end

end
