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

PipelineFilter = function() {
    function PipelineSelectorHandler() {}

    PipelineSelectorHandler.prototype = new MicroContentPopup.NeverCloseHandler();

    PipelineSelectorHandler.prototype.after_close = function() {
        unHook();
        AjaxRefreshers.enableAjax();
    };

    PipelineSelectorHandler.prototype.before_show = function() {
        hookupCheckboxEvents();
        AjaxRefreshers.disableAjax();
    };

    var pipeline_selector_shower;
    function hookupPipelineSelectorHideShow() {
        var popup = new MicroContentPopup(jQuery('#pipelines_selector').get(0), new PipelineSelectorHandler());
        pipeline_selector_shower = new MicroContentPopup.ClickShower(popup);
        pipeline_selector_shower.bindShowButton(jQuery('#show_pipelines_selector').get(0), jQuery('.page_header').get(0));
        jQuery('#apply_pipelines_selector').click(function() {
            Util.disable('show_pipelines_selector');
        });
    }

    function setCheckedStateTo(value) {
        return function() {
            jQuery("#pipelines_selector_pipelines input[type='checkbox']").attr('checked', value);
        };
    }

    function propagateSelectionToAllUnder() {
        var self = jQuery(this);
        var checked  = self.attr('checked') == undefined ? false : self.attr('checked');
        self.parent(".selector_group").find("input[name='selector[pipeline][]']").attr('checked', checked);
    }

    function hookupAllNoneEvents() {
        jQuery('#select_all_pipelines').click(setCheckedStateTo(true));
        jQuery('#select_no_pipelines').click(setCheckedStateTo(false));
    }

    function areAllCheckboxesChecked(group) {
        var checked = true;
        group.find("input[name='selector[pipeline][]']").each(function(_, checkbox) {
            checked = checked && checkbox.checked;
        });
        return checked;
    }

    function unHook() {        
        jQuery('#pipelines_selector').find("input[type='checkbox']").unbind();
    }
    
    function hookupCheckboxEvents() {
        var pipelines_selector_dom = jQuery('#pipelines_selector');
        pipelines_selector_dom.find("input[name='selector[group][]']").click(propagateSelectionToAllUnder);

        pipelines_selector_dom.find("input[name='selector[pipeline][]']").click(function() {
            var group = jQuery(this).parents(".selector_group");
            group.find("input[name='selector[group][]']").attr('checked', areAllCheckboxesChecked(group));
        });
    }

    return {
        initialize: function() {
            hookupPipelineSelectorHideShow();
            hookupAllNoneEvents();
        },
        close: function(){
            pipeline_selector_shower && pipeline_selector_shower.close();
        }
    };
}();

RevisionSearch = function() {
    function initialize(searchInput, emptySearchMessage) {
        var $searchInput = jQuery(searchInput);
        var $emptySearchMessage = jQuery(emptySearchMessage);
        $searchInput.autocomplete("/go/revisionsearch.json", {
            formatItem: function (item) {
                if (!item.pipelineName || !item.pipelineLabel)
                    return item.revision;
                return item.revision + " - " + item.pipelineName + " - " + item.pipelineLabel;
            },
            formatResult: function (item) {
                return item.revision;
            },
            formatMatch: function (item) {
                return item.revision;
            },
            extraParams: {
                revision: function () {
                    return $searchInput.val().trim();
                }
            },
            parse: function (data) {
                if (data.length == 0)
                    $emptySearchMessage.show();
                else
                    $emptySearchMessage.hide();
                var items = [];
                for (var i = 0; i < data.length; i++)
                    items[i] = {data: data[i], value: data[i].revision, result: data[i].revision};
                return items;
            },
            highlight: function (value, term) {
                return value.replace(new RegExp("(?![^&;]+;)(?!<[^<>]*)(" + term.replace(/([\^\$\(\)\[\]\{\}\*\.\+\?\|\\])/gi, "\\$1") + ")(?![^<>]*>)(?![^&;]+;)", "gi"), "<strong style='font-weight: bold'>$1</strong>");
            },
            dataType: "json", cacheLength: 1
        }).result(function (event, item) {
            window.location.href = "/go/materials/value_stream_map/" + item.fingerprint + "/" + item.revision;
        });
    }

    return {
        initialize: initialize
    };
}();
