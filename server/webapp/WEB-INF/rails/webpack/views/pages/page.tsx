/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import {HeaderPanel} from "views/components/header_panel";
import {PageLoadError} from "views/components/page_load_error";
import {Spinner} from "views/components/spinner";

enum PageState {
  LOADING, OK, FAILED
}

export abstract class Page extends MithrilViewComponent {

  private pageState: PageState = PageState.LOADING;

  abstract componentToDisplay(): JSX.Element | undefined;

  abstract pageName(): string;

  abstract fetchData(): Promise<any>;

  oninit() {
    return this.fetchData().then(this._onSuccess.bind(this), this._onFailure.bind(this));
  }

  view() {
    switch (this.pageState) {
      case PageState.FAILED:
        return <PageLoadError message={`There was a problem fetching ${this.pageName()}`}/>;
      case PageState.LOADING:
        return <Spinner/>;
      case PageState.OK:
        const component = this.componentToDisplay();
        if (component) {
          return <main className="main-container">
            <HeaderPanel title={this.pageName()}/>
            {component}
          </main>;
        } else {
          return <PageLoadError message={`There was a problem fetching ${this.pageName()}`}/>;
        }

    }
    return <PageLoadError message={`There was a problem fetching ${this.pageName()}`}/>;
  }

  private _onSuccess() {
    this.pageState = PageState.OK;
  }

  private _onFailure() {
    this.pageState = PageState.FAILED;
  }
}
