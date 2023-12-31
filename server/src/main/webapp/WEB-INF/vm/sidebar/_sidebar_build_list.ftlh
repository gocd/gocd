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
<div id="build_history_holder" class="sidebar-container">
    <h4 class="entity_title dropdown-arrow-icon" data-toggle='dropdown'>Job History</h4>
    <div id="buildlist-container" class="round-content">
        <#if presenter.recent25?size == 0 >
            <p class="text-only">No jobs found.</p>
        </#if>
        <ul class="buildlist dropdown-menu">
            <#list presenter.recent25 as listPresenter>
            <li id="build_list_${listPresenter?counter}" <#if listPresenter.isSame(presenter.id)> class="current" </#if> >
                <a href="${req.getContextPath()}/tab/build/detail/${listPresenter.buildLocator}">
                    <#if listPresenter.copy>
                    <div class="color_code_small copied_job"></div>
                    <#else>
                    <div class="color_code_small"></div>
                    </#if>
                    <strong>${listPresenter.buildLocatorForDisplay}</strong>
                    <div class="time_ago" data="${listPresenter.scheduledTime?c}"></div>
                </a>
            </li>
            </#list>
        </ul>
    </div>
    <script type="text/javascript">
        <#list presenter.recent25 as listPresenter>
        json_to_css.update_build_list(eval(${listPresenter.toJsonString()?no_esc}), ${listPresenter?counter}, "${req.getContextPath()}/${concatenatedStageBarCancelledIconFilePath}");
        </#list>

      $(function() {
          $(".buildlist .time_ago").each(function(idx, timeSpan) {
            var timestamp = parseInt($(timeSpan).attr("data"));
            if (isNaN(timestamp)) return;
            var time = new Date(timestamp);
            $(timeSpan).text(moment(time).format('DD MMM YYYY [at] HH:mm:ss [Local Time]'));
          });
        });
    </script>
</div>
