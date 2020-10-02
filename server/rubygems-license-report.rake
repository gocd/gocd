##########################################################################
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
##########################################################################

missing_licenses = {
  dynamic_form: {
    moduleName: 'dynamic_form',
    moduleVersion: '1.1.4',
    moduleLicenses: [
      {
        :moduleLicense => "MIT",
        :moduleLicenseUrl => "https://spdx.org/licenses/MIT.html"
      }
    ]
  },
  method_source: {
    moduleName: 'method_source',
    moduleVersion: '0.9.0',
    moduleLicenses: [
      {
        :moduleLicense => "MIT",
        :moduleLicenseUrl => "https://spdx.org/licenses/MIT.html"
      }
    ]
  }

}

require 'active_support/core_ext/hash/deep_merge'

task :default do
  OUTPUT_FILE = (ENV['OUTPUT_FILE'] or raise 'OUTPUT_FILE not defined')
  (ENV['BUNDLE_GEMFILE'] or raise 'BUNDLE_GEMFILE not defined')
  require 'bundler'
  require 'json'
  definition = ::Bundler.definition
  all = definition.specs.to_a
  #puts "*** All gems - #{all.collect(&:full_name)}"

  requested = definition.specs_for(definition.groups.collect(&:to_sym) - [:development, :test, :assets]).to_a
  #puts "*** Gems that should be packaged - #{requested.collect(&:full_name)}"

  rubygem_specs = requested.collect(&:to_spec)
  report = rubygem_specs.inject({}) do |memo, gem_spec|
    memo[gem_spec.name] = {
      moduleName: gem_spec.name,
      moduleVersion: gem_spec.version.to_s,
      moduleUrls: [gem_spec.homepage].compact,
      moduleLicenses: gem_spec.licenses.inject([]) do |license_memo, license|
        license_memo << {moduleLicense: license, moduleLicenseUrl: "https://spdx.org/licenses/#{license}.html"}
      end
    }

    if missing_licenses[gem_spec.name.to_sym] && missing_licenses[gem_spec.name.to_sym][:moduleVersion] == memo[gem_spec.name][:moduleVersion]
      memo[gem_spec.name] = memo[gem_spec.name].deep_merge(missing_licenses[gem_spec.name.to_sym])
    end
    memo
  end

  open(OUTPUT_FILE, 'w') {|f| f.puts(JSON.pretty_generate(report))}
end
