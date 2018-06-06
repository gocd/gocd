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

describe("Metrics Consent Widget", () => {
  const $             = require("jquery");
  const m             = require("mithril");
  const simulateEvent = require('simulate-event');

  const MetricsSettings      = require('models/metrics_consent/metrics_settings');
  const MetricsConsentWidget = require("views/metrics_consent/metrics_consent_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const metricsSettingsJSON = {'consent': true, 'consented_by': 'Admin'};

  let metricsSettings;
  beforeEach(() => {
    metricsSettings = MetricsSettings.fromJSON(metricsSettingsJSON);
    m.mount(root, {
      view() {
        return m(MetricsConsentWidget, {metricsSettings});
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

    expect(metricsSettings.consent()).toBe(true);
    expect($root.find('.switch input')).toBeChecked();

    simulateEvent.simulate($root.find('.switch input').get(0), 'click');
    m.redraw();

    expect(metricsSettings.consent()).toBe(false);
    expect($root.find('.switch input')).not.toBeChecked();
  });

  it('should show human readable consent text', () => {
    expect(metricsSettings.consent()).toBe(true);
    expect($root.find('.human-readable-consent')).toContainText('Yes');

    metricsSettings.toggleConsent();
    m.redraw();

    expect(metricsSettings.consent()).toBe(false);
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
      expect(metricsSettings.consent()).toBe(true);
      metricsSettings.toggleConsent();
      expect(metricsSettings.consent()).toBe(false);

      simulateEvent.simulate($root.find('.reset-consent').get(0), 'click');
      m.redraw();

      expect(metricsSettings.consent()).toBe(true);
    });

    it('should update the consent consent value on clicking save button', () => {
      expect(metricsSettings.consent()).toBe(true);
      metricsSettings.toggleConsent();
      expect(metricsSettings.consent()).toBe(false);

      simulateEvent.simulate($root.find('.reset-consent').get(0), 'click');
      m.redraw();

      expect(metricsSettings.consent()).toBe(true);

      jasmine.Ajax.withMock(() => {
        const updatedMetricsSettings = {'consent': false, 'consented_by': 'Bob'};
        jasmine.Ajax.stubRequest('/go/api/metrics/settings', undefined, 'PATCH').andReturn({
          responseText:    JSON.stringify(updatedMetricsSettings),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        expect(metricsSettings.consent()).toBe(true);
        metricsSettings.toggleConsent();
        expect(metricsSettings.consent()).toBe(false);

        simulateEvent.simulate($root.find('.update-consent').get(0), 'click');
        m.redraw();

        expect(metricsSettings.consent()).toBe(false);
      });
    });
  });
});
