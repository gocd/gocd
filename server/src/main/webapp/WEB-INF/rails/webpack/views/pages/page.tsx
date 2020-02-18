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
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {PageLoadError} from "views/components/page_load_error";
import {Spinner} from "views/components/spinner";

export enum PageState {
  LOADING, OK, FAILED
}

export abstract class Page<Attrs = {}, State = {}> extends MithrilComponent<Attrs, State> {
  protected flashMessage = new FlashMessageModelWithTimeout(10000);

  protected pageState: PageState = PageState.LOADING;

  abstract componentToDisplay(vnode: m.Vnode<Attrs, State>): m.Children;

  abstract pageName(): string;

  abstract fetchData(vnode: m.Vnode<Attrs, State>): Promise<any>;

  oninit(vnode: m.Vnode<Attrs, State>) {
    this.fetchData(vnode).then(this._onSuccess.bind(this), this._onFailure.bind(this));
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children {
    switch (this.pageState) {
      case PageState.FAILED:
        return <PageLoadError message={`There was a problem fetching ${this.pageName()}`}/>;
      case PageState.LOADING:
        return <Spinner/>;
      case PageState.OK:
        const component = this.componentToDisplay(vnode);
        if (component) {
          return <main>
            {this.headerPanel(vnode)}
            {component}
          </main>;
        } else {
          return <PageLoadError message={`There was a problem fetching ${this.pageName()}`}/>;
        }
    }
    return <PageLoadError message={`There was a problem fetching ${this.pageName()}`}/>;
  }

  protected headerPanel(vnode: m.Vnode<Attrs, State>) {
    return <HeaderPanel title={this.pageName()}/>;
  }

  protected setErrorState() {
    this.pageState = PageState.FAILED;
  }

  protected getMeta(): any {
    const meta = document.body.getAttribute("data-meta");
    return meta ? JSON.parse(meta) : {};
  }

  protected scrollToTop(): void {
    window.scrollTo({
                      top: 0,
                      left: 0,
                      behavior: "smooth"
                    });
  }

  protected static readAttribute(name: string) {
    return document.body.getAttribute(name);
  }

  protected static isUserAnAdmin() {
    const attribute = Page.readAttribute("data-is-user-admin");
    return attribute ? attribute.toLowerCase() === "true" : false;
  }

  protected static isUserAGroupAdmin() {
    const attribute = Page.readAttribute("data-is-user-group-admin");
    return attribute ? attribute.toLowerCase() === "true" : false;
  }

  private _onSuccess() {
    this.pageState = PageState.OK;
  }

  private _onFailure() {
    this.pageState = PageState.FAILED;
  }
}
