/*
 * Copyright 2016 ThoughtWorks, Inc.
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

PipelineErrors = function ($) {
    var refreshPipelineErrors = function() {
        var pipelinesHaveError = [];
        $("#cruise_message_body [data-pipelines]").each(function (i, ele) {
            if ($(ele).data("pipelines")) {
                var pipelines = $(ele).data("pipelines");
                $.each(pipelines, function(i, name) {
                   if (pipelinesHaveError.indexOf(name) == -1) {
                       pipelinesHaveError.push(name);
                   }
                });
            }
        });

        $(".pipeline").each(function (i, pipeline) {
            var name = $(pipeline).find(".pipeline_name_link a").text();
            if (pipelinesHaveError.include(name)) {
                if ($(pipeline).find(".pipeline-error").length == 0) {
                    var icon = $('<i class="pipeline-error"></i>');
                    icon.data("pipeline", name);
                    $(pipeline).find(".pipeline_operations").prepend(icon);
                }
            } else {
                $(pipeline).find(".pipeline-error").remove();
            }
        });
    };
    var showPipelineErrors = function(errorIcon) {
        var pipelineName = $(errorIcon).data("pipeline");
        var errors = $('<div class="cruise_message_body"/>');
        $('#cruise_message_body .error').each(function(i, ele) {
            var pipelines = $(ele).data("pipelines");
            if (pipelines.include(pipelineName)) {
                errors.append($(ele).clone());
            }
        });
        Modalbox.show(errors[0], { title: pipelineName + ' error and warning messages', overlayClose: false });
    };
    var initialize = function() {
        if (document.location.href.indexOf("?show-warnings") < 0) {
            return;
        }

        $(document).bind("server-health-messages-refresh-completed", refreshPipelineErrors);
        refreshPipelineErrors();
        $(document).on("click", ".pipeline-error", function(e) {
            showPipelineErrors(e.target);
        });
    };

    return {
        initialize: initialize,
        showPipelineErrors: showPipelineErrors
    };
}(jQuery);
