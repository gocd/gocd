/*
 * Copyright 2024 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
describe("value_stream_map_renderer", function () {
  beforeEach(function () {
    setFixtures(`<div id="vsm-container"></div>`);
  });

  it("testCurrentPipelineShouldHaveHighlightingBackground", function () {
    var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p1"]', 1,
      '[{"modifications": [{"revision": "revision1","comment":"comment1","user":"user1","modified_time":"modified_time1"}, ' +
      '{"revision": "revision2","comment":"comment2","user":"user2","modified_time":"modified_time2"}]}]');
    var node_p1 = pipelineNode("p1", '["hg_fingerprint"]', '[]', 1, "", '[]');
    var vsm = eval('({"current_pipeline":"p1","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + node_p1 + ']}]})');
    new Graph_Renderer("#vsm-container").invoke(vsm);

    expect(1).toBe($('#vsm-container .highlight').length);
  });

  it("testShouldCheckIfPipelineHasRunMessageVisible", function () {

    var vsm = {
      "current_material": "sample", "levels": [
        {
          "nodes": [
            {
              "name": "Repository: [repo_url=file:///tmp/repo] - Package: [package_spec=go-agent]",
              "node_type": "PACKAGE",
              "depth": 1,
              "parents": [],
              "instances": [
                {
                  "modified_time": "5 months ago",
                  "user": "anonymous",
                  "comment": "{\"COMMENT\":\"Built on server.\",\"TRACKBACK_URL\":\"google.com\",\"TYPE\":\"PACKAGE_MATERIAL\"}",
                  "revision": "go-agent-13.1.1-16714.noarch"
                }
              ],
              "locator": "",
              "id": "pkg_id",
              "dependents": ["sample"],
              "material_names": ["yum:go-agent"]
            }
          ]
        },
        {
          "nodes": [
            {
              "name": "sample", "node_type": "PIPELINE", "locator": "/go/pipeline/activity/sample", "instances": [
                {
                  "stages": [
                    {"locator": "/go/pipelines/sample/1/defaultStage/1", "status": "Building", "name": "defaultStage"}
                  ], "locator": "", "counter": 1, "label": "1"
                }
              ], "parents": ["pkg_id"], "depth": 1, "id": "sample", "dependents": []
            }
          ]
        }
      ]
    };
    new Graph_Renderer("#vsm-container").invoke(vsm);

    expect(1).toBe($('#vsm-container #sample .waiting').length);
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

    expect($("#vsm-container #no_view_permission h3").hasClass("restricted")).toBe(true);
    expect($("#vsm-container #no_view_permission h3").find("a").length).toBe(0);
    expect($("#vsm-container #no_view_permission .message span").text()).toBe(pipeline_no_permission_message);

    expect($("#vsm-container #deleted_pipeline h3").hasClass("deleted")).toBe(true);
    expect($("#vsm-container #deleted_pipeline h3").find("a").length).toBe(0);
    expect($("#vsm-container #deleted_pipeline .message span").text()).toBe(deleted_pipeline_message);
  });

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

    expect($("#vsm-container #hg_fingerprint .material_revisions").hasClass("hg")).toBe(true);
    expect($('ul[data-materialname="hg_fingerprint"] li.instance').length).toBe(2);

    /*
     * material url
     */
    expect($("#hg_fingerprint .material_type").html()).toBe("../manual-testing/ant_hg/dummy");

    /*
     * material image
     */
    var boundingRectOfMaterialNode = $("#hg_fingerprint")[0].getBoundingClientRect();
    var boundingRectOfMaterialImageNode = $("#hg_fingerprint .material_type")[0].getBoundingClientRect();
    var centerOfNode = boundingRectOfMaterialNode.left + (boundingRectOfMaterialNode.width / 2);
    var centerOfImage = boundingRectOfMaterialImageNode.left + (boundingRectOfMaterialImageNode.width / 2);
    expect(Math.abs(centerOfNode - centerOfImage) < 5).toBe(true);


    /*
     * hide/show revisions
     */
    expect($(".instances[data-materialname='hg_fingerprint']").is(':visible')).toBe(false);
    $("#hg_fingerprint .more").trigger('click');
    expect($(".instances[data-materialname='hg_fingerprint']").is(':visible')).toBe(true);

    /*
     first commit
     */
    expect($('ul[data-materialname="hg_fingerprint"] li.instance').eq('0').find('div').eq('0').text().trim()).toBe("Revision: revision1");
    expect($('ul[data-materialname="hg_fingerprint"] li.instance').eq('0').find('div').eq('1').text().trim()).toBe("comment1");
    expect($('ul[data-materialname="hg_fingerprint"] li.instance').eq('0').find('div').eq('2').find('p').eq('0').text().trim()).toBe("user1");
    expect($('ul[data-materialname="hg_fingerprint"] li.instance').eq('0').find('div').eq('2').find('p').eq('1').text()).toBe("modified_time1");
    /*
     second commit
     */
    expect($('ul[data-materialname="hg_fingerprint"] li.instance').eq('1').find('div').eq('0').text().trim()).toBe("Revision: revision2");
    expect($('ul[data-materialname="hg_fingerprint"] li.instance').eq('1').find('div').eq('1').text().trim()).toBe("comment2");
    expect($('ul[data-materialname="hg_fingerprint"] li.instance').eq('1').find('div').eq('2').find('p').eq('0').text().trim()).toBe("user2");
    expect($('ul[data-materialname="hg_fingerprint"] li.instance').eq('1').find('div').eq('2').find('p').eq('1').text()).toBe("modified_time2");
  });

  it("testShouldCommitDetailsForPackageMaterial", function () {
    var vsm = {
      "current_pipeline": "sample", "levels": [
        {
          "nodes": [
            {
              "name": "Repository: [repo_url=file:///tmp/repo] - Package: [package_spec=go-agent]",
              "node_type": "PACKAGE",
              "depth": 1,
              "parents": [],
              "material_revisions": [
                {
                  "modifications": [{
                    "modified_time": "5 months ago",
                    "user": "anonymous",
                    "comment": "{\"COMMENT\":\"Built on server.\",\"TRACKBACK_URL\":\"google.com\",\"TYPE\":\"PACKAGE_MATERIAL\"}",
                    "revision": "go-agent-13.1.1-16714.noarch"
                  }]
                }
              ],
              "locator": "",
              "id": "pkg_id",
              "dependents": ["sample"],
              "material_names": ["yum:go-agent"]
            }
          ]
        },
        {
          "nodes": [
            {
              "name": "sample", "node_type": "PIPELINE", "locator": "/go/pipeline/activity/sample", "instances": [
                {
                  "stages": [
                    {"locator": "/go/pipelines/sample/1/defaultStage/1", "status": "Building", "name": "defaultStage"}
                  ], "locator": "/go/pipelines/value_stream_map/sample/1", "counter": 1, "label": "1"
                }
              ], "parents": ["pkg_id"], "depth": 1, "id": "sample", "dependents": []
            }
          ]
        }
      ]
    };
    new Graph_Renderer("#vsm-container").invoke(vsm);

    expect($('ul[data-materialname="pkg_id"] li.instance').eq('0').find('div').eq('0').text().trim())
      .toBe("Revision: go-agent-13.1.1-16714.noarch");
    expect($('ul[data-materialname="pkg_id"] li.instance').eq('0').find('div').eq('1').html())
      .toBe('Built on server.<br>Trackback: <a href="google.com">google.com</a>');
    expect($('#pkg_id .material_revisions_label').html())
      .toBe('Built on server.<br>Trackback: <a href="google.com">google.com</a>');
    expect($('#pkg_id .material_revisions_label').attr("title"))
      .toBe('Built on server.\nTrackback: google.com');
  });


  it("testShouldShowTrackbackUrlAsNotProvidedWhenItIsEmpty", function () {
    var vsm = {
      "current_pipeline": "sample", "levels": [
        {
          "nodes": [
            {
              "name": "Repository: [repo_url=file:///tmp/repo] - Package: [package_spec=go-agent]",
              "node_type": "PACKAGE",
              "depth": 1,
              "parents": [],
              "material_revisions": [
                {
                  "modifications": [{
                    "modified_time": "5 months ago",
                    "user": "anonymous",
                    "comment": "{\"TYPE\":\"PACKAGE_MATERIAL\"}",
                    "revision": "go-agent-13.1.1-16714.noarch"
                  }]
                }
              ],
              "locator": "",
              "id": "pkg_id",
              "dependents": ["sample"],
              "material_names": ["yum:go-agent"]
            }
          ]
        },
        {
          "nodes": [
            {
              "name": "sample", "node_type": "PIPELINE", "locator": "/go/pipeline/activity/sample", "instances": [
                {
                  "stages": [
                    {"locator": "/go/pipelines/sample/1/defaultStage/1", "status": "Building", "name": "defaultStage"}
                  ], "locator": "/go/pipelines/value_stream_map/sample/1", "counter": 1, "label": "1"
                }
              ], "parents": ["pkg_id"], "depth": 1, "id": "sample", "dependents": []
            }
          ]
        }
      ]
    };
    new Graph_Renderer("#vsm-container").invoke(vsm);

    expect($('ul[data-materialname="pkg_id"] li.instance').eq('0').find('div').eq('0').text().trim()).toBe("Revision: go-agent-13.1.1-16714.noarch");
    expect($('ul[data-materialname="pkg_id"] li.instance').eq('0').find('div').eq('1').html()).toBe('Trackback: Not Provided');
    expect($('#pkg_id .material_revisions_label').html()).toBe('Trackback: Not Provided');
    expect($('#pkg_id .material_revisions_label').attr("title")).toBe('Trackback: Not Provided');
  });

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

    expect($(".instances[data-materialname='svn_fingerprint_1']").is(':visible')).toBe(false);
    expect($(".instances[data-materialname='svn_fingerprint_2']").is(':visible')).toBe(false);
    $("#svn_fingerprint_1 .more").trigger('click');
    $("#svn_fingerprint_2 .more").trigger('click');
    expect($(".instances[data-materialname='svn_fingerprint_1']").is(':visible')).toBe(true);
    expect($(".instances[data-materialname='svn_fingerprint_2']").is(':visible')).toBe(true);
  });

  it("testShouldDisplayAllDetailsForPipelineNodes", function () {
    /*
     hg_fingerprint -> current -> downstream
     */

    var current_pipeline_instance_details = '{"stages": [{"locator": "/go/pipelines/current/1/defaultStage/1","duration": 117,"status": "Passed","name": "defaultStage"},' +
      '{"locator": "","status": "Unknown","name": "oneMore"}],"locator": "/go/pipelines/value_stream_map/current/1","counter": 1,"label": "1" }';

    var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["current"]', 1,
      '[{"modifications":[{"revision": "revision1"}, {"revision": "revision2"}]}]');
    var current = pipelineNode("current", '["hg_fingerprint"]', '[]', 1, "/go/pipeline/activity/current", '[' + current_pipeline_instance_details + ']');

    var vsm = eval('({"current_pipeline":"p1","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + current + ']}]})');
    new Graph_Renderer("#vsm-container").invoke(vsm);

    expect($("#vsm-container #current h3 a").attr("href")).toBe("/go/pipeline/activity/current");
    expect($("#vsm-container #current ul li h4 a").attr("href")).toBe("/go/pipelines/value_stream_map/current/1");
    expect($("#vsm-container #current ul").find("li.instance").length).toBe(1);
    expect($("#vsm-container #current ul ul").find(".stage_bar").length).toBe(2);
    expect($("#vsm-container #current ul ul li.stage_bar.Passed").attr('title')).toBe("defaultStage (took 1m 57.0s)");
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
    var current = pipelineNode("current", '["hg_fingerprint"]', '["downstream"]', 1, "/go/pipeline/activity/current", '[' + current_pipeline_instance_details + ']');
    var downstream = pipelineNode("downstream", '["current"]', '[]', 2, "/go/pipeline/activity/downstream", '[' + downstream_pipeline_instance_details + ']');

    var vsm = eval('({"current_pipeline":"current","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + current + ']},{"nodes":[' + downstream + ']}]})');
    new Graph_Renderer("#vsm-container").invoke(vsm);

    expect($("#downstream .show-more").find("a").text()).toBe("1 more...");
    $("#downstream .show-more a").trigger('click');
    expect($("#downstream .show-more").find("a").text()).toBe("1 less...");
  });

  it("shouldShowThePipelineRunDurationForACompletedPipeline", function () {
    var completedPipeline = '{"stages": [{"locator": "/go/pipelines/current/1/defaultStage/1","status": "Passed","name": "defaultStage", "duration": 63},' +
      '{"locator": "/go/pipelines/current/1/NextStage/1","status": "Failed","name": "NextStage", "duration": 63},' +
      '{"locator": "/go/pipelines/current/1/ManualStage/1","status": "Unknown","name": "manualStage", "duration": null}],' +
      '"locator": "/go/pipelines/value_stream_map/current/1","counter": 1,"label": "1" }';

    var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p1"]', 1,
      '[{modifications:[{"revision": "revision1","comment":"comment1","user":"user1","modified_time":"modified_time1"}, ' +
      '{"revision": "revision2","comment":"comment2","user":"user2","modified_time":"modified_time2"}]}]');

    var node_p1 = pipelineNode("p1", '["hg_fingerprint"]', '[]', 1, "/go/pipeline/activity/p1", '[' + completedPipeline + ']');

    var vsm = eval('({"current_pipeline":"p1","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + node_p1 + ']}]})');
    new Graph_Renderer("#vsm-container").invoke(vsm);

    expect($("#p1 .duration").text()).toBe("Duration: 2m 6.0s");
  });

  it("shouldShowThePipelineRunAsInProgressForAPipelineWhichIsInProgress", function () {
    var inProgressPipeline = '{"stages": [{"locator": "/go/pipelines/current/1/defaultStage/1","status": "Completed","name": "defaultStage", "duration": 63},' +
      '{"locator": "/go/pipelines/current/1/nextStage/1","status": "Building","name": "defaultStage", "duration": null}],' +
      '"locator": "/go/pipelines/value_stream_map/current/1","counter": 1,"label": "1" }';

    var hg_material = scmMaterialNode('hg_fingerprint', '../manual-testing/ant_hg/dummy', "hg", '["p1"]', 1,
      '[{modifications:[{"revision": "revision1","comment":"comment1","user":"user1","modified_time":"modified_time1"}, ' +
      '{"revision": "revision2","comment":"comment2","user":"user2","modified_time":"modified_time2"}]}]');

    var node_p1 = pipelineNode("p1", '["hg_fingerprint"]', '[]', 1, "/go/pipeline/activity/p1", '[' + inProgressPipeline + ']');

    var vsm = eval('({"current_pipeline":"p1","levels":[{"nodes":[' + hg_material + ']},{"nodes":[' + node_p1 + ']}]})');
    new Graph_Renderer("#vsm-container").invoke(vsm);

    expect($("#p1 .duration").text()).toBe("Duration: In Progress");
  });

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

  VsmNode = function () {
    var id;
    var nodePosition;
    var level;
    var depth;
    var type;

    this.getInfo = function () {
      return 'id: ' + this.id + ' left: ' + this.nodePosition.left + ' width: ' + this.nodePosition.width + ' top: ' + this.nodePosition.top + ' height: ' + this.nodePosition.height + ' level: ' + this.level + ' depth: ' + this.depth + ' type:' + this.type;
    };
  };

  VsmGrid = function (container) {
    var allNodes = [];
    var levelVersusNodes = [];

    var svgPosition = $("div#vsm-container")[0].getBoundingClientRect();
    var verticalDistanceBetweenNodesAtSameLevel = 50;
    var borderSize = 0;
    var verticalOffsetFromTop = 20 + borderSize;

    var horizontalDistanceBetweenLevels = 75;
    var horizontalOffsetFromLeft = 20 + borderSize;

    var nodeWidth = 200;
    var nodeHeight = 100;

    function init() {
      var nodes = $(container).find('div.vsm-entity');

      $.each(nodes, function (i, node) {
        nodeObj = new VsmNode();
        nodeObj.id = $(this).id;

        nodeObj.type = ($(node).hasClass('material')) ?
          'material' : (($(node).hasClass('current')) ?
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

    this.getInfo = function () {
      var str = '';
      for (var i = 0; i < allNodes.length; i++) {
        str += allNodes[i].getInfo() + '\n';
      }
      return str;
    };

    init();
  };
});
