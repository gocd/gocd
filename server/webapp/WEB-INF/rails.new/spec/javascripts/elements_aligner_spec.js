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

describe("elements_aligner", function(){
    beforeEach(function(){
        setFixtures("<div class='pipelines'>\n" +
            "    <div id=\"00\" class=\"pipeline t10 h20\">content blah blah blah</div>\n" +
            "    <div id=\"01\" class=\"pipeline t10 h10\">content blah blah blah</div>\n" +
            "    <div id=\"02\" class=\"pipeline t10 h30\">content blah blah blah</div>\n" +
            "    <div id=\"03\" class=\"pipeline t10 h50\">content blah blah blah</div>\n" +
            "    <div id=\"04\" class=\"pipeline t10 h40\">content blah blah blah</div>\n" +
            "\n" +
            "    <div id=\"10\" class=\"pipeline t20 h20\">content blah blah blah</div>\n" +
            "    <div id=\"11\" class=\"pipeline t20 h10\">content blah blah blah</div>\n" +
            "    <div id=\"12\" class=\"pipeline t20 h30\">content blah blah blah</div>\n" +
            "    <div id=\"13\" class=\"pipeline t20 h50\">content blah blah blah</div>\n" +
            "    <div id=\"14\" class=\"pipeline t20 h40\">content blah blah blah</div>\n" +
            "</div>\n" +
            "<div class='pipelines'>\n" +
            "    <div id=\"20\" class=\"pipeline t30 h20\">content blah blah blah</div>\n" +
            "    <div id=\"21\" class=\"pipeline t30 h10\">content blah blah blah</div>\n" +
            "    <div id=\"22\" class=\"pipeline t30 h30\">content blah blah blah</div>\n" +
            "    <div id=\"23\" class=\"pipeline t30 h50\">content blah blah blah</div>\n" +
            "    <div id=\"24\" class=\"pipeline t30 h40\">content blah blah blah</div>\n" +
            "\n" +
            "    <div id=\"40\" class=\"pipeline t40 h20\">content blah blah blah</div>\n" +
            "    <div id=\"41\" class=\"pipeline t40 h10\">content blah blah blah</div>\n" +
            "    <div id=\"42\" class=\"pipeline t40 h30\">content blah blah blah</div>\n" +
            "    <div id=\"43\" class=\"pipeline t40 h50\">content blah blah blah</div>\n" +
            "    <div id=\"44\" class=\"pipeline t40 h40\">content blah blah blah</div>\n" +
            "</div>");
    });

    afterEach(function(){
    });

    //todo:raghu to fix
    xit("ignored_test_should_align", function(){
        new ElementAligner().alignAll();
        for (var j = 0; j < 4; j++) {
            var baseEle = $j("#" + j + "0");
            for (var i = 1; i < 4; i++) {
                var curPipeline = $j("#" + j + i);
                assertEquals(baseEle.offset().top, curPipeline.offset().top);
                assertEquals(baseEle.height(), curPipeline.height());
            }
            var lastPipeline = $j("#" + j + 4);
            assertEquals(baseEle.offset().top, lastPipeline.offset().top);
            assertEquals(baseEle.height() + 1, lastPipeline.height());
        }
    });
});