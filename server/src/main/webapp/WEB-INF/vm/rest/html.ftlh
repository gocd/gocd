<#--
 * Copyright 2022 Thoughtworks, Inc.
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
 *
 -->
<#-- @ftlvariable name="jobIdentifier" type="com.thoughtworks.go.domain.JobIdentifier" -->
<#assign title = "Artifacts for ${jobIdentifier.pipelineName} > ${jobIdentifier.pipelineLabel} > ${jobIdentifier.stageName} > ${jobIdentifier.stageCounter} > ${jobIdentifier.buildName}">
<#include "../shared/_header.ftlh">

<div id="yui-main">
    <div class="yui-b">
        <div class="bd-container rounded-corner-for-pipeline">
            <b class="c1"></b><b class="c2"></b><b class="c3"></b><b class="c4"></b>
            <div id="rest-output">
                <h2>${title}</h2>

                <div id="artifacts" class="container-in-body">
                    <#include "../shared/_artifacts.ftlh">
                </div>
            </div>
        </div>

<#include "../shared/_footer.ftlh">

