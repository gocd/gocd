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

describe("jquery_wizard_plugin", function(){
    beforeEach(function(){
        setFixtures("<div class=\"wizard tabs\" id=\"getting-started-01\">\n" +
            "    <ul class=\"navigation\">\n" +
            "        <li><a href=\"#step1\">Step 1</a></li>\n" +
            "        <li><a href=\"#step2\">Step 2</a></li>\n" +
            "        <li><a href=\"#step3\">Step 3</a></li>\n" +
            "    </ul>\n" +
            "\n" +
            "    <div class=\"steps\">\n" +
            "        <div id=\"step1\">\n" +
            "            <div class=\"content\">1. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Praesentium, vitae, est, impedit, quae repudiandae porro facilis excepturi facere quo consequatur error illum magni autem necessitatibus omnis cupiditate fugit eveniet molestias.</div>\n" +
            "            <div class=\"footer\">\n" +
            "                <button class=\"prev go-btn\"><span>Prev</span></button>\n" +
            "                <button class=\"next go-btn primary\"><span>Next</span></button>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "\n" +
            "        <div id=\"step2\">\n" +
            "            <div class=\"content\">2. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Praesentium, vitae, est, impedit, quae repudiandae porro facilis excepturi facere quo consequatur error illum magni autem necessitatibus omnis cupiditate fugit eveniet molestias.</div>\n" +
            "            <div class=\"footer\">\n" +
            "                <button class=\"prev go-btn\"><span>Prev</span></button>\n" +
            "                <button class=\"next go-btn primary\"><span>Next</span></button>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "\n" +
            "        <div id=\"step3\">\n" +
            "            <div class=\"content\">3. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Praesentium, vitae, est, impedit, quae repudiandae porro facilis excepturi facere quo consequatur error illum magni autem necessitatibus omnis cupiditate fugit eveniet molestias.</div>\n" +
            "            <div class=\"footer\">\n" +
            "                <button class=\"prev go-btn\"><span>Prev</span></button>\n" +
            "                <button class=\"next go-btn primary\"><span>Schedule pipeline to run</span></button>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "</div>\n" +
            "\n" +
            "<div class=\"wizard tabs\" id=\"getting-started-02\">\n" +
            "    <ul class=\"navigation\">\n" +
            "        <li><a href=\"#step4\">Step 4</a></li>\n" +
            "        <li><a href=\"#step5\">Step 5</a></li>\n" +
            "        <li><a href=\"#step6\">Step 6</a></li>\n" +
            "    </ul>\n" +
            "\n" +
            "    <div class=\"steps\">\n" +
            "        <div id=\"step4\">\n" +
            "            <div class=\"content\">4. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Praesentium, vitae, est, impedit, quae repudiandae porro facilis excepturi facere quo consequatur error illum magni autem necessitatibus omnis cupiditate fugit eveniet molestias.</div>\n" +
            "            <div class=\"footer\">\n" +
            "                <button class=\"prev go-btn\"><span>Prev</span></button>\n" +
            "                <button class=\"next go-btn primary\"><span>Next</span></button>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "\n" +
            "        <div id=\"step5\">\n" +
            "            <div class=\"content\">5. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Praesentium, vitae, est, impedit, quae repudiandae porro facilis excepturi facere quo consequatur error illum magni autem necessitatibus omnis cupiditate fugit eveniet molestias.</div>\n" +
            "            <div class=\"footer\">\n" +
            "                <button class=\"prev go-btn\"><span>Prev</span></button>\n" +
            "                <button class=\"next go-btn primary\"><span>Next</span></button>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "\n" +
            "        <div id=\"step6\">\n" +
            "            <div class=\"content\">6. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Praesentium, vitae, est, impedit, quae repudiandae porro facilis excepturi facere quo consequatur error illum magni autem necessitatibus omnis cupiditate fugit eveniet molestias.</div>\n" +
            "            <div class=\"footer\">\n" +
            "                <button class=\"prev go-btn\"><span>Prev</span></button>\n" +
            "                <button class=\"next go-btn primary\"><span>Schedule pipeline to run</span></button>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "</div>\n" +
            "\n" +
            "<div class=\"wizard\" id=\"not-a-wizard\">\n" +
            "\t<ul class=\"navigation\">\n" +
            "\t\t<li><a href=\"#step7\">Step 7</a></li>\n" +
            "\t\t<li><a href=\"#step8\">Step 8</a></li>\n" +
            "\t\t<li><a href=\"#step9\">Step 9</a></li>\n" +
            "\t</ul>\n" +
            "\t<div class=\"content\">\n" +
            "\t\t<div class=\"steps\">\n" +
            "\t\t\t<div id=\"step7\">7. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Praesentium, vitae, est, impedit, quae repudiandae porro facilis excepturi facere quo consequatur error illum magni autem necessitatibus omnis cupiditate fugit eveniet molestias.</div>\n" +
            "\t\t\t<div id=\"step8\">8. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Praesentium, vitae, est, impedit, quae repudiandae porro facilis excepturi facere quo consequatur error illum magni autem necessitatibus omnis cupiditate fugit eveniet molestias.</div>\n" +
            "\t\t\t<div id=\"step9\">9. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Praesentium, vitae, est, impedit, quae repudiandae porro facilis excepturi facere quo consequatur error illum magni autem necessitatibus omnis cupiditate fugit eveniet molestias.</div>\n" +
            "\t\t</div>\n" +
            "\t\t<div class=\"actions\">\n" +
            "\t\t\t<button class=\"prev go-btn\"><span>Prev</span></button>\n" +
            "\t\t\t<button class=\"next go-btn primary\"><span> </span></button>\n" +
            "\t\t</div>\n" +
            "\t</div>\n" +
            "</div>");
    });

    it("test_wizard_should_label_firststep_current", function() {
        jQuery('#getting-started-01 .steps > div').wizard();
        assertEquals('getting-started-01 does not have first step as current', true, jQuery('#getting-started-01 #step1').hasClass('current'));
        assertEquals('getting-started-02 has first step as current', false, jQuery('#getting-started-02 #step4').hasClass('current'));
        assertEquals('not-a-wizard does has first step as current', false, jQuery('#not-a-wizard #step7').hasClass('current'));
    });

    it("test_wizard_should_add_previous_next_actions", function() {
        jQuery('#getting-started-02 .steps > div').wizard();

        jQuery('#getting-started-02 #step4 .next').click();
        assertEquals('getting-started-02 does not have second step as current', false, jQuery('#getting-started-02 #step4').hasClass('current'));
        assertEquals('getting-started-02 does not have second step as current', true, jQuery('#getting-started-02 #step5').hasClass('current'));
        assertEquals('getting-started-02 does not have second step as current', false, jQuery('#getting-started-02 #step6').hasClass('current'));

        jQuery('#getting-started-02 #step5 .next').click();
        assertEquals('getting-started-02 does not have third step as current', false, jQuery('#getting-started-02 #step4').hasClass('current'));
        assertEquals('getting-started-02 does not have third step as current', false, jQuery('#getting-started-02 #step5').hasClass('current'));
        assertEquals('getting-started-02 does not have third step as current', true, jQuery('#getting-started-02 #step6').hasClass('current'));

        jQuery('#getting-started-02 #step6 .next').click();
        assertEquals('getting-started-02 tries to proceed even after the last step', false, jQuery('#getting-started-02 #step4').hasClass('current'));
        assertEquals('getting-started-02 tries to proceed even after the last step', false, jQuery('#getting-started-02 #step5').hasClass('current'));
        assertEquals('getting-started-02 tries to proceed even after the last step', true, jQuery('#getting-started-02 #step6').hasClass('current'));

        jQuery('#getting-started-02 #step6 .prev').click();
        assertEquals('getting-started-02 does not have second step as current', false, jQuery('#getting-started-02 #step4').hasClass('current'));
        assertEquals('getting-started-02 does not have second step as current', true, jQuery('#getting-started-02 #step5').hasClass('current'));
        assertEquals('getting-started-02 does not have second step as current', false, jQuery('#getting-started-02 #step6').hasClass('current'));

        jQuery('#getting-started-02 #step5 .prev').click();
        assertEquals('getting-started-02 does not have first step as current', true, jQuery('#getting-started-02 #step4').hasClass('current'));
        assertEquals('getting-started-02 does not have first step as current', false, jQuery('#getting-started-02 #step5').hasClass('current'));
        assertEquals('getting-started-02 does not have first step as current', false, jQuery('#getting-started-02 #step6').hasClass('current'));
    });

    xit("test_wizard_should_add_events_to_navigation_element", function() {
        //TODO
    });

    xit("test_wizard_should_reset_event_handlers_when_initialized_again", function() {
        //TODO:
    });

    xit("test_wizard_should_affect_only_elements_in_scope", function() {
        //TODO:
    });
});
