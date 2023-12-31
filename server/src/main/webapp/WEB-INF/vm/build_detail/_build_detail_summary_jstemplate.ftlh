<#--
 * Copyright 2024 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 -->
<textarea id="build-summary-template" style="display:none;">
<li><span class="header">Scheduled on: </span><span id="build_scheduled_date">${r'${% build.build_scheduled_date %}'}</span></li>
<li><span class="header">Agent: </span>
  {if build.agent_uuid != null }
    <span>
      {if ${isAgentAlive?c} }
            <a id="agent_name" href="${req.getContextPath()}/agents/${r'${% build.agent_uuid %}'}">${r'${% _.escape(build.agent) %}'} (${r'${% build.agent_ip %}'})</a>
      {else}
        <#noparse><span id="agent_name">${% _.escape(build.agent) %} (${% build.agent_ip %})</span></#noparse>
      {/if}
    </span>
  {else}
    <span>Not yet assigned</span>
  {/if}
  <#if doesUserHaveViewAccessToStatusReportPage?? && doesUserHaveViewAccessToStatusReportPage>
    <a href="${req.getContextPath()}/admin/status_reports/${elasticAgentPluginId}/agent/${elasticAgentId!'unassigned'}?job_id=${r'${% build.id %}'}"
       class="btn-primary btn-small status-report-btn-small">Check Agent Status</a>
  </#if>
</li>
  <li><span class="header">Completed on: </span><span id="build_completed_date">${r'${% build.build_completed_date %}'}</span></li>
<li><span class="header">Build cause: </span><span id="stage-${r'${% build.id %}'}-buildCause"><#noautoesc>${presenter.buildCauseMessage?html?html}</#noautoesc></span></li>
  {if build.current_status.toLowerCase() == 'passed' ||  build.current_status.toLowerCase() == 'failed'}
<li><span class="header">Duration: </span><span>${r"${% moment.duration(parseInt(build.current_build_duration), 's').humanizeForGoCD() %}"}</span></li>
{/if}
{if isEstimatable(build.current_status) }
<li class="progress-info">
    <span class="header">Progress:</span>
    {if build.last_build_duration && build.last_build_duration != ''}
        {if parseInt(build.current_build_duration) > parseInt(build.last_build_duration)}
            Longer by: ${r"${ moment.duration(parseInt(build.current_build_duration) - parseInt(build.last_build_duration), 's').humanizeForGoCD() }"}
        {else}
            Elapsed: ${r"${% moment.duration(parseInt(build.current_build_duration), 's').humanizeForGoCD() %}"}, ETA: ${r"${ moment.duration(parseInt(build.last_build_duration) - parseInt(build.current_build_duration), 's').humanizeForGoCD() }"}
        {/if}
    {else}
        Elapsed: ${r"${ moment.duration(parseInt(build.current_build_duration), 's').humanizeForGoCD() }"}
    {/if}
    {if build.last_build_duration && build.last_build_duration != '' && (parseInt(build.current_build_duration) <= parseInt(build.last_build_duration))}
        <div class="progress-bar">
            <div class="progress" style="width: {if parseInt(build.current_build_duration) > parseInt(build.last_build_duration)}100{else}${r"${ 100 * parseInt(build.current_build_duration) / parseInt(build.last_build_duration) }"}{/if}%;">
            </div>
        </div>
    {/if}
</li>
{/if}
</textarea>
