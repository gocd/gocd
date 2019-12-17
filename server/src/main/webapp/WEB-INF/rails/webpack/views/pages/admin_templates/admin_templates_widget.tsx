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

import {docsUrl} from "gen/gocd_version";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {TemplateSummary} from "models/admin_templates/templates";
import {headerMeta} from "models/current_user_permissions";
import {PipelineStructure, PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import s from "underscore.string";
import {Anchor, ScrollManager} from "views/components/anchor/anchor";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Delete, Edit, IconGroup, Lock, View} from "views/components/icons";
import {Link} from "views/components/link";
import styles from "views/pages/admin_pipelines/admin_pipelines_widget.scss";
import {CreateOperation, DeleteOperation, EditOperation, SaveOperation} from "views/pages/page_operations";

interface Operations extends SaveOperation, EditOperation<TemplateSummary.TemplateSummaryTemplate>, DeleteOperation<TemplateSummary.TemplateSummaryTemplate>, CreateOperation<TemplateSummary.TemplateSummaryTemplate> {
  doEditPipeline: (pipelineName: string) => void;
  editPermissions: (template: TemplateSummary.TemplateSummaryTemplate) => void;
}

export interface TemplatesScrollOptions {
  sm: ScrollManager;
  shouldOpenReadOnlyView: boolean;
}

interface TemplateAttrs extends Operations {
  doShowTemplate: (templateName: string) => void;
  template: TemplateSummary.TemplateSummaryTemplate;
  pipelineStructure: PipelineStructure;
  scrollOptions: TemplatesScrollOptions;
}

export interface Attrs extends Operations {
  doShowTemplate: (templateName: string) => void;
  templates: TemplateSummary.TemplateSummaryTemplate[];
  pipelineStructure: PipelineStructure;
  scrollOptions: TemplatesScrollOptions;
}

interface PipelineWidgetAttrs {
  template: TemplateSummary.TemplateSummaryTemplate;
  pipeline: TemplateSummary.TemplateSummaryPipeline;
  pipelineStructure: PipelineStructure;
  doEditPipeline: (pipelineName: string) => void;
}

class PipelineWidget extends MithrilViewComponent<PipelineWidgetAttrs> {
  view(vnode: m.Vnode<PipelineWidgetAttrs, this>) {
    return (
      <div data-test-id={`pipeline-${s.slugify(vnode.attrs.pipeline.name)}`} class={styles.pipelineRow}>
        <div data-test-id={`pipeline-name-${s.slugify(vnode.attrs.pipeline.name)}`}
             class={styles.pipelineName}>{vnode.attrs.pipeline.name}</div>
        <div class={styles.pipelineActionButtons}>{this.actions(vnode)}</div>
      </div>
    );
  }

  private static messageForOperation(pipeline: PipelineWithOrigin | undefined,
                                     pipelineWithPermission: TemplateSummary.TemplateSummaryPipeline,
                                     operation: "edit") {
    if (!pipelineWithPermission.can_edit) {
      return `Cannot ${operation} pipeline '${pipelineWithPermission.name}' because you do do not have permission to edit it.`;
    } else {
      return `${s.capitalize(operation)} pipeline '${pipelineWithPermission.name}'`;
    }
  }

  private actions(vnode: m.Vnode<PipelineWidgetAttrs, this>) {
    const pipeline = vnode.attrs.pipelineStructure.findPipeline(vnode.attrs.pipeline.name);

    return (
      <IconGroup>
        <Edit
          disabled={!(vnode.attrs.pipeline.can_edit)}
          data-test-id={`edit-pipeline-${s.slugify(vnode.attrs.pipeline.name)}`}
          title={PipelineWidget.messageForOperation(pipeline, vnode.attrs.pipeline, "edit")}
          onclick={vnode.attrs.doEditPipeline.bind(vnode.attrs, vnode.attrs.pipeline.name)}/>
      </IconGroup>
    );
  }
}

class TemplateWidget extends MithrilViewComponent<TemplateAttrs> {
  view(vnode: m.Vnode<TemplateAttrs, this>) {
    return (<Anchor id={vnode.attrs.template.name}
                    sm={vnode.attrs.scrollOptions.sm}
                    onnavigate={() => {
                      if (vnode.attrs.scrollOptions.shouldOpenReadOnlyView) {
                        vnode.attrs.doShowTemplate.bind(vnode.attrs, vnode.attrs.template.name)();
                      }
                    }}>
        <div data-test-id={`template-${s.slugify(vnode.attrs.template.name)}`}
             class={styles.pipelineGroupRow}>
          <div data-test-id={`template-name-${s.slugify(vnode.attrs.template.name)}`}
               class={styles.pipelineGroupName}>Template: {vnode.attrs.template.name}</div>
          <div class={styles.pipelineGroupActionButtons}>{this.actions(vnode)}</div>
          {this.showPipelinesAssociatedWith(vnode)}
        </div>
      </Anchor>
    );
  }

  private showPipelinesAssociatedWith(vnode: m.Vnode<TemplateAttrs, this>) {
    const pipelines = vnode.attrs.template._embedded.pipelines;
    if (!_.isEmpty(pipelines)) {
      return pipelines.map((eachPipeline) => {
        return <PipelineWidget pipeline={eachPipeline} {...vnode.attrs}/>;
      });
    } else {
      return (
        <div class={styles.noPipelinesDefinedMessage}>
          <FlashMessage message="There are no pipelines associated with this template." type={MessageType.info}/>
        </div>
      );
    }
  }

  private actions(vnode: m.Vnode<TemplateAttrs, this>) {
    const template     = vnode.attrs.template;
    const templateName = template.name;
    return (
      <div>
        <span class={styles.iconGroupWrapper}>
          <IconGroup>
            <View
              title={`View template`}
              data-test-id={`edit-template-permissions-${s.slugify(templateName)}`}
              onclick={vnode.attrs.doShowTemplate.bind(vnode.attrs, templateName)}/>
            <Edit
              disabled={!template.can_edit}
              title={template.can_edit ? `Edit template ${templateName}` : `You do not have permissions to edit this template.`}
              data-test-id={`edit-template-${s.slugify(templateName)}`}
              onclick={vnode.attrs.onEdit.bind(vnode.attrs, template)}/>
            <Lock
              disabled={!headerMeta().isUserAdmin}
              title={headerMeta().isUserAdmin ? `Edit permissions for template ${templateName}` : `You do not have permissions to edit the permissions for this template. Only system administrators can edit templates.`}
              data-test-id={`edit-template-permissions-${s.slugify(templateName)}`}
              onclick={vnode.attrs.editPermissions.bind(vnode.attrs, template)}/>
            <Delete
              disabled={!this.isDeleteEnabled(vnode)}
              data-test-id={`delete-template-${s.slugify(templateName)}`}
              title={this.getDeleteButtonTitle(vnode)}
              onclick={vnode.attrs.onDelete.bind(vnode.attrs, template)}/>
          </IconGroup>
        </span>
      </div>
    );
  }

  private getDeleteButtonTitle(vnode: m.Vnode<TemplateAttrs, this>) {
    if (!vnode.attrs.template.is_admin) {
      return `You do not have permissions to delete this template.`;
    }
    if (_.isEmpty(vnode.attrs.template._embedded.pipelines)) {
      return `Delete template '${vnode.attrs.template.name}'.`;
    }
    return `Cannot delete the template '${vnode.attrs.template.name}' because it is in use by pipelines.`;
  }

  private isDeleteEnabled(vnode: m.Vnode<TemplateAttrs, this>) {
    return vnode.attrs.template.is_admin && _.isEmpty(vnode.attrs.template._embedded.pipelines);
  }
}

export class AdminTemplatesWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const templateUrl = "configuration/pipeline_templates.html";
    const docLink     = <span data-test-id="doc-link">
       <Link href={docsUrl(templateUrl)} target="_blank" externalLinkIcon={true}>
        Learn More
      </Link>
    </span>;

    if (vnode.attrs.scrollOptions.sm.hasTarget()) {
      const target           = vnode.attrs.scrollOptions.sm.getTarget();
      const hasAnchorElement = vnode.attrs.templates.some((temp) => temp.name === target);
      if (!hasAnchorElement) {
        const msg = `Either '${target}' template has not been set up or you are not authorized to view the same.`;
        return <FlashMessage dataTestId="anchor-template-not-present" type={MessageType.alert}>
          {msg} {docLink}
        </FlashMessage>;
      }
    }
    if (_.isEmpty(vnode.attrs.templates)) {
      const noTemplatesFoundMsg = <span>
      Either no templates have been set up or you are not authorized to view the same. {docLink}
    </span>;

      return <FlashMessage type={MessageType.info} message={noTemplatesFoundMsg}
                           dataTestId="no-template-present-msg"/>;
    }
    return (
      <div data-test-id="templates">
        {vnode.attrs.templates.map((eachTemplate) => {
          return <TemplateWidget template={eachTemplate} {...vnode.attrs} />;
        })}
      </div>
    );

  }
}
