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

xdescribe("micro_content_on_agents_rails_new", function () {
    var resources_widget = null;
    var resources_widget_shower = null;
    var environment_widget = null;
    var environment_widget_shower = null;
    var resource_popup_handler = null;
    var util_load_page_fn = null;
    var xhr = null;

    beforeEach(function () {
        AjaxRefreshers.clear();
        setFixtures("<div class='under_test'>\n" +
            "<div class='page_header' style='overflow:inherit;'><h1 class='entity_title'>Agents</h1>\n" +
            "\n" +
            "    <div class='filter_agents'>\n" +
            "        <form accept-charset='UTF-8' action='/agents' id='agents_filter_form' method='get'>\n" +
            "            <div style='margin:0;padding:0;display:inline'><input name='utf8' type='hidden' value='&#x2713;'/></div>\n" +
            "            <div class='enhanced_dropdown hidden' id='filter_help'>\n" +
            "                <div class='filter_help_instructions'><p class='heading'>Available tags</p>\n" +
            "\n" +
            "                    <div><p>name:</p>\n" +
            "                        <p>os:</p>\n" +
            "                        <p>ip:</p>\n" +
            "                        <p>status:</p>\n" +
            "                        <p>resource:</p>\n" +
            "                        <p>environment:</p></div>\n" +
            "                    <p class='heading'>Values</p>\n" +
            "\n" +
            "                    <div><p>Put filter values in quotes for exact match</p></div>\n" +
            "                    <p><a class='' href='https://docs.gocd.org/current/navigation/agents_page.html#filtering-agents' target='_blank'>More...</a></p>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <input id='filter_text' name='filter' placeholder='tag: value' type='text'/>\n" +
            "            <button class='submit primary' type='submit'>Filter</button>\n" +
            "            <a class='link_as_button' href='/agents?filter=' id='clear_filter'>Clear</a></form>\n" +
            "    </div>\n" +
            "    <div class='edit_panel' id='dd_ajax_float' style='float:right;'>\n" +
            "        <form accept-charset='UTF-8' action='/agents/edit_agents' id='agents_form' method='post'>\n" +
            "            <div style='margin:0;padding:0;display:inline'><input name='utf8' type='hidden' value='&#x2713;'/></div>\n" +
            "            <input id='agent_edit_operation' name='operation' type='hidden'/>\n" +
            "\n" +
            "            <div id='actual_agent_selectors' style='display: none'>\n" +
            "                <input class='agent_select' name='selected[]' type='checkbox' value='UUID_host1'/>\n" +
            "                <input class='agent_select' name='selected[]' type='checkbox' value='uuid4'/>\n" +
            "                <input class='agent_select' name='selected[]' type='checkbox' value='uuid3'/>\n" +
            "                <input class='agent_select' name='selected[]' type='checkbox' value='UUID_host4'/>\n" +
            "                <input class='agent_select' name='selected[]' type='checkbox' value='UUID_host5'/>\n" +
            "                <input class='agent_select' name='selected[]' type='checkbox' value='UUID_host6'/>\n" +
            "                <input class='agent_select' name='selected[]' type='checkbox' value='UUID_host7'/>\n" +
            "                <input class='agent_select' name='selected[]' type='checkbox' value='UUID_host8'/>\n" +
            "                <input class='agent_select' name='selected[]' type='checkbox' value='UUID_host9'/></div>\n" +
            "            <button class='submit' id='UUID' name='Enable' type='submit' value='Enable'><span>ENABLE</span></button>\n" +
            "            <script type='text/javascript'>\n" +
            "                Util.on_load(function () {\n" +
            "                    Event.observe($('UUID'), 'click',\n" +
            "                            function (evt) {\n" +
            "                                Util.set_value('agent_edit_operation', 'Enable')(evt);\n" +
            "                            });\n" +
            "                });\n" +
            "            </script>\n" +
            "\n" +
            "            <button class='submit' id='UUID' name='Disable' type='submit' value='Disable'><span>DISABLE</span>\n" +
            "            </button>\n" +
            "            <script type='text/javascript'>\n" +
            "                Util.on_load(function () {\n" +
            "                    Event.observe($('UUID'), 'click',\n" +
            "                            function (evt) {\n" +
            "                                Util.set_value('agent_edit_operation', 'Disable')(evt);\n" +
            "                            });\n" +
            "                });\n" +
            "            </script>\n" +
            "            <button class='submit' id='UUID' name='Delete' type='submit' value='Delete'><span>DELETE</span></button>\n" +
            "            <script type='text/javascript'>\n" +
            "                Util.on_load(function () {\n" +
            "                    Event.observe($('UUID'), 'click',\n" +
            "                            function (evt) {\n" +
            "                                Util.set_value('agent_edit_operation', 'Delete')(evt);\n" +
            "                            });\n" +
            "                });\n" +
            "            </script>\n" +
            "            <button class='show_panel select submit button' id='show_resources_panel' text_color='dark'\n" +
            "                    type='button' value='Resources'><span> RESOURCES <img\n" +
            "                    src='/images/g9/button_select_icon_dark.png'/> </span></button>\n" +
            "            <div class='hidden resources_panel agent_edit_popup enhanced_dropdown' id='resources_panel'>\n" +
            "                <div class='resources_selector scrollable_panel'>\n" +
            "                    <div class='loading'></div>\n" +
            "                </div>\n" +
            "                <div class='add_panel hidden'>\n" +
            "                    <input class='new_resource new_field' name='add_resource' type='text'/>\n" +
            "                    <button class='apply_resources apply_button submit_small primary submit' name='resource_operation' type='submit' value='Add'><span>ADD</span></button>\n" +
            "                    <div class='validation_message error hidden'> Invalid character. Please use a-z, A-Z, 0-9, fullstop, underscore, hyphen and pipe.\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                <div class='no_selection_error error hidden'>Please select one or more agents first.</div>\n" +
            "            </div>\n" +
            "            <button class='show_panel select submit button' id='show_environments_panel' text_color='dark'\n" +
            "                    type='button' value='Environments'><span> ENVIRONMENTS <img\n" +
            "                    src='/images/g9/button_select_icon_dark.png'/> </span></button>\n" +
            "            <div class='hidden environments_panel agent_edit_popup enhanced_dropdown' id='environments_panel'>\n" +
            "                <div class='environments_selector scrollable_panel'>\n" +
            "                    <div class='loading'></div>\n" +
            "                </div>\n" +
            "                <div class='add_panel hidden'>\n" +
            "                    <button class='submit_small primary apply_button submit' id='1e9de87d-55ac-4ac1-9bdd-93ff8725821e'\n" +
            "                            name='Apply' type='submit'\n" +
            "                            value='Apply'><span>APPLY</span></button>\n" +
            "                    <script type='text/javascript'>\n" +
            "                        Util.on_load(function () {\n" +
            "                            Event.observe($('1e9de87d-55ac-4ac1-9bdd-93ff8725821e'), 'click',\n" +
            "                                    function (evt) {\n" +
            "                                        Util.set_value('agent_edit_operation', 'Apply_Environment')(evt);\n" +
            "                                    });\n" +
            "                        });\n" +
            "                    </script>\n" +
            "                </div>\n" +
            "                <div class='no_selection_error error hidden'>Please select one or more agents first.</div>\n" +
            "                <div class='no_environments_error error hidden'>No environments are defined.</div>\n" +
            "            </div>\n" +
            "        </form>\n" +
            "    </div>\n" +
            "</div>\n" +
            "<div class='content_wrapper_outer'>\n" +
            "    <div class='content_wrapper_inner'>\n" +
            "        <div id='agents_form_container'>\n" +
            "            <div class='clear_float' id='ajax_agents_header'>\n" +
            "                <ul class='agent_counts list_aggregation clear_float'>\n" +
            "                    <li class='pending'>Pending: </li>\n" +
            "                    <li class='enabled'>Enabled: </li>\n" +
            "                    <li class='disabled'>Disabled: </li>\n" +
            "                </ul>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div class='agents_table' id='ajax_agents_table'>\n" +
            "            <table class='agents list_table sortable_table selectable_table' id='agent_details'>\n" +
            "                <thead>\n" +
            "                <tr class='agent_header'>\n" +
            "                    <th class='selector'><input class='agent_select' id='select_all_agents' name='accept'\n" +
            "                                                type='checkbox' value=''/></th>\n" +
            "                    <th class='hostname'><a href='/agents?column=hostname&amp;order=ASC'> <span>Agent Name</span>\n" +
            "                    </a></th>\n" +
            "                    <th class='location'><a href='/agents?column=location&amp;order=ASC'> <span>Sandbox</span> </a>\n" +
            "                    </th>\n" +
            "                    <th class='operating_system'><a href='/agents?column=operating_system&amp;order=ASC'>\n" +
            "                        <span>OS</span> </a></th>\n" +
            "                    <th class='ip_address'><a href='/agents?column=ip_address&amp;order=ASC'>\n" +
            "                        <span>IP Address</span> </a></th>\n" +
            "                    <th class='status'><a href='/agents?column=status&amp;order=ASC'> <span>Status</span> </a></th>\n" +
            "                    <th class='usable_space'><a href='/agents?column=usable_space&amp;order=ASC'>\n" +
            "                        <span>Free Space</span> </a></th>\n" +
            "                    <th class='resources'><a href='/agents?column=resources&amp;order=ASC'> <span>Resources</span>\n" +
            "                    </a></th>\n" +
            "                    <th class='environments'><a href='/agents?column=environments&amp;order=ASC'>\n" +
            "                        <span>Environments</span> </a></th>\n" +
            "                </tr>\n" +
            "                </thead>\n" +
            "                <tbody>\n" +
            "                <tr class='agent_instance Idle' id='UUID_host1'>\n" +
            "                    <td class='selector'><input class='agent_select' name='selected[]' type='checkbox'\n" +
            "                                                value='UUID_host1'/></td>\n" +
            "                    <td class='hostname' title='host1'><span class='agent_hostname'> <a\n" +
            "                            href='/agents/UUID_host1'>host1</a> </span>\n" +
            "                    </td>\n" +
            "                    <td class='location' title='LOCATION'><span>LOCATION</span></td>\n" +
            "                    <td class='operating_system' title='OPERATING SYSTEM'><span>OPERATING SYSTEM</span></td>\n" +
            "                    <td class='ip_address' title='10.18.5.1'><span>10.18.5.1</span></td>\n" +
            "                    <td class='status' title='idle'><span>idle</span></td>\n" +
            "                    <td class='usable_space' title='10.0 KB'><span>10.0 KB</span></td>\n" +
            "                    <td class='resources' title='no resources specified'><span>no resources specified</span></td>\n" +
            "                    <td class='environments' title='no environments specified'>\n" +
            "                        <span>no environments specified</span></td>\n" +
            "                </tr>\n" +
            "                <tr class='agent_instance Pending' id='uuid4'>\n" +
            "                    <td class='selector'><input class='agent_select' name='selected[]' type='checkbox'\n" +
            "                                                value='uuid4'/></td>\n" +
            "                    <td class='hostname' title='CCeDev03'><span class='agent_hostname'>CCeDev03</span></td>\n" +
            "                    <td class='location' title='LOCATION'><span>LOCATION</span></td>\n" +
            "                    <td class='operating_system' title='linux'><span>linux</span></td>\n" +
            "                    <td class='ip_address' title='127.0.0.1'><span>127.0.0.1</span></td>\n" +
            "                    <td class='status' title='pending'><span>pending</span></td>\n" +
            "                    <td class='usable_space' title='0 bytes'><span>0 bytes</span></td>\n" +
            "                    <td class='resources' title='no resources specified'><span>no resources specified</span></td>\n" +
            "                    <td class='environments' title='no environments specified'>\n" +
            "                        <span>no environments specified</span></td>\n" +
            "                </tr>\n" +
            "                <tr class='agent_instance Idle' id='uuid3'>\n" +
            "                    <td class='selector'><input class='agent_select' name='selected[]' type='checkbox'\n" +
            "                                                value='uuid3'/></td>\n" +
            "                    <td class='hostname' title='CCeDev01'><span class='agent_hostname'> <a\n" +
            "                            href='/agents/uuid3'>CCeDev01</a> </span>\n" +
            "                    </td>\n" +
            "                    <td class='location' title='LOCATION'><span>LOCATION</span></td>\n" +
            "                    <td class='operating_system' title='linux'><span>linux</span></td>\n" +
            "                    <td class='ip_address' title='10.6.6.6'><span>10.6.6.6</span></td>\n" +
            "                    <td class='status' title='idle'><span>idle</span></td>\n" +
            "                    <td class='usable_space' title='10.0 GB'><span>10.0 GB</span></td>\n" +
            "                    <td class='resources' title='db | dbSync'><span>db | dbSync</span></td>\n" +
            "                    <td class='environments' title='no environments specified'>\n" +
            "                        <span>no environments specified</span></td>\n" +
            "                </tr>\n" +
            "                <tr class='agent_instance Building' id='UUID_host4'>\n" +
            "                    <td class='selector'><input class='agent_select' name='selected[]' type='checkbox'\n" +
            "                                                value='UUID_host4'/></td>\n" +
            "                    <td class='hostname' title='CCeDev01'><span class='agent_hostname'> <a\n" +
            "                            href='/agents/UUID_host4'>CCeDev01</a> </span></td>\n" +
            "                    <td class='location' title='LOCATION'><span>LOCATION</span></td>\n" +
            "                    <td class='operating_system' title='OPERATING SYSTEM'><span>OPERATING SYSTEM</span></td>\n" +
            "                    <td class='ip_address' title='10.18.5.1'><span>10.18.5.1</span></td>\n" +
            "                    <td class='status' title='blue/2/stage/3/job/'><span> <a\n" +
            "                            href='/go/tab/build/detail/blue/2/stage/3/job/'>building</a> </span></td>\n" +
            "                    <td class='usable_space' title='0 bytes'><span>0 bytes</span></td>\n" +
            "                    <td class='resources' title='java'><span>java</span></td>\n" +
            "                    <td class='environments' title='no environments specified'>\n" +
            "                        <span>no environments specified</span></td>\n" +
            "                </tr>\n" +
            "                <tr class='agent_instance Idle' id='UUID_host5'>\n" +
            "                    <td class='selector'><input class='agent_select' name='selected[]' type='checkbox'\n" +
            "                                                value='UUID_host5'/></td>\n" +
            "                    <td class='hostname' title='foo_baz_host'><span class='agent_hostname'> <a\n" +
            "                            href='/agents/UUID_host5'>foo_baz_host</a> </span></td>\n" +
            "                    <td class='location' title='LOCATION'><span>LOCATION</span></td>\n" +
            "                    <td class='operating_system' title='Windows'><span>Windows</span></td>\n" +
            "                    <td class='ip_address' title='10.18.5.1'><span>10.18.5.1</span></td>\n" +
            "                    <td class='status' title='idle'><span>idle</span></td>\n" +
            "                    <td class='usable_space' title='12.0 GB'><span>12.0 GB</span></td>\n" +
            "                    <td class='resources' title='nant | vs.net'><span>nant | vs.net</span></td>\n" +
            "                    <td class='environments' title='no environments specified'>\n" +
            "                        <span>no environments specified</span></td>\n" +
            "                </tr>\n" +
            "                <tr class='agent_instance Missing' id='UUID_host6'>\n" +
            "                    <td class='selector'><input class='agent_select' name='selected[]' type='checkbox'\n" +
            "                                                value='UUID_host6'/></td>\n" +
            "                    <td class='hostname' title='foo_bar_host'><span class='agent_hostname'> <a\n" +
            "                            href='/agents/UUID_host6'>foo_bar_host</a> </span></td>\n" +
            "                    <td class='location' title='LOCATION'><span>LOCATION</span></td>\n" +
            "                    <td class='operating_system' title=''><span/></td>\n" +
            "                    <td class='ip_address' title='192.168.0.1'><span>192.168.0.1</span></td>\n" +
            "                    <td class='status' title='missing'><span>missing</span></td>\n" +
            "                    <td class='usable_space' title='Unknown'><span>Unknown</span></td>\n" +
            "                    <td class='resources' title='no resources specified'><span>no resources specified</span></td>\n" +
            "                    <td class='environments' title='uat | blah'><span>uat | blah</span></td>\n" +
            "                </tr>\n" +
            "                <tr class='agent_instance Cancelled' id='UUID_host7'>\n" +
            "                    <td class='selector'><input class='agent_select' name='selected[]' type='checkbox'\n" +
            "                                                value='UUID_host7'/></td>\n" +
            "                    <td class='hostname' title='CCeDev01'><span class='agent_hostname'> <a\n" +
            "                            href='/agents/UUID_host7'>CCeDev01</a> </span></td>\n" +
            "                    <td class='location' title='LOCATION'><span>LOCATION</span></td>\n" +
            "                    <td class='operating_system' title='OPERATING SYSTEM'><span>OPERATING SYSTEM</span></td>\n" +
            "                    <td class='ip_address' title='10.18.5.1'><span>10.18.5.1</span></td>\n" +
            "                    <td class='status' title='pink/2/stage/3/job/'><span> <a\n" +
            "                            href='/go/tab/build/detail/pink/2/stage/3/job/'>building (cancelled)</a> </span></td>\n" +
            "                    <td class='usable_space' title='0 bytes'><span>0 bytes</span></td>\n" +
            "                    <td class='resources' title='no resources specified'><span>no resources specified</span></td>\n" +
            "                    <td class='environments' title='no environments specified'>\n" +
            "                        <span>no environments specified</span></td>\n" +
            "                </tr>\n" +
            "                <tr class='agent_instance LostContact' id='UUID_host8'>\n" +
            "                    <td class='selector'><input class='agent_select' name='selected[]' type='checkbox'\n" +
            "                                                value='UUID_host8'/></td>\n" +
            "                    <td class='hostname' title='localhost'><span class='agent_hostname'> <a\n" +
            "                            href='/agents/UUID_host8'>localhost</a> </span></td>\n" +
            "                    <td class='location' title='LOCATION'><span>LOCATION</span></td>\n" +
            "                    <td class='operating_system' title=''><span/></td>\n" +
            "                    <td class='ip_address' title='192.168.0.1'><span>192.168.0.1</span></td>\n" +
            "                    <td class='status'\n" +
            "                        title='lost contact at REPLACED_DATE while building french/2/stage/3/job/: job rescheduled'>\n" +
            "                        <span> <a href='/go/tab/build/detail/french/2/stage/3/job/'>lost contact</a> </span></td>\n" +
            "                    <td class='usable_space' title='Unknown'><span>Unknown</span></td>\n" +
            "                    <td class='resources' title='no resources specified'><span>no resources specified</span></td>\n" +
            "                    <td class='environments' title='no environments specified'>\n" +
            "                        <span>no environments specified</span></td>\n" +
            "                </tr>\n" +
            "                <tr class='agent_instance LostContact' id='UUID_host9'>\n" +
            "                    <td class='selector'><input class='agent_select' name='selected[]' type='checkbox'\n" +
            "                                                value='UUID_host9'/></td>\n" +
            "                    <td class='hostname' title='localhost'><span class='agent_hostname'> <a\n" +
            "                            href='/agents/UUID_host9'>localhost</a> </span></td>\n" +
            "                    <td class='location' title='LOCATION'><span>LOCATION</span></td>\n" +
            "                    <td class='operating_system' title=''><span/></td>\n" +
            "                    <td class='ip_address' title='192.168.0.1'><span>192.168.0.1</span></td>\n" +
            "                    <td class='status' title='lost contact at REPLACED_DATE'><span>lost contact</span></td>\n" +
            "                    <td class='usable_space' title='Unknown'><span>Unknown</span></td>\n" +
            "                    <td class='resources' title='no resources specified'><span>no resources specified</span></td>\n" +
            "                    <td class='environments' title='no environments specified'>\n" +
            "                        <span>no environments specified</span></td>\n" +
            "                </tr>\n" +
            "                </tbody>\n" +
            "            </table>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "</div>");

        resource_popup_handler = new EditPopupHandler.AddEditHandler('http://foo/bar', $('agents_form'), Util.are_any_rows_selected('.agents .agent_select'), resource_validator, 'agent_edit_operation', 'Apply_Resource', 'Add_Resource');
        resources_widget = new MicroContentPopup($('resources_panel'), resource_popup_handler);
        resources_widget_shower = new MicroContentPopup.ClickShower(resources_widget);
        resources_widget_shower.bindShowButton($('show_resources_panel'));
        resource_popup_handler.setDefaultText(jQuery('.new_resource')[0], "my default text");

        environment_widget = new MicroContentPopup($('environments_panel'), new EditPopupHandler.AddOnlyHandler('http://foo/baz', $('agents_form'), Util.are_any_rows_selected('.agents .agent_select')));
        environment_widget_shower = new MicroContentPopup.ClickShower(environment_widget);
        environment_widget_shower.bindShowButton($('show_environments_panel'));
    });
    function resource_validator(value) {
        return /.*?X.*/.test(value);
    }

    var actual_ajax_updater = Ajax.Updater;
    var actual_ajax_request = jQuery.ajax;

    var actual_periodical_updater = Ajax.PeriodicalUpdater;

    var response_pane;
    var resource_selection_url;
    var resource_selection_request_option;

    var periodical_opts = null;
    var header = null, content = null;
    var ajax_request_for_tri_state_boxes_fired = false;
    var otherAjax = null;

    var replicator = null;
    var replicated_checkbox_parents = ['ajax_agents_table', 'actual_agent_selectors'];
    var replicated_checkbox_id_reader = function (chk_bx) {
        return chk_bx.value;
    };

    beforeEach(function () {
        ajax_request_for_tri_state_boxes_fired = false;
        Ajax.Updater = function (container, url, options) {
            response_pane = container;
            resource_selection_url = url;
            resource_selection_request_option = options;
            ajax_request_for_tri_state_boxes_fired = true;
        };

        jQuery.ajax = function (options) {
            periodical_opts = options;
        };
        AjaxRefreshers.addRefresher(otherAjax = {stopRefresh: function () {
            this.stopped = true;
        }, restartRefresh: function () {
            this.started = true;
        }});
        fire_event($(document.body), 'click');
        header = $('ajax_agents_header').innerHTML;
        content = $('ajax_agents_table').innerHTML;
        jQuery('.new_resource.new_field').each(function (input) {
            input.value = "my default text";
        });

        replicator = new FieldStateReplicator();

        for (var i = 0; i < replicated_checkbox_parents.length; i++) {
            replicator.register_all_matching($(replicated_checkbox_parents[i]), '.agent_select', replicated_checkbox_id_reader);
        }
        util_load_page_fn = Util.loadPage;
        Util.loadPage = function (url) {
            newPageUrl = url;
        };
        xhr = {
            getResponseHeader: function (name) {
                return "holy_cow_new_url_is_sooooo_cool!!!";
            }
        };
    });

    afterEach(function () {
        Util.loadPage = util_load_page_fn;
        replicator.unregister_all();
        Ajax.Updater = actual_ajax_updater;
        Ajax.PeriodicalUpdater = actual_periodical_updater;
        jQuery.ajax = actual_ajax_request;
        AjaxRefreshers.clear();
        jQuery('.agent_select').each(function (check_box) {
            check_box.checked = false;
        });
        $('ajax_agents_header').innerHTML = header;
        $('ajax_agents_table').innerHTML = content;
        jQuery("#uuid3 .selector input").checked = false;
        jQuery('.new_resource.new_field').val("");
    });

    it("test_looks_up_resource_panel_given_a_child_element", function () {
        var env_handler = MicroContentPopup.lookupHandler(jQuery('.no_environments_error')[0]);
        assertTrue(environment_widget.callback_handler == env_handler);
        var resource_handler = MicroContentPopup.lookupHandler(jQuery('.new_resource')[0]);
        assertTrue(resources_widget.callback_handler == resource_handler);
        var no_handler = MicroContentPopup.lookupHandler($('agents_form'));
        assertEquals("must return null when no parent popup can be found", null, no_handler);
    });

    it("test_validates_new_resource_using_passed_in_validator_function", function () {
        fire_event($("show_resources_panel"), 'click');
        var new_field = jQuery('.new_resource.new_field')[0];
        new_field.value = "foo";
        jQuery(new_field).trigger("change");
//        fire_event(new_field, 'keyup');
        assertFalse("validation message should have been shown for invalid new value", jQuery('.validation_message')[0].hasClassName('hidden'));
        new_field.value = "bXr";
        jQuery(new_field).trigger("change");
//        fire_event(new_field, 'keyup');
        assertTrue("validation message should NOT have been shown for valid new value", jQuery('.validation_message')[0].hasClassName('hidden'));
    });

    xit("should_still_show_popup_when_no_add_field_registered", function () {
        fire_event($("show_environments_panel"), 'click');
        assertFalse("environment panel should be shown", $('environments_panel').hasClassName('hidden'));
        fire_event($(document.body), 'click');
        assertTrue("environment panel should be hidden", $('environments_panel').hasClassName('hidden'));
    });

    it("test_should_toggle_popup_visibility_when_clicked_on_show_button", function () {
        fire_event($("show_environments_panel"), 'click');
        assertFalse("environment panel should be shown", $('environments_panel').hasClassName('hidden'));
        fire_event($("show_environments_panel"), 'click');
        assertTrue("environment panel should NOT be shown", $('environments_panel').hasClassName('hidden'));
        fire_event($("show_environments_panel"), 'click');
        assertFalse("environment panel should be shown", $('environments_panel').hasClassName('hidden'));
    });

    it("test_closes_other_popups_on_show", function () {
        fire_event($("show_environments_panel"), 'click');
        fire_event($("show_resources_panel"), 'click');
        assertFalse("resource panel should be visible", $('resources_panel').hasClassName('hidden'));
        assertTrue("environment panel should be hidden", $('environments_panel').hasClassName('hidden'));
    });

    it("test_should_not_show_resource_popup_by_default", function () {
        assertEquals("resources_panel should not be visible.", true, $("resources_panel").hasClassName('hidden'));
    });

    it("test_should_have_loading_when_page_loaded_or_when_resource_panel_is_opened", function () {
        var chk_box = jQuery('#ajax_agents_table .agent_select').get(1);
        jQuery(chk_box).attr("checked", "checked");
        fire_event(chk_box, 'change');
        var selector_pane = $$('.resources_selector').first();
        assertEquals('loading', jQuery(selector_pane.innerHTML).select('div')[0].className);
        selector_pane.innerHTML = "foo";
        fire_event($("show_resources_panel"), 'click');
        assertEquals('loading', jQuery(selector_pane.innerHTML).select('div')[0].className);
        assertEquals("resources_panel should be visible.", false, $("resources_panel").hasClassName('hidden'));
    });

    it("test_should_hide_resource_popup_when_anything_outside_of_the_popup_is_clicked", function () {
        var show_button = $("show_resources_panel");
        fire_event(show_button, 'click');
        fire_event($(document.body), 'click');
        assertTrue("resources_panel should NOT be visible.", $("resources_panel").hasClassName('hidden'));
    });

    it("test_should_NOT_hide_resource_popup_when_anything_inside_the_popup_is_clicked", function () {
        var show_button = $("show_resources_panel");
        fire_event(show_button, 'click');
        fire_event(jQuery('.new_resource')[0], 'click');
        assertFalse("resources_panel should be visible.", $("resources_panel").hasClassName('hidden'));
    });


    it("test_not_should_show_error_message_if_resource_is_ok", function () {
        var show_button = $("show_resources_panel");
        fire_event(show_button, 'click');
        $("resources_panel").getElementsBySelector("input[type='text']")[0].setValue("foo");
        assertEquals(true, $($("resources_panel").getElementsBySelector(".validation_message")[0]).hasClassName('hidden'));
    });

    it("test_shows_NO_AGENTS_SELECTED_message", function () {
        var add_panel = $("resources_panel").getElementsBySelector('.add_panel')[0];
        var edit_panel = $("resources_panel").getElementsBySelector('.scrollable_panel')[0];
        var no_agents_message = $("resources_panel").getElementsBySelector('.no_selection_error')[0];
        var selector_pane = jQuery('.resources_selector').first();
        selector_pane.innerHTML = "foo";// was dirty
        var show_button = $("show_resources_panel");
        fire_event(show_button, 'click');
        assertTrue("add panel should not be shown", add_panel.hasClassName('hidden'));
        assertTrue("edit panel should not be shown", edit_panel.hasClassName('hidden'));
        assertFalse("no agents selected message should be shown", no_agents_message.hasClassName('hidden'));
        assertFalse("ajax request should not have been fired when no agents are selected", ajax_request_for_tri_state_boxes_fired);


        fire_event(show_button, 'click'); //hide
        $(document.body).getElementsBySelector("input[type='checkbox']").each(function (checkbox) {
            jQuery(checkbox).attr("checked", "checked");
        });
        fire_event(show_button, 'click'); //show again
        assertFalse("add panel should be shown", add_panel.hasClassName('hidden'));
        assertFalse("edit panel should be shown", edit_panel.hasClassName('hidden'));
        assertTrue("no agents selected message should not be shown", no_agents_message.hasClassName('hidden'));
        assertTrue("ajax request should have been fired when no agents are selected", ajax_request_for_tri_state_boxes_fired);
    });

    it("test_should_fire_ajax_request_to_load_resource_selection_list", function () {
        var agent_check_box = jQuery('#ajax_agents_table .agent_select').get(1);
        jQuery(agent_check_box).attr("checked", "checked");
        fire_event(agent_check_box, 'change');
        var show_button = $("show_resources_panel");
        fire_event(show_button, 'click');
        assertTrue(response_pane.hasClassName('resources_selector'));
        assertTrue("ajax request for loading tristate boxes should have been fired", ajax_request_for_tri_state_boxes_fired);
        assertTrue("should stop other ajax request", otherAjax.stopped);
        assertEquals("http://foo/bar", resource_selection_url);
        assertEquals(true, resource_selection_request_option['evalScripts']);
        assertEquals('UUID_host1', resource_selection_request_option['parameters']['selected[]']);
    });

    it("test_switches_to_add_mode_on_show", function () {
        jQuery('.agent_select').first().attr("checked", "checked");
        var show_button = $("show_resources_panel");
        fire_event(show_button, 'click');
        resource_popup_handler.tristate_clicked();
        fire_event(show_button, 'click');//hide it
        fire_event(show_button, 'click');
        var new_field = jQuery(".new_resource")[0];
        var apply_resources_button = jQuery(".apply_resources")[0];
        assertFalse("add resource field should be shown on re-show", new_field.hasClassName('hidden'));
        assertEquals("has add button on re-show", "Add", apply_resources_button.value);
    });

    it("test_update_page", function () {
        var refresher = new AgentsAjaxRefresher('http://blah/refresh', "foo", replicator, replicated_checkbox_parents, replicated_checkbox_id_reader, 0);
        AjaxRefreshers.addRefresher(refresher);
        refresher.stopRefresh();
        refresher.restartRefresh();

        periodical_opts.success({ajax_agents_header: {html: "counts"}, ajax_agents_table: {html: "tablecontent"}});
        periodical_opts.complete(xhr);
        assertEquals("counts", $('ajax_agents_header').innerHTML);
        assertEquals("tablecontent", $('ajax_agents_table').innerHTML);
    });

    it("test_update_page_keeps_the_current_selections", function () {
        var table = $("ajax_agents_table").innerHTML;
        var chkbox = jQuery("#uuid3 .selector input")[0];

        var refresher = new AgentsAjaxRefresher('http://blah/refresh', "foo", replicator, replicated_checkbox_parents, replicated_checkbox_id_reader, 0);
        refresher.stopRefresh();
        refresher.restartRefresh();

        jQuery(chkbox).attr("checked", "checked");
        fire_event(chkbox, 'change');
        periodical_opts.success({ajax_agents_header: {html: "counts"}, ajax_agents_table: {html: table}});
        periodical_opts.complete(xhr);
        assertTrue(jQuery("#uuid3 .selector input")[0].checked);
    });

    it("test_agent_resource_name_validator", function () {
        assertTrue(AgentUtil.validate_resource_name('foo-bar.baz|quux'));
        assertFalse(AgentUtil.validate_resource_name('foo$bar'));
    });

    it("test_add_should_clear_default_text_model", function () {
        resource_popup_handler = new EditPopupHandler.AddEditHandler('http://foo/bar', $('agents_form'), Util.are_any_rows_selected('.agents .agent_select'), resource_validator, 'agent_edit_operation', 'Apply_Resource', 'Add_Resource');
        resources_widget = new MicroContentPopup($('resources_panel'), resource_popup_handler);
        resource_popup_handler.setDefaultText(jQuery('.new_resource')[0], "my default text");
        var form = $('agents_form');
        var new_resource_value = null;
        var apply_resource_button = jQuery(".apply_resources")[0];
        fire_event(apply_resource_button, "click");
        new_resource_value = form.serialize();
        assertEquals(1, new_resource_value.match(/\badd_resource=(?:$|&)/).length);
    });

    it("test_understand_modify_mode", function () {
        var show_button = $("show_resources_panel");
        fire_event(show_button, 'click');
        var new_field = jQuery(".new_resource")[0];
        var apply_resources_button = jQuery(".apply_resources")[0];
        assertFalse("add resource field should be shown on load", new_field.hasClassName('hidden'));
        assertEquals("operation should be add resource", "Add_Resource", $('agent_edit_operation').value);
        resource_popup_handler.tristate_clicked();
        fire_event(apply_resources_button, "click");
        assertEquals("operation should be apply resource", "Apply_Resource", $('agent_edit_operation').value);
        assertTrue("add resource field should not be shown in modified mode", new_field.hasClassName('hidden'));

    });

    it("test_should_show_error_when_there_are_no_environments", function () {
        jQuery("#UUID_host5 .selector input").first().attr("checked", "checked");
        fire_event($("show_environments_panel"), 'click');
        resource_selection_request_option.onComplete(xhr);
        assertTrue(jQuery("#environments_panel .add_panel")[0].hasClassName("hidden"));
        assertFalse(jQuery("#environments_panel .no_environments_error")[0].hasClassName("hidden"));
    });

    it("test_should_show_error_when_there_are_environments", function () {
        Ajax.Updater = function (container, url, options) {
            container.update("<div class='selectors'>checkboxes here</div>");
            options.onComplete(xhr);
        };
        jQuery("#UUID_host5 .selector input").first().attr("checked", "checked");
        fire_event($("show_environments_panel"), 'click');
        assertFalse(jQuery("#environments_panel .add_panel")[0].hasClassName("hidden"));
        assertTrue(jQuery("#environments_panel .no_environments_error")[0].hasClassName("hidden"));
    });

    it("test_should_call_after_close_callback_handler", function () {
        var called = false;
        var handler = new MicroContentPopup.NoOpHandler();
        handler.after_close = function () {
            called = true;
        }
        var microContentPopup = new MicroContentPopup($('resources_panel'), handler);
        var microContentPopupShower = new MicroContentPopup.ClickShower(microContentPopup);
        var showButton = $('show_resources_panel');
        microContentPopupShower.bindShowButton(showButton);
        fire_event(showButton, "click")
        microContentPopup.close()
        assertTrue(called);
    });
});
