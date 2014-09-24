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

describe("helpers", function () {
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <div id=\"elem_id\">\n" +
            "        <a href=\"turner\">Turner's legacy</a>\n" +
            "    </div>\n" +
            "</div>");
    });

    it("test_should_replace_element_with_spinny_image", function () {
        spinny($('elem_id'));
        assertEquals($('elem_id').innerHTML, "&nbsp;");
        assertTrue($('elem_id').hasClassName('spinny'));
    });

    it("test_hide_show", function () {
        showElement($('elem_id'), true);
        assertTrue($('elem_id').visible());
        showElement($('elem_id'), false);
        assertFalse($('elem_id').visible());
    });
});
