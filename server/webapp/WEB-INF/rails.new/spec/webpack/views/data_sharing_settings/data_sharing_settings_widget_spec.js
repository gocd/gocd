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

describe("Data Sharing Settings Widget", () => {
  const $             = require("jquery");
  const m             = require("mithril");
  const simulateEvent = require('simulate-event');

  const DataSharingSettings       = require('models/data_sharing_settings/data_sharing_settings');
  const DataSharingSettingsWidget = require("views/data_sharing_settings/data_sharing_settings_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const metricsSettingsJSON = {
    "_embedded": {
      "allow":      true,
      "updated_by": "Admin"
    }
  };

  let settings;
  beforeEach(() => {
    settings = DataSharingSettings.fromJSON(metricsSettingsJSON, {getResponseHeader: () => 'ETag'});
    m.mount(root, {
      view() {
        return m(DataSharingSettingsWidget, {settings});
      }
    });

    m.redraw(true);
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  it("should show metrics collection title", () => {
    expect($root.find('.page-header')).toContainText('GoCD Metrics Collection');
  });

  it('should show the consent toggle button', () => {
    expect($root.find('.consent-toggle p')).toContainText('Allow GoCD to collect following data:');
    expect($root.find('.switch')).toBeInDOM();
  });

  it('should show the consent toggle value same as of metrics settings consent value', () => {
    expect($root.find('.switch')).toBeInDOM();

    expect(settings.allowed()).toBe(true);
    expect($root.find('.switch input')).toBeChecked();

    simulateEvent.simulate($root.find('.switch input').get(0), 'click');
    m.redraw();

    expect(settings.allowed()).toBe(false);
    expect($root.find('.switch input')).not.toBeChecked();
  });

  it('should show human readable consent text', () => {
    expect(settings.allowed()).toBe(true);
    expect($root.find('.human-readable-consent')).toContainText('Yes');

    settings.toggleConsent();
    m.redraw();

    expect(settings.allowed()).toBe(false);
    expect($root.find('.human-readable-consent')).toContainText('No');
  });

  it('should show the consent for collected metrics list', () => {
    expect($root.find('.consent-for-wrapper .consent-for')).toHaveLength(2);
    const consentFor = $root.find('.consent-for-wrapper .consent-for');

    expect($(consentFor.get(0))).toContainText('Number of pipelines');
    expect($(consentFor.get(1))).toContainText('Number of agents');
  });

  it('should render the consent description', () => {
    const description = [
      'We, GoCD team, strive to understand our users better and provide the best product experience. You can help us! We would like to ask your permission to collect your GoCD usage data.',
      'We will never collect your privacy information, and we will always be transparent on what we are collecting. If we add any metrics to the collecting list in future, we will notify you and let you make the decision.',
      'Choose your settings below. You can change these settings at any time.'
    ];

    const consentDescription = $root.find('.consent-description p');

    expect(consentDescription).toHaveLength(3);
    expect($(consentDescription.get(0))).toContainText(description[0]);
    expect($(consentDescription.get(1))).toContainText(description[1]);
    expect($(consentDescription.get(2))).toContainText(description[2]);
  });

  describe('Buttons', () => {
    it('should render save button', () => {
      expect($root.find('.update-consent')).toBeInDOM();
    });

    it('should render reset button', () => {
      expect($root.find('.reset-consent')).toBeInDOM();
    });

    it('should reset the settings consent value on clicking reset button', () => {
      expect(settings.allowed()).toBe(true);
      settings.toggleConsent();
      expect(settings.allowed()).toBe(false);

      simulateEvent.simulate($root.find('.reset-consent').get(0), 'click');
      m.redraw();

      expect(settings.allowed()).toBe(true);
    });

    it('should update the consent consent value on clicking save button', () => {
      expect(settings.allowed()).toBe(true);
      settings.toggleConsent();
      expect(settings.allowed()).toBe(false);

      simulateEvent.simulate($root.find('.reset-consent').get(0), 'click');
      m.redraw();

      expect(settings.allowed()).toBe(true);

      jasmine.Ajax.withMock(() => {
        const updatedMetricsSettings = {
          "_embedded": {
            'allow': false, 'updated_by': 'Bob'
          }
        };
        jasmine.Ajax.stubRequest('/go/api/data_sharing/settings', undefined, 'PATCH').andReturn({
          responseText:    JSON.stringify(updatedMetricsSettings),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        expect(settings.allowed()).toBe(true);
        settings.toggleConsent();
        expect(settings.allowed()).toBe(false);

        simulateEvent.simulate($root.find('.update-consent').get(0), 'click');
        m.redraw();

        expect(settings.allowed()).toBe(false);
      });
    });
  });

  describe('flash message', () => {
    beforeEach(jasmine.clock().install);
    afterEach(jasmine.clock().uninstall);

    it('should not render flash message container if no flash message present', () => {
      expect($root.find('.callout')).not.toBeInDOM();
    });

    it('should show the success flash message when updating consent value is successful', () => {
      jasmine.Ajax.withMock(() => {
        const updatedMetricsSettings = {
          "_embedded": {
            'allow': false, 'updated_by': 'Bob'
          }
        };
        jasmine.Ajax.stubRequest('/go/api/data_sharing/settings', undefined, 'PATCH').andReturn({
          responseText:    JSON.stringify(updatedMetricsSettings),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        expect($root.find('.callout')).not.toBeInDOM();

        simulateEvent.simulate($root.find('.update-consent').get(0), 'click');
        m.redraw();

        expect($root.find('.callout')).toBeInDOM();
        expect($root.find('.callout')).toHaveClass('success');
        expect($root.find('.callout')).toContainText('Data Sharing Settings updated Successfully!');
      });
    });


    it('should show the error flash message when updating consent value is unsuccessful', () => {
      jasmine.Ajax.withMock(() => {
        const response = {
          "message": "boom!"
        };

        jasmine.Ajax.stubRequest('/go/api/data_sharing/settings', undefined, 'PATCH').andReturn({
          responseText:    JSON.stringify(response),
          status:          500,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        expect($root.find('.callout')).not.toBeInDOM();

        simulateEvent.simulate($root.find('.update-consent').get(0), 'click');
        m.redraw();

        expect($root.find('.callout')).toBeInDOM();
        expect($root.find('.callout')).toHaveClass('alert');
        expect($root.find('.callout')).toContainText(response.message);
      });
    });

    it('should clear flash message after 5 secs of render', () => {
      jasmine.Ajax.withMock(() => {
        const response = {
          "message": "boom!"
        };

        jasmine.Ajax.stubRequest('/go/api/data_sharing/settings', undefined, 'PATCH').andReturn({
          responseText:    JSON.stringify(response),
          status:          500,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        expect($root.find('.callout')).not.toBeInDOM();

        simulateEvent.simulate($root.find('.update-consent').get(0), 'click');
        m.redraw();

        expect($root.find('.callout')).toBeInDOM();
        expect($root.find('.callout')).toHaveClass('alert');
        expect($root.find('.callout')).toContainText(response.message);

        jasmine.clock().tick(5001);

        expect($root.find('.callout')).not.toBeInDOM();
      });
    });
  });

});
