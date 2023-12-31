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
<#-- @ftlvariable name="customized_name" type="java.lang.String" -->
<#-- @ftlvariable name="customized_path" type="java.lang.String" -->
<div id="tab-content-of-${customized_name?lower_case}" style='display:none'>
</div>
<script>
window["tab-content-of-${customized_name?lower_case}_callback"] = function() {
    const contentElement = $("#tab-content-of-${customized_name?lower_case}");
    if ((contentElement.html() || "").includes("iframe")) {
        return;
    }
    const iframe = '<iframe src="${req.getContextPath()}/${presenter.getRestfulUrl(customized_path)}" frameborder="0"></iframe>';
    contentElement.html('<div><a href="${req.getContextPath()}/${presenter.getRestfulUrl(customized_path)}">Download ${customized_name}</a></div>' + iframe);
    contentElement.show();
}
</script>
