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
describe("Publish artifact view", function () {

  function externalArtifactTemplate() {
    return '<div class="artifact">' +
      '    <div class="row expanded">' +
      '        <div class="columns medium-2 large-2">' +
      '            <h4 class="type">Type</h4>' +
      '        </div>' +
      '        <div class="columns medium-3 large-3">' +
      '            <h4>ID</h4>' +
      '        </div>' +
      '        <div class="columns medium-3 large-3 end">' +
      '            <h4>Store ID</h4>' +
      '        </div>' +
      '    </div>' +
      '    <div class="row expanded">' +
      '        <div class="name_value_cell columns medium-2 large-2">' +
      '            <label class="type_label">External Artifact</label>' +
      '            <input class="form_input artifact_source" name="job[artifactTypeConfigs][][artifactTypeValue]" type="hidden" value="Pluggable Artifact" /> </div>' +
      '        <div class="name_value_cell columns medium-3 large-3">' +
      '            <input class="form_input artifact_source" id="job_artifactTypeConfigs__id" name="job[artifactTypeConfigs][][id]" type="text" />' +
      '        </div>' +
      '        <div class="name_value_cell columns medium-3 large-3">' +
      '            <input class="form_input artifact_destination stores_auto_complete" id="job_artifactTypeConfigs__storeId" name="job[artifactTypeConfigs][][storeId]" type="text" />' +
      '        </div>' +
      '        <div class="name_value_cell columns medium-4 large-4 end">' +
      '            <span class="icon_remove delete_artifact" />' +
      '        </div>' +
      '    </div>' +
      '    <div class="row expanded plugin_dropdown_background hidden">' +
      '        <div class="columns medium-8 medium-offset-2 large-8 end">' +
      '            <div class="plugin-select-form">' +
      '                <label>Plugin</label>' +
      '                <select class="artifact_plugin_selection" id="_pluginId" name="job[artifactTypeConfigs][][pluginId]">' +
      '                    <option value="">Select plugin</option>' +
      '                    <option value="cd.go.artifact.docker.registry">Artifact plugin for docker</option>' +
      '                    <option value="cd.go.artifact.s3">Artifact plugin for s3</option>' +
      '                </select>' +
      '            </div>' +
      '        </div>' +
      '    </div>' +
      '    <div class="row expanded">' +
      '        <div class="columns medium-8 medium-offset-2 large-8 end">' +
      '            <div class="form_content">' +
      '                <div id="material">' +
      '                    <div class="fieldset">' +
      '                        <div class="plugged_artifact">' +
      '                            <div class="form_item">' +
      '                                <div class="plugged_artifact_template"></div>' +
      '                                <span class="plugged_artifact_data" style="display: none">{}</span>' +
      '                            </div>' +
      '                        </div>' +
      '                    </div>' +
      '                </div>' +
      '            </div>' +
      '        </div>' +
      '    </div>' +
      '    <div class="row expanded plugin_key_value plugin_form_background">' +
      '        <div class="columns medium-8 medium-offset-2 large-8 end">' +
      '        </div>' +
      '    </div>' +
      '</div>'
  }

  function noArtifactStoreConfigured() {
    return '<div class="artifact"' +
      '    <div class="information no_artifact_store">' +
      '        <div class="errors">' +
      '            No artifact store is configured' +
      '            <br> Go to <a href="/go/admin/artifact_stores">artifact store page</a> to configure artifact store.' +
      '        </div>' +
      '    </div>' +
      '</div>'
  }

  function dockerPluginView() {
    return '<div class="form_item_block">' +
      '    <label ng-class="{\'is-invalid-label\': GOINPUTNAME[Image].$error.server}">Image:</label>' +
      '    <input ng-class="{\'is-invalid-input\': GOINPUTNAME[Image].$error.server}" type="text" ng-model="Image" placeholder="gocd/app-image"/>' +
      '    <span class="form_error form-error" ng-class="{\'is-visible\': GOINPUTNAME[Image].$error.server}" ng-show="GOINPUTNAME[Image].$error.server">{{GOINPUTNAME[Image].$error.server}}</span>' +
      '</div>' +
      '' +
      '<div class="form_item_block">' +
      '    <label ng-class="{\'is-invalid-label\': GOINPUTNAME[Tag].$error.server}">Tag Pattern:</label>' +
      '    <input ng-class="{\'is-invalid-input\': GOINPUTNAME[Tag].$error.server}" type="text" ng-model="Tag" placeholder="v${GO_PIPELINE_COUNTER}"/>' +
      '    <span class="form_error form-error" ng-class="{\'is-visible\': GOINPUTNAME[Tag].$error.server}" ng-show="GOINPUTNAME[Tag].$error.server">{{GOINPUTNAME[Tag].$error.server}}</span>' +
      '</div>';
  }

  function s3PluginView() {
    return '<div class="form_item_block">' +
      '    <label ng-class="{\'is-invalid-label\': GOINPUTNAME[BucketName].$error.server}">Bucket name:</label>' +
      '    <input ng-class="{\'is-invalid-input\': GOINPUTNAME[BucketName].$error.server}" type="text" ng-model="BucketName" placeholder="gocd/app-image"/>' +
      '    <span class="form_error form-error" ng-class="{\'is-visible\': GOINPUTNAME[BucketName].$error.server}" ng-show="GOINPUTNAME[BucketName].$error.server">{{GOINPUTNAME[BucketName].$error.server}}</span>' +
      '</div>';
  }

  describe("Builtin artifact", function () {
    beforeEach(function () {
      setFixtures('<div>' +
        '<textarea id="build_artifact_template"><div class=\'foo\'>FooBar</div></textarea>' +
        '<div class="artifact-container"></div></div>');
    });

    it('should add build artifact to the container', function () {
      var publishArtifact = new PublishArtifact("Build Artifact", "#build_artifact_template", {});
      expect(jQuery('.foo')).not.toBeInDOM();

      publishArtifact.renderView();

      expect(jQuery('.foo')).toBeInDOM();
    });

    it('should add test artifact to the container', function () {
      var publishArtifact = PublishArtifact("Test Artifact", "#build_artifact_template", {});
      expect(jQuery('.foo')).not.toBeInDOM();

      publishArtifact.renderView();

      expect(jQuery('.foo')).toBeInDOM();
    });
  });

  describe("External artifact", function () {
    beforeEach(function () {
      setFixtures('<div>' +
        '<textarea id="external_artifact_template">'+externalArtifactTemplate()+'</textarea>' +
        '<div class="artifact-container"></div></div>');
    });

    it('should create external artifact view with artifact id and store id input visible', function () {
      var publishArtifact = PublishArtifact("External Artifact", "#external_artifact_template", {
        storeIds:             ["dockerhub"],
        storeIdToPluginId:    {"dockerhub": "cd.go.artifact.docker.registry"},
        artifactPluginToView: {"cd.go.artifact.docker.registry": dockerPluginView()}
      });

      expect(jQuery('.artifact-container')).toBeInDOM();
      expect(jQuery('.artifact-container').html()).toEqual("");

      publishArtifact.renderView();

      var labels = jQuery('.artifact-container .artifact h4');
      expect(jQuery(labels[0])).toHaveText("Type");
      expect(jQuery(labels[1])).toHaveText("ID");
      expect(jQuery(labels[2])).toHaveText("Store ID");

      expect(jQuery("label.type_label")).toHaveText("External Artifact");
      expect(jQuery('input[name="job[artifactTypeConfigs][][artifactTypeValue]"]')).toBeInDOM();
      expect(jQuery('input[name="job[artifactTypeConfigs][][id]"]')).toBeInDOM();
      expect(jQuery('input[name="job[artifactTypeConfigs][][storeId]"]')).toBeInDOM();

      expect(jQuery('select[name="job[artifactTypeConfigs][][pluginId]"]')).not.toBeVisible();
      expect(jQuery('.plugged_artifact_template')).toBeEmpty();
      expect(jQuery('.plugged_artifact_data')).toHaveText("{}");
    });

    it('should show plugin dropdown on change of store id', function () {
      PublishArtifact("External Artifact", "#external_artifact_template", {
        storeIds:             ["dockerhub"],
        storeIdToPluginId:    {"dockerhub": "cd.go.artifact.docker.registry"},
        artifactPluginToView: {"cd.go.artifact.docker.registry": dockerPluginView()}
      }).renderView();

      expect(jQuery('select[name="job[artifactTypeConfigs][][pluginId]"]')).not.toBeVisible();

      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').val("foo");
      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').trigger('input');

      expect(jQuery('select[name="job[artifactTypeConfigs][][pluginId]"]')).toBeVisible();
    });

    it('should show plugin view on select of plugin when multiple plugin is installed', function () {
      PublishArtifact("External Artifact", "#external_artifact_template", {
        storeIds:             ["dockerhub", "s3"],
        storeIdToPluginId:    {"dockerhub": "cd.go.artifact.docker.registry", "s3": "cd.go.artifact.s3"},
        artifactPluginToView: {"cd.go.artifact.docker.registry": dockerPluginView(), "s3": s3PluginView()}
      }).renderView();

      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').val("#{foo}");
      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').trigger('input');

      expect(jQuery('.plugged_artifact_template')).toBeEmpty();

      jQuery('select[name="job[artifactTypeConfigs][][pluginId]"]').val('cd.go.artifact.docker.registry');
      jQuery('select[name="job[artifactTypeConfigs][][pluginId]"]').change();

      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Image]"]')).toBeInDOM();
      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Tag]"]')).toBeInDOM();
    });

    it('should auto select plugin view when only one plugin is installed', function () {
      PublishArtifact("External Artifact", "#external_artifact_template", {
        storeIds:             ["dockerhub"],
        storeIdToPluginId:    {"dockerhub": "cd.go.artifact.docker.registry"},
        artifactPluginToView: {"cd.go.artifact.docker.registry": dockerPluginView()}
      }).renderView();

      expect(jQuery('.plugged_artifact_template')).toBeEmpty();

      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').val("#{foo}");
      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').trigger('input');

      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Image]"]')).toBeInDOM();
      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Tag]"]')).toBeInDOM();
    });

    it('should show plugin view when proper store id is provided', function () {
      PublishArtifact("External Artifact", "#external_artifact_template", {
        storeIds:             ["dockerhub"],
        storeIdToPluginId:    {"dockerhub": "cd.go.artifact.docker.registry"},
        artifactPluginToView: {"cd.go.artifact.docker.registry": dockerPluginView()}
      }).renderView();

      expect(jQuery('.plugged_artifact_template')).toBeEmpty();

      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').val("dockerhub");
      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').trigger('input');

      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Image]"]')).toBeInDOM();
      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Tag]"]')).toBeInDOM();
    });

    it('should change view on change of plugin', function () {
      PublishArtifact("External Artifact", "#external_artifact_template", {
        storeIds:             ["dockerhub", "s3"],
        storeIdToPluginId:    {"dockerhub": "cd.go.artifact.docker.registry", "s3": "cd.go.artifact.s3"},
        artifactPluginToView: {
          "cd.go.artifact.docker.registry": dockerPluginView(),
          "cd.go.artifact.s3":              s3PluginView()
        }
      }).renderView();

      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').val("#{foo}");
      jQuery('input[name="job[artifactTypeConfigs][][storeId]"]').trigger('input');

      jQuery('select[name="job[artifactTypeConfigs][][pluginId]"]').val('cd.go.artifact.docker.registry');
      jQuery('select[name="job[artifactTypeConfigs][][pluginId]"]').change();

      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Image]"]')).toBeInDOM();
      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Tag]"]')).toBeInDOM();

      jQuery('select[name="job[artifactTypeConfigs][][pluginId]"]').val('cd.go.artifact.s3');
      jQuery('select[name="job[artifactTypeConfigs][][pluginId]"]').change();

      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][BucketName]"]')).toBeInDOM();
      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Image]"]')).not.toBeInDOM();
      expect(jQuery('.plugged_artifact_template input[name="job[artifactTypeConfigs][][configuration][Tag]"]')).not.toBeInDOM();
    });


    it('should render key value when store id is parameterized and multiple plugin installed', function () {
      PublishArtifact("External Artifact", "#external_artifact_template", {
        storeId:                   "#{foo}",
        artifactId:                "bar",
        storeIds:                  ["dockerhub", "s3"],
        storeIdToPluginId:         {"dockerhub": "cd.go.artifact.docker.registry", "s3": "cd.go.artifact.s3"},
        artifactPluginToView:      {
          "cd.go.artifact.docker.registry": dockerPluginView(),
          "cd.go.artifact.s3":              s3PluginView()
        },
        keyValuePairConfiguration: {
          "Image": {"displayValue": "gocd/gocd-docker-agent", "isSecure": true, "value": "gocd-docker-agent"},
          "Tag":   {"displayValue": "18.7", "isSecure": false, "value": "18.7.0"}
        }
      }).renderView();

      var allKeyValue = jQuery(".key-value-pair ");
      expect(jQuery('input[name="job[artifactTypeConfigs][][configuration][Image][value]"]', allKeyValue[0])).toHaveValue("gocd-docker-agent");
      expect(jQuery('input[name="job[artifactTypeConfigs][][configuration][Image][isSecure]"]', allKeyValue[0])).toHaveValue("true");
      expect(jQuery('dt', allKeyValue[0])).toHaveText("Image");
      expect(jQuery('dd', allKeyValue[0])).toHaveText("gocd/gocd-docker-agent");

      expect(jQuery('input[name="job[artifactTypeConfigs][][configuration][Tag][value]"]', allKeyValue[1])).toHaveValue("18.7.0");
      expect(jQuery('input[name="job[artifactTypeConfigs][][configuration][Tag][isSecure]"]', allKeyValue[1])).toHaveValue("false");
      expect(jQuery('dt', allKeyValue[1])).toHaveText("Tag");
      expect(jQuery('dd', allKeyValue[1])).toHaveText("18.7");
    });

    it('should render link to artifact store page when not artifact store is configured', function () {
      jQuery("#external_artifact_template").html(noArtifactStoreConfigured());

      PublishArtifact("External Artifact", "#external_artifact_template", {
        storeIds:          [],
        storeIdToPluginId: {}
      }).renderView();

      expect(jQuery('.artifact .errors')).toBeInDOM();
      expect(jQuery('.artifact a')).toHaveAttr("href", "/go/admin/artifact_stores");
    });
  });
});
