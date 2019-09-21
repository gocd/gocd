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
import {UsageData} from "models/shared/data_sharing/usage_data";
import {DataSharingSettings} from "models/shared/data_sharing/data_sharing_settings";
import {TestHelper} from "views/pages/spec/test_helper";
import {DataSharingSettingsWidget} from "views/data_sharing_settings/data_sharing_settings_widget";
import m from "mithril";

describe("Data Sharing Settings Widget", () => {
  const helper        = new TestHelper();


  afterEach(helper.unmount.bind(helper));

  const metricsSettingsJSON = {
    "_embedded": {
      "allow":      true,
      "updated_by": "Default",
      "updated_on": "2018-06-14T04:42:26Z"
    }
  };

  const usageDataJSON = {
    "_embedded": {
      "pipeline_count":                 1,
      "agent_count":                    0,
      "oldest_pipeline_execution_time": 1528949998195
    }
  };

  let settings, usageData;
  beforeEach(() => {
    usageData = UsageData.fromJSON(usageDataJSON);
    settings  = DataSharingSettings.fromJSON(metricsSettingsJSON, {getResponseHeader: () => 'ETag'});
    helper.mount(() => m(DataSharingSettingsWidget, {settings, usageData}));
  });

  it("should show metrics collection title", () => {
    expect(helper.text('.page-header')).toContain('Help improve GoCD by sharing technical data');
  });

  it('should render the consent description', () => {
    const description = [
      'We strive to understand our users better and provide the best product experience. You can help us! Please give us permission to collect your GoCD usage data. We will never collect any private or personal information, and we will always be transparent about what is being shared.',
      'Choose your settings below. You can change these settings at any time.'
    ];

    const consentDescription = helper.qa('.consent-description p');

    expect(consentDescription).toHaveLength(description.length);
    expect(consentDescription.item(0)).toContainText(description[0]);
    expect(consentDescription.item(1)).toContainText(description[1]);
  });

  it('should not show the last updated by when settings hasn\'t been changed by any admin', () => {
    expect(helper.q('.updated-by')).toBeFalsy();
  });

  it('should not show the last updated by time and username', () => {
    settings.updatedBy('Bob');
    helper.redraw();

    const updatedByMessage = `${settings.updatedBy()} changed the data sharing permission on ${settings.updatedOn()}.`;

    expect(helper.q('.updated-by')).toBeInDOM();
    expect(helper.text('.updated-by')).toContain(updatedByMessage);
  });

  it('should show the consent toggle button', () => {
    expect(helper.text('.consent-toggle p')).toContain('Allow GoCD to collect the following data:');
    expect(helper.q('.switch')).toBeInDOM();
  });

  it('should show the consent toggle value same as of metrics settings consent value', () => {
    expect(helper.q('.switch')).toBeInDOM();

    expect(settings.allowed()).toBe(true);
    expect(helper.q('.switch input')).toBeChecked();

    helper.click('.switch input');

    expect(settings.allowed()).toBe(false);
    expect(helper.q('.switch input')).not.toBeChecked();
  });

  it('should show human readable consent text', () => {
    expect(settings.allowed()).toBe(true);
    expect(helper.text('.human-readable-consent')).toContain('Yes');

    settings.toggleConsent();
    helper.redraw();

    expect(settings.allowed()).toBe(false);
    expect(helper.text('.human-readable-consent')).toContain('No');
  });

  it('should show the consent for collected metrics list', () => {
    expect(helper.qa('.consent-for-wrapper .consent-for')).toHaveLength(10);
    const consentFor = helper.qa('.consent-for-wrapper .consent-for');

    const pipelineConsentKey         = 'Number of pipelines (pipeline_count)';
    const pipelineConsentDescription = 'This allows the calculation of the average number of pipelines a GoCD instance has. Knowing the average number of pipelines helps us optimize the GoCD experience.';

    const configRepoPipelineConsentKey         = 'Number of config repo pipelines (config_repo_pipeline_count) [Added in GoCD v18.8.0]';
    const configRepoPipelineConsentDescription = 'This count provides a measure of usefulness of the pipeline as code feature. We plan to make this feature better soon and this metric will be used as an indicator of success of this effort.';

    const agentConsentKey          = 'Number of agents (agent_count)';
    const agentsConsentDescription = 'This allows the calculation of the average number of agents a GoCD instance has. This will help us ensure GoCD can handle a reasonable number of requests from the average number of agents.';

    const oldestPipelineRuntimeKey         = 'Oldest pipeline run time (oldest_pipeline_execution_time)';
    const oldestPipelineRuntimeDescription = 'This provides data around the age of the GoCD instance. Along with the number of pipelines data point, it helps establish an expected growth in the number of pipelines.';

    const elasticAgentJobConsentKey         = 'Number of elastic agent jobs (job_count and elastic_agent_job_count) [Added in GoCD v18.8.0]';
    const elasticAgentJobConsentDescription = 'These counts provides a measure of usefulness of elastic agent plugins. We’ve recently spent effort on elastic agents plugins (for Kubernetes, Docker, etc). This helps decide which plugins to put more effort into and improve.';

    const testDriveConsentKey         = "Test drive instance (test_drive_instance)";
    const testDriveConsentDescription = "Whether or not this GoCD server is a test drive instance";

    const testDriveMetricsConsentKey         = "Test drive metrics (test_drive_metrics)";
    const testDriveMetricsConsentDescription = "These metrics are only collected for GoCD test drive instances";

    const gocdVersionConsentKey         = "GoCD version (gocd_version)";
    const gocdVersionConsentDescription = "This is the version of GoCD the server is on.";

    const serverIdConsentKey         = "Server ID (server_id)";
    const serverIdConsentDescription = "A randomly-generated identifier for this instance of GoCD to help correlate the data. This does not tie into any other ID in this instance.";

    const messageVersionConsentKey         = "Message version (message_version)";
    const messageVersionConsentDescription = "Schema version number for this message.";

    expect(consentFor.item(0)).toContainText(pipelineConsentKey);
    expect(consentFor.item(0)).toContainText(pipelineConsentDescription);

    expect(consentFor.item(1)).toContainText(configRepoPipelineConsentKey);
    expect(consentFor.item(1)).toContainText(configRepoPipelineConsentDescription);

    expect(consentFor.item(2)).toContainText(agentConsentKey);
    expect(consentFor.item(2)).toContainText(agentsConsentDescription);

    expect(consentFor.item(3)).toContainText(oldestPipelineRuntimeKey);
    expect(consentFor.item(3)).toContainText(oldestPipelineRuntimeDescription);

    expect(consentFor.item(4)).toContainText(elasticAgentJobConsentKey);
    expect(consentFor.item(4)).toContainText(elasticAgentJobConsentDescription);

    expect(consentFor.item(5)).toContainText(testDriveConsentKey);
    expect(consentFor.item(5)).toContainText(testDriveConsentDescription);

    expect(consentFor.item(6)).toContainText(testDriveMetricsConsentKey);
    expect(consentFor.item(6)).toContainText(testDriveMetricsConsentDescription);

    expect(consentFor.item(7)).toContainText(gocdVersionConsentKey);
    expect(consentFor.item(7)).toContainText(gocdVersionConsentDescription);

    expect(consentFor.item(8)).toContainText(serverIdConsentKey);
    expect(consentFor.item(8)).toContainText(serverIdConsentDescription);

    expect(consentFor.item(9)).toContainText(messageVersionConsentKey);
    expect(consentFor.item(9)).toContainText(messageVersionConsentDescription);
  });

  it('should show what data will be sent when GoCD data sharing is allowed', () => {
    expect(settings.allowed()).toBe(true);

    expect(helper.text('.data-share-message')).toContain('Data that will be sent:');
    expect(helper.text('.shared-data')).toContain(usageData.represent());
  });

  it('should show what data would have been sent when GoCD data sharing is not allowed', () => {
    settings.allowed(false);

    helper.redraw();

    expect(settings.allowed()).toBe(false);

    expect(helper.text('.data-share-message')).toContain('Data that would have been sent, if allowed:');
    expect(helper.text('.shared-data')).toContain(usageData.represent());
  });

  describe('Buttons', () => {
    it('should render save button', () => {
      expect(helper.q('.update-consent')).toBeInDOM();
    });

    it('should render reset button', () => {
      expect(helper.q('.reset-consent')).toBeInDOM();
    });

    it('should reset the settings consent value on clicking reset button', () => {
      expect(settings.allowed()).toBe(true);
      settings.toggleConsent();
      expect(settings.allowed()).toBe(false);

      helper.click('.reset-consent');

      expect(settings.allowed()).toBe(true);
    });

    it('should update the consent consent value on clicking save button', () => {
      expect(settings.allowed()).toBe(true);
      settings.toggleConsent();
      expect(settings.allowed()).toBe(false);

      helper.click('.reset-consent');

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

        helper.click('.update-consent');

        expect(settings.allowed()).toBe(false);
      });
    });
  });

  describe('flash message', () => {
    beforeEach(jasmine.clock().install);
    afterEach(jasmine.clock().uninstall);

    it('should not render flash message container if no flash message present', () => {
      expect(helper.q('.callout')).toBeFalsy();
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

        expect(helper.q('.callout')).toBeFalsy();

        helper.click('.update-consent');

        expect(helper.q('.callout')).toBeInDOM();
        expect(helper.q('.callout')).toHaveClass('success');
        expect(helper.q('.callout')).toContainText('Data Sharing Settings updated Successfully!');
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

        expect(helper.q('.callout')).toBeFalsy();

        helper.click('.update-consent');

        expect(helper.q('.callout')).toBeInDOM();
        expect(helper.q('.callout')).toHaveClass('alert');
        expect(helper.q('.callout')).toContainText(response.message);
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

        expect(helper.q('.callout')).toBeFalsy();

        helper.click('.update-consent');

        expect(helper.q('.callout')).toBeInDOM();
        expect(helper.q('.callout')).toHaveClass('alert');
        expect(helper.q('.callout')).toContainText(response.message);

        jasmine.clock().tick(5001);
        helper.redraw();

        expect(helper.q('.callout')).toBeFalsy();
      });
    });
  });
});
