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
 -->
<textarea rows="0" cols="0" id="tab-content-of-materials-template" style="display: none;">
    {for revision in revisions}
    <div class="material_revision">
        <div class="material">
            <span class="revision_scmtype">${r"${%revision.scmType%}"}</span>
            <span class="revision_location">{if revision.materialName}${r"${%revision.materialName.concat(' - ')%}"} {/if}${r"${%revision.location%}"}</span>
            <br/>
            {if revision.scmType=='Dependency'}
            {var modification = revision.modifications[0]}
            <div class="dependency_revision_highlight-${r"${%revision.changed%}"}">
                <span class="revision_revision">
                    <a href="${req.getContextPath()}/${r"${% revision.revision_href %}"}">${r"${% revision.revision %}"}</a>
                </span>
                <span>${r"${%revision.action%}"}</span>
                on
                <span>${r"${%revision.date%}"}</span>
            </div>
            {/if}
        </div>
        {if revision.scmType != 'Dependency'}
        <table class="modifications">
            {for modification in revision.modifications}
            {var comment = modification.comment}
            <tbody>
            <tr>
                <th colspan="2" class="highlight-${r"${%revision.changed%}"}">

                    <span class="normal revision_information">Revision: ${r"${%modification.revision%}"}, modified by ${r"${%modification.user.escapeHTML()%}"} on ${r"${%modification.date%}"} </span>
                    <br/>

                    <span title="Comment" class="comment">

                        {if revision.scmType == 'Package' }
                            <#include '../shared/_package_material_revision_comment.ftlh'>
                        {else}
                            <#noparse>"${%comment.replace(/\n/g,"<br>")%}"</#noparse>
                        {/if}
                    </span>
                </th>
            </tr>
            {for file in modification.modifiedFiles}
            <tr class="{if file_index%2==0} even {else} odd {/if}">
                <td title="${r"${%file.action%}"}" class="${r"${%file.action%}"}">
                        <span class="modified_file">${r"${%file.fileName%}"}</span>
                </td>
            </tr>
            {/for}
            </tbody>
            {/for}
        </table>
        {/if}
    </div>
    {forelse}
    No materials found.
    {/for}
</textarea>
