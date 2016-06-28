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

class AgentAutocompleteController < ApplicationController

  def resource
    resources = go_config_service.getResourceList()
    render json: starts_with_search_string(resources)
  end

  def status
    status_vals = AgentStatus.values()
    status_vals = status_vals.map {|status| status.name().downcase}
    render json: starts_with_search_string(status_vals)
  end

  def environment
    env_names = environment_config_service.environmentNames()
    env_names = env_names.map {|env| env.toString()}
    render json: starts_with_search_string(env_names)
  end

  def name
    names = agent_service.getUniqueAgentNames()
    render json: starts_with_search_string(names)
  end

  def ip
    ip_addrs = agent_service.getUniqueIPAddresses()
    render json: starts_with_search_string(ip_addrs)
    end

  def os
    os_list = agent_service.getUniqueAgentOperatingSystems()
    render json: starts_with_search_string(os_list)
  end

  private

  def starts_with_search_string(list)
    query = params[:q].downcase
    list.select { |item| item.downcase.start_with?(query) }.inject("") {|init, item| init + "\n" + ERB::Util.h(item) }.strip
  end

end
