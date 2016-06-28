##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

module AgentMother
  def idle_agent(options={})
    agent = AgentInstanceMother.idle()
    handle_options(agent, options)
  end

  def pending_agent(options={})
    agent = AgentInstanceMother.pendingInstance()
    handle_options(agent, options)
  end

  def missing_agent(options={})
    agent = AgentInstanceMother.missing()
    handle_options(agent, options)
  end

  def cancelled_agent(options={})
    agent = options[:locator] ? AgentInstanceMother.cancelled(options.delete(:locator)) : AgentInstanceMother.cancelled()
    handle_options(agent, options)
  end

  def disabled_agent(options={})
    if locator = options.delete(:locator)
      agent = AgentInstanceMother.building(locator)
      agent.deny()
    else
      agent = AgentInstanceMother.disabled()
    end
    handle_options(agent, options)
  end

  def building_agent(options={})
    agent = options[:locator] ? AgentInstanceMother.building(options.delete(:locator)) : AgentInstanceMother.building()
    handle_options(agent, options)
  end

  def lost_contact_agent(options={})
    agent = options[:locator] ? AgentInstanceMother.lostContact(options.delete(:locator)) : AgentInstanceMother.lostContact()
    handle_options(agent, options)
  end

  def agent_with_config_errors(options={})
    agent = AgentInstanceMother.agentWithConfigErrors()
    handle_options(agent, options)
  end


  private

  def handle_options(agent, options)
    environments = options.delete(:environments) || []

    options.each do |key, value|
      AgentInstanceMother.send("update_#{key}", agent, value) #if AgentInstanceMother.respond_to?("update_#{key}")
    end

    AgentViewModel.new(agent, environments)
  end

end
