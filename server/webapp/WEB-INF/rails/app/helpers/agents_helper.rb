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

module AgentsHelper

  AgentStatus = com.thoughtworks.go.domain.AgentStatus

  def agent_selector uuid, selector_name, selected_uuids
    "<td class='selector'>#{agent_selector_without_cell(uuid, selector_name, selected_uuids)}</td>".html_safe
  end

  def agent_selector_without_cell uuid, selector_name, selected_uuids
    "<input type='checkbox' name='#{ERB::Util.h(selector_name)}' value='#{ERB::Util.h(uuid)}' class='agent_select' #{(selected_uuids && selected_uuids.include?(uuid)) ? "checked='true'" : ""}/>".html_safe
  end

  def cell_with_title value, klass, title = value
    "<td class='#{ERB::Util.h(klass)}' title='#{ERB::Util.h(title)}'><span>#{ERB::Util.h(value)}</span></td>".html_safe
  end

  def to_display_name(status)
    status.to_s.titleize.downcase
  end

  def build_link(build_locator, link_text = build_locator)
    link_to(link_text, build_locator_url(build_locator))
  end

  def agent_status_cell agent
    status = to_display_name(agent.getStatus())
    case agent.getStatus()
      when AgentStatus::Disabled
        disabled agent, status
      when AgentStatus::LostContact
        lostcontact agent, status
      when AgentStatus::Building
        if has_view_or_operate_permission_on_pipeline?(agent.buildLocator())
          cell_with_title(build_link(agent.buildLocator(), status), "status", agent.buildLocator())
        else
          cell_with_title(status, "status")
        end
      when AgentStatus::Cancelled
        cancelled agent
      else
        cell_with_title(status, "status")
    end
  end

  def has_view_or_operate_permission_on_pipeline?(build_locator)
    pipeline = build_locator.split("/").first
    security_service.hasViewOrOperatePermissionForPipeline(current_user, pipeline)
  end

  def lostcontact agent, status
    last_heard = agent.getLastHeardTime().iso8601
    if agent.buildLocator().empty?
      cell_with_title(status, "status", "lost contact at #{last_heard}")
    else
      cell_with_title(build_link(agent.buildLocator(), status), "status", "lost contact at #{last_heard} while building #{agent.buildLocator()}: job rescheduled")
    end
  end

  def disabled agent, status
    if (agent.isBuilding() or agent.isCancelled()) and !agent.build_locator.empty?
      cell_with_title(link_to("disabled (building)", build_locator_url(agent.buildLocator())), "status", agent.buildLocator())
    else
      cell_with_title(status, "status")
    end
  end

  def cancelled agent
    if agent.build_locator.empty?
      cell_with_title("building (cancelled)", "status")
    else
      cell_with_title(link_to("building (cancelled)", build_locator_url(agent.buildLocator())), "status", agent.buildLocator())
    end
  end

  def join_with_pipe(default_text, values)
    (values.nil? || values.empty?) ? default_text : values.join(" | ")
  end

  def piped_cell values, default_text, title
    text = join_with_pipe(default_text, values)
    cell_with_title(text, title)
  end

  def build_locator_url locator
    "#{context_path}/tab/build/detail/#{locator}"
  end

  def context_path
    servlet_request.getContextPath()
  end

  def job_on_agent_page_handler page
    link_to(page.getLabel(), job_run_history_on_agent_path(:page => page.getNumber(), :column => params[:column], :order => params[:order]))
  end

  def label_for label, default=""
    (label.nil? || label.empty?) ? default : label
  end

  def get_agent_status_class(show_only_disabled, agent_current_status)
      if show_only_disabled
       agent_current_status = agent_current_status == AgentStatus::Disabled ? agent_current_status : ""
      end
      agent_current_status
  end
end
