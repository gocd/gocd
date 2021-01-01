/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import Stream from "mithril/stream";
import {PipelineGroup, PipelineGroups, PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import s from "underscore.string";
import {Anchor, ScrollManager} from "views/components/anchor/anchor";
import {ButtonIcon, Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {ChevronRightCircle, Clone, Delete, Download, Edit, IconGroup, Plus} from "views/components/icons";
import {Link} from "views/components/link";
import {SaveOperation} from "views/pages/page_operations";
import styles from "./admin_pipelines_widget.scss";

interface Operations extends SaveOperation {
  doClonePipeline: (pipeline: PipelineWithOrigin) => void;
  doMovePipeline: (sourceGroup: PipelineGroup, pipeline: PipelineWithOrigin) => void;
  doEditPipeline: (pipeline: PipelineWithOrigin) => void;
  doDownloadPipeline: (pipeline: PipelineWithOrigin) => void;
  doDeletePipeline: (pipeline: PipelineWithOrigin) => void;
  doExtractPipeline: (pipeline: PipelineWithOrigin) => void;
  doEditPipelineGroup: (groupName: string) => void;
  doDeleteGroup: (group: PipelineGroup) => void;
  createPipelineInGroup: (groupName: string) => void;
}

interface PipelineGroupAttrs extends Operations {
  group: PipelineGroup;
  scrollOptions: PipelinesScrollOptions;
  canMovePipeline: boolean;
}

export interface PipelinesScrollOptions {
  sm: ScrollManager;
  shouldOpenEditView: boolean;
}

export interface Attrs extends Operations {
  pipelineGroups: Stream<PipelineGroups>;
  createPipelineGroup: () => void;
  scrollOptions: PipelinesScrollOptions;
}

type PipelineWidgetAttrs = PipelineGroupAttrs & { pipeline: PipelineWithOrigin; };

class PipelineWidget extends MithrilViewComponent<PipelineWidgetAttrs> {
  view(vnode: m.Vnode<PipelineWidgetAttrs, this>) {
    return (
      <div data-test-id={`pipeline-${s.slugify(vnode.attrs.pipeline.name())}`} class={styles.pipelineRow}>
        <div data-test-id={`pipeline-name-${s.slugify(vnode.attrs.pipeline.name())}`}
             class={styles.pipelineName}>{vnode.attrs.pipeline.name()}</div>
        <div class={styles.pipelineActionButtons}>{this.actions(vnode, vnode.attrs.pipeline)}</div>
      </div>
    );
  }

  private static messageForOperation(pipeline: PipelineWithOrigin,
                                     operation: "move" | "clone" | "edit" | "delete" | "extract template from") {
    if (operation === "extract template from" && pipeline.usesTemplate()) {
      return `Cannot ${operation} pipeline '${pipeline.name()}' because it uses a template.`;
    }
    if (operation === "delete") {
      if (pipeline.isDefinedRemotely()) {
        return `Cannot delete pipeline '${pipeline.name()}' defined in configuration repository '${pipeline.origin().id()}'.`;
      }
      if (pipeline.environment() !== undefined && pipeline.environment() !== null) {
        return `Cannot delete pipeline '${pipeline.name()}' as it is present in environment '${pipeline.environment()}'.`;
      }
      if (pipeline.dependantPipelines() !== undefined && pipeline.dependantPipelines()!.length > 0) {
        const dependentPipelineNames = pipeline.dependantPipelines().map(d => d.dependent_pipeline_name);
        return `Cannot delete pipeline '${pipeline.name()}' as pipeline(s) '${dependentPipelineNames}' depends on it.`;
      }
    }

    return `${s.capitalize(operation)} pipeline '${pipeline.name()}'`;
  }

  private static messageForMove(pipeline: PipelineWithOrigin, canMovePipeline: boolean) {
    if (pipeline.origin().isDefinedInConfigRepo()) {
      return `Cannot move pipeline '${pipeline.name()}' as it is defined in configuration repository '${pipeline.origin().id()}'.`;
    }
    if (!canMovePipeline) {
      return `Cannot move pipeline '${pipeline.name()}' as there are no other group(s).`;
    }
    return `Move pipeline '${pipeline.name()}'`;
  }

  private actions(vnode: m.Vnode<PipelineWidgetAttrs, this>, eachPipeline: PipelineWithOrigin) {
    const titleForMove = PipelineWidget.messageForMove(eachPipeline, vnode.attrs.canMovePipeline);
    return (
      <IconGroup>
        <Edit
          data-test-id={`edit-pipeline-${s.slugify(eachPipeline.name())}`}
          title={PipelineWidget.messageForOperation(eachPipeline, "edit")}
          onclick={vnode.attrs.doEditPipeline.bind(vnode.attrs, eachPipeline)}/>
        <ChevronRightCircle
          disabled={eachPipeline.origin().isDefinedInConfigRepo() || !vnode.attrs.canMovePipeline}
          data-test-id={`move-pipeline-${s.slugify(eachPipeline.name())}`}
          title={titleForMove}
          onclick={vnode.attrs.doMovePipeline.bind(vnode.attrs, vnode.attrs.group, eachPipeline)}/>
        <Download
          data-test-id={`download-pipeline-${s.slugify(eachPipeline.name())}`}
          title={`Download pipeline configuration for '${eachPipeline.name()}'`}
          onclick={vnode.attrs.doDownloadPipeline.bind(vnode.attrs, eachPipeline)}/>
        <Clone
          disabled={eachPipeline.origin().isDefinedInConfigRepo()}
          data-test-id={`clone-pipeline-${s.slugify(eachPipeline.name())}`}
          title={PipelineWidget.messageForOperation(eachPipeline, "clone")}
          onclick={vnode.attrs.doClonePipeline.bind(vnode.attrs, eachPipeline)}/>
        <Delete
          disabled={!eachPipeline.canBeDeleted()}
          data-test-id={`delete-pipeline-${eachPipeline.name()}`}
          title={PipelineWidget.messageForOperation(eachPipeline, "delete")}
          onclick={vnode.attrs.doDeletePipeline.bind(vnode.attrs, eachPipeline)}/>
        <Plus
          disabled={eachPipeline.origin().isDefinedInConfigRepo() || eachPipeline.usesTemplate()}
          data-test-id={`extract-template-from-pipeline-${eachPipeline.name()}`}
          title={PipelineWidget.messageForOperation(eachPipeline, "extract template from")}
          onclick={vnode.attrs.doExtractPipeline.bind(vnode.attrs, eachPipeline)}/>
      </IconGroup>
    );
  }
}

class PipelineGroupWidget extends MithrilViewComponent<PipelineGroupAttrs> {
  view(vnode: m.Vnode<PipelineGroupAttrs, this>) {
    const grpName    = vnode.attrs.group.name();
    const onNavigate = () => {
      if (vnode.attrs.scrollOptions.sm.getTarget() === grpName && vnode.attrs.scrollOptions.shouldOpenEditView) {
        vnode.attrs.doEditPipelineGroup(grpName);
      }
    };
    return (<Anchor id={grpName} sm={vnode.attrs.scrollOptions.sm} onnavigate={onNavigate}>
        <div data-test-id={`pipeline-group-${s.slugify(grpName)}`}
             class={styles.pipelineGroupRow}>
          <div data-test-id={`pipeline-group-name-${s.slugify(grpName)}`}
               class={styles.pipelineGroupName}>
            <span>Pipeline Group:</span>
            <span data-test-id="pipeline-group-name" class={styles.value}>{grpName}</span>
          </div>
          <div class={styles.pipelineGroupActionButtons}>{this.actions(vnode)}</div>
          {this.showPipelines(vnode)}
        </div>
      </Anchor>
    );
  }

  private showPipelines(vnode: m.Vnode<PipelineGroupAttrs, this>) {
    if (vnode.attrs.group.hasPipelines()) {
      return vnode.attrs.group.pipelines().map((eachPipeline) => {
        return <PipelineWidget pipeline={eachPipeline} {...vnode.attrs}/>;
      });
    } else {
      return (
        <div class={styles.noPipelinesDefinedMessage}>
          <FlashMessage message="There are no pipelines defined in this pipeline group." type={MessageType.info}/>
        </div>
      );
    }
  }

  private actions(vnode: m.Vnode<PipelineGroupAttrs, this>) {
    return (
      <div>
        <Primary icon={ButtonIcon.ADD}
                 dataTestId={`create-pipeline-in-group-${s.slugify(vnode.attrs.group.name())}`}
                 onclick={vnode.attrs.createPipelineInGroup.bind(vnode.attrs, vnode.attrs.group.name())}>
          Add new pipeline
        </Primary>
        <span class={styles.iconGroupWrapper}>
          <IconGroup>
            <Edit
              data-test-id={`edit-pipeline-group-${s.slugify(vnode.attrs.group.name())}`}
              onclick={() => vnode.attrs.doEditPipelineGroup(vnode.attrs.group.name())}/>
            <Delete disabled={vnode.attrs.group.hasPipelines()}
                    data-test-id={`delete-pipeline-group-${s.slugify(vnode.attrs.group.name())}`}
                    title="Move or delete all pipelines within this group in order to delete it."
                    onclick={vnode.attrs.doDeleteGroup.bind(vnode.attrs, vnode.attrs.group)}/>
          </IconGroup>
        </span>
      </div>
    );
  }
}

export class PipelineGroupsWidget extends MithrilViewComponent<Attrs> {

  public static helpTextWhenEmpty() {
    return <ul data-test-id="pipelines-help-text">
      <li>Only GoCD system administrators are allowed to create a pipeline group.
        <Link href={docsUrl("configuration/pipelines.html")} externalLinkIcon={true}> Learn More</Link>
      </li>
      <li>A GoCD Administrator can authorize users and roles to be administrators for pipeline groups.
        <Link href={docsUrl("configuration/delegating_group_administration.html")} externalLinkIcon={true}> Learn More</Link>
      </li>
    </ul>;
  }

  view(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.scrollOptions.sm.hasTarget()) {
      const target    = vnode.attrs.scrollOptions.sm.getTarget();
      const hasTarget = vnode.attrs.pipelineGroups().some((grp) => grp.name() === target);
      if (!hasTarget) {
        const pipelineUrl = "configuration/pipeline_group_admin_config.html";
        const docLink     = <span data-test-id="doc-link">
       <Link href={docsUrl(pipelineUrl)} target="_blank" externalLinkIcon={true}>
        Learn More
      </Link>
    </span>;
        const msg         = `Either '${target}' pipeline group has not been set up or you are not authorized to view the same.`;
        return <FlashMessage dataTestId="anchor-pipeline-grp-not-present" type={MessageType.alert}>
          {msg} {docLink}
        </FlashMessage>;
      }
    }
    if (_.isEmpty(vnode.attrs.pipelineGroups())) {
      const pipelineUrl = "configuration/pipelines.html";
      const docLink     = <span data-test-id="doc-link">
       <Link href={docsUrl(pipelineUrl)} target="_blank" externalLinkIcon={true}>
        Learn More
      </Link>
    </span>;
      return [
        <FlashMessage type={MessageType.info}>
          Either no pipelines have been defined or you are not authorized to view the same. {docLink}
        </FlashMessage>,
        <div className={styles.tips}>
          {PipelineGroupsWidget.helpTextWhenEmpty()}
        </div>
      ];
    }
    const canMovePipeline = vnode.attrs.pipelineGroups().length > 1;
    return (
      <div data-test-id="pipeline-groups">
        {vnode.attrs.pipelineGroups().map((group) => {
          return <PipelineGroupWidget canMovePipeline={canMovePipeline} group={group} {...vnode.attrs} />;
        })}
      </div>
    );
  }
}
