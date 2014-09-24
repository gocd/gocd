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

describe("agent", function () {
    var agent;
    var agentPage;

    var originalStringProcess = String.prototype.process;
    beforeEach(function () {
        String.prototype.process = originalStringProcess;
        setFixtures("<div id=\"uuid\">\n" +
                "    <a title=\"Add Resources\" ></a>\n" +
                "    <ul class=\"resources\">\n" +
                "        <li><span>ajax</span>\n" +
                "            <a href=\"#\" title=\"Remove\">ss</a>\n" +
                "        </li>\n" +
                "        <li><span>php</span>\n" +
                "            <a href=\"#\" title=\"Remove\"></a>\n" +
                "        </li>\n" +
                "        <li><span>mysql</span>\n" +
                "            <a href=\"#\" title=\"Remove\"></a>\n" +
                "        </li>\n" +
                "        <div class=\"clear\"></div>\n" +
                "    </ul>\n" +
                "    <div class=\"clear\"></div>\n" +
                "    <div id=\"edit_resources_panel\" class=\"positioned-panel\" style=\"display: none;\">\n" +
                "            <span class=\"notes\">(separate multiple tags with commas)</span>\n" +
                "            <input id=\"agentId\" type=\"hidden\"/>\n" +
                "            <input id=\"resources\" class=\"field\"/>\n" +
                "            <div id=\"edit_resources_panel_error\" class=\"error\" style=\"display: ;\">only a-z, A-Z, 0-9, _, -, |, dot, whitespace is available in tag name.</div>\n" +
                "            <p>\n" +
                "                <input type=\"button\" id=\"editResources\" onclick=\"agentPage.addResources()\" value=\"Add resources\"/>\n" +
                "                <input type=\"button\" onclick=\"$('edit_resources_panel').hide()\" value=\"Close\"/>\n" +
                "            </p>\n" +
                "            <img id=\"edit_resources_panel_transport_spinner\" src=\"$req.getContextPath()/images/spinner.gif\" alt=\"transfering data\" style=\"display: none;\"/>\n" +
                "    </div>\n" +
                "    <textarea style=\"display: none;\" id=\"agent-template\">\n" +
                "        </textarea>\n" +
                "    <div id=\"agent-list\"></div>\n" +
                "</div>"
        );

        checker = new InputChecker('resources', 'edit_resources_panel_error');

        agentPage = new AgentPage();
        agent = new Agent('uuid');
        $("edit_resources_panel").hide();
        $("agentId").value = "";
    });

    afterEach(function () {
        checker = undefined;
        String.prototype.process = originalStringProcess;
    });

    it("test_should_get_resources", function () {
        assertEquals("ajax", agent.get_resources()[0]);
        assertEquals("php", agent.get_resources()[1]);
        assertEquals("mysql", agent.get_resources()[2]);
    });
    it("test_should_show_edit_resources_panel", function () {
        agent.get_resources = function () {
            return ["jdk1.4", "jdk1.5"]
        }
        agent.show_edit_resources_panel();
        assertTrue("edit_resources_panel should be visible.", $("edit_resources_panel").visible());
        assertEquals("", $("resources").value);
        assertEquals(agent.uuid, $("agentId").value);

    });
    it("test_should_remove_resource", function () {
        agent.get_resources = function () {
            return ["jdk1.4", "jdk1.5"]
        }
        var invoked = false;
        agent._remote_edit_resources = function (param) {
            assertEquals("jdk1.5", param["agentResources"]);
            invoked = true;
        }
        agent.remove_resource("jdk1.4");
    });
    it("test_should_append_user_input_after_current_resource", function () {
        agent.get_resources = function () {
            return ["jdk1.5"]
        }
        $("resources").value = "jdk1.6";
        var invoked = false;
        agent._remote_edit_resources = function (param) {
            invoked = true;
            assertEquals("jdk1.5,jdk1.6", param["agentResources"]);
        }
        agent.edit_resources();
        assertTrue("should invoke remote update", invoked);

    });
    it("test_should_update_resources_and_invoke_process_on_template", function () {
        var invoked = false;
        var json_expected = [agent_resources('uuid')];
        String.prototype.process = function (json_actual) {
            invoked = true;
            assertEquals(json_expected[0].agentId, json_actual.objs[0].agentId);
        }
        agent.edit_resources_on_success(json_expected);
        assertTrue("template engine should try to render json", invoked)

    });
});