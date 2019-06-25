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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {DefaultCache, PipelineGroupCache} from "models/pipeline_configs/pipeline_groups_cache";
import {TemplateCache} from "models/pipeline_configs/templates_cache";
import {Form, FormBody} from "views/components/forms/form";
import {Option, SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import {TemplateEditor} from "views/pages/pipelines/template_editor";
import {IDENTIFIER_FORMAT_HELP_MESSAGE} from "./messages";

interface Attrs {
  pipelineConfig: PipelineConfig;
  cache?: PipelineGroupCache<Option>;
  isUsingTemplate: Stream<boolean>;
  templatesCache?: TemplateCache<Option>;
}

export class PipelineInfoEditor extends MithrilViewComponent<Attrs> {
  private pipelineGroups: Stream<Option[]> = stream();
  private cache: PipelineGroupCache<Option> = new DefaultCache();

  oninit(vnode: m.Vnode<Attrs, {}>) {
    if (vnode.attrs.cache) {
      this.cache = vnode.attrs.cache;
    }

    this.cache.prime(() => {
      this.pipelineGroups(this.cache.pipelineGroups());
    });
  }

  view(vnode: m.Vnode<Attrs>) {
    return <FormBody>
      <Form last={true} compactForm={true}>
        <TextField label="Pipeline Name" helpText={IDENTIFIER_FORMAT_HELP_MESSAGE} placeholder="e.g., My-New-Pipeline" property={vnode.attrs.pipelineConfig.name} errorText={vnode.attrs.pipelineConfig.errors().errorsForDisplay("name")} required={true}/>
      <AdvancedSettings forceOpen={this.hasErrors(vnode)}>
        <SelectField label="Pipeline Group" property={vnode.attrs.pipelineConfig.group} errorText={vnode.attrs.pipelineConfig.errors().errorsForDisplay("group")} required={true}>
          <SelectFieldOptions selected={vnode.attrs.pipelineConfig.group()} items={this.pipelineGroups()}/>
        </SelectField>
        <TemplateEditor
          pipelineConfig={vnode.attrs.pipelineConfig}
          cache={vnode.attrs.templatesCache}
          isUsingTemplate={vnode.attrs.isUsingTemplate}
        />
      </AdvancedSettings>
      </Form>
    </FormBody>;
  }

  hasErrors(vnode: m.Vnode<Attrs>): boolean {
    const errors = vnode.attrs.pipelineConfig.errors();
    return errors.hasErrors("group") || errors.hasErrors("template");
  }
}
