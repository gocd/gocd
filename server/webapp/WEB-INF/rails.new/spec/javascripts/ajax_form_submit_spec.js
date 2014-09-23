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

describe("ajax_form_submit", function(){
    var orignialAjax = jQuery.ajax;

    beforeEach(function(){
        setFixtures("<form>\n" +
                "    <button class=\"primary\"/>\n" +
                "    <div id=\"message_pane\"></div>\n" +
                "</form>"
        );
    });
    afterEach(function(){
        jQuery.ajax = orignialAjax;
    });
    it("testShouldCallbackFormErrorBindingWhenSubmitFails", function(){
        var wasCalled = false;
        var request = function () {
            return {
                status: 500,
                getResponseHeader: function () {
                    return "error";
                }
            }
        }
        var xhr = new request();

        function formErrorBindingCallback(request) {
            wasCalled = true;
            assertEquals(xhr, request)
        }

        jQuery.ajax = function (options) {
            options.error(xhr);
        };

        AjaxForm.jquery_ajax_submit(jQuery("form")[0], AjaxForm.ConfigFormEditHandler, null, formErrorBindingCallback);
        assertEquals(true, wasCalled);
        assertEquals("<p class=\"error\">error</p>", jQuery("#message_pane").html())
    });
    it("testShouldNotFailIfCallbackNotProvided", function(){
        var request = function () {
            return {
                status: 500,
                getResponseHeader: function () {
                    return "error";
                }
            }
        }
        var xhr = new request();

        jQuery.ajax = function (options) {
            options.error(xhr);
        };

        AjaxForm.jquery_ajax_submit(jQuery("form")[0], AjaxForm.ConfigFormEditHandler);
        assertEquals("<p class=\"error\">error</p>", jQuery("#message_pane").html())
    });
});