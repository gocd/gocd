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

describe("dashboard_periodical_executer", function(){
    var dashboard_periodical_executer = new DashboardPeriodicalExecuter('pipelineStatus.json');

    beforeEach(function(){
        contextPath = "/go";
        setFixtures("<div id=\"project1_profile\">\n" +
            "    <div id=\"control_panel\">\n" +
            "        <a id=\"project1_forcebuild\"></a>\n" +
            "        <a id=\"project1_config_panel\"></a>\n" +
            "        <a id=\"project1_all_builds\"></a>\n" +
            "        <a id=\"project1_all_successful_builds\"></a>\n" +
            "    </div>\n" +
            "</div>");
        dashboard_periodical_executer.clean();
    });

    afterEach(function(){
        contextPath = null;
    });

    it("test_should_call_observer_notify_when_success", function(){
        spyOn(jQuery, "ajax").and.callFake(function(options) {
            options.success([1, 2, 3]);
        });

        var invoked = false;
        var fakeObserver = {notify: function() {
            invoked = true;
        }};
        dashboard_periodical_executer.clean();
        dashboard_periodical_executer.register(fakeObserver);
        assertEquals(1, dashboard_periodical_executer.observers.size());
        dashboard_periodical_executer.start();
        assertTrue(invoked);
    });

    it("test_should_show_error_when_got_invalid_json", function(){
        var invoked = false;
        var msg;
        window.flash = {error: function(title, body) {
            invoked = true;
            msg = title;
        }}
        spyOn(jQuery, "ajax").and.callFake(function(options) {
            options.error({}, "parsererror");
        });
        var fakeOb = {notify: function() {
        }};
        dashboard_periodical_executer.clean();
        dashboard_periodical_executer.register(fakeOb);
        assertEquals(1, dashboard_periodical_executer.observers.size());
        dashboard_periodical_executer.start();
        dashboard_periodical_executer.fireNow();

        assertTrue(invoked);
        assertEquals('The server encountered a problem (json error).', msg);
    });

    /* Disabled, because we don't want to show the error message when user leave some page
     The browser will return a empty transport object when user cancel the transfer and leave the page.
     */    xit("should_show_error_when_got_no_response_text", function(){
        var invoked = false;
        var msg;
        window.flash = {error: function(title, body){
            invoked = true;
            msg = title;
        }}
        prepareMockRequest({status: 200}, null);
        var fakeOb = {notify: function(){}};
        dashboard_periodical_executer.clean();
        dashboard_periodical_executer.register(fakeOb);
        assertEquals(1, dashboard_periodical_executer.observers.size());
        dashboard_periodical_executer.start();
        dashboard_periodical_executer.fireNow();

        assertTrue(invoked);
        assertEquals('Server cannot be reached. Either there is a network problem or the server is down.', msg);
    });

    it("test_should_show_404_error_when_response_header_is_404", function(){
        var invoked = false;
        var msg;
        window.flash = {error: function(title, body) {
            invoked = true;
            msg = title;
        }}
        spyOn(jQuery, "ajax").and.callFake(function(options) {
            options.statusCode[404]();
        });

        dashboard_periodical_executer.start();
        dashboard_periodical_executer.fireNow();

        assertTrue(invoked);
        assertEquals('Server cannot be reached (404). Either there is a network problem or the server is down.', msg);
    });

    it("test_should_call_redirect_to_login_when_response_header_is_401", function(){
        var invoked = false;
        spyOn(jQuery, "ajax").and.callFake(function(options) {
            options.statusCode[401]();
        });

        dashboard_periodical_executer.redirectToLoginPage = function() {
            invoked = true;
        }

        dashboard_periodical_executer.start();
        dashboard_periodical_executer.fireNow();

        assertTrue(invoked);
    });

    it("", function(){
    });

    it("test_should_show_500_error_and_reason_when_response_header_is_500", function(){
        var invoked = false;
        var msg1, msg2;
        window.flash = {error: function(title, body) {
            invoked = true;
            msg1 = title;
            msg2 = body;
        }}
        spyOn(jQuery, "ajax").and.callFake(function(options) {
            options.statusCode[500]({responseText: 'I\'m the reason'});
        });
        dashboard_periodical_executer.start();
        dashboard_periodical_executer.fireNow();

        assertTrue(invoked);
        assertEquals('The server encountered an internal problem.', msg1);
        assertEquals('I\'m the reason', msg2);
    });

    it("test_should_show_unknow_error_when_response_header_is_105", function(){
        var invoked = false;
        var msg;
        window.flash = {error: function(title, body) {
            invoked = true;
            msg = title;
        }}
        spyOn(jQuery, "ajax").and.callFake(function(options) {
            options.error();
        });

        dashboard_periodical_executer.start();
        dashboard_periodical_executer.fireNow();

        assertTrue(invoked);
        assertEquals('Server cannot be reached (failure). Either there is a network problem or the server is down.', msg);
    });

    it("test_should_show_error_message_when_server_return_error_message_in_json", function(){
        var invoked = false;
        var msg1, msg2;
        window.flash = {error: function(title, body) {
            invoked = true;
            msg1 = title;
            msg2 = body;
        }}

        spyOn(jQuery, "ajax").and.callFake(function(options) {
            options.success({error: "There is some error."});
        });

        dashboard_periodical_executer.start();
        dashboard_periodical_executer.fireNow();

        assertTrue(invoked);
        assertEquals('The server encountered a problem.', msg1);
        assertEquals('There is some error.', msg2);
    });

    it("test_should_not_call_observer_when_excuter_is_paused", function(){
        spyOn(jQuery, "ajax").and.callFake(function(options) {
            options.success([1,2,3.4]);
        });
        var invoked = false;
        var fakeOb = {notify: function() {
            invoked = true;
        }};
        dashboard_periodical_executer.clean();
        dashboard_periodical_executer.pause();
        dashboard_periodical_executer.register(fakeOb);
        assertEquals(1, dashboard_periodical_executer.observers.size());
        dashboard_periodical_executer.start();
        dashboard_periodical_executer.fireNow();

        assertFalse(invoked);
        prepareMockRequest({status: 200}, '[1, 2, 3, 4]');
        dashboard_periodical_executer.resume();
        dashboard_periodical_executer.fireNow();
        assertTrue(invoked);
    });

    it("test_should_have_default_path_when_there_is_no_context_path", function(){
        dashboard_periodical_executer.setUrl('json.json');
        assertEquals('/go/json.json', dashboard_periodical_executer.url);
    });

    it("test_sn_should_be_expired_after_call_generate_method_2_times", function(){
        var sn = dashboard_periodical_executer.generateSequenceNumber();
        dashboard_periodical_executer.generateSequenceNumber();
        assertFalse(dashboard_periodical_executer.isSequenceNumberValid(sn));
    });

    it("test_should_remove_all_observers_after_clean", function(){
        var invoked = false;
        var fakeOb = {notify: function() {
            invoked = true;
        }};
        dashboard_periodical_executer.clean();
        assertEquals(0, dashboard_periodical_executer.observers.size());

        dashboard_periodical_executer.register(fakeOb);
        dashboard_periodical_executer.register(fakeOb);
        dashboard_periodical_executer.register(fakeOb);
        dashboard_periodical_executer.register(fakeOb);

        assertEquals(4, dashboard_periodical_executer.observers.size());
        dashboard_periodical_executer.clean();
        assertEquals(0, dashboard_periodical_executer.observers.size());
    });

    it("test_should_remove_observer_when_unregister", function(){
        var invoked = false;
        var fakeOb = {notify: function() {
            invoked = true;
        }};
        dashboard_periodical_executer.clean();
        assertEquals(0, dashboard_periodical_executer.observers.size());

        dashboard_periodical_executer.register(fakeOb);
        dashboard_periodical_executer.register(fakeOb);

        assertEquals(2, dashboard_periodical_executer.observers.size());
        dashboard_periodical_executer.unregister(fakeOb);
        assertEquals(1, dashboard_periodical_executer.observers.size());
    });

    it("test_should_register_one_observer", function(){
        assertEquals(0, dashboard_periodical_executer.observers.size());
        dashboard_periodical_executer.register("fake_observer");
        assertEquals(1, dashboard_periodical_executer.observers.size());
    });

    it("test_should_register_multiple_observer", function(){
        assertEquals(0, dashboard_periodical_executer.observers.size());
        dashboard_periodical_executer.register("fake_observer", "fake_observer2");
        assertEquals(2, dashboard_periodical_executer.observers.size());
    });

    it("test_executer_should_pause_when_pause_condition_is_true", function() {
        spyOn(jQuery, "ajax").and.callFake(function(options) {
            options.success({pause: true});
        });

        var invoked = false;

        var fakeOb = {notify: function() {
            invoked = true;
        }};

        var pausable_dashboard_periodical_executer = new DashboardPeriodicalExecuter('pipelineStatus.json', function(data) {return data.pause;});
        pausable_dashboard_periodical_executer.start();

        pausable_dashboard_periodical_executer.fireNow();

        pausable_dashboard_periodical_executer.register(fakeOb);
        pausable_dashboard_periodical_executer.start();
        pausable_dashboard_periodical_executer.fireNow();

        assertEquals(pausable_dashboard_periodical_executer.is_paused, true);
        assertFalse(invoked);
    });

    it("test_should_invoke_notify_method_on_observer", function(){
        var is_invoked = false;
        var observer = {
            notify : function() {
                is_invoked = true;
            }
        }
        dashboard_periodical_executer.register(observer);
        dashboard_periodical_executer._loop_observers({responseText: "{bla:'bla'}"}, 1);
        assertTrue(is_invoked);
    });
});