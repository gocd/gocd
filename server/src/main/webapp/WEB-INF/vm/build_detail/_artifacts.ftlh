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
<#-- @ftlvariable name="artifacts_extra_attrs" type="java.lang.String" -->
<div class="widget container-in-body" id="tab-content-of-artifacts" ${artifacts_extra_attrs}>
    <#if presenter.artifactFiles??>
        <#if presenter.artifactFiles?size > 0>
          <div class="artifacts-operations-container">
            <a class="collapse-all" onclick="BuildDetail.expandAll()">(+) Expand All</a> <a class="expand-all" onclick="BuildDetail.collapseAll()">(-) Collapse All</a>
          </div>
        </#if>
    </#if>
  <div class="files">
    ${presenter.renderArtifactFiles(req.getContextPath())?no_esc}

    <script type="text/javascript">
      try {
        BuildDetail.collapseAll();
      } catch(e) {}
    </script>
  </div>
</div>
