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
<#-- @ftlvariable name="tests_extra_attrs" type="java.lang.String" -->
<div id="tab-content-of-tests" class="widget" ${tests_extra_attrs}>
    <div class="files">
        <#if presenter.hasTests()>
            <iframe sandbox="allow-scripts" src="${req.getContextPath()}/${presenter.indexPageURL}" width="95%" height="500" frameborder="0"></iframe>
        <#else>
            <#include "_test_output_config.ftlh">
        </#if>
    </div>
</div>
