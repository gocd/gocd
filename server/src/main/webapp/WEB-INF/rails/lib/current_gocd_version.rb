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

# This is a hack because calling static methods on `com.thoughtworks.go.CurrentGoCDVersion` seems to have a perf overhead on jruby
#
class CurrentGoCDVersion
  @@__api_docs_url = Hash.new do |hash, key|
    hash[key] = com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl(key)
  end

  @@__docs_url = Hash.new do |hash, key|
    hash[key] = com.thoughtworks.go.CurrentGoCDVersion.docsUrl(key)
  end

  def self.api_docs_url(fragment)
    @@__api_docs_url[fragment]
  end

  def self.docs_url(suffix)
    @@__docs_url[suffix]
  end
end
