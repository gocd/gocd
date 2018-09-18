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

describe("word_break_inserter", function () {
    beforeEach(function () {
        setFixtures("<div id=\"wbr1\" class=\"abc wbrSensitive\">addpr</div>");
    });

    it("test_should_insert_wbr_to_content_of_wbr_sensitive_element_in_firefox", function () {
        Prototype.Browser.Gecko = true;
        new WordBreaker(2).insert();
        var content = $('wbr1').innerHTML.toLowerCase();
        assertEquals('ad<wbr>dp<wbr>r', content);
    });

    it("test_should_insert_wbr_to_content_of_wbr_sensitive_every_3_char_in_firefox", function () {
        Prototype.Browser.Gecko = true;
        new WordBreaker(3).insert();
        var content = $('wbr1').innerHTML.toLowerCase();
        assertEquals('add<wbr>pr', content);
    });

    it("test_should_insert_wbr_to_individual_element_in_firefox", function () {
        Prototype.Browser.Gecko = true;
        new WordBreaker(2).word_break($('wbr1'));
        var content = $('wbr1').innerHTML.toLowerCase();
        assertEquals('ad<wbr>dp<wbr>r', content);
    });

    xit("test_should_insert_shy_to_individual_element_not_in_firefox", function () {
        Prototype.Browser.Gecko = false;
        new WordBreaker(2).word_break($('wbr1'));
        var content = $('wbr1').innerHTML.toLowerCase().unescapeHTML();
        var specialchar = '&shy;'.unescapeHTML();
        assertEquals('ad' + specialchar + 'dp' + specialchar + 'r', content);
    });

    it("test_should_not_insert_wbr_when_wbr_already_exist", function () {
        Prototype.Browser.Gecko = true;
        var wbr = new WordBreaker(3);
        var expected = 'ad<wbr>dpr';
        $('wbr1').innerHTML = expected;
        wbr.insert();
        var content = $('wbr1').innerHTML.toLowerCase();
        assertEquals(expected, content);
    });
});


