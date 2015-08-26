##########################GO-LICENSE-START################################
# Copyright 2015 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################


#responsible for pretty printing hal+json responses for API clients

%w(v1 v2).each do |version|
  mime_type = "application/vnd.go.cd.#{version}+json"
  symbol    = "json_hal_#{version}".to_sym

  Mime::Type.register mime_type, symbol

  ActionController::Renderers.add symbol do |json, options|
    json = JSON.pretty_generate(json, options) << "\n" unless json.kind_of?(String)
    json = "#{options[:callback]}(#{json})" unless options[:callback].blank?
    self.content_type ||= mime_type
    json
  end
end
