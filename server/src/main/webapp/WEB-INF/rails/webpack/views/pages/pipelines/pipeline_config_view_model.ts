/*
 * Copyright 2020 ThoughtWorks, Inc.
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

// utils
import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import Stream from "mithril/stream";
import s from "underscore.string";

// models and such
import {ConfigRepo} from "models/config_repos/types";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {ConfigRepoExtension} from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfosCache} from "models/shared/plugin_infos_new/plugin_infos_cache";
import {Option} from "views/components/forms/input_fields";

const VANITY_PLUGIN_NAMES: {[key: string]: string} = {
  [ConfigRepo.YAML_PLUGIN_ID]: "YAML",
  [ConfigRepo.JSON_PLUGIN_ID]: "JSON",
  [ConfigRepo.GROOVY_PLUGIN_ID]: "Groovy",
};

export class PipelineConfigVM {
  material: Material = new Material("git", new GitMaterialAttributes(undefined, true));
  job: Job = new Job("", []);
  stage: Stage = new Stage("", [this.job]);
  pipeline: PipelineConfig = new PipelineConfig("", [this.material], []);
  isUsingTemplate = Stream(false);

  private pluginCache = new PluginInfosCache<Option>(ExtensionTypeString.CONFIG_REPO, toOption, onlyExportPlugins);

  whenTemplateAbsent(fn: () => m.Children) {
    const { pipeline, stage, isUsingTemplate } = this;

    if (!isUsingTemplate()) {
      if (!pipeline.stages().has(stage)) {
        pipeline.stages(new NameableSet([stage]));
      }
      return fn();
    }
  }

  exportPlugins() {
    this.pluginCache.prime(m.redraw);
    return this.pluginCache.contents();
  }

  preview(pluginId: string, validate?: boolean) {
    const { group, pipeline } = this.pipeline.toApiPayload();

    if (!validate) {
      if (s.isBlank(pipeline.name)) {
        pipeline.name = "** UNNAMED PIPELINE **";
      }

      for (let i = pipeline.stages.length - 1; i >= 0; i--) {
        const stage = pipeline.stages[i];
        if (stage && s.isBlank(stage.name)) {
          stage.name = `** UNNAMED STAGE ${i + 1} **`;
        }

        for (let k = stage.jobs.length; k >= 0; k--) {
          const job = stage.jobs[k];
          if (job && s.isBlank(job.name)) {
            job.name = `** UNNAMED JOB ${k + 1} **`;
          }
        }
      }
    }

    return ApiRequestBuilder.POST(SparkRoutes.pacPreview(pluginId, group, validate), ApiVersion.v1, { payload: pipeline });
  }
}

export interface PipelineConfigVMAware {
  vm: PipelineConfigVM;
}

function toOption(plugin: PluginInfo): Option {
  return { id: plugin.id, text: VANITY_PLUGIN_NAMES[plugin.id] || plugin.about.name };
}

function onlyExportPlugins(plugin: PluginInfo): boolean {
  return "active" === plugin.status.state && plugin.extensions.some((ext: any) => (ext as ConfigRepoExtension).capabilities.supportsPipelineExport);
}
