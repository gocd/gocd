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

module RouteExt

  def url_for(options = {}, route_name = nil, url_strategy = ActionDispatch::Routing::RouteSet::UNKNOWN)
    sorted_options_for_cache_key = java.util.TreeMap.new(options)
    cache_key = ActiveSupport::Cache.expand_cache_key(sorted_options_for_cache_key)

    unless url = Services.go_cache.get(com.thoughtworks.go.listener.BaseUrlChangeListener::URLS_CACHE_KEY, cache_key)
      url = super
      Services.go_cache.put(com.thoughtworks.go.listener.BaseUrlChangeListener::URLS_CACHE_KEY, cache_key, url)
    end
    url
  end

  ActionDispatch::Routing::RouteSet.send(:prepend, RouteExt)
end
