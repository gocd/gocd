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

import classnames from "classnames";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {Template} from "models/pipeline_configs/templates_cache";
import {Primary, Reset} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Option, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons/index";
import pipelineConfigStyles from "views/pages/clicky_pipeline_config/index.scss";
import {ConfirmationDialog} from "views/pages/pipeline_activity/confirmation_modal";
import styles from "../stages.scss";

interface Attrs {
  readonly: boolean;
  pipelineConfig: PipelineConfig;
  templates: Stream<Template[]>;
  pipelineConfigSave: () => Promise<any>;
  pipelineConfigReset: () => Promise<any>;
  isPipelineDefinedOriginallyFromTemplate: Stream<boolean>;
}

export class PipelineTemplateWidget extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const disableLinks       = !vnode.attrs.pipelineConfig.isUsingTemplate();
    const doesTemplatesExist = vnode.attrs.templates() && vnode.attrs.templates().length > 0;

    if (!doesTemplatesExist) {
      return (<FlashMessage type={MessageType.warning}>
        <code>There are no templates configured or you are unauthorized to view the existing templates.
          Add one via the <a href="/go/admin/templates" title="Pipeline Templates">templates page</a>.</code>
      </FlashMessage>);
    }

    return <div>
      <div class={styles.templateWrapper}>
        {this.templateOptions(vnode)}
        <div class={classnames(styles.templateLink, {[styles.disabled]: disableLinks})}
             onclick={this.openViewTemplatePage.bind(this, vnode)}
             data-test-id="view-template">
          <Icons.View disabled={disableLinks} iconOnly={true}/> View
        </div>
        <div class={classnames(styles.templateLink, {[styles.disabled]: disableLinks})}
             onclick={this.openEditTemplatePage.bind(this, vnode)}
             data-test-id="edit-template">
          <Icons.Edit disabled={disableLinks} iconOnly={true}/> Edit
        </div>
      </div>
      {this.buttons(vnode)}
    </div>;
  }

  templateOptions({attrs}: { attrs: Attrs }) {
    const config = attrs.pipelineConfig;

    const templatesAsOptions = _.map(attrs.templates(), (template: Template) => {
      return {id: template.name, text: template.name} as Option;
    });

    return <SelectField label="Template"
                        property={config.template}
                        readonly={attrs.readonly}
                        errorText={config.errors().errorsForDisplay("template")}
                        onchange={this.clearStages.bind(this, attrs)}
                        required={true}>
      <SelectFieldOptions selected={config.template()} items={templatesAsOptions}/>
    </SelectField>;
  }

  private clearStages(attrs: Attrs) {
    attrs.pipelineConfig.stages(new NameableSet<Stage>());
  }

  private openViewTemplatePage(vnode: m.Vnode<Attrs>) {
    const template = vnode.attrs.pipelineConfig.template();
    if (template) {
      window.open(`/go/admin/templates#!${template}/view`);
    }
  }

  private renderConfirmation(vnode: m.Vnode<Attrs>) {
    const body = <p>Switching to a template will cause all of the currently defined stages in this pipeline to be lost.
      Are you sure you want to continue?
    </p>;
    new ConfirmationDialog("Confirm Save", body, () => {
      vnode.attrs.isPipelineDefinedOriginallyFromTemplate(true);
      return vnode.attrs.pipelineConfigSave();
    }).render();
  }

  private buttons(vnode: m.Vnode<Attrs>): m.Children {
    if (vnode.attrs.readonly) {
      return;
    }

    return <div className={pipelineConfigStyles.buttonContainer}>
      <Reset data-test-id={"cancel"}
             onclick={vnode.attrs.pipelineConfigReset}>
        RESET
      </Reset>
      <Primary data-test-id={"save"}
               disabled={!vnode.attrs.pipelineConfig.isUsingTemplate()}
               onclick={this.renderConfirmation.bind(this, vnode)}>
        SAVE
      </Primary>
    </div>;
  }

  private openEditTemplatePage(vnode: m.Vnode<Attrs>) {
    const template = vnode.attrs.pipelineConfig.template();
    if (template) {
      window.open(`/go/admin/templates/${template}/general`);
    }
  }
}
