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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {footerMeta, headerMeta} from "models/current_user_permissions";
import {DataSharingCleaner} from "models/shared/data_sharing_cleaner";
import {VersionUpdater} from "models/shared/version_updater";
import {ModalManager} from "views/components/modal/modal_manager";
import {Attrs as SiteFooterAttrs, SiteFooter} from "views/pages/partials/site_footer";
import {Attrs as SiteHeaderAttrs, SiteHeader} from "views/pages/partials/site_header";
import styles from "./spa_base.scss";

interface Attrs {
  showHeader: boolean;
  showFooter: boolean;
  headerData: SiteHeaderAttrs;
  footerData: SiteFooterAttrs;
}

class MainPage extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return (
      <div class={styles.page}>
        <div class={styles.pagewrap}>
          {this.showHeader(vnode)}
          <main class={styles.mainContainer}>
            {vnode.children}
          </main>
        </div>
        {this.showFooter(vnode)}
      </div>
    );
  }

  private showFooter(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.showFooter) {
      return <SiteFooter {...vnode.attrs.footerData}/>;
    }
  }

  private showHeader(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.showHeader) {
      return <SiteHeader {...vnode.attrs.headerData}/>;
    }
  }
}

abstract class AbstractPage {
  protected render() {
    window.addEventListener("DOMContentLoaded", () => {
      if (this.enableUsageDataAndVersionUpdating()) {
        VersionUpdater.update();
      }

      DataSharingCleaner.clean();

      const footerData = footerMeta();
      const headerData = headerMeta();

      this.contents({headerData, footerData, showHeader: this.showHeader(), showFooter: this.showFooter()});
      ModalManager.onPageLoad();
    });
  }

  protected abstract contents(pageAttrs: Attrs): void;

  protected showHeader() {
    return true;
  }

  protected showFooter() {
    return true;
  }

  protected enableUsageDataAndVersionUpdating() {
    return true;
  }
}

type GoCDComponentTypes = (new(...args: any[]) => m.Component<any, any>) | (() => m.Component<any, any>) | m.ComponentTypes<any,any>; // slightly more specialized than m.ComponentTypes

interface RoutesTable {
  [route: string]: GoCDComponentTypes;
}

export abstract class RoutedSinglePageApp extends AbstractPage {
  private readonly routes: RoutesTable;

  constructor(routes: RoutesTable) {
    super();
    this.routes = routes;
    this.render();
  }

  contents(attrs: Attrs) {
    const routes             = Object.keys(this.routes);
    const table: m.RouteDefs = {};

    for (const route of routes) {
      table[route] = this.componentToDisplay(this.routes[route], attrs);
    }

    m.route(document.body, "", table);
  }

  componentToDisplay(contents: m.ComponentTypes<any, any>, attrs: Attrs): m.Component<any, any> {
    return {
      view: () => <MainPage {...attrs}>{m(contents)}</MainPage>
    };
  }
}

export abstract class SinglePageAppBase extends AbstractPage {
  private readonly pageToMount: GoCDComponentTypes;

  constructor(pageToMount: GoCDComponentTypes) {
    super();
    this.pageToMount = pageToMount;
    this.render();
  }

  contents(attrs: Attrs) {
    m.mount(document.body, {
      view: () => <MainPage {...attrs}>{m(this.pageToMount)}</MainPage>
    });
  }
}
