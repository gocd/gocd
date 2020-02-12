#
# Copyright 2020 ThoughtWorks, Inc.
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
#

module DeprecatedApiHelper

  def add_deprecation_headers(request, response, deprecated_api_version, successor_api_url, successor_api_version, deprecated_in, removal_in, entity_name)
    version_text = "unversioned".eql?(deprecated_api_version) ? "unversioned API" : "API version #{deprecated_api_version}"
    successor_api_url = (successor_api_url == nil || successor_api_url.blank?) ? request.url: "#{request.protocol}#{request.host}#{successor_api_url}"

    changelog_url = "https://api.gocd.org/#{deprecated_in}/#api-changelog"
    link = "<#{successor_api_url}>; Accept=\"application/vnd.go.cd.#{successor_api_version}+json\"; rel=\"successor-version\""
    warning = "299 GoCD/v#{deprecated_in} \"The #{entity_name} #{version_text} has been deprecated in GoCD Release v#{deprecated_in}. This version will be removed in GoCD Release v#{removal_in}. Version #{successor_api_version} of the API is available, and users are encouraged to use it\""

    response.set_header("X-GoCD-API-Deprecated-In", "v#{deprecated_in}")
    response.set_header("X-GoCD-API-Removal-In", "v#{removal_in}")
    response.set_header("X-GoCD-API-Deprecation-Info", changelog_url)
    response.set_header("Link", link)
    response.set_header("Warning", warning)
  end
end
