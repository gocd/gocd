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

import classnames from "classnames";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineParameter} from "models/pipeline_configs/parameter";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {DefaultTemplatesCache, TemplateCache} from "models/pipeline_configs/templates_cache";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Option, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {SwitchBtn} from "views/components/switch/index";
import css from "./template_editor.scss";

interface Attrs {
  pipelineConfig: PipelineConfig;
  cache?: TemplateCache<Option>;
  isUsingTemplate: Stream<boolean>;
  paramList: Stream<PipelineParameter[]>;
}

export class TemplateEditor extends MithrilViewComponent<Attrs> {
  selectedTemplate: Stream<TemplateConfig> = Stream();
  private cache: TemplateCache<Option> = new DefaultTemplatesCache();
  private templates: Stream<Option[]> = Stream();

  oninit(vnode: m.Vnode<Attrs, {}>) {
    if (vnode.attrs.cache) {
      this.cache = vnode.attrs.cache;
    }

    this.cache.prime(() => {
      this.templates(this.cache.templates());
    }, () => {
      this.templates([]);
    });
  }

  view(vnode: m.Vnode<Attrs>) {
    const errors = vnode.attrs.pipelineConfig.errors();
    return <div class={classnames({[css.errorText]: errors.hasErrors("template")})}>
      <SwitchBtn small={true}
        label="Use Template:"
        field={vnode.attrs.isUsingTemplate}
        onclick={this.toggleTemplate.bind(this, vnode.attrs.pipelineConfig, vnode.attrs.paramList)}
      />
      {this.templateOptions(vnode.attrs)}
    </div>;
  }

  setTemplateParams(templateId: string, paramList: Stream<PipelineParameter[]>, config: PipelineConfig): void {
    TemplateConfig.getTemplate(templateId, (result: TemplateConfig) => {
      const params = result.parameters();
      if (params.length !== 0) {
        paramList(params);
        config.parameters(params);
      }
    });
  }

  templateOptions(attrs: Attrs): m.Children {
    const config = attrs.pipelineConfig;
    const errors = config.errors();

    if (attrs.isUsingTemplate()) {
      if (!this.templates().length) {
        return <FlashMessage type={errors.hasErrors("template") ? MessageType.alert : MessageType.warning}>
          <code>
            There are no templates configured or you are unauthorized to view the existing templates.
            Add one via the <a href="/go/admin/templates" title="Pipeline Templates">templates page</a>.
          </code>
        </FlashMessage>;
      } else {
        return <SelectField label="Template" property={config.template} errorText={errors.errorsForDisplay("template")} required={true} onchange={(e) => this.setTemplateParams(e.target.value, attrs.paramList, config)}>
            <SelectFieldOptions selected={config.template()} items={this.templates()}/>
        </SelectField>;
      }
    }
  }

  toggleTemplate(pipelineConfig: PipelineConfig, paramList: Stream<PipelineParameter[]>, event: MouseEvent): void {
    const target = event.target as HTMLInputElement;
    if (target.checked) {
      pipelineConfig.stages().clear();
      if (this.templates() && this.templates().length !== 0) {
        const templateId = this.templates()[0].id;
        this.setTemplateParams(templateId, paramList, pipelineConfig);
        pipelineConfig.template(templateId);
      }
    } else {
      pipelineConfig.template = Stream();
    }
  }
}
