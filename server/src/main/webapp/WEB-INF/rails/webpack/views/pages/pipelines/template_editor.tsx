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
import {makeEvent} from "helpers/compat";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineParameter} from "models/pipeline_configs/parameter";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {Template, TemplateCache} from "models/pipeline_configs/templates_cache";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Option, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {SwitchBtn} from "views/components/switch/index";
import css from "./template_editor.scss";

interface Attrs {
  pipelineConfig: PipelineConfig;
  cache?: TemplateCache;
  isUsingTemplate: Stream<boolean>;
  paramList: Stream<PipelineParameter[]>;
}

interface State {
  notifyChange(): void;
}

export class TemplateEditor extends MithrilComponent<Attrs, State> {
  selectedTemplate: Stream<TemplateConfig> = Stream();
  private cache: TemplateCache = new TemplateCache();
  private templates: Stream<Template[]> = Stream();

  oncreate(vnode: m.VnodeDOM<Attrs, State>) {
    vnode.state.notifyChange = () => vnode.dom.dispatchEvent(makeEvent("change"));
  }

  oninit(vnode: m.Vnode<Attrs, State>) {
    if (vnode.attrs.cache) {
      this.cache = vnode.attrs.cache;
    }

    this.cache.prime(() => {
      this.templates(this.cache.contents());
    }, () => {
      this.templates([]);
    });
  }

  view(vnode: m.Vnode<Attrs, State>) {
    const errors = vnode.attrs.pipelineConfig.errors();
    const {pipelineConfig, paramList, isUsingTemplate} = vnode.attrs;

    return <div class={classnames({[css.errorText]: errors.hasErrors("template")})}>
      <SwitchBtn small={true}
        label="Use Template:"
        field={isUsingTemplate}
        onclick={this.toggleTemplate.bind(this, pipelineConfig, paramList, vnode.state)}
      />
      {this.templateOptions(vnode)}
    </div>;
  }

  templatesAsOptions() {
    return _.map(this.templates(), (template) => {
      return {id: template.name, text: template.name } as Option;
    });
  }

  setTemplateParams(templateId: string, paramList: Stream<PipelineParameter[]>, config: PipelineConfig, onSetParams: () => void): void {
    const template = _.find(this.templates(), (template) => template.name === templateId);

    let params = template ? _.map(template.parameters, (param) => new PipelineParameter(param, "")) : [];
    params = params.concat(new PipelineParameter("", ""));
    paramList(params);
    config.parameters(_.filter(params, (p) => (p.name() || "").trim() !== ""));
    onSetParams();
  }

  templateOptions({attrs, state}: {attrs: Attrs, state: State}) {
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
        return <SelectField label="Template" property={config.template} errorText={errors.errorsForDisplay("template")} required={true} onchange={(e: any) => {this.setTemplateParams(e.target.value, attrs.paramList, config, state.notifyChange); }}>
          <SelectFieldOptions selected={config.template()} items={this.templatesAsOptions()}/>
        </SelectField>;
      }
    }
  }

  toggleTemplate(pipelineConfig: PipelineConfig, paramList: Stream<PipelineParameter[]>, state: State, event: MouseEvent) {
    const checkbox = event.currentTarget as HTMLInputElement;
    if (checkbox.checked) {
      pipelineConfig.stages().clear();

      if (this.templates() && this.templates().length > 0) {
        const templateId = this.templates()[0].name;
        this.setTemplateParams(templateId, paramList, pipelineConfig, state.notifyChange);
        pipelineConfig.template(templateId);
      }
    } else {
      pipelineConfig.template = Stream();
      paramList([new PipelineParameter("", "")]);
    }
  }
}
