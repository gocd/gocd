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

describe("value_stream_map_renderer", function () {
    beforeEach(function () {
        setFixtures("<div id=\"vsm-container\">\n" +
            "</div>");
    });

    xit("testShouldRenderGraphForASimplePipelineWithDotInTheName", function () {
        /*
         hg_fingerprint -> p2.dot -> p3.dot
         */
        var node1 = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p2.dot"]', 1, '[]');
        var node2 = pipelineNode("p2.dot", '["hg_fingerprint"]', '["p3.dot"]', 1, "", '[]');
        var node3 = pipelineNode("p3.dot", '["p2.dot"]', '[]', 1, "", '[]');
        var vsm = eval('({"current_pipeline":"p2.dot","levels":[{"nodes":[' + node1 + ']},{"nodes":[' + node2 + ']},{"nodes":[' + node3 + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);
        assertEquals("scm node is missing", 1, jQuery("#vsm-container #hg_fingerprint").length)
        assertEquals("pipeline node p2.dot is missing", 1, jQuery("#vsm-container #p2_id-dot").length)
        assertEquals("pipeline node p3.dot is missing", 1, jQuery("#vsm-container #p3_id-dot").length)
        assertEquals("invalid number of paths", 2, (jQuery("svg#svg").find('path').length))
        assertEquals("path missing from hg to p2", 'dependency hg_fingerprint p2_id-dot', jQuery("svg#svg .hg_fingerprint.p2_id-dot").attr('class'))

        var grid = new VsmGrid("#vsm-container");
        assertEquals("graph has overlap", true, grid.hasNoOverlap())
        assertEquals("incorrect node position", 'hg_fingerprint', grid.nodeIdAt(0, 0))
        assertEquals("incorrect node position", 'p2_id-dot', grid.nodeIdAt(1, 0))
        assertEquals("incorrect node position", 'p3_id-dot', grid.nodeIdAt(2, 0))

        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('hg_fingerprint', 'p2_id-dot'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('p2_id-dot', 'p3_id-dot'))

        // pinning
        jQuery("#p2_id-dot .pin").click(); // click on pin
        assertEquals("does not have edge highlighted - 1 - hg_fingerprint to p2\\.dot", true, grid.hasHighlightedEdgeBetween('hg_fingerprint', 'p2_id-dot'))
        assertEquals("does not have edge highlighted - 1 - p2\\.dot to p3\\.dot", true, grid.hasHighlightedEdgeBetween('p2_id-dot', 'p3_id-dot'))
        jQuery("#hg_fingerprint").click(); // remove focus from pinned pipeline
        assertEquals("does not have edge highlighted - 2 - hg_fingerprint to p2\\.dot", true, grid.hasHighlightedEdgeBetween('hg_fingerprint', 'p2_id-dot'))
        assertEquals("does not have edge highlighted - 2 - p2\\.dot to p3\\.dot", true, grid.hasHighlightedEdgeBetween('p2_id-dot', 'p3_id-dot'))

        assertIfItIsStartNode(grid.nodeIdAt(0, 0))
    });

    it("testCurrentPipelineShouldHaveHighlightingBackground", function () {
        var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p1"]', 1,
          '[{"modifications": [{"revision": "revision1","comment":"comment1","user":"user1","modified_time":"modified_time1"}, ' +
          '{"revision": "revision2","comment":"comment2","user":"user2","modified_time":"modified_time2"}]}]');
        var node_p1 = pipelineNode("p1", '["hg_fingerprint"]', '[]', 1, "", '[]');
        var vsm = eval('({"current_pipeline":"p1","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + node_p1 + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);

        assertEquals("Should have highlighting behind current pipeline", jQuery('#vsm-container .highlight').length, 1);
    });

    it("testVSMForCommitShouldNotHaveHighlightingBackground", function () {
        var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p1"]', 1,
          '[{"modifications": [{"revision": "revision1","comment":"comment1","user":"user1","modified_time":"modified_time1"}, ' +
          '{"revision": "revision2","comment":"comment2","user":"user2","modified_time":"modified_time2"}]}]');
        var node_p1 = pipelineNode("p1", '["hg_fingerprint"]', '[]', 1, "", '[]');
        var vsm = eval('({"current_material":"hg_fingerprint","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + node_p1 + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);

        assertEquals("Should not have highlighting anywhere", jQuery('#vsm-container .highlight').length, 0);
    });


    it("testShouldCheckIfPipelineHasRunMessageVisible", function () {

        var vsm = {"current_material": "sample", "levels": [
            {"nodes": [
                {"name": "Repository: [repo_url=file:///tmp/repo] - Package: [package_spec=go-agent]", "node_type": "PACKAGE", "depth": 1, "parents": [], "instances": [
                    {"modified_time": "5 months ago", "user": "anonymous", "comment": "{\"COMMENT\":\"Built on server.\",\"TRACKBACK_URL\":\"google.com\",\"TYPE\":\"PACKAGE_MATERIAL\"}", "revision": "go-agent-13.1.1-16714.noarch"}
                ], "locator": "", "id": "pkg_id", "dependents": ["sample"], "material_names": ["yum:go-agent"]}
            ]},
            {"nodes": [
                {"name": "sample", "node_type": "PIPELINE", "locator": "/go/tab/pipeline/history/sample", "instances": [
                    {"stages": [
                        {"locator": "/go/pipelines/sample/1/defaultStage/1", "status": "Building", "name": "defaultStage"}
                    ], "locator": "", "counter": 1, "label": "1"}
                ], "parents": ["pkg_id"], "depth": 1, "id": "sample", "dependents": []}
            ]}
        ]}
        new Graph_Renderer("#vsm-container").invoke(vsm);

        assertEquals("Should have message if pipeline has not run and waiting", jQuery('#vsm-container #sample .waiting').length, 1);
    });

    xit("testShouldRenderGraphForASimplePipeline", function () {
        /*
         hg_fingerprint -> p2
         */
        var node1 = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p2"]', 1, '[]');
        var node2 = pipelineNode("p2", '["hg_fingerprint"]', '[]', 1, "", '[]');
        var vsm = eval('({"current_pipeline":"p2","levels":[{"nodes":[' + node1 + ']},{"nodes":[' + node2 + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);
        assertEquals("scm node is missing", 1, jQuery("#vsm-container #hg_fingerprint").length)
        assertEquals("pipeline node p2 is missing", 1, jQuery("#vsm-container #p2").length)
        assertEquals("invalid number of paths", 1, (jQuery("svg#svg").find('path').length))
        assertEquals("path missing from hg to p2", 'dependency hg_fingerprint p2', jQuery("svg#svg .hg_fingerprint.p2").attr('class'))

        var grid = new VsmGrid("#vsm-container");
        assertEquals("graph has overlap", true, grid.hasNoOverlap())
        assertEquals("incorrect node position", 'hg_fingerprint', grid.nodeIdAt(0, 0))
        assertEquals("incorrect node position", 'p2', grid.nodeIdAt(1, 0))

        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('hg_fingerprint', 'p2'))

        assertIfItIsStartNode(grid.nodeIdAt(0, 0))
    });


    xit("testShouldRenderGraphForTriangleCrossLevelDependency", function () {
        /*  hg_fingerprint ---> p2 ---> p4
         |                   ^
         +-----------X-------+
         */
        var node1 = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p2","uuid"]', 1, '[]');
        var node2 = pipelineNode("p2", '["hg_fingerprint"]', '["p4"]', 1, "", '[]');
        var dummy = dummyNode("uuid", "dummy-uuid", '["hg_fingerprint"]', '["p4"]', 2);
        var node3 = pipelineNode("p4", '["p2","uuid"]', '[]', 1, "", '[]');
        var vsm = eval('({"current_pipeline":"p4","levels":[{"nodes":[' + node1 + ']},{"nodes":[' + node2 + ',' + dummy + ']},{"nodes":[' + node3 + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);

        hasNodeWithType("#hg_fingerprint", "material hg");
        hasNodeWithType("#p2", "PIPELINE");
        hasNodeWithType("#p4", "PIPELINE current");
        hasNodeWithType("#uuid", "DUMMY");
        assertEquals("invalid number of paths", 3, (jQuery("svg#svg").find('path').length))
        assertEquals("path missing from hg to p2", 'dependency hg_fingerprint p2', jQuery("svg#svg .hg_fingerprint.p2").attr('class'))
        assertEquals("path missing from hg_fingerprint to p4", 'dependency hg_fingerprint p4', jQuery("svg#svg .hg_fingerprint.p4").attr('class'))
        assertEquals("path missing from p2 to p4", 'dependency p2 p4', jQuery("svg#svg .p2.p4").attr('class'))

        var grid = new VsmGrid("#vsm-container");
        assertEquals("graph has overlap", true, grid.hasNoOverlap())
        assertEquals("incorrect node position", 'hg_fingerprint', grid.nodeIdAt(0, 0))
        assertEquals("incorrect node position", 'p2', grid.nodeIdAt(1, 0))
        assertEquals("incorrect node position", 'uuid', grid.nodeIdAt(1, 1))
        assertEquals("incorrect node position", 'p4', grid.nodeIdAt(2, 0))

        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('hg_fingerprint', 'p2'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('p2', 'p4'))
        // assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('hg_fingerprint', 'p4'))

        assertIfItIsStartNode(grid.nodeIdAt(0, 0))
    });


    xit("testShouldRenderGraphForADiamondDependency", function () {
        /* hg_fingerprint ---> p2 ---> p3
         |                  ^
         +--------> p1 -----+
         */
        var node1 = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p2","p1"]', 1, '[]');
        var node2 = pipelineNode("p2", '["hg_fingerprint"]', '["p3"]', 1, "", '[]');
        var node3 = pipelineNode("p1", '["hg_fingerprint"]', '["p3"]', 2, "", '[]');
        var node4 = pipelineNode("p3", '["p1", "p2"]', '[]', 1, "", '[]');
        var vsm = eval('({"current_pipeline":"p3","levels":[{"nodes":[' + node1 + ']},{"nodes":[' + node2 + ',' + node3 + ']},{"nodes":[' + node4 + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);
        assertEquals("scm node is missing", 1, jQuery("#vsm-container #hg_fingerprint").length)
        assertEquals("pipeline node p2 is missing", 1, jQuery("#vsm-container #p2").length)
        assertEquals("pipeline node p3 is missing", 1, jQuery("#vsm-container #p3").length)
        assertEquals("pipeline node p1 is missing", 1, jQuery("#vsm-container #p1").length)
        assertEquals("invalid number of paths", 4, (jQuery("svg#svg").find('path').length))
        assertEquals("path missing from hg to p2", 'dependency hg_fingerprint p2', jQuery("svg#svg .hg_fingerprint.p2").attr('class'))
        assertEquals("path missing from hg to p1", 'dependency hg_fingerprint p1', jQuery("svg#svg .hg_fingerprint.p1").attr('class'))
        assertEquals("path missing from p2 to p3", 'dependency p2 p3', jQuery("svg#svg .p2.p3").attr('class'))
        assertEquals("path missing from p1 to p3", 'dependency p1 p3', jQuery("svg#svg .p1.p3").attr('class'))

        var grid = new VsmGrid("#vsm-container");
        assertEquals("graph has overlap", true, grid.hasNoOverlap())
        assertEquals("incorrect node position", 'hg_fingerprint', grid.nodeIdAt(0, 0))
        assertEquals("incorrect node position", 'p2', grid.nodeIdAt(1, 0))
        assertEquals("incorrect node position", 'p1', grid.nodeIdAt(1, 1))
        assertEquals("incorrect node position", 'p3', grid.nodeIdAt(2, 0))

        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('hg_fingerprint', 'p2'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('hg_fingerprint', 'p1'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('p2', 'p3'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('p1', 'p3'))

        assertIfItIsStartNode(grid.nodeIdAt(0, 0))
    });


    xit("testShouldRenderNodesAtTheRightDepth", function () {
        /*                  g1----> p
         /  ^
         /   /
         hg1--->p1  /
         \    /
         \ /
         hg2-->p2

         */
        var node1 = scmMaterialNode('hg1', '../manual-testing/ant_hg/dummy', "hg", '["p1","p2"]', 2, '[]');
        var node2 = scmMaterialNode('hg2', '../manual-testing/ant_hg/second', "hg", '["p2"]', 3, '[]');
        var node3 = scmMaterialNode('g1', '../git_repo', "hg", '["p"]', 1, '[]');
        var node4 = pipelineNode("p1", '["hg1"]', '["p"]', 2, "", '[]');
        var node5 = pipelineNode("p2", '["hg1","hg2"]', '["p"]', 3, "", '[]');
        var node6 = pipelineNode("p", '["g1","p1", "p2"]', '[]', 1, "", '[]');


        var vsm = eval('({"current_pipeline":"p","levels":[{"nodes":[' + node1 + ',' + node2 + ']},{"nodes":[' + node3 + ',' + node4 + ',' + node5 + ']},{"nodes":[' + node6 + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);
        assertEquals("hg1 scm node is missing", 1, jQuery("#vsm-container #hg1").length)
        assertEquals("hg2 scm node is missing", 1, jQuery("#vsm-container #hg2").length)
        assertEquals("g1 scm node is missing", 1, jQuery("#vsm-container #g1").length)
        assertEquals("pipeline node p1 is missing", 1, jQuery("#vsm-container #p1").length)
        assertEquals("pipeline node p2 is missing", 1, jQuery("#vsm-container #p2").length)
        assertEquals("pipeline node p is missing", 1, jQuery("#vsm-container #p").length)
        assertEquals("invalid number of paths", 6, (jQuery("svg#svg").find('path').length))
        assertEquals("path missing from hg1 to p1", 'dependency hg1 p1', jQuery("svg#svg .hg1.p1").attr('class'))
        assertEquals("path missing from hg1 to p2", 'dependency hg1 p2', jQuery("svg#svg .hg1.p2").attr('class'))
        assertEquals("path missing from hg2 to p2", 'dependency hg2 p2', jQuery("svg#svg .hg2.p2").attr('class'))
        assertEquals("path missing from g1 to p", 'dependency g1 p', jQuery("svg#svg .g1.p").attr('class'))
        assertEquals("path missing from p1 to p", 'dependency p1 p', jQuery("svg#svg .p1.p").attr('class'))
        assertEquals("path missing from p2 to p", 'dependency p2 p', jQuery("svg#svg .p2.p").attr('class'))

        var grid = new VsmGrid("#vsm-container");
        assertEquals("graph has overlap", true, grid.hasNoOverlap())
        assertEquals("incorrect node position", 'hg1', grid.nodeIdAt(0, 1))
        assertEquals("incorrect node position", 'hg2', grid.nodeIdAt(0, 2))
        assertEquals("incorrect node position", 'g1', grid.nodeIdAt(1, 0))
        assertEquals("incorrect node position", 'p1', grid.nodeIdAt(1, 1))
        assertEquals("incorrect node position", 'p2', grid.nodeIdAt(1, 2))
        assertEquals("incorrect node position", 'p', grid.nodeIdAt(2, 0))

        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('hg1', 'p1'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('hg1', 'p2'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('hg2', 'p2'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('g1', 'p'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('p1', 'p'))
        assertEquals("does not have expected edge", true, grid.hasAnEdgeBetween('p2', 'p'))

        assertIfItIsStartNode(grid.nodeIdAt(0, 1))
        assertIfItIsStartNode(grid.nodeIdAt(0, 2))
    });


    it("testShouldRenderNodeWithRestrictions_WhenUserDoesNotHavePermissionToViewPipelineOrPipelineHasBeenDeleted", function () {
        /*
         hg_fingerprint -> deleted_pipeline -> no_view_permission -> current
         */

        var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["deleted_pipeline"]', 1, '[]');

        var deleted_pipeline_message = "Pipeline 'deleted_pipeline' has been deleted.";
        var deleted_pipeline = pipelineNodeWithRestriction("PIPELINE", "deleted_pipeline", '["hg_fingerprint"]', '["no_view_permission"]', 1, "DELETED",
            deleted_pipeline_message);

        var pipeline_no_permission_message = "You do not have view permissions for pipeline 'no_view_permission'.";
        var no_view_permission = pipelineNodeWithRestriction("PIPELINE", "no_view_permission", '["deleted_pipeline"]', '["current"]', 1, "NO_PERMISSION",
            pipeline_no_permission_message);

        var current = pipelineNode("current", '["no_view_permission"]', '[]', 1, "", '[]');

        var vsm = eval('({"current_pipeline":"current","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + deleted_pipeline + ']},{"nodes":[' + no_view_permission + ']},{"nodes":[' + current + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);

        assertEquals("pipeline without view permission is being shown.", true, jQuery("#vsm-container #no_view_permission h3").hasClass("restricted"));
        assertEquals("pipeline without view permission is being shown.", 0, jQuery("#vsm-container #no_view_permission h3").find("a").length);
        assertEquals("pipeline without view permission is being shown.", pipeline_no_permission_message, jQuery("#vsm-container #no_view_permission .message span").text());

        assertEquals("details of deleted pipeline are shown.", true, jQuery("#vsm-container #deleted_pipeline h3").hasClass("deleted"));
        assertEquals("details of deleted pipeline are shown.", 0, jQuery("#vsm-container #deleted_pipeline h3").find("a").length);
        assertEquals("details of deleted pipeline are shown.", deleted_pipeline_message, jQuery("#vsm-container #deleted_pipeline .message span").text());
    });

    if (window.navigator.userAgent.indexOf("MSIE")<=0) {
        it("testShouldDisplayAllDetailsForSCMMaterialNodes", function () {
            /*
             hg_fingerprint -> p1
             */

            var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p1"]', 1,
                '[{modifications:[{"revision": "revision1","comment":"comment1","user":"user1","modified_time":"modified_time1"}, ' +
                '{"revision": "revision2","comment":"comment2","user":"user2","modified_time":"modified_time2"}]}]');
            var node_p1 = pipelineNode("p1", '["hg_fingerprint"]', '[]', 1, "", '[]');

            var vsm = eval('({"current_pipeline":"p1","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + node_p1 + ']}]})');
            new Graph_Renderer("#vsm-container").invoke(vsm);

            assertEquals("material details are not populated correctly.", true, jQuery("#vsm-container #hg_fingerprint .material_revisions").hasClass("hg"));
            assertEquals("material details are not populated correctly.", 2, jQuery('ul[data-materialname="hg_fingerprint"] li.instance').length);

            /*
             * material url
             */
            assertEquals("material url is not populated correctly.", "../manual-testing/ant_hg/dummy", jQuery("#hg_fingerprint .material_type").html());

            /*
             * material image
             */
            var boundingRectOfMaterialNode = jQuery("#hg_fingerprint")[0].getBoundingClientRect();
            var boundingRectOfMaterialImageNode = jQuery("#hg_fingerprint .material_type")[0].getBoundingClientRect();
            var centerOfNode = boundingRectOfMaterialNode.left + (boundingRectOfMaterialNode.width / 2);
            var centerOfImage = boundingRectOfMaterialImageNode.left + (boundingRectOfMaterialImageNode.width / 2);
            assertEquals("material image should be positioned at center of node", true, Math.abs(centerOfNode - centerOfImage) < 5);


            /*
             * hide/show revisions
             */
            assertEquals("revision details should be hidden by default", false, jQuery(".instances[data-materialname='hg_fingerprint']").is(':visible'));
            jQuery(jQuery("#hg_fingerprint .more")).trigger('click');
            assertEquals("revision details should be on click of more", true, jQuery(".instances[data-materialname='hg_fingerprint']").is(':visible'));

            /*
             first commit
             */
            assertEquals("first revision is not populated correctly.", "revision1", jQuery('ul[data-materialname="hg_fingerprint"] li.instance').eq('0').find('div').eq('0').text().trim());
            assertEquals("first comment is not populated correctly.", "comment1", jQuery('ul[data-materialname="hg_fingerprint"] li.instance').eq('0').find('div').eq('1').text().trim());
            assertEquals("first user is not populated correctly.", "user1", jQuery('ul[data-materialname="hg_fingerprint"] li.instance').eq('0').find('div').eq('2').find('p').eq('0').text().trim());
            assertEquals("first modified_time is populated correctly.", "modified_time1", jQuery('ul[data-materialname="hg_fingerprint"] li.instance').eq('0').find('div').eq('2').find('p').eq('1').text());
            /*
             second commit
             */
            assertEquals("second revision is not populated correctly.", "revision2", jQuery('ul[data-materialname="hg_fingerprint"] li.instance').eq('1').find('div').eq('0').text().trim());
            assertEquals("second comment is not populated correctly.", "comment2", jQuery('ul[data-materialname="hg_fingerprint"] li.instance').eq('1').find('div').eq('1').text().trim());
            assertEquals("second user is not populated correctly.", "user2", jQuery('ul[data-materialname="hg_fingerprint"] li.instance').eq('1').find('div').eq('2').find('p').eq('0').text().trim());
            assertEquals("second modified_time is populated correctly.", "modified_time2", jQuery('ul[data-materialname="hg_fingerprint"] li.instance').eq('1').find('div').eq('2').find('p').eq('1').text());
        });
    }

    it("testShouldCommitDetailsForPackageMaterial", function () {
        var vsm = {"current_pipeline": "sample", "levels": [
            {"nodes": [
                {"name": "Repository: [repo_url=file:///tmp/repo] - Package: [package_spec=go-agent]", "node_type": "PACKAGE", "depth": 1, "parents": [], "material_revisions": [
                    {"modifications": [{"modified_time": "5 months ago", "user": "anonymous", "comment": "{\"COMMENT\":\"Built on server.\",\"TRACKBACK_URL\":\"google.com\",\"TYPE\":\"PACKAGE_MATERIAL\"}", "revision": "go-agent-13.1.1-16714.noarch"}]}
                ], "locator": "", "id": "pkg_id", "dependents": ["sample"], "material_names": ["yum:go-agent"]}
            ]},
            {"nodes": [
                {"name": "sample", "node_type": "PIPELINE", "locator": "/go/tab/pipeline/history/sample", "instances": [
                    {"stages": [
                        {"locator": "/go/pipelines/sample/1/defaultStage/1", "status": "Building", "name": "defaultStage"}
                    ], "locator": "/go/pipelines/value_stream_map/sample/1", "counter": 1, "label": "1"}
                ], "parents": ["pkg_id"], "depth": 1, "id": "sample", "dependents": []}
            ]}
        ]}
        new Graph_Renderer("#vsm-container").invoke(vsm);
        var boundingRectOfMaterialNode = jQuery("#pkg_id")[0].getBoundingClientRect();
        var boundingRectOfMaterialImageNode = jQuery("#pkg_id .material_type")[0].getBoundingClientRect();
        var centerOfNode = boundingRectOfMaterialNode.left + (boundingRectOfMaterialNode.width / 2);
        var centerOfImage = boundingRectOfMaterialImageNode.left + (boundingRectOfMaterialImageNode.width / 2);

        assertEquals("first revision is not populated correctly.", "go-agent-13.1.1-16714.noarch", jQuery('ul[data-materialname="pkg_id"] li.instance').eq('0').find('div').eq('0').text().trim());
        assertEquals("first comment is not populated correctly.", 'Built on server.<br>Trackback: <a href="google.com">google.com</a>',
            jQuery('ul[data-materialname="pkg_id"] li.instance').eq('0').find('div').eq('1').html());
        assertEquals("Brief comment is not correct", 'Built on server.<br>Trackback: <a href="google.com">google.com</a>',
            jQuery('#pkg_id .material_revisions_label').html());
        assertEquals("Brief comment is not correct", 'Built on server.\nTrackback: google.com',
            jQuery('#pkg_id .material_revisions_label').attr("title"));
    });


    it("testShouldShowTrackbackUrlAsNotProvidedWhenItIsEmpty", function () {
        var vsm = {"current_pipeline": "sample", "levels": [
            {"nodes": [
                {"name": "Repository: [repo_url=file:///tmp/repo] - Package: [package_spec=go-agent]", "node_type": "PACKAGE", "depth": 1, "parents": [], "material_revisions": [
                    {"modifications": [{"modified_time": "5 months ago", "user": "anonymous", "comment": "{\"TYPE\":\"PACKAGE_MATERIAL\"}", "revision": "go-agent-13.1.1-16714.noarch"}]}
                ], "locator": "", "id": "pkg_id", "dependents": ["sample"], "material_names": ["yum:go-agent"]}
            ]},
            {"nodes": [
                {"name": "sample", "node_type": "PIPELINE", "locator": "/go/tab/pipeline/history/sample", "instances": [
                    {"stages": [
                        {"locator": "/go/pipelines/sample/1/defaultStage/1", "status": "Building", "name": "defaultStage"}
                    ], "locator": "/go/pipelines/value_stream_map/sample/1", "counter": 1, "label": "1"}
                ], "parents": ["pkg_id"], "depth": 1, "id": "sample", "dependents": []}
            ]}
        ]}
        new Graph_Renderer("#vsm-container").invoke(vsm);
        var boundingRectOfMaterialNode = jQuery("#pkg_id")[0].getBoundingClientRect();
        var boundingRectOfMaterialImageNode = jQuery("#pkg_id .material_type")[0].getBoundingClientRect();
        var centerOfNode = boundingRectOfMaterialNode.left + (boundingRectOfMaterialNode.width / 2);
        var centerOfImage = boundingRectOfMaterialImageNode.left + (boundingRectOfMaterialImageNode.width / 2);

        assertEquals("first revision is not populated correctly.", "go-agent-13.1.1-16714.noarch", jQuery('ul[data-materialname="pkg_id"] li.instance').eq('0').find('div').eq('0').text().trim());
        assertEquals("first comment is not populated correctly.", 'Trackback: Not Provided',
            jQuery('ul[data-materialname="pkg_id"] li.instance').eq('0').find('div').eq('1').html());
        assertEquals("Brief comment is not correct", 'Trackback: Not Provided',
            jQuery('#pkg_id .material_revisions_label').html());
        assertEquals("Brief comment is not correct", 'Trackback: Not Provided',
            jQuery('#pkg_id .material_revisions_label').attr("title"));
    });

    if (window.navigator.userAgent.indexOf("MSIE")<=0) {
        it("testShouldCheckIfCommentsBoxIsShownCorrectlyIfTwoOrMoreSameSVNorTFSorP4IsConfiguredWithDifferentCredentials", function () {
            var svn_material_1 = scmMaterialNode('svn_fingerprint_1', 'http://username1:password1@svn.com', "svn", '["p1"]', 1,
                '[{"modifications":[{"revision": "revision1","comment":"comment1","user":"user1","modified_time":"modified_time1"}, ' +
                '{"revision": "revision2","comment":"comment2","user":"user2","modified_time":"modified_time2"}]}]');
            var svn_material_2 = scmMaterialNode('svn_fingerprint_2', 'http://username2:password2@svn.com', "svn", '["p1"]', 1,
                '[{"modifications":[{"revision": "revision1","comment":"comment1","user":"user1","modified_time":"modified_time1"}, ' +
                '{"revision": "revision2","comment":"comment2","user":"user2","modified_time":"modified_time2"}]}]');
            var node_p1 = pipelineNode("p1", '["svn_fingerprint_1", "svn_fingerprint_2"]', '[]', 1, "", '[]');

            var vsm = eval('({"current_pipeline":"p1","levels":[{"nodes":[' + svn_material_1 + ',' + svn_material_2 + ']},{"nodes":[' + node_p1 + ']}]})');
            new Graph_Renderer("#vsm-container").invoke(vsm);

            assertEquals("revision details should be hidden by default", false, jQuery(".instances[data-materialname='svn_fingerprint_1']").is(':visible'));
            assertEquals("revision details should be hidden by default", false, jQuery(".instances[data-materialname='svn_fingerprint_2']").is(':visible'));
            jQuery(jQuery("#svn_fingerprint_1 .more")).trigger('click');
            jQuery(jQuery("#svn_fingerprint_2 .more")).trigger('click');
            assertEquals("revision details should be on click of more", true, jQuery(".instances[data-materialname='svn_fingerprint_1']").is(':visible'));
            assertEquals("revision details should be on click of more", true, jQuery(".instances[data-materialname='svn_fingerprint_2']").is(':visible'));
        });
    }

    it("testShouldDisplayAllDetailsForPipelineNodes", function () {
        /*
         hg_fingerprint -> current -> downstream
         */

        var current_pipeline_instance_details = '{"stages": [{"locator": "/go/pipelines/current/1/defaultStage/1","status": "Passed","name": "defaultStage"},' +
            '{"locator": "","status": "Unknown","name": "oneMore"}],"locator": "/go/pipelines/value_stream_map/current/1","counter": 1,"label": "1" }';

        var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["current"]', 1,
          '[{"modifications":[{"revision": "revision1"}, {"revision": "revision2"}]}]');
        var current = pipelineNode("current", '["hg_fingerprint"]', '[]', 1, "/go/tab/pipeline/history/current", '[' + current_pipeline_instance_details + ']');

        var vsm = eval('({"current_pipeline":"p1","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + current + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);

        assertEquals("pipeline node name does not point to pipeline history page.", "/go/tab/pipeline/history/current", jQuery("#vsm-container #current h3 a").attr("href"));
        assertEquals("label of a pipeline instance does not point to th vsm page.", "/go/pipelines/value_stream_map/current/1", jQuery("#vsm-container #current ul li h4 a").attr("href"));
        assertEquals("pipeline node does not have all instances populated correctly.", 1, jQuery("#vsm-container #current ul").find("li.instance").length);
        assertEquals("stage details for pipeline instances are not populated correctly.", 2, jQuery("#vsm-container #current ul ul").find(".stage_bar").length);
        assertEquals("stage details for pipeline instances are not populated correctly.", "/go/pipelines/current/1/defaultStage/1",
            jQuery("#vsm-container #current ul ul li.stage_bar.Passed a").attr("href"));
        assertEquals("stage details for pipeline instances are not populated correctly.", 1, jQuery("#vsm-container #current ul ul li.stage_bar.Unknown > span").length);
    });


    it("testShouldCheckIfOneMoreAndOneLessComeUpForMultiplePipelineInstances", function () {
        /*
         hg_fingerprint -> current -> downstream (twice)
         */

        var current_pipeline_instance_details = '{"stages": [{"locator": "/go/pipelines/current/1/defaultStage/1","status": "Passed","name": "defaultStage"}],' +
            '"locator": "/go/pipelines/value_stream_map/current/1","counter": 1,"label": "1" }';
        var downstream_pipeline_instance_details = '{"stages": [{"locator": "/go/pipelines/downstream/1/defaultStage/1","status": "Passed","name": "defaultStage"}],' +
            ' "locator": "/go/pipelines/value_stream_map/downstream/1","counter": 1,"label": "1" },' + '{"stages": [{"locator": "/go/pipelines/downstream/2/defaultStage/1","status": "Passed","name": "defaultStage"}],' +
            ' "locator": "/go/pipelines/downstream/current/2","counter": 2,"label": "2" }';

        var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["current"]', 1,
          '[{"modifications":[{"revision": "revision1"}, {"revision": "revision2"}]}]');
        var current = pipelineNode("current", '["hg_fingerprint"]', '["downstream"]', 1, "/go/tab/pipeline/history/current", '[' + current_pipeline_instance_details + ']');
        var downstream = pipelineNode("downstream", '["current"]', '[]', 2, "/go/tab/pipeline/history/downstream", '[' + downstream_pipeline_instance_details + ']');

        var vsm = eval('({"current_pipeline":"current","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + current + ']},{"nodes":[' + downstream + ']}]})');
        new Graph_Renderer("#vsm-container").invoke(vsm);

        assertEquals("pipeline should show all instance details.", "1 more...", jQuery("#downstream .show-more").find("a").text());
        jQuery(jQuery("#downstream .show-more a")).trigger('click');
        assertEquals("pipeline should show all instance details.", "1 less...", jQuery("#downstream .show-more").find("a").text());
    });


    function assertIfItIsStartNode(nodeId) {
        var nodeIdSelector = "#" + nodeId;
        var nodeHeight = jQuery(nodeIdSelector).css("height").replace(/px/, "");
        var nodeWidth = jQuery(nodeIdSelector).css("width").replace(/px/, "");
        var nodeBorderRadius = jQuery(nodeIdSelector).css("border-bottom-left-radius").replace(/px/, "");
        var nodeBorderRadiusBottomRight = jQuery(nodeIdSelector).css("border-bottom-right-radius").replace(/px/, "");
        var nodeBorderRadiusTopLeft = jQuery(nodeIdSelector).css("border-top-left-radius").replace(/px/, "");
        var nodeBorderRadiusTopRight = jQuery(nodeIdSelector).css("border-top-right-radius").replace(/px/, "");

        assertEquals("border radius should be equal across all edges", nodeBorderRadius, nodeBorderRadiusBottomRight);
        assertEquals("border radius should be equal across all edges", nodeBorderRadius, nodeBorderRadiusTopLeft);
        assertEquals("border radius should be equal across all edges", nodeBorderRadius, nodeBorderRadiusTopRight);

        assertEquals("height and width of scm node should be same", 0, (nodeHeight - nodeWidth));

        var expectedBorderRadius = ((nodeHeight / 2) + 10);
        assertEquals("border radius should be more than 50% of height/width", true, Math.round(expectedBorderRadius - nodeBorderRadius) < 1);
    }


    function dummyNode(nodeId, nodeName, parents, dependents, depth) {
        return'{"node_type":"' + "DUMMY" + '","name":"' + nodeName + '","parents":' + parents + ',"dependents":' + dependents +
            ',"id":"' + nodeId + '", "depth":' + depth + ', "locator":' + '""' + '}';
    }

    function scmMaterialNode(nodeId, nodeName, type, dependents, depth, material_revisions) {
        return '{"node_type":"' + type + '","name":"' + nodeName + '","parents":' + '[]' + ',"dependents":' + dependents +
            ',"id":"' + nodeId + '", "depth":' + depth + ', "locator":' + '""' + ', "material_revisions":' + material_revisions + '}';
    }

    function pipelineNode(nodeId, parents, dependents, depth, locator, instances) {
        return '{"node_type":"' + "PIPELINE" + '","name":"' + nodeId + '","parents":' + parents + ',"dependents":' + dependents +
            ',"id":"' + nodeId + '", "depth":' + depth + ', "locator":"' + locator + '", "instances":' + instances + '}';
    }

    function pipelineNodeWithRestriction(type, nodeName, parents, dependents, depth, view_type, message) {
        return '{"node_type":"' + type + '","name":"' + nodeName + '","parents":' + parents + ',"dependents":' + dependents +
            ',"id":"' + nodeName + '", "depth":' + depth + ', "locator":' + '""' + ', "view_type":"' + view_type + '", "message":"' + message + '"}';
    }

    function hasNodeWithType(nodeId, entity_type) {
        var node = jQuery("#vsm-container " + nodeId);
        assertEquals("Missing node : " + nodeId, 1, node.length)
        assertEquals("Missing node entity_type : " + entity_type, "vsm-entity " + entity_type.toLowerCase(), node.attr('class'))
    }

    VsmNode = function () {
        var id;
        var nodePosition;
        var level;
        var depth;
        var type;

        this.getInfo = function () {
            return 'id: ' + this.id + ' left: ' + this.nodePosition.left + ' width: ' + this.nodePosition.width + ' top: ' + this.nodePosition.top + ' height: ' + this.nodePosition.height + ' level: ' + this.level + ' depth: ' + this.depth + ' type:' + this.type;
        }
    };

    VsmGrid = function (container) {
        var allNodes = [];
        var nodePositions = {};
        var levelVersusNodes = [];

        var svgPosition = jQuery("div#vsm-container")[0].getBoundingClientRect();
        var verticalDistanceBetweenNodesAtSameLevel = 50;
        var borderSize = 0;
        var verticalOffsetFromTop = 20 + borderSize;

        var horizontalDistanceBetweenLevels = 75;
        var horizontalOffsetFromLeft = 20 + borderSize;

        var nodeWidth = 200;
        var nodeHeight = 100;

        function init() {
            var nodes = jQuery(container).find('div.vsm-entity');

            jQuery.each(nodes, function (i, node) {
                nodeObj = new VsmNode();
                nodeObj.id = $(this).id;

                nodeObj.type = (jQuery(node).hasClass('material')) ?
                    'material' : ((jQuery(node).hasClass('current')) ?
                    'current-pipeline' :
                    'pipeline');

                nodeObj.nodePosition = $(this).getBoundingClientRect();

                if (nodeObj.type == 'current-pipeline') {
                    nodeObj.level = Math.round((nodeObj.nodePosition.left - svgPosition.left - horizontalOffsetFromLeft + 25) / (nodeWidth + horizontalDistanceBetweenLevels));
                    nodeObj.depth = Math.round((nodeObj.nodePosition.top - svgPosition.top - verticalOffsetFromTop + 25) / (nodeHeight + verticalDistanceBetweenNodesAtSameLevel));
                } else {
                    nodeObj.level = Math.round((nodeObj.nodePosition.left - svgPosition.left - horizontalOffsetFromLeft) / (nodeWidth + horizontalDistanceBetweenLevels));
                    nodeObj.depth = Math.round((nodeObj.nodePosition.top - svgPosition.top - verticalOffsetFromTop) / (nodeHeight + verticalDistanceBetweenNodesAtSameLevel));
                }


                allNodes.push(nodeObj);

                if (typeof levelVersusNodes[nodeObj.level] === "undefined") {
                    levelVersusNodes[nodeObj.level] = [];
                }
                levelVersusNodes[nodeObj.level].push(nodeObj);
            });
        }

        var nodeAt = function (level, depth) {
            for (var i = 0; i < allNodes.length; i++) {
                var node = allNodes[i];
                if (node.level == level && node.depth == depth) {
                    return node;
                }
            }
            return null;
        };

        this.getInfo = function () {
            var str = '';
            for (var i = 0; i < allNodes.length; i++) {
                str += allNodes[i].getInfo() + '\n';
            }
            return str;
        };

        this.nodeIdAt = function (level, depth) {
            var node = nodeAt(level, depth);
            if (node) {
                return node.id;
            }
            return null;
        };

        this.hasNoOverlap = function () {
            var expectedHeight;
            var expectedWidth;

            for (i = 0; i < allNodes.length; i++) {
                if (allNodes[i].type == 'pipeline') {
                    expectedHeight = allNodes[i].nodePosition.height;
                    expectedWidth = allNodes[i].nodePosition.width;
                    i = allNodes.length; // exit loop
                }
            }

            //make sure every node has same height & width
            for (var i = 0; i < allNodes.length; i++) {
                var nodePosition = allNodes[i].nodePosition;
                var node = allNodes[i];
                if (node.type == 'pipeline') {
                    if (nodePosition.height != expectedHeight || nodePosition.width != expectedWidth) {
                        return false;
                    }
                }
            }

            // check there is enough gap between levels
            for (var level = 0; level < levelVersusNodes.length - 1; level++) {
                var nodesAtCurrentLevel = levelVersusNodes[level];
                var nodesAtNextLevel = levelVersusNodes[level + 1];

                var allNodesHaveSameLeftPosition = function (nodes) {
                    var firstNode = nodes[0];
                    if (firstNode.type != 'current-pipeline') {
                        for (var i = 1; i < nodes.length; i++) {
                            if (firstNode.nodePosition.right != nodes[i].nodePosition.right) {
                                return false;
                            }
                        }
                    }
                    return true;
                };

                if (!allNodesHaveSameLeftPosition(nodesAtCurrentLevel) || !allNodesHaveSameLeftPosition(nodesAtNextLevel) ||
                    (nodesAtCurrentLevel[0].nodePosition.right + 20) > (nodesAtNextLevel[0].nodePosition.left)) {
                    return false;
                }
            }

            // for each level - check there is enough gap between depths
            for (var level = 0; level < levelVersusNodes.length - 1; level++) {
                for (var i = 0; i < levelVersusNodes[level].length - 1; i++) {
                    if ((levelVersusNodes[level][i].nodePosition.bottom) > (levelVersusNodes[level][i + 1].nodePosition.top)) {
                        return false;
                    }
                }
            }


            return true;
        };

        this.hasAnEdgeBetween = function (fromNodeId, toNodeId) {
            var nodePositionFromNode = jQuery("#" + fromNodeId)[0].getBoundingClientRect();
            var nodePositionToNode = jQuery("#" + toNodeId)[0].getBoundingClientRect();
            var classOfSvg = "." + fromNodeId + "." + toNodeId;
            var nodePositionSvgArrow = jQuery(classOfSvg)[0].getBoundingClientRect();
            // incident on from node
            if (Math.abs(nodePositionFromNode.right - nodePositionSvgArrow.left) > 15) {
                alert(Math.abs(nodePositionFromNode.right) + ':' + Math.abs(nodePositionSvgArrow.left) + ':' + classOfSvg);
                return false;
            }
            //incident on to node
            if (Math.abs(nodePositionSvgArrow.right - nodePositionToNode.left) > 10) {
                return false;
            }
            // is between from node
            if ((isBetween(nodePositionFromNode.top, nodePositionSvgArrow.top, nodePositionFromNode.bottom))) {
                if (!(isBetween(nodePositionToNode.top, nodePositionSvgArrow.bottom, nodePositionToNode.bottom))) {
                    return false;
                }
            }
            else if ((isBetween(nodePositionFromNode.top, nodePositionSvgArrow.bottom, nodePositionFromNode.bottom))) {
                if (!(isBetween(nodePositionToNode.top, nodePositionSvgArrow.top, nodePositionToNode.bottom))) {
                    return false;
                }
            }
            else {
                return false;
            }
            return true;
        };

        this.hasHighlightedEdgeBetween = function (fromNodeId, toNodeId) {
            var classOfSvg = "." + fromNodeId + "." + toNodeId + ".pinned";
            var nodePositionSvgArrow = jQuery(classOfSvg);
            if (nodePositionSvgArrow && nodePositionSvgArrow.length > 0) {
                return true;
            }
            return false;
        };

        function isBetween(left, middle, right) {
            if (left < middle && middle < right) {
                return true;
            }
            return false;
        }

        init();
    };
});