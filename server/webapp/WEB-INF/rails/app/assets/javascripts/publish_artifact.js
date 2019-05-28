/*
 * Copyright 2019 ThoughtWorks, Inc.
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
PublishArtifact = function (artifactType, artifactTemplateSelector, options) {
  var self = this;
  var uuid = _.uniqueId();

  self.dataId = function () {
    return "data_" + uuid;
  };

  self.controllerId = function () {
    return "angular_" + uuid;
  };

  self.templateId = function () {
    return "plugged_artifact_template_" + uuid;
  };

  self.uuid = function () {
    return uuid;
  };

  self.renderView = function () {
    addFormRowFor();
    if (artifactType === 'External Artifact' && artifactStoreConfigured()) {
      hookupStoresAutoComplete();
      registerPluginDropdownChangeListener();
      findInTemplate("input[name='job[artifactConfigs][][storeId]']").on('input propertychange paste result', onChangeOfStoreId);
      bindExistingArtifactData();
      initializeAngular();
      return;
    }
    showArtifactStoreNotConfiguredErrorMessage();
  };

  var findInTemplate = function (selector) {
    return jQuery(selector, currentArtifactView());
  };

  var renderKeyValueConfiguration = function () {
    if (_.isEmpty(options.storeId)) {
      return;
    }

    var pluginId = options.storeIdToPluginId[options.storeId];
    if (!_.isEmpty(pluginId)) {
      return;
    }

    if (_.isEmpty(options.keyValuePairConfiguration)) {
      return;
    }

    hidePluginDropdown();
    _.forEach(options.keyValuePairConfiguration, function (config, key) {
      findInTemplate(".plugin_key_value .columns").append(buildKeyValuePairView(key, config));
    });
  };

  var buildKeyValuePairView = function (key, config) {
    var keyValueContainer = jQuery('<div class="plugin-config-read-only show" id="plugin_key_value_' + self.uuid() + '">');
    var dl                = jQuery('<dl class="key-value-pair">');
    keyValueContainer.append(dl);

    jQuery('<input type="hidden" name="job[artifactConfigs][][configuration][' + key + '][value]" value="' + config.value + '"/>').appendTo(dl);
    jQuery('<input type="hidden" name="job[artifactConfigs][][configuration][' + key + '][isSecure]" value="' + config.isSecure + '"/>').appendTo(dl);
    jQuery('<dt>' + key + '</dt>').appendTo(dl);
    jQuery('<dd>' + config.displayValue + '</dd>').appendTo(dl);

    return keyValueContainer;
  };

  var bindExistingArtifactData = function () {
    findInTemplate("#job_artifactConfigs__id").val(options.artifactId);
    findInTemplate("#job_artifactConfigs__storeId").val(options.storeId);
    findInTemplate(".plugged_artifact_data").text(options.configuration);
    findInTemplate("#job_artifactConfigs__storeId").trigger("input");
  };

  var showArtifactStoreNotConfiguredErrorMessage = function () {
    var container = jQuery(".artifact-container");
    if (!artifactStoreConfigured() && container.find(".no_artifact_store").length === 0) {
      container.append(jQuery(artifactTemplateSelector));
    }
  };

  var artifactStoreConfigured = function () {
    return !_.isEmpty(options.storeIdToPluginId);
  };

  var loadTemplate = function (selector) {
    var wrapperDiv = jQuery('<div>');
    wrapperDiv.append(jQuery(selector).text());

    wrapperDiv.find(".artifact")
      .attr("id", self.templateId());

    wrapperDiv.find(".plugged_artifact")
      .attr("id", self.controllerId())
      .attr("name", self.controllerId());

    wrapperDiv.find(".plugged_artifact_data")
      .attr("id", self.dataId());

    return wrapperDiv;
  };

  var hookupStoresAutoComplete = function () {
    jQuery(".stores_auto_complete").autocomplete(options.storeIds, {
      multiple:      false,
      minChars:      0,
      matchContains: true,
      selectFirst:   false,
      width:         248
    });
  };

  var registerPluginDropdownChangeListener = function () {
    findInTemplate('.artifact_plugin_selection').change(function (event) {
      showAppropriatePluginView((event.currentTarget || event.target).value);
    })
  };

  var onChangeOfStoreId = function (event) {
    var selectedInput = event.currentTarget || event.target;
    var defaultValue  = selectedInput.defaultValue;
    var currentValue  = selectedInput.value;

    //Nothing is changed
    if (!_.isEmpty(defaultValue) && defaultValue === currentValue) {
      return;
    }

    var artifactView = currentArtifactView();
    artifactView.find(".plugin-config-read-only").remove();
    artifactView.find(".plugged_artifact_template").closest(".row").addClass("plugin_form_background");
    hidePluginView();

    if (_.isEmpty(selectedInput.value)) {
      hidePluginDropdown();
      return;
    }

    if (_.size(options.artifactPluginToView) === 1) {
      showPluginDropdown();
      findInTemplate(".plugin-select-form span").remove();
      showAppropriatePluginView(_.keys(options.artifactPluginToView)[0]);
      return;
    }

    var pluginId = options.storeIdToPluginId[selectedInput.value];
    artifactView.find('.artifact_plugin_selection').val(pluginId);
    if (pluginId) {
      hidePluginDropdown();
      showAppropriatePluginView(pluginId);
    } else {
      showPluginDropdown();
      renderKeyValueConfiguration();
    }
  };

  var showAppropriatePluginView = function (pluginId) {
    var pluginView = options.artifactPluginToView[pluginId];
    if (pluginView) {
      findInTemplate(".plugged_artifact_template").html(pluginView);
      findInTemplate('.artifact_plugin_selection').val(pluginId);
      showPluginView();
      initializeAngular();
    }
  };

  var initializeAngular = function () {
    var form_name_prefix = "job[artifactConfigs][][configuration]";
    var angular_html     = jQuery("#" + self.controllerId()).html();
    if (angular_html) {
      var taskPluginView = new TaskPluginView();
      taskPluginView.initialize(self.controllerId(), self.dataId(), form_name_prefix);
      taskPluginView.bootstrapAngular();
    }
  };

  function currentArtifactView() {
    return jQuery("#" + self.templateId());
  }

  var hidePluginView = function () {
    findInTemplate(".plugged_artifact_template").hide();
  };

  var showPluginView = function () {
    findInTemplate(".plugged_artifact_template").show();
  };

  var hidePluginDropdown = function () {
    findInTemplate(".plugin_dropdown_background").hide();
  };

  var showPluginDropdown = function () {
    findInTemplate(".plugin_dropdown_background").show();
  };

  var addFormRowFor = function () {
    var rowCreator = new EnvironmentVariables.RowCreator(loadTemplate(artifactTemplateSelector), 'div', '.delete_artifact', true);
    var variables  = new EnvironmentVariables(jQuery('div.artifact-container'), rowCreator, null, function (inputs) {
        inputs.dirty_form();
      },
      function (row) {
        row.parents("form.dirtyform").data("dirty", true);
      }
    );
    variables.addDefaultRow();
  };

  return self;
};
