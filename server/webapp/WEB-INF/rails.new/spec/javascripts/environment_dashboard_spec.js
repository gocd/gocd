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

describe("environment_dashboard", function(){
    var originalAjaxRequest = jQuery.ajax;
    var element_under_test;
    var util_load_page_fn = null;
    var xhr = null;
    beforeEach(function(){
        setFixtures("<div class='under_test'>\n" +
                "<div id=\"ajax_environments\">\n" +
                "<div id=\"environment_UAT_panel\">\n" +
                "<div class=\"pipeline_bundle environment\">\n" +
                "<h2 class=\"entity_title\">UAT</h2>\n" +
                "\n" +
                "<div class=\"pipelines\">\n" +
                "<div class=\"content_wrapper_outer\">\n" +
                "<div class=\"content_wrapper_inner\">\n" +
                "\n" +
                "<div  id=\"environment_pipeline_blahPipeline1_panel\" class=\"pipeline hidereveal_collapsed\">\n" +
                "    <h3 class=\"title entity_title\">\n" +
                "\n" +
                "        <a href=\"/tab/pipeline/history/blahPipeline1\">blahPipeline1</a>\n" +
                "    </h3>\n" +
                "\n" +
                "    <div>\n" +
                "    </div>\n" +
                "    <div class=\"alert\" id=\"trigger-result-blahPipeline1\"></div>\n" +
                "\n" +
                "    <div class=\"pipeline_instance\">\n" +
                "\n" +
                "\n" +
                "        <div class='status details'>\n" +
                "            <div class=\"label\">Label: <a href=\"/pipelines/blahPipeline1/5\">label</a>\n" +
                "\n" +
                "            </div>\n" +
                "            <div class=\"pipeline_instance_details\">\n" +
                "                <div class=\"schedule_time\" title=\"2010-05-24T13:55:05+05:30\">(triggered <span class='time'>less than a minute ago</span> by <span class='who'></span>)</div>\n" +
                "                <div class=\"stages\">\n" +
                "                    <div class=\"latest_stage\">\n" +
                "                        Passed: cruise\n" +
                "                    </div>\n" +
                "\n" +
                "\n" +
                "                    <a href='/pipelines/blahPipeline1/5/cruise/10' class='stage'>\n" +
                "                        <div class=\"stage_bar_wrapper  last_run_stage\">\n" +
                "                            <div class=\"stage_bar Passed\"\n" +
                "                                 title=\"cruise (Passed)\"\n" +
                "                                 style='width: 19.75em'>\n" +
                "\n" +
                "                            </div>\n" +
                "                        </div>\n" +
                "                    </a>\n" +
                "\n" +
                "                </div>\n" +
                "\n" +
                "            </div>\n" +
                "\n" +
                "        </div>\n" +
                "\n" +
                "    </div>\n" +
                "    <div class=\"pipeline_instance\">\n" +
                "\n" +
                "\n" +
                "        <div class='status details'>\n" +
                "            <div class=\"label\">Label: <a href=\"/pipelines/blahPipeline1/1\">1</a>\n" +
                "\n" +
                "            </div>\n" +
                "            <div class=\"pipeline_instance_details\">\n" +
                "                <div class=\"schedule_time\" title=\"2010-05-24T13:55:05+05:30\">(triggered <span class='time'>less than a minute ago</span> by <span class='who'>cruise</span>)</div>\n" +
                "                <div class=\"stages\">\n" +
                "                    <div class=\"latest_stage\">\n" +
                "                        Building: blahStage-0\n" +
                "                    </div>\n" +
                "\n" +
                "\n" +
                "                    <a href='/pipelines/blahPipeline1/1/blahStage-0/1' class='stage'>\n" +
                "                        <div class=\"stage_bar_wrapper  last_run_stage\">\n" +
                "                            <div class=\"stage_bar Building\"\n" +
                "                                 title=\"blahStage-0 (Building)\"\n" +
                "                                 style='width: 3.75em'>\n" +
                "\n" +
                "                            </div>\n" +
                "                        </div>\n" +
                "                    </a>\n" +
                "\n" +
                "                    <a href='/pipelines/blahPipeline1/1/blahStage-1/1' class='stage'>\n" +
                "                        <div class=\"stage_bar_wrapper \">\n" +
                "                            <div class=\"stage_bar Failed\"\n" +
                "                                 title=\"blahStage-1 (Failed)\"\n" +
                "                                 style='width: 3.9167em'>\n" +
                "\n" +
                "                            </div>\n" +
                "                        </div>\n" +
                "                    </a>\n" +
                "\n" +
                "                    <a href='/pipelines/blahPipeline1/1/blahStage-2/1' class='stage'>\n" +
                "                        <div class=\"stage_bar_wrapper \">\n" +
                "                            <div class=\"stage_bar Cancelled\"\n" +
                "                                 title=\"blahStage-2 (Cancelled)\"\n" +
                "                                 style='width: 3.9167em'>\n" +
                "                                <img src='/images/g9/stage_bar_cancelled_icon.png' alt=''/>\n" +
                "                            </div>\n" +
                "                        </div>\n" +
                "                    </a>\n" +
                "\n" +
                "                    <a href='/pipelines/blahPipeline1/1/blahStage-3/1' class='stage'>\n" +
                "                        <div class=\"stage_bar_wrapper \">\n" +
                "                            <div class=\"stage_bar Passed\"\n" +
                "                                 title=\"blahStage-3 (Passed)\"\n" +
                "                                 style='width: 3.9167em'>\n" +
                "\n" +
                "                            </div>\n" +
                "                        </div>\n" +
                "                    </a>\n" +
                "\n" +
                "        <span class='stage'>\n" +
                "<div class=\"stage_bar_wrapper \">\n" +
                "    <div class=\"stage_bar Unknown\"\n" +
                "         title=\"blahStage-4 (Unknown)\"\n" +
                "         style='width: 3.9167em'>\n" +
                "\n" +
                "    </div>\n" +
                "</div></span>\n" +
                "\n" +
                "                </div>\n" +
                "\n" +
                "            </div>\n" +
                "\n" +
                "        </div>\n" +
                "\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "    <div class='deploy'>\n" +
                "        <form action=\"/pipelines/show\" method=\"post\"\n" +
                "              onsubmit=\"AjaxRefreshers.disableAjax();; new Ajax.Request('/pipelines/show', {asynchronous:true, evalScripts:true, on401:function(request){redirectToLoginPage(&apos;/auth/login&apos;);}, onComplete:function(request){AjaxRefreshers.enableAjax();}, onSuccess:function(request){Modalbox.show(request.responseText, { title: 'blahPipeline1 - Change Materials', overlayClose: false, width: 850, height: 525, slideDownDuration: 0, overlayDuration: 0});}, parameters:Form.serialize(this)}); return false;\">\n" +
                "            <input type=\"hidden\" name=\"pipeline_name\" value=\"blahPipeline1\"/>\n" +
                "            <button class=\"change_revision submit\" type=\"submit\" value=\"Deploy Specific Revision\"><span>DEPLOY SPECIFIC REVISION</span></button>\n" +
                "        </form>\n" +
                "        <form action=\"/api/pipelines/blahPipeline1/schedule\" method=\"post\"\n" +
                "              onsubmit=\"AjaxRefreshers.disableAjax();$('deploy-blahPipeline1').disabled = true;; new Ajax.Updater({success:'trigger-result-blahPipeline1',failure:'trigger-result-blahPipeline1'}, '/api/pipelines/blahPipeline1/schedule', {asynchronous:true, evalScripts:true, on401:function(request){redirectToLoginPage(&apos;/auth/login&apos;);}, onComplete:function(request){AjaxRefreshers.enableAjax();}, parameters:Form.serialize(this)}); return false;\">\n" +
                "            <div class='operate'>\n" +
                "                <button class=\"submit\" id=\"deploy-blahPipeline1\" type=\"submit\" value=\"Deploy Latest\"><span>DEPLOY LATEST</span></button>\n" +
                "            </div>\n" +
                "        </form>\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "</div>\n" +
                "<div class=\"divider\"></div>\n" +
                "\n" +
                "<div  id=\"environment_pipeline_blahPipeline2_panel\" class=\"pipeline hidereveal_collapsed\">\n" +
                "    <h3 class=\"title entity_title\">\n" +
                "\n" +
                "        <a href=\"/tab/pipeline/history/blahPipeline2\">blahPipeline2</a>\n" +
                "    </h3>\n" +
                "\n" +
                "    <div>\n" +
                "    </div>\n" +
                "    <div class=\"alert\" id=\"trigger-result-blahPipeline2\"></div>\n" +
                "\n" +
                "    <div class=\"pipeline_instance\">\n" +
                "\n" +
                "\n" +
                "        <div class='status details'>\n" +
                "            <div class=\"label\">Label: <a href=\"/pipelines/blahPipeline2/5\">label</a>\n" +
                "\n" +
                "            </div>\n" +
                "            <div class=\"pipeline_instance_details\">\n" +
                "                <div class=\"schedule_time\" title=\"2010-05-24T13:55:05+05:30\">(triggered <span class='time'>less than a minute ago</span> by <span class='who'></span>)</div>\n" +
                "                <div class=\"stages\">\n" +
                "                    <div class=\"latest_stage\">\n" +
                "                        Passed: cruise\n" +
                "                    </div>\n" +
                "\n" +
                "\n" +
                "                    <a href='/pipelines/blahPipeline2/5/cruise/10' class='stage'>\n" +
                "                        <div class=\"stage_bar_wrapper  last_run_stage\">\n" +
                "                            <div class=\"stage_bar Passed\"\n" +
                "                                 title=\"cruise (Passed)\"\n" +
                "                                 style='width: 19.75em'>\n" +
                "\n" +
                "                            </div>\n" +
                "                        </div>\n" +
                "                    </a>\n" +
                "\n" +
                "                </div>\n" +
                "\n" +
                "            </div>\n" +
                "\n" +
                "        </div>\n" +
                "\n" +
                "    </div>\n" +
                "    <div class=\"pipeline_instance\">\n" +
                "\n" +
                "        <div class='status'>\n" +
                "            <span class='message'>\n" +
                "            No historical data\n" +
                "            </span>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "    <div class='deploy'>\n" +
                "        <form action=\"/pipelines/show\" method=\"post\"\n" +
                "              onsubmit=\"AjaxRefreshers.disableAjax();; new Ajax.Request('/pipelines/show', {asynchronous:true, evalScripts:true, on401:function(request){redirectToLoginPage(&apos;/auth/login&apos;);}, onComplete:function(request){AjaxRefreshers.enableAjax();}, onSuccess:function(request){Modalbox.show(request.responseText, { title: 'blahPipeline2 - Change Materials', overlayClose: false, width: 850, height: 525, slideDownDuration: 0, overlayDuration: 0});}, parameters:Form.serialize(this)}); return false;\">\n" +
                "            <input type=\"hidden\" name=\"pipeline_name\" value=\"blahPipeline2\"/>\n" +
                "            <button class=\"change_revision submit\" type=\"submit\" value=\"Deploy Specific Revision\"><span>DEPLOY SPECIFIC REVISION</span></button>\n" +
                "        </form>\n" +
                "        <form action=\"/api/pipelines/blahPipeline2/schedule\" method=\"post\"\n" +
                "              onsubmit=\"AjaxRefreshers.disableAjax();$('deploy-blahPipeline2').disabled = true;; new Ajax.Updater({success:'trigger-result-blahPipeline2',failure:'trigger-result-blahPipeline2'}, '/api/pipelines/blahPipeline2/schedule', {asynchronous:true, evalScripts:true, on401:function(request){redirectToLoginPage(&apos;/auth/login&apos;);}, onComplete:function(request){AjaxRefreshers.enableAjax();}, parameters:Form.serialize(this)}); return false;\">\n" +
                "            <div class='operate'>\n" +
                "                <button class=\"submit\" id=\"deploy-blahPipeline2\" type=\"submit\" value=\"Deploy Latest\"><span>DEPLOY LATEST</span></button>\n" +
                "            </div>\n" +
                "        </form>\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "</div>\n" +
                "<div class=\"divider\"></div>\n" +
                "\n" +
                "<div id=\"environment_pipeline_blahPipeline3_panel\" class=\"pipeline hidereveal_collapsed\">\n" +
                "    <h3 class=\"title entity_title\">\n" +
                "\n" +
                "        <a href=\"/tab/pipeline/history/blahPipeline3\">blahPipeline3</a>\n" +
                "    </h3>\n" +
                "\n" +
                "    <div>\n" +
                "    </div>\n" +
                "    <div class=\"alert\" id=\"trigger-result-blahPipeline3\"></div>\n" +
                "\n" +
                "    <div class=\"pipeline_instance\">\n" +
                "\n" +
                "\n" +
                "        <div class='status details'>\n" +
                "            <div class=\"label\">Label: <a href=\"/pipelines/blahPipeline3/5\">label</a>\n" +
                "\n" +
                "            </div>\n" +
                "            <div class=\"pipeline_instance_details\">\n" +
                "                <div class=\"schedule_time\" title=\"2010-05-24T13:55:05+05:30\">(triggered <span class='time'>less than a minute ago</span> by <span class='who'></span>)</div>\n" +
                "                <div class=\"stages\">\n" +
                "                    <div class=\"latest_stage\">\n" +
                "                        Passed: cruise\n" +
                "                    </div>\n" +
                "\n" +
                "\n" +
                "                    <a href='/pipelines/blahPipeline3/5/cruise/10' class='stage'>\n" +
                "                        <div class=\"stage_bar_wrapper  last_run_stage\">\n" +
                "                            <div class=\"stage_bar Passed\"\n" +
                "                                 title=\"cruise (Passed)\"\n" +
                "                                 style='width: 19.75em'>\n" +
                "\n" +
                "                            </div>\n" +
                "                        </div>\n" +
                "                    </a>\n" +
                "\n" +
                "                </div>\n" +
                "\n" +
                "            </div>\n" +
                "\n" +
                "        </div>\n" +
                "\n" +
                "    </div>\n" +
                "    <div class=\"pipeline_instance\">\n" +
                "\n" +
                "        <div class='status'>\n" +
                "            <span class='message'>\n" +
                "            No historical data\n" +
                "            </span>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "    <div class='deploy'>\n" +
                "        <form action=\"/pipelines/show\" method=\"post\"\n" +
                "              onsubmit=\"AjaxRefreshers.disableAjax();; new Ajax.Request('/pipelines/show', {asynchronous:true, evalScripts:true, on401:function(request){redirectToLoginPage(&apos;/auth/login&apos;);}, onComplete:function(request){AjaxRefreshers.enableAjax();}, onSuccess:function(request){Modalbox.show(request.responseText, { title: 'blahPipeline3 - Change Materials', overlayClose: false, width: 850, height: 525, slideDownDuration: 0, overlayDuration: 0});}, parameters:Form.serialize(this)}); return false;\">\n" +
                "            <input type=\"hidden\" name=\"pipeline_name\" value=\"blahPipeline3\"/>\n" +
                "            <button class=\"change_revision submit\" type=\"submit\" value=\"Deploy Specific Revision\"><span>DEPLOY SPECIFIC REVISION</span></button>\n" +
                "        </form>\n" +
                "        <form action=\"/api/pipelines/blahPipeline3/schedule\" method=\"post\"\n" +
                "              onsubmit=\"AjaxRefreshers.disableAjax();$('deploy-blahPipeline3').disabled = true;; new Ajax.Updater({success:'trigger-result-blahPipeline3',failure:'trigger-result-blahPipeline3'}, '/api/pipelines/blahPipeline3/schedule', {asynchronous:true, evalScripts:true, on401:function(request){redirectToLoginPage(&apos;/auth/login&apos;);}, onComplete:function(request){AjaxRefreshers.enableAjax();}, parameters:Form.serialize(this)}); return false;\">\n" +
                "            <div class='operate'>\n" +
                "                <button class=\"submit\" id=\"deploy-blahPipeline3\" type=\"submit\" value=\"Deploy Latest\"><span>DEPLOY LATEST</span></button>\n" +
                "            </div>\n" +
                "        </form>\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "</div>\n" +
                "<div class=\"divider\"></div>\n" +
                "\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>"
        );
    });

    beforeEach(function() {
        util_load_page_fn = Util.loadPage;
        Util.loadPage = function(url) {
            newPageUrl = url;
        };
        xhr = {
            getResponseHeader: function(name) {
                return "holy_cow_new_url_is_sooooo_cool!!!";
            }
        };
        element_under_test = $$('.under_test').first().innerHTML;
    });

    afterEach(function() {
        Util.loadPage = util_load_page_fn;
        jQuery.ajax = originalAjaxRequest;
        $$('.under_test').first().update(element_under_test);

    });

    it("test_add_newly_added_environment_to_the_page", function(){
        jQuery.ajax = function(options) {
            options.success( {"environment_UAT_panel" : {html: "group1" , parent_id: "ajax_environments", index: 0, type: "group_of_pipelines"},
                "thingy" : {html:"new_text", parent_id: "ajax_environments", index: 1, type: "group_of_pipelines"}});
            options.complete(xhr);
        };

        var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html",{redirectUrl: "foo", className: ".environment"});
        refresher.stopRefresh();
        refresher.restartRefresh();

        assertEquals("new_text", $("thingy").innerHTML);
    });

    it("test_should_keep_materials_exapnsion_after_refresh", function(){
        var pipeline = $("environment_pipeline_blahPipeline1_panel").down("div");
        pipeline.removeClassName("hidereveal_collapsed");
        pipeline.addClassName("hidereveal_expander");
        jQuery.ajax = function(options) {
            options.success( {
                "environment_UAT_panel" : {html: "group1" , parent_id: "ajax_environments", index: 0, type: "group_of_pipelines"},
                "environment_blahPipeline1_panel" : {html: "whatever" , parent_id: "environment_UAT_panel", index: 0, type: "pipeline"}
            });
            options.complete(xhr);
        };
        new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".environment"});
        assertTrue("Must have class 'hidereveal_expander'. Instead it has '" + pipeline.classNames() + "'", pipeline.hasClassName("hidereveal_expander"));
    });

    it("test_remove_environment", function(){
        jQuery.ajax = function(options) {
            options.success( { "thingy" : {html:"new_text", parent_id:"ajax_environments", index:1, type: "group_of_pipelines"}});
            options.complete(xhr);
        };
        var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".environment"});
        refresher.stopRefresh();
        refresher.restartRefresh();
        assertNull($("environment_UAT_panel"));
    });

});
