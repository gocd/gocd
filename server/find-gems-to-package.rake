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

  ignored_gems = all - requested
  #puts "*** Ignored gems - #{ignored_gems.collect(&:full_name)}"
  ignored_gem_names = ignored_gems.collect(&:full_name)

  open(OUTPUT_FILE, 'w') {|f| f.puts(JSON.pretty_generate(ignored_gem_names))}
end
