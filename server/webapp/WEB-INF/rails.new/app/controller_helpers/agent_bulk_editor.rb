##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

module AgentBulkEditor
  include JavaImports
  include ApplicationHelper

  def bulk_edit
    result = HttpOperationResult.new
    if params[:selected].nil? || params[:selected].size == 0
      result.notAcceptable("No agents were selected. Please select at least one agent and try again.", HealthStateType.general(HealthStateScope::GLOBAL))
    elsif params[:operation] == 'Enable'
      agent_service.enableAgents(current_user, result, params[:selected])
    elsif params[:operation] == 'Disable'
      agent_service.disableAgents(current_user, result, params[:selected])
    elsif params[:operation] == 'Delete'
      agent_service.deleteAgents(current_user, result, params[:selected])
    elsif params[:operation] == 'Apply_Resource'
      agent_service.modifyResources(current_user, result, params[:selected], selections)
    elsif params[:operation] == 'Add_Resource'
      agent_service.modifyResources(current_user, result, params[:selected], [TriStateSelection.new(params[:add_resource], "add")]);
    elsif params[:operation] == 'Apply_Environment'
      agent_service.modifyEnvironments(current_user, result, params[:selected], selections)
    else
      result.notAcceptable("The operation #{params[:operation]} is not recognized.", HealthStateType.general(HealthStateScope::GLOBAL))
    end
    result
  end
end