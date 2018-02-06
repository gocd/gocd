##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################

module ApiV4
  module Plugin
    class AnalyticsCapabilitiesRepresenter < BaseRepresenter
      alias_method :capabilities, :represented

      property :supports_pipeline_analytics
      property :supported_dashboard_analytics_metrics, exec_context: :decorator

      def supported_dashboard_analytics_metrics
        capabilities.supportedAnalyticsDashboardMetrics() || []
      end
    end
  end
end