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

Graph_Renderer = function (container) {
    'use strict'
    var container = $j(container);
    var width = 200; // Default width of a node
    var height = 110; // Default height of a node
    var current;
    var isCurrent;
    var levels;
    var pipeline_gui;
    var nodeClassName = '';
    var maxWidth = 100; //width of svg container
    var maxHeight = 100; //height of svg container
    var svg;
    var noInstanceMessage = "No instance of this pipeline has run for any of the direct upstream dependency revision.";

    Graph_Renderer.prototype.invoke = function (vsm) {
        current = vsm.current_pipeline;
        levels = vsm.levels;

        if (current != null && current != undefined) {
            container.append('<div class="highlight"></div>');
        }

        renderEntities(levels);
        materialBoxCreation();
        resetContainerPosition();
        if (d3) {
            renderConnections(levels);
        }

        initMiniMap();
        addBehaviors();
    }

    function resetContainerPosition() {
        container.scrollTop(0);
        container.scrollLeft(0);
    }

    function renderEntities(levels) {
        $j.each(levels, function (i, level) {
            $j.each(level.nodes, function (j, node) {
                var depth = node.depth - 1;

                if (node.node_type != 'PIPELINE' && node.node_type != 'DUMMY') {
                    node.id = (/\d/.test(node.id.charAt(0))) ? 'a' + node.id : node.id;
                }

                if (node.id != current) {
                    if (node.node_type != 'PIPELINE' && node.node_type != 'DUMMY') {
                        pipeline_gui = renderMaterialCommits(node);
                        var material_conflicts = node.view_type == 'WARNING' ? 'conflicts' : '';
                        pipeline_gui += '<div id="' + node.id.replace(/\./g, '_id-') + '" class="vsm-entity material ' + node.node_type.toLowerCase() + ' ' + material_conflicts + '" style="';
                        pipeline_gui += 'top:' + (((height * depth) + (50 * depth)) + 50) + 'px; left:' + (((width * i) + (90 * i)) + 100) + 'px';
                        pipeline_gui += '">';
                        pipeline_gui += renderScmEntity(node);

                    }
                    else {
                        pipeline_gui = '<div id="' + node.id.replace(/\./g, '_id-') + '" class="vsm-entity ' + node.node_type.toLowerCase() + '" style="';
                        pipeline_gui += 'top:' + (((height * depth) + (50 * depth)) + 50) + 'px; left:' + (((width * i) + (90 * i)) + 20) + 'px';
                        pipeline_gui += '">';
                    }
                    isCurrent = false;
                } else {
                    $j(container).find('.highlight').css({ 'left': (((width * i) + (90 * i))) })
                    pipeline_gui = '<div id="' + node.id.replace(/\./g, '_id-') + '" class="vsm-entity ' + node.node_type.toLowerCase() + ' current" style="';
                    pipeline_gui += 'top:' + (((height * depth) + (50 * depth)) + 30) + 'px; left:' + (((width * i) + (90 * i))) + 'px';
                    pipeline_gui += '">';
                    isCurrent = true;
                }

                if (node.node_type == 'PIPELINE') {
                    pipeline_gui += renderPipelineEntity(node);
                } else if (node.node_type == 'DUMMY') {
                    pipeline_gui += renderDummyEntity(node);
                }
                pipeline_gui += '</div>';
                container.append(pipeline_gui);
                $j(container).find('.highlight').show();
            });
        });
    }

    function renderScmEntity(node) {
        var gui = '', node_name = '';
        var modification = firstModification(node);

        if (modification) {
            nodeClassName = node.node_type.toLowerCase();
            gui += '<div class= "material_revisions ' + nodeClassName + '"></div>'
            if (node.node_type == 'PACKAGE' && typeof(node.material_names) !== "undefined") {
                node_name = node.material_names.join();
            } else {
                node_name = node.name;
            }
            gui += '<h3 class="material_type" title="' + nodeClassName + ': ' + node_name + '">' + node_name + '</h3>';

            if (modification && modification.revision) {
                gui += '<div title="' + parseCommentForTooltip(modification.comment) + '" class= "material_revisions_label">'
                gui += parseComment(modification.comment);
                gui += '</div>';

                gui += '<div class="more">...</div>';
                //rendering dropdown of remaining instances
            }

            gui += '<div class="actions">';
            gui += '<button class="pin" title="Keep dependencies highlighted">pin</button>';
            gui += '</div>';

            return gui;
        }
    }

    function firstModification(node) {
        if (node.material_revisions != null && node.material_revisions != undefined && node.material_revisions.length != 0) {
            return node.material_revisions[0].modifications[0];
        }
    }

    function renderMaterialCommits(node) {
        var gui = '';
        var instancesCount;
        var material_name;

        if (node.material_revisions != null && node.material_revisions != undefined && node.material_revisions.length != 0) {
            instancesCount = node.material_revisions.length;
            var list_of_material_name = '';
            if (node.material_names != undefined) {
                for (var i = 0; i < node.material_names.length; i++) {
                    material_name = node.material_names[i];
                    list_of_material_name += material_name + ', ';
                }
            }
            gui += '<ul class="instances" data-materialname=' + node.id + '>';
            gui += '<li></li>';
            for (var i = 0; i < instancesCount; i++) {
                gui += '<li class="material_revision_header"><div title="' + node.name + '">' + node.name + '</div></li>'
                var modificationsCount = node.material_revisions[i].modifications.length;
                for (var j = 0; j < modificationsCount; j++) {
                    gui += renderScmInstance(node.material_revisions[i].modifications[j]);
                }
            }
            gui += '</ul>';
        }
        return gui;
    }

    function materialBoxCreation() {
        var $MaterialRevision = $j('.vsm-entity.material');

        $MaterialRevision.click(function (event) {
            var CommentsBox = $j('ul[data-materialname="' + $j(this).attr('id') + '"]');
            CommentsBox.slideToggle(100);

            var top = $j(this).offset().top + $j(this).height() - 3;
            var left = $j(this).offset().left + ($j(this).outerWidth() / 2) - ($MaterialRevision.outerWidth() / 2) - 20;

            CommentsBox.offset({top: top, left: left})

            //keeping last opened box on the top - start
            var index_highest = 0;
            $j("ul[data-materialname]").each(function () {
                var index_current = parseInt($j(this).css("zIndex"));
                if (index_current > index_highest) {
                    index_highest = index_current;
                }
                CommentsBox.css({'z-index': index_highest + 1});
            });
            //keeping last opened box on the top - ends

            event.stopPropagation();
            $j(CommentsBox).click(function (event) {
                event.stopPropagation();
            })

        });

        $j(document).click(function () {
            if ($j('ul[data-materialname]').is(':visible')) {
                $j('ul[data-materialname]').hide();
            }
        });

    }

    function renderScmInstance(instance) {

        return '<li class="instance">'
                + '<div title="' + instance.revision + '" class="icon revision">' + '<a href="' + instance.locator + '">' + instance.revision + '</a>' + ' </div>'
                + '<div class="usercomment wraptext">' + parseComment(instance.comment) + '</div>'
                + '<div class="author">'
                + '<p>' + _.escape(instance.user) + ' </p>'
                + '<p>' + instance.modified_time + '</p>'
                + '</div>'
                + '</li>';
    }

    function parseComment(comment) {
        if (/"TYPE":"PACKAGE_MATERIAL"/.test(comment)) {
            var comment_markup = "";
            var comment_map = JSON.parse(comment);
            var package_comment = comment_map['COMMENT'];
            var trackback_url = comment_map['TRACKBACK_URL'];
            if (typeof package_comment !== "undefined" || package_comment != null) {
                comment_markup = package_comment + "<br/>";
            }
            if (typeof trackback_url !== "undefined" || trackback_url != null) {
                return comment_markup + "Trackback: " + "<a href=" + trackback_url + ">" + trackback_url + "</a>";
            }
            return comment_markup + "Trackback: " + "Not Provided";
        }
        return _.escape(comment);
    }

    function parseCommentForTooltip(comment) {
        if (/"TYPE":"PACKAGE_MATERIAL"/.test(comment)) {
            var comment_tooltip = "";
            var comment_map = JSON.parse(comment);
            var package_comment = comment_map['COMMENT'];
            var trackback_url = comment_map['TRACKBACK_URL'];
            if (typeof package_comment !== "undefined" || package_comment != null) {
                comment_tooltip = package_comment + "\n";
            }
            if (typeof trackback_url !== "undefined" || trackback_url != null) {
                return comment_tooltip + "Trackback: " + trackback_url;
            }
            return comment_tooltip + "Trackback: " + "Not Provided";
        }
        return _.escape(comment);
    }

    function renderPipelineEntity(node) {
        var gui = '';
        if (node.view_type != null && node.view_type != undefined) {
            if (node.view_type == 'NO_PERMISSION') {
                return renderRestrictedPipeline(node);
            } else if (node.view_type == 'DELETED') {
                return renderDeletedPipeline(node);
            } else if (node.view_type == 'WARNING') {
                gui = renderWarning(node);
            }
        }

        var instancesCount;
        if (node.instances != null && node.instances != undefined) {
            gui += '<h3 title="' + node.name + '"><a href="' + node.locator + '">' + node.name + '</a></h3>';
            if (node.instances != undefined && node.instances.length > 0 && node.instances[0].stages) {
                gui += '<ul class="instances">';
                for (var i = 0; i < node.instances.length; i++) {
                    gui += renderPipelineInstance(node.id, node.instances[i])
                }
                gui += '</ul>';
            }
            instancesCount = node.instances.length;
            if (instancesCount > 1) {
                gui += '<div class="show-more"><a href="#" class="';
                if (instancesCount > 3) {
                    gui += 'xl';
                } else if (instancesCount > 2) {
                    gui += 'l';
                }
                gui += '">' + (node.instances.length - 1) + ' more...</a></div>';
            }
        }
        gui += '<div class="actions"><button class="pin" title="Keep dependencies highlighted">pin</button></div>';
        return gui;
    }

    function renderRestrictedPipeline(node) {
        var gui = '';
        gui += '<h3 title="' + node.name + '" class="restricted">' + node.name + '</h3>';
        if (node.message) {
            gui += '<div class="message restricted"><span>' + node.message + '</span></div>';
        }
        gui += '<div class="actions restricted"><button class="pin" title="Keep dependencies highlighted">pin</button></div>';
        return gui;
    }

    function renderWarning(node) {
        var gui = '';
        if (node.message) {
            gui += '<div class="warning"><span>' + node.message + '</span></div>';
        }
        return gui;
    }

    function renderDeletedPipeline(node) {
        var gui = '';
        gui += '<h3 title="' + node.name + '" class="deleted">' + node.name + '</h3>';
        if (node.message) {
            gui += '<div class="message deleted"><span>' + node.message + '</span></div>';
        }
        gui += '<div class="actions deleted"><button class="pin" title="Keep dependencies highlighted">pin</button></div>';
        return gui;
    }

    function renderPipelineInstance(node_id, instance) {
        var gui = '';
        var stagesCount = 0;
        gui += '<li class="instance">';
        if (instance.label != '') {
            if (isCurrent) {
                gui += '<h4 title="' + instance.label + '"><i class="label">Label: </i>' + instance.label + '</h4>';
            }
            else {
                gui += '<h4 title="' + instance.label + '"><i class="label">Label: </i><a href="' + instance.locator + '">' + instance.label + '</a></h4>';
            }
        }
        if(instance.locator.trim() != "") {
            gui += '<ul class="stages">';
            stagesCount = instance.stages.length;
            for (var i = 0; i < stagesCount; i++) {
                var stagesWidth = (node_id == current) ? 238 : 196;
                gui += '<li class="stage_bar ';
                gui += ((instance.stages[i].status != undefined) ? instance.stages[i].status : 'Unknown');
                if (instance.stages[i].status == 'Unknown') {
                    gui += '" style="width:' + ((stagesWidth - (stagesCount * 4)) / stagesCount) + 'px" title="' + instance.stages[i].name + '"><span>' + instance.stages[i].name + '</span></li>'
                }
                else {
                    gui += '" style="width:' + ((stagesWidth - (stagesCount * 4)) / stagesCount) + 'px" title="' + instance.stages[i].name + '"><a href="' + instance.stages[i].locator + '"><span>' + instance.stages[i].name + '</span></a></li>'
                }
            }
            gui += '</ul>';
        }

        if(instance.locator.trim() == "") {
            gui += "<div style='clear:both;' class='message waiting' title='"+ noInstanceMessage +"'>";
            gui += noInstanceMessage;
            gui += "</div>";
        }

        gui += '</li>';
        return gui;
    }

    function renderDummyEntity(node) {
        return '<h3><a href="#">' + node.name + '</a></h3>';
    }

    // Edges =============================================================================================

    var dependencyArrows = [];
    var pinnedEntities = [];
    var currentZIndex = 0;

    function renderConnections() {
        svg = d3.select('#vsm-container').append('svg:svg').attr('id', 'svg').attr('width', 500).attr('height', 500).append('svg:g');
        parseConnections(levels);
        var endPoint;

        var line = d3.svg.line()
                .interpolate("basis")
                .x(function (d) {
                    return d.x;
                })
                .y(function (d) {
                    return d.y;
                });

        d3.select('svg').attr('width', maxWidth)
                .attr('height', maxHeight);


        svg.selectAll("path")
                .data(dependencyArrows)
                .enter().append("svg:path")
                .attr('d', function (d) {
                    endPoint = d.pathData[d.pathData.length - 1];
                    d.pathData = d.pathData.concat([
                        {"x": (endPoint.x), "y": (endPoint.y)},
                        {"x": (endPoint.x - 12), "y": (endPoint.y - 14)},
                        {"x": (endPoint.x), "y": (endPoint.y)},
                        {"x": (endPoint.x - 12), "y": (endPoint.y + 14)},
                        {"x": (endPoint.x), "y": (endPoint.y)},
                        {"x": (endPoint.x), "y": (endPoint.y)}
                    ]);
                    return line(d.pathData);
                })
                .attr('class', function (d) {
                    return 'dependency ' + d.source.replace(/\./g, '_id-') + ' ' + d.target.replace(/\./g, '_id-');
                })
                .append('title')
                .text(function (d) {
                    if (!($j('#' + d.source).hasClass('pipeline') || $j('#' + d.source).hasClass('dummy'))) {
                        return $j('#' + d.source.replace(/\./g, '_id-') + ' h3').attr('title') + ' -> ' + d.target;
                    }
                    else {
                        return d.source + ' -> ' + d.target;
                    }
                });

    }

    function parseConnections(levels) {
        var source, target, p1, p2, x1, x2, y1, y2;
        $j.each(levels, function (i, level) {
            $j.each(level.nodes, function (j, node) {
                source = $j(Util.escapeDotsFromId(node.id.replace(/\./g, '_id-')));

                p1 = source.position();
                x1 = p1.left + source.outerWidth();
                y1 = p1.top + (source.outerHeight() / 2);
                if (node.id == current) {
                    x1 += 0;
                    y1 += 0
                }
                $j.each(node.dependents, function (k, dependent) {
                    target = $j(Util.escapeDotsFromId(dependent.replace(/\./g, '_id-')));
                    p2 = target.position();
                    x2 = p2.left;
                    y2 = p2.top + (source.outerHeight() / 2);
                    if (dependent == current) {
                        x1 -= 0;
                        y2 += 20
                    }
                    if (node.id == current) {
                        x1 += 0;
                        y2 += -20
                    }
                    addConnection(x1, y1, x2, y2, node.id, dependent);
                });
            });
        });
    }

    function addConnection(x1, y1, x2, y2, source, target) {
        maxWidth = maxWidth < (x2 + 500) ? x2 + 500 : maxWidth;
        maxHeight = maxHeight < (y2 + 150) ? y2 + 150 : maxHeight;
        maxHeight = maxHeight < (y1 + 150) ? y1 + 150 : maxHeight;
        var arrowData;
        if (y1 == y2 || Math.abs(y1 - y2) < 25) {
            arrowData = [
                {"x": x1, "y": y1},
                {"x": x2, "y": y2}
            ];
        } else if (y1 > y2) {
            arrowData = [
                {"x": x1, "y": y1},
                {"x": x1 + 25, "y": y1 - 5},
                {"x": x2 - 25, "y": y2 + 5},
                {"x": x2, "y": y2}
            ];
        } else if (y1 < y2) {
            arrowData = [
                {"x": x1, "y": y1},
                {"x": x1 + 25, "y": y1 + 5},
                {"x": x2 - 25, "y": y2 - 5},
                {"x": x2, "y": y2}
            ];
        }
        addDependencyArrow(source, target, arrowData, ($j(Util.escapeDotsFromId(source)).text() == 'dummy-' + source));
    }

    function dependencyArrow(source, target, pathData) {
        this.source = source;
        this.target = target;
        this.pathData = pathData;
        this.zIndex = currentZIndex;

        this.setTarget = function (target) {
            this.target = target
        }

        this.appendPath = function (pathData) {
            this.pathData = this.pathData.concat(pathData);
        }
    }

    function sortDependencyArrows(source, target) {
        source = source.replace(/\./g, '_id-');
        target = target.replace(/\./g, '_id-');

        $j.each(dependencyArrows, function (i, arrow) {
            currentZIndex++;
            if (arrow.source.replace(/\./g, '_id-') == source && arrow.target.replace(/\./g, '_id-') == target) {
                arrow.zIndex = currentZIndex;
            }
            else if (arrow.source.replace(/\./g, '_id-') == source && target == 'dependency') {
                arrow.zIndex = currentZIndex;
            }
            else if (arrow.target.replace(/\./g, '_id-') == target && source == 'dependency') {
                arrow.zIndex = currentZIndex;
            }
        });

        dependencyArrows.sort(zIndexCompare);

        var sortSelector = '';

        $j.each(dependencyArrows, function (i, e) {
            sortSelector += '.' + e.source.replace(/\./g, '_id-') + '.' + e.target.replace(/\./g, '_id-') + ', ';
        });

        d3.selectAll('path.dependency').sort(function (a, b) {
            return a.zIndex - b.zIndex;
        })
    }

    function zIndexCompare(a, b) {
        return a.zIndex - b.zIndex;
    }

    function pinEntity(entity) {
        pinnedEntities.splice(pinnedEntities.length, 0, entity);
        d3.select('svg#svg').selectAll('path.pinned').classed('pinned', false);
        $j.each(pinnedEntities, function (i, e) {
            d3.select('svg#svg').selectAll('path.' + e).classed('pinned', true);
            sortDependencyArrows(e, 'dependency');
            sortDependencyArrows('dependency', e);
        })
    }

    function unpinEntity(entity) {
        pinnedEntities.splice(pinnedEntities.indexOf(entity), 1);
        d3.select('svg#svg').selectAll('path.pinned').classed('pinned', false);
        $j.each(pinnedEntities, function (i, e) {
            d3.select('svg#svg').selectAll('path.' + e).classed('pinned', true);
            sortDependencyArrows(e, 'dependency');
            sortDependencyArrows('dependency', e);
        })
    }

    function addDependencyArrow(source, target, pathData, isDummy) {
        if (isDummy) {
            for (var i = 0; i < dependencyArrows.length; i++) {
                if (dependencyArrows[i].target == source) {
                    dependencyArrows[i].setTarget(target);
                    dependencyArrows[i].appendPath(pathData);
                }
            }
        } else {
            dependencyArrows.push(new dependencyArrow(source, target, pathData));
        }
    }

    // Minimap ===========================================================================================

    var context;
    var minimap = $j('.pan');
    var miniKnob = $j('.knob');
    var constrainer = $j('.pan .constrainer');

    var minimapWidth;
    var minimapHeight;
    var miniContentWidth = 200;
    var miniContentHeight = 150;
    var ratio;

    function initMiniMap() {

        miniMapContainerSize();
        calculateScaleDownRatio();

        constrainer.width((maxWidth * ratio) + 4);
        constrainer.height((maxHeight * ratio) + 4);

        resizeKnob();
        toggleMiniMap();
        $j(window).bind('resize', function () {
            resizeKnob();
            toggleMiniMap();
        });

        renderPanCanvas(levels);
    }

    function miniMapContainerSize() {
        if (maxHeight > 5000 && maxHeight < 10000) {
            minimap.height(300);
            $j('.pan canvas').attr({'height': 300, 'width': 200});
        } else if (maxHeight > 10000) {
            minimap.height(450);
            $j('.pan canvas').attr({'height': 450, 'width': 200});
        } else {
            $j('.pan canvas').attr({'height': 150, 'width': 200});
        }
    }

    function calculateScaleDownRatio() {
        minimapWidth = minimap.width();
        minimapHeight = minimap.height();

        var priority = (maxHeight / maxWidth < 0.75) ? 'w' : 'h'; // checking 4:3 ratio

        if (priority == 'w') {
            ratio = minimapWidth / ((maxWidth > container.width()) ? maxWidth : container.width());
        } else {
            ratio = minimapHeight / ((maxHeight > container.height()) ? maxHeight : container.height());
        }
    }

    function toggleMiniMap() {
        if (($j('#vsm-container')[0].scrollHeight - $j('#vsm-container').height()) > 200) {
            $j('.pan').show();
        } else if ($j('#vsm-container')[0].scrollWidth - $j('#vsm-container').width() > 200) {
            $j('.pan').show();
        } else {
            $j('.pan').hide();
        }
    }

    function resizeKnob() {
        miniKnob.width(container.width() * ratio);
        miniKnob.height(container.height() * ratio);
    }

    function renderPanCanvas(levels) {
        var panCanvas = document.getElementById('canvas');
        if (panCanvas != undefined && panCanvas != null) {
            context = panCanvas.getContext('2d');
        }

        //scrollbar moves knob
        $j(container).bind('scroll', setKnobPosition);


        $j('.pan .constrainer').on('click', function (e) {
            if (e.target !== this) {
                return;
            }
            var top = e.clientY - $j(this).offset().top - (miniKnob.height() / 2);
            var left = e.clientX - $j(this).offset().left - (miniKnob.width() / 2 );

            if (top + miniKnob.height() > $j('.constrainer').height()) {
                top = $j('.constrainer').height() - miniKnob.height() - 3;
            }
            if (left + miniKnob.width() > $j('.constrainer').width()) {
                left = $j('.constrainer').width() - miniKnob.width() - 3;
            }
            if (top < 1) {
                top = 0;
            }
            if (left < 0) {
                left = 0;
            }

            miniKnob.animate({'left': left, 'top': top}, 'fast');
            scrollDocument(top / ratio, left / ratio);

        });


        function setKnobPosition() {
            var topPossible = $j('.constrainer').height() - miniKnob.height();
            var leftPossible = $j('.constrainer').width() - miniKnob.width();
            var top = ($j('#vsm-container')[0].scrollHeight - $j('#vsm-container').height());
            var left = ($j('#vsm-container')[0].scrollWidth - $j('#vsm-container').width());
            miniKnob.css({'top': $j('#vsm-container').scrollTop() * (topPossible / top), 'left': $j('#vsm-container').scrollLeft() * (leftPossible / left)});
        }

        function scrollDocument(top, left) {
            container.scrollTop(top).scrollLeft(left);
        }

        //knob moves scrollbar
        $j('.pan .knob').draggable({cursor: "move", containment: ".constrainer", start: function () {
            $j(container).unbind('scroll')
        }, stop: function () {
            $j(container).bind('scroll', setKnobPosition)
        }}).bind('drag', function (event) {
                    var knobPos = miniKnob.position();
                    var knobX = knobPos.left;
                    var knobY = knobPos.top;
                    scrollDocument(knobY / ratio, knobX / ratio);
                });

        $j('#vsm-container .vsm-entity').each(function (i, level) {
            var position = $j(this).position();
            var x = position.left * ratio;
            var y = position.top * ratio;
            var w = $j(this).outerWidth() * ratio;
            var h = $j(this).outerHeight() * ratio;
            if ($j(this).hasClass('current')) {
                if (context != null && context != undefined) {
                    context.fillStyle = 'rgb(242,242,242)';
                    context.fillRect(x, 0, w, minimap.height());
                }
            }
            if (!($j(this).hasClass('dummy'))) {
                if (context != null && context != undefined) {
                    context.fillStyle = 'rgb(255,255,255)';
                    context.strokeStyle = 'rgb(60, 60, 60)';
                    context.lineWidth = 1;
                    context.beginPath();
                    if ($j(this).hasClass('material')) {//check if it is material, then we are rendering cirlce in VSM
                        context.arc(x + (w / 2), y + (h / 2), h / 2, 0, 2 * Math.PI, false);
                        context.fill();
                        context.stroke();
                    }
                    else {
                        context.fillRect(x, y, w, h);
                        context.strokeRect(x + 1, y + 1, w - 1, h - 1);
                    }
                    context.closePath();

                }
            }
        });
    }

    function addBehaviors() {
        var currentExpanded;
        var currentExpandedLink;

        $j('#vsm-container .highlight').css({'height': maxHeight, 'min-height': $j('#vsm-container').height() - 20}); // Expand

        // Keep current in viewport when initally loaded
        if (container && $j('.pipeline.current') && $j('.pipeline.current').position()) {
            container.scrollLeft($j('.pipeline.current').position().left - ($j(window).width() / 2) + 100);
        }

        $j('#vsm-container').find('.show-more a').click(function (event) {
            currentExpanded = $j('.expanded').not($j(this).closest('.vsm-entity'));
            if (currentExpanded.length > 0) {
                currentExpandedLink = currentExpanded.find('.show-more a');
                currentExpandedLink.html(currentExpandedLink.html().replace('less', 'more'));
                currentExpanded.removeClass('expanded xl l');
            }
            $j(this).closest('.vsm-entity').toggleClass('expanded');
            if ($j(this).hasClass('xl')) {
                $j(this).closest('.vsm-entity').toggleClass('xl');
            }
            if ($j(this).hasClass('l')) {
                $j(this).closest('.vsm-entity').toggleClass('l');
            }
            if ($j(this).html().indexOf('more') > -1) {
                $j(this).html($j(this).html().replace('more', 'less'));
            } else {
                $j(this).html($j(this).html().replace('less', 'more'));
            }
            return false;
        });

        $j('.vsm-entity .pin').click(function (e) {
            e.stopPropagation();

            var _entity = $j(this).closest('.vsm-entity');
            var _id = _entity.attr('id');
            if ($j(this).hasClass('pinned')) {
                $j(this).removeClass('pinned');
                _entity.removeClass('pinned');
                unpinEntity(_id);
            } else {
                $j(this).addClass('pinned');
                _entity.addClass('pinned');
                pinEntity(_id);
            }
        });

        $j('.vsm-entity').hover(function () {
            var _id = $j(this).attr('id');
            d3.select('svg#svg').selectAll('path.' + _id).classed('hovered', true);
            $j(this).addClass('hovered');
            sortDependencyArrows(_id, 'dependency');
            sortDependencyArrows('dependency', _id);
        }, function () {
            var _id = $j(this).attr('id');
            d3.select('svg#svg').selectAll('path.' + _id).classed('hovered', false);
            $j(this).removeClass('hovered');
        });

        var material_names;
        var material_title;
        $j('.vsm-entity .material_names').each(function () {
            $j(this).attr('title', function () {
                material_names = $j(this).find('span');
                material_title = '';
                material_names.each(function () {
                    material_title += $j(this).attr('data-title') + ', ';
                });
                material_title = material_title.substring(0, material_title.length - 2);
                return material_title;
            });
        });

        d3.selectAll('path.dependency')
                .on('mouseover', function (d) {
                    d3.selectAll('#' + d.source).classed('hovered', true);
                    d3.selectAll('#' + d.target).classed('hovered', true);
                    d3.select(this).classed('hovered', true);
                    sortDependencyArrows(d.source, d.target);
                })
                .on('mouseout', function (d) {
                    d3.selectAll('#' + d.source).classed('hovered', false);
                    d3.selectAll('#' + d.target).classed('hovered', false);
                    d3.select(this).classed('hovered', false);
                })
    }
};
