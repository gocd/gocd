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

describe("value_stream_map", function () {
    beforeEach(function () {
        setFixtures("<div id=\"vsm-container\">\n" +
            "</div>");
    });
    var called;
    beforeEach(function () {
        jQuery('#vsm-container').html("");
        called = false;
    });

    mockRenderer = function () {
        mockRenderer.prototype.invoke = function (data) {
            called = true;
        }
    }

    it("testShouldRenderGraphIfDataDoesNotHaveError", function () {
        var node1 = '{"node_type":"MATERIAL","name":"../manual-testing/ant_hg/dummy","parents":[],"dependents":["p2"],"id":"hg_fingerprint"}';
        var node2 = '{"node_type":"PIPELINE","name":"p2","parents":["hg_fingerprint"],"dependents":[],"id":"p2"}]}';
        var vsm = eval('({"current_pipeline":"p2","levels":[{"nodes":[' + node1 + ']},{"nodes":[' + node2 + ']})');
        new VSM(vsm, "#vsm-container", new mockRenderer()).render();
        assertEquals(true, called);
    });

    it("testShouldRenderErrorMessageIfVSMHasErrors", function () {
        var unableToFind = '<div class="pagenotfound"><div class="biggest">:(</div><h3>' + _.escape("error message") + '</h3><span>Go to <a href="/go/pipelines">Pipelines</a></span></div>';
        var vsm = eval('({"error":"error message"})');
        new VSM(vsm, "#vsm-container", new mockRenderer()).render();
        assertEquals(false, called);
        assertEquals(unableToFind, jQuery("#vsm-container").html())
    });
});
