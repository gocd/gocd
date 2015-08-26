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

require 'spec_helper'

describe ApiV2::AgentsController do

  before do
    controller.stub(:agent_service).and_return(@agent_service = double('agent-service'))
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
        two_agents = AgentsViewModelMother.getTwoAgents()

        @agent_service.should_receive(:agents).and_return(two_agents)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(two_agents, ApiV2::AgentsRepresenter))
      end

      it 'should get empty json when there are no agents' do
        zero_agents = AgentsViewModelMother.getZeroAgents()

        @agent_service.should_receive(:agents).and_return(zero_agents)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(zero_agents, ApiV2::AgentsRepresenter))
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
        expect(actual_response).to eq(expected_response(AgentViewModel.new(agent), ApiV2::AgentRepresenter))
      end

      it 'should return 404 when agent is not found' do
        null_agent = NullAgentInstance.new('some-uuid')
        @agent_service.should_receive(:findAgent).with(null_agent.getUuid()).and_return(null_agent)

        get_with_api_header :show, uuid: null_agent.getUuid()
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
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
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', nil, TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname'
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(AgentViewModel.new(agent), ApiV2::AgentRepresenter))
      end

      it 'should return agent json when agent resources update is successful by specifing a comma separated string' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', resources: "java,linux,firefox"
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(AgentViewModel.new(agent), ApiV2::AgentRepresenter))
      end

      it 'should return agent json when agent is enabled' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", TriState.TRUE) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', resources: "java,linux,firefox", agent_config_state: 'enabled'
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(AgentViewModel.new(agent), ApiV2::AgentRepresenter))
      end

      it 'should return agent json when agent is disabled' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", TriState.FALSE) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', resources: "java,linux,firefox", agent_config_state: "diSAbled"
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(AgentViewModel.new(agent), ApiV2::AgentRepresenter))
      end

      it 'should return agent json when agent resources update is successful by specifying a resource array' do
        agent = AgentInstanceMother.idle()
        @agent_service.should_receive(:findAgent).twice.with(agent.getUuid()).and_return(agent)
        @agent_service.should_receive(:updateAgentAttributes).with(@user, anything(), agent.getUuid(), 'some-hostname', "java,linux,firefox", TriState.UNSET) do |user, result, uuid, new_hostname|
          result.ok("Updated agent with uuid #{agent.getUuid()}")
        end

        patch_with_api_header :update, uuid: agent.getUuid(), hostname: 'some-hostname', resources: ['java', 'linux', 'firefox']
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(AgentViewModel.new(agent), ApiV2::AgentRepresenter))
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
  end

end
