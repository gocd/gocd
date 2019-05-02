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
import {DefaultCache, PipelineGroupsCache} from "models/pipeline_configs/pipeline_groups_cache";
import {Form, FormBody} from "views/components/forms/form";
import {Option, SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";

interface Attrs {
  pipelineConfig: PipelineConfig;
  cache?: PipelineGroupsCache<Option>;
}

export class PipelineInfoEditor extends MithrilViewComponent<Attrs> {
  private pipelineGroups: Stream<Option[]> = stream();
  private cache: PipelineGroupsCache<Option> = new DefaultCache();

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
        <TextField label="Pipeline Name" placeholder="e.g., My-New-Pipeline" required={true} property={vnode.attrs.pipelineConfig.name} errorText={vnode.attrs.pipelineConfig.errors().errorsForDisplay("name")}/>
      <div style="display:none;"><AdvancedSettings>
        <SelectField label="Pipeline Group" property={vnode.attrs.pipelineConfig.group} errorText={vnode.attrs.pipelineConfig.errors().errorsForDisplay("group")} required={true}>
          <SelectFieldOptions selected={vnode.attrs.pipelineConfig.group()} items={this.pipelineGroups()}/>
        </SelectField>
      </AdvancedSettings></div>
      </Form>
    </FormBody>;

  }
}
