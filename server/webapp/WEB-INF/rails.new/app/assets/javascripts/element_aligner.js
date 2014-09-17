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

var ElementAligner = function() {

    var RowsIterator = function() {
        var remainingPipelines;

        function nextRow() {
            if (remainingPipelines.length == 0) {
                return null;
            }

            var zeroth_pipeline = remainingPipelines[0];
            var nextRows = [zeroth_pipeline];
            var remaining = [];
            var zeroth_offset = zeroth_pipeline.offset();
            var minTopSeen = zeroth_offset.top;
            var maxBottomSeen = minTopSeen + zeroth_pipeline.height();

            for(var i = 1; i < remainingPipelines.length; i++) {
                var thisPipeline = remainingPipelines[i];
                var thisTop = thisPipeline.offset().top;
                if (minTopSeen > thisTop) {
                    minTopSeen = thisTop;
                    maxBottomSeen = minTopSeen + thisPipeline.height();
                    remaining = remaining.concat(nextRows);
                    nextRows = [thisPipeline];
                } else if(minTopSeen === thisTop) {
                    nextRows.push(thisPipeline);
                    var potentialMaxBottom = minTopSeen + thisPipeline.height();
                    if (maxBottomSeen < potentialMaxBottom) {
                        maxBottomSeen = potentialMaxBottom;
                    }
                } else {
                    remaining.push(thisPipeline);
                }
            }

            remainingPipelines = remaining;

            return {rows: nextRows, maxBottom: maxBottomSeen, top: minTopSeen};
        }

        function init(pipelines) {
            remainingPipelines = [];
            for(var i = 0; i < pipelines.length; i++) {
                var pipeline = jQuery(pipelines[i]);
                remainingPipelines.push(pipeline);
            }
        }

        init.prototype.nextRow = nextRow;

        return init;
    }();


    function makeSameHeight(row) {
        var max_height = row.maxBottom - row.top;
        for(var i = 0; i < row.rows.length; i++) {
            var ele = row.rows[i];
            ele.css('padding-bottom',max_height - ele.height() + ((i == row.rows.length-1)?16:15));
        }
    }


    function reduce(elements, fn, seed) {
        elements.each(function() {
            seed = fn(this, seed);
        });
        return seed;
    }

    function init() {}

    init.prototype.alignAll = alignAll;

    function alignAll() {
        var groups = document.getElementsByClassName('pipelines');
        for(var i = 0; i < groups.length; i++) {
            var group = groups[i];
            if (group.tagName != "BODY") {
                if (ViewportPredicate.dom_visible(group)) {
                    alignSection(group);
                }
            }
        }
    }

    var cooldown = null;

    function alignAllOnCooldown() {
        if (cooldown) {
            clearTimeout(cooldown);
        }
        cooldown = setTimeout(alignAll, jQuery.browser.msie ? 500 : 150);
    }

    function alignSection(section) {
        align(section.getElementsByClassName('pipeline'));
    }

    function align(elements) {
        var iterator = new RowsIterator(elements);
        var row;
        while (row = iterator.nextRow()) {
            if (ViewportPredicate.in_y(row.top, row.maxBottom)) {
                makeSameHeight(row);
            }
        }
    }

    init.hookupAlignEvents = function() {
        Util.on_load(alignAll);
        $j(window).resize(alignAll);
        $j(window).scroll(alignAllOnCooldown);
    };

    return init;
}();

