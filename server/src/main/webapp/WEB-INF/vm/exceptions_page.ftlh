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
<#-- @ftlvariable name="errorMessage" type="java.lang.String" -->
<#assign title = 'Exception Detail - GoCD'>
<#include "shared/_header.ftlh">
<#include "shared/_flash_message.ftlh">
    <div id="yui-main">
        <div class="yui-b"></div>
    </div>
    <script type="text/javascript">
        $('#trans_content').html("Sorry, an unexpected error occurred<#if errorMessage??> [${errorMessage?js_string?no_esc}]</#if>. :( Please check the server logs for more information.");
        var transMessage = new TransMessage('trans_message', document.body, {type: TransMessage.TYPE_ERROR, offsetTop: 200, autoHide: false});
    </script>

</body>
</html>
