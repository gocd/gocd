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

import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import {PipelineGroup} from "models/new-environments/pipeline_groups";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {CheckboxField, SearchField, TriStateCheckboxField} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons/index";
import {Modal, ModalState, Size} from "views/components/modal";
import {PipelinesViewModel} from "views/pages/new-environments/models/pipelines_view_model";
import styles from "./edit_pipelines.scss";

const classnames = bind(styles);

interface PipelineGroupWidgetAttrs {
  group: PipelineGroup;
  pipelinesVM: PipelinesViewModel;
}

export class PipelineGroupWidget extends MithrilViewComponent<PipelineGroupWidgetAttrs> {
  view(vnode: m.Vnode<PipelineGroupWidgetAttrs>) {
    const pipelinesVM = vnode.attrs.pipelinesVM;
    const group       = vnode.attrs.group;
    const isExpanded  = pipelinesVM.searchText() || pipelinesVM.isPipelineGroupExpanded(group.name());

    const expandedIconForGroup = isExpanded
      ? <Icons.ChevronDown iconOnly={true}
                           onclick={pipelinesVM.togglePipelineGroupState.bind(pipelinesVM, group.name())}/>
      : <Icons.ChevronRight iconOnly={true}
                            onclick={pipelinesVM.togglePipelineGroupState.bind(pipelinesVM, group.name())}/>;

    return <div class={styles.pipelineGroupWrapper}>
      <div class={styles.groupCheckbox} data-test-id="pipeline-group-checkbox"
           data-test-expanded-state={isExpanded ? "on" : "off"}>
        <div class={styles.expandedIconWrapper}>{expandedIconForGroup}</div>
        <TriStateCheckboxField label={group.name()}
                               onchange={pipelinesVM.toggleGroupSelectionFn(group)}
                               property={pipelinesVM.groupSelectedFn(group)}/>
      </div>
      <div className={classnames(styles.pipelinesWrapper, {[styles.expanded]: isExpanded})}>
        {
          group.pipelines().map((pipeline) => {
            return <div class={styles.pipelineCheckbox} data-test-id={`pipeline-checkbox-for-${pipeline.name()}`}>
              <CheckboxField label={pipeline.name()}
                             property={vnode.attrs.pipelinesVM.pipelineSelectedFn(pipeline)}/>
            </div>;
          })
        }
      </div>
    </div>;
  }
}

interface SelectAllNoneWidgetAttrs {
  pipelinesVM: PipelinesViewModel;
}

export class PipelineFilterWidget extends MithrilViewComponent<SelectAllNoneWidgetAttrs> {
  view(vnode: m.Vnode<SelectAllNoneWidgetAttrs>) {
    return <div class={styles.pipelineFilterWrapper}>
      <SearchField label="pipeline-search" placeholder="Search for a pipeline"
                   property={vnode.attrs.pipelinesVM.searchText}/>
    </div>;
  }
}

export class EditPipelinesModal extends Modal {
  readonly pipelinesVM: PipelinesViewModel;

  constructor(env: EnvironmentWithOrigin) {
    super(Size.medium);
    this.modalState  = ModalState.LOADING;
    this.fixedHeight = true;
    this.pipelinesVM = new PipelinesViewModel(env);
  }

  oninit() {
    super.oninit();
    this.pipelinesVM.fetchAllPipelines(() => {
      this.modalState = ModalState.OK;
    });
  }

  title(): string {
    return "Edit Pipelines Association";
  }

  body(): m.Children {
    const pipelineGroups = this.pipelinesVM.filteredPipelineGroups();
    if (!pipelineGroups) {
      return;
    }

    return <div>
      <FlashMessage type={MessageType.alert} message={this.pipelinesVM.errorMessage()}/>
      <PipelineFilterWidget pipelinesVM={this.pipelinesVM}/>
      <div class={styles.pipelineGroupsWrapper}>
        {pipelineGroups.map((group) => <PipelineGroupWidget group={group} pipelinesVM={this.pipelinesVM}/>)}
      </div>
    </div>;
  }

}
