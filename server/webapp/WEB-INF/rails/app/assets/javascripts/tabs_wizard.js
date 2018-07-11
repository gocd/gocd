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

TabsWizard = function() {

    function init(tab_manager) {
        this.tab_manager = tab_manager;
    }

    init.prototype.hookupCancel = function(cancelButtonClass, url) {
        jQuery('.' + cancelButtonClass).click(function() {
            window.location.href = url;
            return false;
        });
    };

    init.prototype.wireButtons = function(button_tabname_mapping) {
        var self = this;
        for (var button_name in button_tabname_mapping) {
            var _ = function() {
                var tab = self.tab_manager.subTabByName(button_tabname_mapping[button_name]);
                jQuery('#' + button_name).click(function() {
                    tab.open();
                    return false;
                });
            }();
        }
    };

    init.EnvironmentWizard = function() {
        sub_init.prototype = new init();

        function sub_init() {
            init.apply(this, arguments);
        }

        return sub_init;
    }();

    init.NewPipelineWizard = function() {
        sub_init.prototype = new init();

        var tab_validation = {
            next_to_settings : function() {
                var group_text_field = jQuery('#pipeline_group_name_text_field');
                return jQuery("#pipeline_group_pipeline_name").valid() && ((group_text_field.length > 0) ? group_text_field.valid() : true);
            },
            next_to_materials : function() {
                var material = jQuery("select[name='pipeline_group[pipeline][materials][materialType]']").val();
                switch (material) {
                    case "SvnMaterial": return jQuery("input[name='pipeline_group[pipeline][materials][SvnMaterial][url]']").valid();
                    case "GitMaterial": return jQuery("input[name='pipeline_group[pipeline][materials][GitMaterial][url]']").valid();
                    case "HgMaterial": return jQuery("input[name='pipeline_group[pipeline][materials][HgMaterial][url]']").valid();
                    case "TfsMaterial":
                        return (
                            jQuery("input[name='pipeline_group[pipeline][materials][TfsMaterial][url]']").valid() &&
                                    jQuery("input[name='pipeline_group[pipeline][materials][TfsMaterial][username]']").valid() &&
                                    jQuery("input[name='pipeline_group[pipeline][materials][TfsMaterial][projectPath]']").valid());
                    case "P4Material": return (
                            jQuery("input[name='pipeline_group[pipeline][materials][P4Material][serverAndPort]']").valid() &&
                                    jQuery("textarea[name='pipeline_group[pipeline][materials][P4Material][view]']").valid());
                    case "DependencyMaterial": return jQuery("input[name='pipeline_group[pipeline][materials][DependencyMaterial][pipelineStageName]']").valid();
                    case "PackageMaterial": return _isPackageRepositoryFormValid();
                }
            },
            finish : function() {
                var selected = jQuery("input[name='pipeline_group[pipeline][configurationType]']:checked").val();
                if (selected == "configurationType_stages") {
                    var valid = jQuery("input[name='pipeline_group[pipeline][stage][name]']").valid() && jQuery("input[name='pipeline_group[pipeline][stage][jobs][][name]']").valid();
                    var taskType = jQuery("select[name='pipeline_group[pipeline][stage][jobs][][tasks][taskOptions]']").val();
                    switch (taskType) {
                        case "exec" : return jQuery("input[name='pipeline_group[pipeline][stage][jobs][][tasks][exec][command]']").valid() && valid;
                    }
                    return valid;
                }
                return true;
            }
        };

        function _isPackageRepositoryFormValid() {
            var valid = true;
            valid = valid && jQuery("#repository").valid();
            if (jQuery("#chooseExisting").is(':enabled') && jQuery("#chooseExisting").is(':checked')) {
                return valid && jQuery("#package").valid();
            } else if (jQuery("#addNew").is(':enabled') && jQuery("#addNew").is(':checked')) {
                jQuery('.PackageMaterial').find('.package_configuration').find(':input').each(function() {
                    valid = valid && jQuery(this).valid();
                });
                return valid;
            }
        }

        function sub_init() {
            init.apply(this, arguments);
        }

        sub_init.prototype.wireButtonsWithValidations = function(button_tabname_mapping) {
            var self = this;
            for (var i = 0; i < button_tabname_mapping.length; i++) {
                for (var button_name in button_tabname_mapping[i]) {
                    var _ = function() {
                        var tab = self.tab_manager.subTabByName(button_tabname_mapping[i][button_name]);
                        var validate_method = tab_validation[button_name];
                        jQuery('#' + button_name).click(function() {
                            if (validate_method()) {
                                tab.open();
                                return false;
                            }
                            return false;
                        });
                    }();
                }
            }
        };

        sub_init.prototype.hookupFinishValidations = function() {
            var validate_method = tab_validation["finish"];
            jQuery("button[type='submit']").click(function(evt) {
                var is_valid = validate_method();
                if (! is_valid) {
                    evt.preventDefault();
                }
                return is_valid;
            });
        };

        sub_init.prototype.addExtraValidations = function(allPipelines) {
            jQuery.validator.addMethod("pattern_match", function(value, element) {
                var regex = /^[a-zA-Z0-9_\-]{1}[a-zA-Z0-9_\-.]*$/
                return (value.length == 0) || regex.test(value);
            }, "Value should contain only alphanumeric characters, dashes, underscores and periods.");

            jQuery.validator.addMethod("uniquePipelineName", function(value, element) {
                var pipelines = allPipelines;
                return pipelines.indexOf(value.toLowerCase()) == -1;
            }, "Pipeline name is already in use.");
        };
        return sub_init;
    }();

    return init;
}();
