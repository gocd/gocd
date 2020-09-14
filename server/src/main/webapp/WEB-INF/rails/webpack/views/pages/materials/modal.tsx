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
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {stringOrUndefined} from "models/compare/pipeline_instance_json";
import {MaterialModification} from "models/config_repos/types";
import {MaterialAPIs, MaterialModifications, MaterialUsages, MaterialWithFingerprint} from "models/materials/materials";
import {FlashMessage, FlashMessageModel, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {Link} from "views/components/link";
import linkStyles from "views/components/link/index.scss";
import {Modal, ModalState, Size} from "views/components/modal";
import {Spinner} from "views/components/spinner";
import {Table} from "views/components/table";
import spinnerCss from "views/pages/agents/spinner.scss";
import styles from "./index.scss";
import {MaterialWidget} from "./material_widget";

export class ShowModificationsModal extends Modal {
  errorMessage: Stream<string>                         = Stream();
  private material: MaterialWithFingerprint;
  private modifications: Stream<MaterialModifications> = Stream();
  private service: ApiService;
  private searchQuery: Stream<string>                  = Stream("");
  private operationInProgress: Stream<boolean>         = Stream();

  constructor(material: MaterialWithFingerprint, service: ApiService = new FetchHistoryService()) {
    super(Size.large);
    this.material = material;
    this.service  = service;
    this.fetchModifications();
  }

  body(): m.Children {
    const title     = `${this.material.typeForDisplay()} : ${this.material.displayName()}`;
    const searchBox = <div className={styles.searchBoxWrapper}>
      <SearchField property={this.searchQuery} dataTestId={"search-box"} name={"some-name"}
                   oninput={this.onPatternChange.bind(this)}
                   placeholder="Search in revision, comment or username"/>
    </div>;
    const header    = <HeaderPanel title={title} buttons={searchBox}/>;

    if (this.errorMessage()) {
      return <div data-test-id="modifications-modal" class={styles.modificationModal}>
        {header}
        <FlashMessage type={MessageType.alert} message={this.errorMessage()}/>
      </div>;
    }

    if (this.operationInProgress() || this.isLoading()) {
      return <div data-test-id="modifications-modal" class={styles.modificationModal}>
        {header}
        <div class={classnames(styles.modificationWrapper, styles.spinnerWrapper)}>
          <Spinner css={spinnerCss}/>
        </div>
      </div>;
    }

    if (_.isEmpty(this.modifications())) {
      const msg = _.isEmpty(this.searchQuery())
        ? "This material has not been parsed yet!"
        : <span>No modifications found for query: <i>{this.searchQuery()}</i></span>;
      return <div data-test-id="modifications-modal" class={styles.modificationModal}>
        {header}
        <div class={styles.modificationWrapper}>
          {msg}
        </div>
      </div>;
    }

    return <div data-test-id="modifications-modal" class={styles.modificationModal}>
      {header}
      <div class={styles.modificationWrapper}>
        {this.modifications().map((mod, index) => {
          const details = MaterialWidget.showModificationDetails(mod);
          ShowModificationsModal.updateWithVsmLink(details, mod, this.material.fingerprint());
          return <div data-test-id={`mod-${index}`} class={styles.modification}>
            <div class={styles.user}>
              <div class={styles.username} data-test-id="mod-username">{details.get("Username")}</div>
              <div class={styles.username} data-test-id="mod-modified-time">{details.get("Modified Time")}</div>
            </div>
            <div class={styles.commentWrapper} data-test-id="mod-comment">
              <CommentRenderer text={details.get("Comment")}/>
            </div>
            <div class={styles.rev} data-test-id="mod-rev">{details.get("Revision")}</div>
          </div>;
        })}
      </div>
      <PaginationWidget previousLink={this.modifications().previousLink} nextLink={this.modifications().nextLink}
                        onPageChange={this.onPageChange.bind(this)}/>
    </div>;
  }

  title(): string {
    return 'Modifications';
  }

  private static updateWithVsmLink(details: Map<string, m.Children>, mod: MaterialModification, fingerprint: string) {
    const vsmLink = <Link dataTestId={"vsm-link"} href={SparkRoutes.materialsVsmLink(fingerprint, mod.revision)}
                          title={"Value Stream Map"}>VSM</Link>;
    details.set("Revision", <div><span class={styles.revision}>{details.get("Revision")} </span>| {vsmLink}</div>);
  }

  private onPageChange(link: string) {
    this.fetchModifications(link);
  }

  private onPatternChange() {
    _.throttle(() => this.fetchModifications(), 500, {trailing: true})();
  }

  private fetchModifications(link?: string) {
    this.operationInProgress(true);
    this.service.fetchHistory(this.material.fingerprint(), this.searchQuery(), link,
                              (mods) => {
                                this.modifications(mods);
                                this.operationInProgress(false);
                                this.focusOnSearchBox();
                              },
                              (errMsg) => {
                                this.errorMessage(errMsg);
                                this.operationInProgress(false);
                              });

  }

  private focusOnSearchBox() {
    if (!_.isEmpty(this.searchQuery())) {
      document.getElementsByTagName('input')[1].focus();
    }
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
  fetchHistory(fingerprint: string, searchPattern: string, link: stringOrUndefined,
               onSuccess: (data: MaterialModifications) => void,
               onError: (message: string) => void): void;
}

class FetchHistoryService implements ApiService {
  fetchHistory(fingerprint: string, searchPattern: string, link: stringOrUndefined,
               onSuccess: (data: MaterialModifications) => void,
               onError: (message: string) => void): void {

    MaterialAPIs.modifications(fingerprint, searchPattern, link).then((result) => {
      result.do((successResponse) => onSuccess(successResponse.body),
                (errorResponse) => onError(errorResponse.message));
    });
  }

}

export class ShowUsagesModal extends Modal {
  usages?: MaterialUsages;
  private readonly name: string;
  private message: FlashMessageModel = new FlashMessageModel();

  constructor(material: MaterialWithFingerprint, usages?: MaterialUsages) {
    super();
    this.name = material.displayName();
    // used for testing
    if (usages) {
      this.usages = usages;
    } else {
      this.fetchUsages(material);
    }
  }

  title(): string {
    return 'Usages';
  }

  body(): m.Children {
    if (this.isLoading()) {
      return;
    }
    if (this.message.hasMessage()) {
      return <FlashMessage type={this.message.type} message={this.message.message}/>;
    }
    if (this.usages !== undefined && this.usages.length <= 0) {
      return (<i> No usages for material '{this.name}' found.</i>);
    }

    const data: m.Child[][] = [];
    if (this.usages !== undefined) {
      data.push(...this.usages
                       .map((pipeline: string, index) => {
                         return [
                           <span>{pipeline}</span>,
                           <Link href={SparkRoutes.pipelineEditPath('pipelines', pipeline, 'materials')} target={"_blank"}
                                 dataTestId={`material-link-${index}`}>View/Edit Material</Link>
                         ];
                       }));
    }
    return <div class={styles.usages}>
      <Table headers={["Pipeline", "Material Setting"]} data={data}/>
    </div>;
  }

  private fetchUsages(material: MaterialWithFingerprint) {
    this.modalState = ModalState.LOADING;
    MaterialAPIs.usages(material.fingerprint())
                .then((result) => {
                  result.do((successResponse) => {
                    this.usages = successResponse.body;
                  }, (errorResponse) => {
                    this.message.alert(JSON.parse(errorResponse.body!).message);
                  });
                }).finally(() => this.modalState = ModalState.OK);
  }
}

interface EllipseAttrs {
  text: string;
}

interface EllipseState {
  expanded: Stream<boolean>;
  setExpandedTo: (state: boolean, e: MouseEvent) => void;
}

class CommentRenderer extends MithrilComponent<EllipseAttrs, EllipseState> {
  private static MIN_CHAR_COUNT = 73;

  oninit(vnode: m.Vnode<EllipseAttrs, EllipseState>): any {
    vnode.state.expanded = Stream<boolean>(false);

    vnode.state.setExpandedTo = (state: boolean) => {
      vnode.state.expanded(state);
    };
  }

  view(vnode: m.Vnode<EllipseAttrs, EllipseState>): m.Children | void | null {
    const charactersToShow = Math.min(this.getCharCountToShow(vnode), vnode.attrs.text.length);
    if (this.shouldRenderWithoutEllipse(vnode)) {
      return <span>{vnode.attrs.text}</span>;
    }
    return <span class={classnames(styles.ellipseWrapper, styles.comment)}
                 data-test-id="ellipsized-content">
      {vnode.state.expanded() ? vnode.attrs.text : CommentRenderer.getEllipsizedString(vnode, charactersToShow)}
      {vnode.state.expanded() ? CommentRenderer.element(vnode, "less", false) : CommentRenderer.element(vnode, "more", true)}
      </span>;
  }

  private static getEllipsizedString(vnode: m.Vnode<EllipseAttrs, EllipseState>, charactersToShow: number) {
    return vnode.attrs.text.substr(0, charactersToShow).concat("...");
  }

  private static element(vnode: m.Vnode<EllipseAttrs, EllipseState>, text: string, state: boolean) {
    return <span data-test-id={`ellipse-action-${text}`} class={styles.ellipsisActionButton}
                 onclick={vnode.state.setExpandedTo.bind(this, state)}>{text}</span>;
  }

  private getCharCountToShow(vnode: m.Vnode<EllipseAttrs, EllipseState>) {
    return (vnode.attrs.text.includes('\n')
      ? Math.min(vnode.attrs.text.indexOf('\n'), CommentRenderer.MIN_CHAR_COUNT)
      : CommentRenderer.MIN_CHAR_COUNT);
  }

  private shouldRenderWithoutEllipse(vnode: m.Vnode<EllipseAttrs, EllipseState>) {
    return vnode.attrs.text.length <= CommentRenderer.MIN_CHAR_COUNT && !vnode.attrs.text.includes('\n');
  }
}
