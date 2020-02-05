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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {ConfigRepoExtensionJSON} from "models/shared/plugin_infos_new/serialization";
import {pluginImageLink} from "models/shared/plugin_infos_new/spec/test_data";
import {PipelineConfigVM} from "../pipeline_config_view_model";

describe("PipelineConfig View Model", () => {
  let vm: PipelineConfigVM;

  beforeEach(() => (vm = new PipelineConfigVM()));

  it("whenTemplateAbsent() only performs callback when not using template", () => {
    const callback = jasmine.createSpy();

    vm.isUsingTemplate(true);
    vm.whenTemplateAbsent(callback);

    expect(callback).not.toHaveBeenCalled();

    vm.isUsingTemplate(false);
    vm.whenTemplateAbsent(callback);

    expect(callback).toHaveBeenCalled();
  });

  it("exportPlugins() only returns plugins that support PaC export", (done) => {
    spyOn(PluginInfoCRUD, "all").and.returnValue(
      new Promise<ApiResult<PluginInfos>>((resolve, _) => {
        resolve(
          ApiResult.success("", 200, new Map()).map<PluginInfos>(
            (s) =>
              new PluginInfos(
                configRepoPlugin("yes.export", true),
                configRepoPlugin("no.export", false)
              )
          )
        );

        setTimeout(() => {
          const plugins = vm.exportPlugins();
          expect(plugins).toEqual([{id: "yes.export", text: "yes.export"}]);
          done();
        }, 0);
      })
    );

    vm.exportPlugins(); // prime the cache
    expect(PluginInfoCRUD.all).toHaveBeenCalledWith({type: ExtensionTypeString.CONFIG_REPO});
  });

  it("preview() fills placeholders for missing names when not validating", () => {
    spyOn(ApiRequestBuilder, "POST");

    vm.pipeline.group("group");
    vm.preview("foo");

    expect(ApiRequestBuilder.POST).toHaveBeenCalledWith(
      SparkRoutes.pacPreview("foo", "group"),
      ApiVersion.v1,
      {
        payload: {
          name: "** UNNAMED PIPELINE **",
          materials: [{attributes: {invert_filter: false, password: ""}, type: "git"}],
          stages: [],
          parameters: []
        }
      }
    );
  });
});

function configRepoPlugin(id: string, supportExport: boolean) {
  return PluginInfo.fromJSON({
    _links: pluginImageLink(),
    id,
    about: {
      name: id,
      version: "0.6.1",
      target_go_version: "16.12.0",
      description: "Docker Based Elastic Agent Plugins for GoCD",
      target_operating_systems: [],
      vendor: {
        name: "GoCD Contributors",
        url: "https://github.com/gocd-contrib/docker-elastic-agents"
      }
    },
    status: {state: "active"},
    extensions: [
      {
        type: "configrepo",
        plugin_settings: {},
        capabilities: {
          supports_pipeline_export: supportExport,
          supports_parse_content: true
        }
      } as ConfigRepoExtensionJSON
    ],
    plugin_file_location: "/tmp/foo",
    bundled_plugin: false
  }) as PluginInfo;
}
