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

describe('Metrics Consent', () => {
  const MetricsSettings       = require('models/metrics_consent/metrics_settings');
  const metricsSettingsGetUrl = '/go/api/metrics/settings';

  const metricsSettingsJSON = {
    'consent':      true,
    'consented_by': 'Admin'
  };

  it('should deserialize consent from JSON', () => {
    const metricsSettings = MetricsSettings.fromJSON(metricsSettingsJSON);

    expect(metricsSettings.consent()).toBe(metricsSettingsJSON.consent);
    expect(metricsSettings.consentedBy()).toBe(metricsSettingsJSON.consented_by);
  });

  it('should toggle consent value', () => {
    const metricsSettings = MetricsSettings.fromJSON(metricsSettingsJSON);

    expect(metricsSettings.consent()).toBe(true);
    metricsSettings.toggleConsent();
    expect(metricsSettings.consent()).toBe(false);
  });

  it('should reset consent value', () => {
    const metricsSettings = MetricsSettings.fromJSON(metricsSettingsJSON);

    expect(metricsSettings.consent()).toBe(true);
    metricsSettings.toggleConsent();
    expect(metricsSettings.consent()).toBe(false);
    metricsSettings.resetConsent();
    expect(metricsSettings.consent()).toBe(true);
    metricsSettings.resetConsent();
    expect(metricsSettings.consent()).toBe(true);
  });

  it('should fetch metrics settings', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(metricsSettingsGetUrl).andReturn({
        responseText:    JSON.stringify(metricsSettingsJSON),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v1+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake((metricsSettings) => {
        expect(metricsSettings.consent()).toBe(metricsSettingsJSON.consent);
        expect(metricsSettings.consentedBy()).toBe(metricsSettingsJSON.consented_by);
      });

      MetricsSettings.get().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  it('should patch metrics settings', () => {
    jasmine.Ajax.withMock(() => {
      const updatedMetricsSettings = {'consent': false, 'consented_by': 'Bob'};

      jasmine.Ajax.stubRequest(metricsSettingsGetUrl, undefined, 'PATCH').andReturn({
        responseText:    JSON.stringify(updatedMetricsSettings),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v1+json'
        }
      });

      const metricsSettings = MetricsSettings.fromJSON(metricsSettingsJSON);

      const successCallback = jasmine.createSpy().and.callFake(() => {
        expect(metricsSettings.consent()).toBe(updatedMetricsSettings.consent);
        expect(metricsSettings.consentedBy()).toBe(updatedMetricsSettings.consented_by);
      });

      expect(metricsSettings.consent()).toBe(true);
      metricsSettings.toggleConsent();
      expect(metricsSettings.consent()).toBe(false);

      metricsSettings.save().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });
});
