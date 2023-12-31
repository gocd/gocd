/*
 * Copyright 2024 Thoughtworks, Inc.
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
class TabsContainer {
  constructor(element, type, id, subtab_callback_map) {
    const container = $(element).find('ul');
    this.container = container[0];
    this.container.tab_type = type;
    this.container.tab_id = id;
    this.container.tabs = container.find('li').filter(function (i, tab) {
      return !$(tab).hasClass('external_pointing');
    }).map(function (i, tab) {
      const tab_name = $(tab).find('.tab_button_body_match_text').text();
      return (subtab_callback_map && subtab_callback_map[tab_name]) || new SubTabs(tab);
    });
    this.container.hideAllSubTabs = this.hideAllSubTabs.bind(this);
  }

  hideAllSubTabs() {
    this.container.tabs.each(function (i, tab) {
      tab.hideContent();
    });
  }
}

class SubTabs {
  constructor(selector, before_open_callback) {
    this.element = $(selector);
    this.container = this.element.parent()[0];
    this.link = $(selector).find('a');
    this.initializeLinkAndContent();
    this.element.get(0).open = this.open.bind(this);
    this.element.on('click', this.handleTabClick.bind(this));
    this.before_open_callback = before_open_callback;
  }

  initializeLinkAndContent() {
    const content_name = this.element.find('a.tab_button_body_match_text').html();
    this.tab_name = content_name;
    this.link.attr('id', 'tab-link-of-' + content_name);
    this.link.attr('href', 'javascript:void(0)');
    this.content = $('#tab-content-of-' + content_name);
  }

  hideContent() {
    this.element.removeClass('current_tab');

    if (this.content.length > 0) {
      this.content.hide();
    }
  }

  handleTabClick(event) {
    if (event) {
      const target = $(event.target);
      const hasDisabledParent = target.parents("li.disabled").length > 0;
      const isDisabled = target.hasClass("disabled");
      if (!(hasDisabledParent || isDisabled)) {
        this.open(event);
      }
    }
  }

  open(event) {
    if (event != true) {
      try {
        window.location.hash = '#';
      } catch (e) {
      }
    }
    this.container.hideAllSubTabs();
    this.element.addClass('current_tab');
    if (this.before_open_callback) {
      this.before_open_callback(this.tab_name);
    }
    if (this.content.length > 0) {
      this.content.show();
      const init_method_name = this.content.attr('id') + "_callback";
      if (window[init_method_name]) {
        window[init_method_name]();
      }
    }
    TabsManager.prototype.updateLinkToThisPage(this.tab_name);
  }
}

class TabsManager {
  constructor(tab, type, id, defaultTabName, subtab_callback_map) {
    this.type = type;
    this.unique_id = id;
    this.containers = [];
    this.defaultTabName = defaultTabName;
    this.bindTabsObserver(subtab_callback_map);
    this.initializeCurrentTab(tab);
  }

  bindTabsObserver(subtab_callback_map) {
    const type = this.type;
    const id = this.unique_id;
    const self = this;
    $('.sub_tabs_container').each(function (i, tabs_container) {
      self.containers.push(new TabsContainer(tabs_container, type, id, subtab_callback_map));
    });
  }

  getCurrentTab(tab) {
    let tabName = tab;

    if (tabName) {
      return tabName;
    } else {
      tabName = this.getCurrentTabFromUrl();
      if (tabName) {
        return tabName;
      } else {
        if (this.defaultTabName) {
          return this.defaultTabName;
        }
        return null;
      }
    }
  }

  getCurrentTabFromUrl() {
    const url = window.location.href;

    try {
      if (url.lastIndexOf('#tab-') > -1) {
        return url.substring(url.lastIndexOf('#tab-') + 5, url.length);
      }
    } catch (e) {
    }

    return null; // return undefined if no name in the tail of URL
  }

  initializeCurrentTab(tab) {
    const current_tab_content_id = this.getCurrentTab(tab);
    if (current_tab_content_id) {
      const current_tab_link = $('#tab-link-of-' + current_tab_content_id);
      if (current_tab_link.length > 0) {
        current_tab_link.parent().get(0).open(true);
      }
      this.updateLinkToThisPage(current_tab_content_id);
    }
  }

  updateLinkToThisPage(tabName) {
    const url = window.location.href;
    const url_without_hash = url.lastIndexOf('#') > -1 ? url.substring(0, url.lastIndexOf('#')) : url;
    const linkToThisPage = $('#link-to-this-page');
    if (linkToThisPage.length > 0 && linkToThisPage.attr('href')) {
      linkToThisPage.attr('href', url_without_hash + '#tab-' + tabName);
    }
  }
}
