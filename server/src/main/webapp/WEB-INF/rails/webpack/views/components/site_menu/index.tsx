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
import classNames from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import styles from "./index.scss";

const classnames = classNames.bind(styles);

const activeItem = (href?: string): boolean => {
  if (!!href) {
    return new URL(href, window.location.origin).pathname === window.location.pathname;
  } else {
    return false;
  }
};

interface SiteHeaderLinkAttrs {
  isNavLink?: boolean;
  target?: string;
  href?: string;
}

class SiteHeaderLink extends MithrilViewComponent<SiteHeaderLinkAttrs> {
  view(vnode: m.Vnode<SiteHeaderLinkAttrs>) {

    const classes = classnames({
      [styles.siteNavLink]: vnode.attrs.isNavLink,
      [styles.siteSubNavLink]: !vnode.attrs.isNavLink,
      [styles.active]: !vnode.attrs.isNavLink && activeItem(vnode.attrs.href),
    });

    return (
      <a class={classes}
         href={vnode.attrs.href || "#"}
         target={vnode.attrs.target || ""}>
        {vnode.children}
      </a>
    );
  }
}

interface Text {
  text?: string;
}

interface TextWithLink extends Text {
  href?: string;
}

interface SiteNavItemAttrs extends TextWithLink {
  isDropDown?: boolean;
}

class SiteNavItem extends MithrilViewComponent<SiteNavItemAttrs> {
  view(vnode: m.Vnode<SiteNavItemAttrs>) {
    const dropDownClass = classnames({
      [styles.isDropDown]: vnode.attrs.isDropDown,
      [styles.active]: activeItem(vnode.attrs.href) || this.isActive(vnode),
    }, styles.siteNavItem);

    if (!vnode.attrs.isDropDown) {
      return (
        <li class={dropDownClass}>
          <SiteHeaderLink href={vnode.attrs.href}
                          isNavLink={true}>
            {vnode.attrs.text}
          </SiteHeaderLink>
        </li>
      );
    }

    return (
      <li class={dropDownClass}>
        <SiteHeaderLink isNavLink={true}>
          {vnode.attrs.text}
        </SiteHeaderLink>
        <i class={styles.caretDownIcon}/>
        {vnode.children}
      </li>
    );
  }

  isActive(vnode: m.Vnode<SiteNavItemAttrs>) {
    if (!vnode.attrs.isDropDown || !vnode.attrs.text) {
      return false;
    }
    return window.location.pathname.toLowerCase().startsWith(`/go/${vnode.attrs.text.toLowerCase()}`);
  }
}

class SiteSubNav extends MithrilViewComponent<{}> {
  view(vnode: m.Vnode<{}>) {
    return (
      <ul class={styles.siteSubNav}>
        {vnode.children}
      </ul>
    );
  }
}

export class SiteSubNavItem extends MithrilViewComponent<TextWithLink> {
  view(vnode: m.Vnode<TextWithLink>) {
    return (
      <li class={styles.siteSubNavItem}>
        <SiteHeaderLink href={vnode.attrs.href}
                        isNavLink={false}>
          {vnode.attrs.text}
        </SiteHeaderLink>
      </li>
    );
  }
}

class SiteSubNavHeading extends MithrilViewComponent<Text> {
  view(vnode: m.Vnode<Text>) {
    return (
      <li class={styles.siteSubNavItem}>
        <h5 class={styles.siteSubNavLinkHead}>{vnode.attrs.text}</h5>
      </li>
    );
  }
}

export interface Attrs {
  canViewTemplates: boolean;
  isGroupAdmin: boolean;
  isUserAdmin: boolean;
  canViewAdminPage: boolean;
  showAnalytics: boolean;
}

export class SiteMenu extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {

    const analyticsMenu: m.Children = vnode.attrs.showAnalytics ?
      <SiteNavItem href="/go/analytics" text="Analytics"/> : null;

    let adminMenu = null;
    if (vnode.attrs.canViewAdminPage) {
      if (vnode.attrs.isUserAdmin) {
        adminMenu = (
          <SiteNavItem isDropDown={true} text="Admin">
            <div class={styles.subNavigation}>
              <SiteSubNav>
                <SiteSubNavItem href="/go/admin/pipelines" text="Pipelines"/>
                <SiteSubNavItem href="/go/admin/environments" text="Environments"/>
                <SiteSubNavItem href="/go/admin/templates" text="Templates"/>
                <SiteSubNavItem href="/go/admin/config_xml" text="Config XML"/>
                <SiteSubNavItem href="/go/admin/package_repositories/new" text="Package Repositories"/>
              </SiteSubNav>
              <SiteSubNav>
                <SiteSubNavItem href="/go/admin/elastic_agent_configurations" text="Elastic Agent Configurations"/>
                <SiteSubNavItem href="/go/admin/config_repos" text="Config Repositories"/>
                <SiteSubNavItem href="/go/admin/artifact_stores" text="Artifact Stores"/>
                <SiteSubNavItem href="/go/admin/secret_configs" text="Secret Management"/>
                <SiteSubNavItem href="/go/admin/scms" text="Pluggable SCMs"/>
              </SiteSubNav>
              <SiteSubNav>
                <SiteSubNavHeading text="Server configuration"/>
                <SiteSubNavItem href="/go/admin/config/server" text="Server Configuration"/>
                <SiteSubNavItem href="/go/admin/maintenance_mode" text="Server Maintenance Mode"/>
                <SiteSubNavItem href="/go/admin/backup" text="Backup"/>
                <SiteSubNavItem href="/go/admin/plugins" text="Plugins"/>
              </SiteSubNav>
              <SiteSubNav>
                <SiteSubNavHeading text="Security"/>
                <SiteSubNavItem href="/go/admin/security/auth_configs" text="Authorization Configuration"/>
                <SiteSubNavItem href="/go/admin/security/roles" text="Role configuration"/>
                <SiteSubNavItem href="/go/admin/users" text="Users Management"/>
                <SiteSubNavItem href="/go/admin/admin_access_tokens" text="Access Tokens Management"/>
              </SiteSubNav>
            </div>
          </SiteNavItem>
        );
      } else if (vnode.attrs.isGroupAdmin) {
        adminMenu = <SiteNavItem isDropDown={true} text="Admin">
          <div class={classnames(styles.subNavigation, styles.hasOnlyOneOption)}>
            <SiteSubNav>
              <SiteSubNavItem href="/go/admin/pipelines" text="Pipelines"/>
              <SiteSubNavItem href="/go/admin/environments" text="Environments"/>
              <SiteSubNavItem href="/go/admin/templates" text="Templates"/>
              <SiteSubNavItem href="/go/admin/pipelines/snippet" text="Config XML"/>
              <SiteSubNavItem href="/go/admin/plugins" text="Plugins"/>
              <SiteSubNavItem href="/go/admin/package_repositories/new" text="Package Repositories"/>
              <SiteSubNavItem href="/go/admin/config_repos" text="Config Repositories"/>
              <SiteSubNavItem href="/go/admin/elastic_agent_configurations" text="Elastic Agent Configurations"/>
              <SiteSubNavItem href="/go/admin/scms" text="Pluggable SCMs"/>
            </SiteSubNav>
          </div>
        </SiteNavItem>;
      } else if (vnode.attrs.canViewTemplates) {
        adminMenu = <SiteNavItem isDropDown={true} text="Admin">
          <div class={classnames(styles.subNavigation, styles.hasOnlyOneOption)}>
            <SiteSubNav>
              <SiteSubNavItem href="/go/admin/environments" text="Environments"/>
              <SiteSubNavItem href="/go/admin/templates" text="Templates"/>
              <SiteSubNavItem href="/go/admin/config_repos" text="Config Repositories"/>
              <SiteSubNavItem href="/go/admin/elastic_agent_configurations" text="Elastic Agent Configurations"/>
            </SiteSubNav>
          </div>
        </SiteNavItem>;
      }
    } else {
      //Normal users now too have access to environments and config-repo!!
      adminMenu = (
        <SiteNavItem isDropDown={true} text="Admin">
          <div class={classnames(styles.subNavigation, styles.hasOnlyOneOption)}>
            <SiteSubNav>
              <SiteSubNavItem href="/go/admin/environments" text="Environments"/>
              <SiteSubNavItem href="/go/admin/config_repos" text="Config Repositories"/>
              <SiteSubNavItem href="/go/admin/elastic_agent_configurations" text="Elastic Agent Configurations"/>
            </SiteSubNav>
          </div>
        </SiteNavItem>
      );
    }

    return <nav class={styles.mainMenu}>
      <ul class={styles.siteNav}>
        <SiteNavItem href="/go/pipelines" text="Dashboard"/>
        <SiteNavItem href="/go/agents" text="Agents"/>
        <SiteNavItem href="/go/materials" text="Materials"/>
        {analyticsMenu}
        {adminMenu}
      </ul>
    </nav>;
  }
}
