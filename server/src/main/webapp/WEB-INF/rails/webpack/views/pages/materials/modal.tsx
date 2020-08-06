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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {stringOrUndefined} from "models/compare/pipeline_instance_json";
import {MaterialAPIs, MaterialModifications, MaterialWithFingerprint} from "models/materials/materials";
import {FlashMessage, MessageType} from "views/components/flash_message";
import linkStyles from "views/components/link/index.scss";
import {Modal, ModalState, Size} from "views/components/modal";
import styles from "./index.scss";
import {MaterialWidget} from "./material_widget";

export class ShowModificationsModal extends Modal {
  errorMessage: Stream<string>                 = Stream();
  private material: MaterialWithFingerprint;
  private modifications: Stream<MaterialModifications> = Stream();
  private service: ApiService;

  constructor(material: MaterialWithFingerprint, service: ApiService = new FetchHistoryService()) {
    super(Size.large);
    this.material = material;
    this.service  = service;
    this.fetchModifications();
  }

  body(): m.Children {
    if (this.isLoading()) {
      return;
    }

    if (this.errorMessage()) {
      return <FlashMessage type={MessageType.alert} message={this.errorMessage()}/>;
    }

    const onPageChange = (link: string) => {
      this.fetchModifications(link);
    };

    return <div data-test-id="modifications-modal">
      {this.modifications().map((mod, index) => {
        return <div data-test-id={`modification-${index}`} class={styles.modification}>
          {MaterialWidget.showModificationDetails(mod)}
        </div>;
      })}
      <PaginationWidget previousLink={this.modifications().previousLink} nextLink={this.modifications().nextLink}
                        onPageChange={onPageChange}/>
    </div>;
  }

  title(): string {
    return `Show Modifications for '${this.material.displayName() || this.material.typeForDisplay()}'`;
  }

  private fetchModifications(link?: string) {
    this.modalState = ModalState.LOADING;
    this.service.fetchHistory(this.material.fingerprint(), link,
                              (mods) => {
                                this.modifications(mods);
                                this.modalState = ModalState.OK;
                              },
                              (errMsg) => {
                                this.errorMessage(errMsg);
                                this.modalState = ModalState.OK;
                              });
  }
}

interface PaginationAttrs {
  previousLink: stringOrUndefined;
  nextLink: stringOrUndefined;
  onPageChange: (link: string) => void;
}

class PaginationWidget extends MithrilViewComponent<PaginationAttrs> {
  view(vnode: m.Vnode<PaginationAttrs, this>): m.Children | void | null {
    const hasPreviousPage = vnode.attrs.previousLink !== undefined;
    const hasNextPage     = vnode.attrs.nextLink !== undefined;
    const onPreviousClick = (e: MouseEvent) => {
      e.stopPropagation();
      if (hasPreviousPage) {
        vnode.attrs.onPageChange(vnode.attrs.previousLink!);
      }
    };
    const onNextClick     = (e: MouseEvent) => {
      e.stopPropagation();
      if (hasNextPage) {
        vnode.attrs.onPageChange(vnode.attrs.nextLink!);
      }
    };

    return <div data-test-id="pagination" className={styles.pagination}>
      <a title="Previous"
         role="button"
         className={classnames(linkStyles.inlineLink, styles.paginationLink,
                               {[styles.disabled]: !hasPreviousPage})}
         href="#"
         onclick={onPreviousClick}>Previous</a>
      <a title="Next"
         role="button"
         className={classnames(linkStyles.inlineLink, styles.paginationLink,
                               {[styles.disabled]: !hasNextPage})}
         href="#"
         onclick={onNextClick}>Next</a>
    </div>;
  }
}

export interface ApiService {
  fetchHistory(fingerprint: string, link: stringOrUndefined,
               onSuccess: (data: MaterialModifications) => void,
               onError: (message: string) => void): void;
}

class FetchHistoryService implements ApiService {
  fetchHistory(fingerprint: string, link: stringOrUndefined,
               onSuccess: (data: MaterialModifications) => void,
               onError: (message: string) => void): void {

    MaterialAPIs.modifications(fingerprint, link).then((result) => {
      result.do((successResponse) => onSuccess(successResponse.body),
                (errorResponse) => onError(errorResponse.message));
    });
  }

}
