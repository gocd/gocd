/*
 * Copyright 2019 ThoughtWorks, Inc.
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
 */
var StageHistory = function() {
    function _bindHistoryLink(id, url, page_num) {
        var elem = jQuery(id).get(0);
        if (!elem) return;        
        var element = $j(elem);
        element.unbind();
        element.click(function() {
            changePage(url, page_num);
        });
    }

    function changePage(url, pageNum) {    
        new Ajax.Updater($('stage_history'), url, {method: 'get', evalScripts: true});
        setCurrentPage(pageNum);
    }

    function setCurrentPage(pageNum) {
        $("stage-history-page").value = pageNum;
    }

    function init() {
    }


    init.prototype._changePage = changePage;

    init.prototype.bindHistoryLink = function(id, url, page_num) {
        Util.on_load(function() {
            _bindHistoryLink(id, url, page_num);
            AjaxRefreshers.main().afterRefreshOf('stage_history', function() {
                _bindHistoryLink(id, url, page_num);
            });
        });
    };

    return new init();
}();