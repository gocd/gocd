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

var ComparePipelines = function() {
    function _bindPagesLink(id, url, page_num) {
        var elem = document.getElementById(id);
        if (!elem) return;        
        var element = $j(elem);
        element.unbind();
        element.click(function() {
            changePage(this, url, page_num);
        });
    }

    function changePage(elem, url, pageNum) {
        var container = $j(elem).parent().parent()[0];
        new Ajax.Updater($(container), url, {method: 'get', evalScripts: true});
        //setCurrentPage(pageNum);
    }

    function setCurrentPage(pageNum) {
        $("pipeline-history-page").value = pageNum;
    }

    function init() {
    }


    init.prototype._changePage = changePage;

    init.prototype.bindPagesLink = function(id, url, page_num) {
        Util.on_load(function() {
            _bindPagesLink(id, url, page_num);
            AjaxRefreshers.main().afterRefreshOf('pipeline_history', function() {
                _bindPagesLink(id, url, page_num);
            });
        });
    };

    return new init();
}();

ComparePipelines.PaginationPopupHandler = function() {};

ComparePipelines.PaginationPopupHandler.prototype = new MicroContentPopup.NoOpHandler();

ComparePipelines.PaginationPopupHandler.prototype.allow_propagation_of = function(event) {
    var elem = event.target;
    return ! jQuery(elem).parent().hasClass("pagination");
};
