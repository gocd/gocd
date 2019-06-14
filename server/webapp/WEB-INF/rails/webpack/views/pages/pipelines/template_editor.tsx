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
import {DefaultTemplatesCache, TemplateCache} from "models/pipeline_configs/templates_cache";
import * as s from "underscore.string";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Option, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {SwitchBtn} from "views/components/switch/index";
import * as css from "./components.scss";

interface Attrs {
  pipelineConfig: PipelineConfig;
  cache?: TemplateCache<Option>;
  isUsingTemplate: Stream<boolean>;
}

export class TemplateEditor extends MithrilViewComponent<Attrs> {
  private cache: TemplateCache<Option> = new DefaultTemplatesCache();
  private templates: Stream<Option[]> = stream();

  oninit(vnode: m.Vnode<Attrs, {}>) {
    if (vnode.attrs.cache) {
      this.cache = vnode.attrs.cache;
    }

    this.cache.prime(() => {
      this.templates(this.cache.templates());
    });
  }

  view(vnode: m.Vnode<Attrs>) {
    let templateOptions;
    let hasErrors = false;
    if (vnode.attrs.isUsingTemplate()) {
      if (this.templates().length === 0) {
        hasErrors = !s.isBlank(vnode.attrs.pipelineConfig.errors().errorsForDisplay("template"));
        templateOptions = <FlashMessage type={hasErrors ? MessageType.alert : MessageType.warning} message={<pre>There are no pipeline templates configured. Add one via the <a href="/go/admin/templates" title="Pipeline Templates">templates page</a>.</pre>}/>;
      } else {
        templateOptions = (
          <SelectField label="Template" property={vnode.attrs.pipelineConfig.template} errorText={vnode.attrs.pipelineConfig.errors().errorsForDisplay("template")} required={true}>
            <SelectFieldOptions items={this.templates()}/>
          </SelectField>
        );
      }
    }
    return (
      <div class={hasErrors ? css.errorText : ""}>
        <SwitchBtn small={true}
          label={<div class={css.switchLabelText}>Use Template:</div>}
          field={vnode.attrs.isUsingTemplate}
          onclick={this.toggleTemplate.bind(this, vnode.attrs.pipelineConfig)}
        />
        {templateOptions}
      </div>
    );

  }

  toggleTemplate(pipelineConfig: PipelineConfig, event: MouseEvent): void {
    const target = event.target as HTMLInputElement;
    if (target.checked) {
      pipelineConfig.stages().clear();
      if (this.templates().length !== 0) {
        pipelineConfig.template(this.templates()[0].id);
      }
    } else {
      pipelineConfig.template = stream();
    }
  }
}
