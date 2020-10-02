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
import * as CONSTANTS from "helpers/constants";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";

export interface Options<T> {
  intervalSeconds: number;
  initialIntervalSeconds: number;
  visibilityBackoffFactor: number;

  repeaterFn(currentPromise: (value: T) => any): Promise<T>;
}

const defaultOptions = {
  intervalSeconds: CONSTANTS.SPA_REFRESH_INTERVAL / 1000,
  initialIntervalSeconds: 0,
  visibilityBackoffFactor: 2
};

const doesBrowserSupportPageVisibilityAPI = "undefined" !== typeof document.hidden;

const autoRefreshDisabled = window.location.search.indexOf("auto_refresh=false") >= 0;

export class AjaxPoller<T> {
  private readonly options: Options<T>;

  private abort: boolean                          = false;
  private readonly currentXHR: Stream<Promise<T>> = Stream();
  private timeout: number | undefined;

  constructor(options: (() => Promise<T>) | Partial<Options<T>>) {
    // @ts-ignore
    this.options = _.assign({}, defaultOptions, "function" === typeof options ? {repeaterFn: options} : options);
    if (doesBrowserSupportPageVisibilityAPI) {
      document.addEventListener("visibilitychange", this.handleVisibilityChange.bind(this), false);
    }
  }

  start(initialInterval = Math.max(this.options.initialIntervalSeconds, 0)) {
    this.abort   = false;
    this.timeout = window.setTimeout(this.fire.bind(this), initialInterval * 1000);
  }

  stop() {
    if (_.isNumber(this.timeout)) {
      window.clearTimeout(this.timeout);
      this.abort   = true;
      this.timeout = undefined;
    }

    if (this.currentXHR()) {
      if ("abort" in this.currentXHR()) {
        ((this.currentXHR() as unknown) as XMLHttpRequest).abort();
      }
    }
  }

  restart() {
    this.stop();
    this.start(0);
  }

  // overridden for tests to override
  protected isPageHidden() {
    return document.hidden;
  }

  private handleVisibilityChange() {
    // we stop and repoll only when the page becomes visible
    if (this.isPageHidden()) {
      this.stop();
    } else {
      this.restart();
    }
  }

  private fire() {
    const repeaterFn = this.options.repeaterFn(this.currentXHR);
    if (repeaterFn.finally) {
      repeaterFn.finally(this.redrawAndInstallNextHook.bind(this));
      // @ts-ignore
    } else if (repeaterFn.always) {
      // `.always` for jquery deferrds
      // @ts-ignore
      repeaterFn.always(this.redrawAndInstallNextHook.bind(this));
    }
  }

  private redrawAndInstallNextHook() {
    m.redraw();
    if (!this.abort && !autoRefreshDisabled) {
      const timeout = this.currentPollInterval();
      this.timeout  = window.setTimeout(this.fire.bind(this), timeout);
    }
  }

  private currentPollInterval() {
    if (this.isPageHidden()) {
      return this.options.intervalSeconds * this.options.visibilityBackoffFactor * 1000;
    } else {
      return this.options.intervalSeconds * 1000;
    }
  }
}
