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
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {AuthPluginInfo} from "models/authentication/auth_plugin_info";
import * as s from "underscore.string";
import * as uuid from "uuid/v4";
import {Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import * as styles from "./login_page_widget.scss";

const gocdLogo = require("./gocd_login.svg");

class LoginFormWidget extends MithrilViewComponent<AuthPluginInfo> {
  private readonly formID = `login-form-${uuid()}`;

  view(vnode: m.Vnode<AuthPluginInfo, this>) {
    return (

      <div className={styles.loginUser}>

        <form method="post" action="/go/auth/security_check" data-form-id={this.formID}>
          <ul className={styles.loginFormFields}>
            <li><input type="text"
                       name="j_username"
                       placeholder="Username"
                       className={styles.loginInput}
                       data-test-id="form-field-input-username"/></li>
            <li><input type="password"
                       name="j_password"
                       placeholder="Password"
                       className={styles.loginInput}
                       data-test-id="form-field-input-password"
            /></li>

            <li>
              <Primary onclick={() => {
                (document.querySelector(`[data-test-id='${this.formID}']`)! as HTMLFormElement).submit();
              }}>Sign in</Primary></li>
          </ul>
        </form>
      </div>
    );
  }
}

class ShowAllWebBasedPluginLinks extends MithrilViewComponent<AuthPluginInfo> {
  view(vnode: m.Vnode<AuthPluginInfo, this>): m.Children | void | null {
    return (
      <div className={styles.otherLoginMethods}>
        <p>Login using one of the login methods:</p>
        <div className={styles.loginOptions}>
          {vnode.attrs.webBasedPlugins.map((eachPlugin) => {
            return (
              <a
                className={styles.webLoginLink}
                data-test-id={s.slugify(`link to login using ${eachPlugin.pluginName}`)}
                title={`Login using ${eachPlugin.pluginName}`}
                href={eachPlugin.redirectUrl}>
                <img
                  data-test-id={s.slugify(`image for ${eachPlugin.pluginName}`)}
                  width="75px"
                  height="75px"
                  alt={`Login using ${eachPlugin.pluginName}`}
                  src={eachPlugin.imageUrl}/>

              </a>
            );
          })}
        </div>
      </div>
    );
  }
}

class ShowRedirectToDefaultPlugin extends MithrilComponent<AuthPluginInfo> {
  oninit(vnode: m.Vnode<AuthPluginInfo, {}>): any {

    if (!vnode.attrs.doNotAutoRedirect) {
      window.setTimeout(() => {
        const defaultWebBasedPlugin = vnode.attrs.webBasedPlugins[0];
        window.location.href        = defaultWebBasedPlugin.redirectUrl;
      }, 5000);
    }
  }

  view(vnode: m.Vnode<AuthPluginInfo, this>) {
    const defaultWebBasedPlugin = vnode.attrs.webBasedPlugins[0];

    return (
      <div className={styles.redirect}>
        <p>You will be automatically redirected to login using the
          plugin
        </p>
        <a
          className={styles.webLoginLink}
          data-test-id={s.slugify(`link to login using ${defaultWebBasedPlugin.pluginName}`)}
          title={`Login using ${defaultWebBasedPlugin.pluginName}`}
          href={defaultWebBasedPlugin.redirectUrl}>
          <img
            data-test-id={s.slugify(`image for ${defaultWebBasedPlugin.pluginName}`)}
            width="75px"
            height="75px"
            alt={`Login using ${defaultWebBasedPlugin.pluginName}`}
            src={defaultWebBasedPlugin.imageUrl}/>
        </a>

      </div>
    );
  }
}

export class LoginPageWidget extends MithrilViewComponent<AuthPluginInfo> {
  view(vnode: m.Vnode<AuthPluginInfo, this>) {
    return (
      <div>
        <div className={styles.loginForm}>
          <div className={styles.loginMethods}>
            {this.maybeShowLoginFormWidget(vnode)}
            {this.maybeShowLoginIcons(vnode)}
          </div>
          <div className={styles.loginGraphics}>
            <img src={gocdLogo}/>
          </div>
        </div>
        <div className={styles.errorBox}>
          {this.maybeLoginError(vnode)}
        </div>
      </div>

    );
  }

  private maybeShowLoginFormWidget(vnode: m.Vnode<AuthPluginInfo, this>) {
    if (vnode.attrs.hasPasswordPlugins) {
      return <LoginFormWidget {...vnode.attrs}/>;
    }
  }

  private maybeShowLoginIcons(vnode: m.Vnode<AuthPluginInfo, this>) {
    if (vnode.attrs.hasWebBasedPlugins) {
      if (vnode.attrs.webBasedPlugins.length === 1 && !vnode.attrs.hasPasswordPlugins) {
        return <ShowRedirectToDefaultPlugin {...vnode.attrs} />;
      } else {
        return <ShowAllWebBasedPluginLinks {...vnode.attrs} />;
      }
    }
  }

  private maybeLoginError(vnode: m.Vnode<AuthPluginInfo, this>) {
    if (vnode.attrs.loginError) {
      return (
        <FlashMessage type={MessageType.alert}>
          {vnode.attrs.loginError.replace(/\.$/, "")}.
          See <a target="_blank" rel="noopener" href={docsUrl("/configuration/dev_authentication.html#common-errors")}>Help
          Topic: Authentication</a>.
        </FlashMessage>
      );
    }
  }
}
