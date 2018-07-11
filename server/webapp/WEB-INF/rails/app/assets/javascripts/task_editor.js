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

TaskEditor = {
    TypeEditor: function() {
        function allEntries(self) {
            return self.container.find(self.entry_to_be_hidden);
        }

        function hideAll(self) {
            hide(allEntries(self));
        }

        function hide(doms) {
            doms.addClass('hidden');
            doms.find('.form_input').attr('disabled', true); //This is to ensure form submit does not pick up hidden elements.
        }

        function show(doms) {
            doms.removeClass('hidden');
            doms.find('.form_input').removeAttr('disabled');
        }

        function showEntry(self, newValue) {
            if (newValue === '') {
                show(allEntries(self).filter(self.entry_to_be_hidden).filter('.default'));
            }
            else {
                show(allEntries(self).filter('.' + newValue));
            }
        }

        function refreshTaskView(self) {
            var newValue = self.switch_control.val();
            hideAll(self);
            showEntry(self, newValue);
        }

        function registerSwitchControlListener(self) {
            self.switch_control.change(function() {
                refreshTaskView(self);
            });
        }

        function init(options) {
            this.container = options.container;
            this.switch_control = options.switch_control;
            this.entry_to_be_hidden = options.entry_to_be_hidden;
            registerSwitchControlListener(this);
            refreshTaskView(this);
        }

        init.prototype.showHideCheckbox = function(checkbox) {
            var self = this;
            checkbox.click(function() {
                if (jQuery(this).is(':checked')) {
                    show(self.container);
                } else {
                    hide(self.container);
                }
            });
        }

        return init;
    }(),

    FetchTaskEditor: function() {
        function init() {
        }

        function prunedStageListForEdit(list, pipelineName) {
            for (var i = 0; i < list.length; i++) {
                if (list[i].pipeline == pipelineName) {
                    return list[i].stages;
                }
            }
            return [];
        }

        function prunedJobListForEdit(list, pipelineName, stageName) {
            for (var i = 0; i < list.length; i++) {
                var pipelineEntry = list[i];
                if (pipelineEntry.pipeline == pipelineName) {
                    for (var j = 0; j < pipelineEntry.stages.length; j++) {
                        var stageEntry = pipelineEntry.stages[j];
                        if (stageEntry.stage == stageName) {
                            return stageEntry.jobs;
                        }
                    }
                }
            }
            return [];
        }

        function bindJobs(job_box, stages_jobs) {
            job_box.removeData('events').autocomplete(stages_jobs, {
                minChars: 0,
                width: 400,
                matchContains: "word",
                cacheLength: 0,
                formatItem: function(row, i, max) {
                    return  row.job;
                },
                formatMatch: function(row, i, max) {
                    return row.job;
                },
                formatResult: function(row) {
                    return row.job;
                }
            });
        }

        init.prototype.wireInAutocomplete = function(pipeline_box, stage_box, job_box, pipelines, currentPipelineName) {
            var pipeline_name = pipeline_box.val() == "" ? currentPipelineName : pipeline_box.val();
            var pipelines_stages = prunedStageListForEdit(pipelines, pipeline_name);
            var stages_jobs = prunedJobListForEdit(pipelines, pipeline_name, stage_box.val());
            pipeline_box.autocomplete(pipelines, {
                minChars: 0,
                width: 400,
                matchContains: "word",
                cacheLength: 0,
                formatItem: function(row, i, max) {
                    return  row.pipeline;
                },
                formatMatch: function(row, i, max) {
                    return row.pipeline;
                },
                formatResult: function(row) {
                    return row.pipeline;
                }
            }).result(function(event, pipelineEntry) {
                pipelines_stages = prunedStageListForEdit(pipelines, pipelineEntry.pipeline);
                stage_box.removeData('events').autocomplete(pipelines_stages, {
                    minChars: 0,
                    width: 400,
                    matchContains: "word",
                    cacheLength: 0,
                    formatItem: function(row, i, max) {
                        return row.stage;
                    },
                    formatMatch: function(row, i, max) {
                        return row.stage;
                    },
                    formatResult: function(row) {
                        return row.stage;
                    }
                }).result(function(event, stageEntry) {
                    stages_jobs = prunedJobListForEdit(pipelines, pipelineEntry.pipeline, stageEntry.stage);
                    bindJobs(job_box, stages_jobs);
                });
                stage_box.val("");
                job_box.val("");
            });
            stage_box.autocomplete(pipelines_stages, {
                minChars: 0,
                width: 400,
                matchContains: "word",
                cacheLength: 0,
                formatItem: function(row, i, max) {
                    return row.stage;
                },
                formatMatch: function(row, i, max) {
                    return row.stage;
                },
                formatResult: function(row) {
                    return row.stage;
                }
            }).result(function(event, stageEntry) {
                stages_jobs = prunedJobListForEdit(pipelines, pipeline_name, stageEntry.stage);
                job_box.val("");
                bindJobs(job_box, stages_jobs);
            });
            bindJobs(job_box, stages_jobs);
        };

        return init;
    }(),

    RunIfEditor: function() {
        function allRunIfs(self) {
            return self.container.find(".runif");
        }

        function checkboxes(self) {
            var all = allRunIfs(self);
            var any = all.filter('.any');
            var other = all.filter(':not(.any)');
            return {other: other, any: any};
        }

        function refreshCheckboxState(self) {
            var checkbox_list = checkboxes(self);
            if (jQuery(checkbox_list.any).is(":checked")) {
                checkbox_list.other.attr('checked', false).attr('disabled', true);
            } else {
                checkbox_list.other.attr('disabled', false);
            }
        }

        function registerActions(self) {
            checkboxes(self).any.click(function() {
                refreshCheckboxState(self);
            });
        }

        function init(container) {
            this.container = jQuery(container);
            registerActions(this);
            refreshCheckboxState(this);
        }

        return init;
    }()
};

TaskSnippet = {
    attachClickHandlers: function(selector, commandLookupUrl, command_definition_url, commandTextBoxXPath, argumentsTextAreaXPath) {
        var container = jQuery(selector);
        var autoCompleteTextBox = container.find(".gist_based_auto_complete .lookup_command");
        var commandTextBox = container.find(commandTextBoxXPath);
        var argumentsTextArea = container.find(argumentsTextAreaXPath);

        if(argumentsTextArea.size() == 0) {
            autoCompleteTextBox.attr("disabled", "disabled");
            container.find(".gist_based_auto_complete .error-message-for-old-args").show();
            return;
        }

        var fillTextBoxWithValues = function(data) {
            commandTextBox.val(data.command);
            argumentsTextArea.val(data.arguments);
        };

        var highlightTextBoxes = function() {
            highlightElement(commandTextBox);
            highlightElement(argumentsTextArea);
        };

        var prependHttpIfSchemeNotPresentInURL = function(value) {
            var needsHttpPrepended = typeof value === "string" && value.match(/^[a-zA-Z]+:\/\//) == null;
            return needsHttpPrepended ? "http://" + value : value;
        };

        var displaySnippet = function(data) {
            var authorName = data.author === null ? "link" : data.author;
            var authorInfo = prependHttpIfSchemeNotPresentInURL(data.authorinfo);
            var moreInfo = prependHttpIfSchemeNotPresentInURL(data.moreinfo);
            var description = data.description === null ? "No description available." : data.description;

            container.find(".invalid_snippets").hide();
            container.find(".snippet_details").show();
            container.find(".snippet_details .name .value").text(data.name);
            container.find(".snippet_details .description .value").text(description);

            var authorData = container.find(".snippet_details .author");
            var authorDataWithLink = container.find(".snippet_details .author .value-with-link a");
            var authorDataWithoutLink = container.find(".snippet_details .author .value");
            var moreInfoLink = container.find(".snippet_details .more-info .value-with-link a");

            authorData.show();
            authorDataWithoutLink.text(authorName).show();
            authorDataWithLink.attr("href", authorInfo).text(authorName).hide();
            moreInfoLink.attr("href", moreInfo).show();

            if (data.author === null && authorInfo === null) {
                authorData.hide();
            }

            if (authorInfo !== null) {
                authorDataWithoutLink.hide();
                authorDataWithLink.show();
            }

            if (moreInfo === null) {
                moreInfoLink.hide();
            }
        };

        var commandLookup = new CommandSnippetLookup(autoCompleteTextBox, commandLookupUrl);
        commandLookup.hookupAutocomplete();

        autoCompleteTextBox.on("result", function(event, data) {
            var relativeFilePathOfSelectedSnippet = data.length > 1 ? data[1] : null;
            if (relativeFilePathOfSelectedSnippet) {
                jQuery.get(command_definition_url, {command_name : relativeFilePathOfSelectedSnippet}, function(data) {
                    var jsonData = eval(data);
                    fillTextBoxWithValues(jsonData);
                    highlightTextBoxes();
                    displaySnippet(jsonData);
                });
            }
        });
    }
};