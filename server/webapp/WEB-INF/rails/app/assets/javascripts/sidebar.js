/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END**********************************/

/* This function is to collapse or expand the side bar */
function toggleSidebar(link) {
    $('sidebar').toggle();
    $('doc3').toggleClassName('yui-t6').toggleClassName('yui-t7');
    if (link.title == "Collapse sidebar") {
        link.title = "Expand sidebar";
        rememberSidebarStatus('collapse');
    } else {
        link.title = "Collapse sidebar";
        rememberSidebarStatus('expand');
    }
    $(link).toggleClassName('collapse').toggleClassName('expand');
}

function expandSidebar(link) {
    $('sidebar').hide();
    $('doc3').removeClassName('yui-t6').addClassName('yui-t7');
    link.title = "Collapse sidebar";
    $(link).removeClassName('collapse').addClassName('expand');
}

function collapseSidebar(link){
    if($('sidebar')){
        $('sidebar').show();
        $('doc3').removeClassName('yui-t7').addClassName('yui-t6');
        link.title = "Expand sidebar";
        $(link).addClassName('collapse').removeClassName('expand');
    }
}

function rememberSidebarStatus(status) {
    setCookie('sidebar-status', status);
}

function restoreSidebarStatus() {
    var status = getCookie('sidebar-status');
    var button = $('collapse-sidebar-link');
    if(status && status == 'collapse' && button){
        toggleSidebar(button);
    }
}