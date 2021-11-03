#
# Copyright 2021 ThoughtWorks, Inc.
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

gem_home = nil
gem_ruby_version = "#{RUBY_VERSION}".gsub(/\.[0-9]+$/, '.0')
gemfile_location = nil

if $servlet_context
  gem_home = $servlet_context.getRealPath("/WEB-INF/rails/gems/jruby/#{gem_ruby_version}")
  gemfile_location ||= $servlet_context.getRealPath('/WEB-INF/rails/Gemfile')
else
  gem_home = File.expand_path(File.join('..', "/rails/gems/jruby/#{gem_ruby_version}"), __FILE__)
  gemfile_location ||= File.expand_path(File.join('..', '/rails/Gemfile'), __FILE__)
end

Gem.paths = {'GEM_HOME' => gem_home, 'GEM_PATH' => gem_home, 'GEM_SPEC_CACHE' => File.join(gem_home, 'specifications')}
ENV['BUNDLE_GEMFILE'] = gemfile_location
ENV['RAILS_ENV'] ||= (ENV['RACK_ENV'] || 'production')

if ENV['RAILS_ENV'] == 'production'
  ENV['EXECJS_RUNTIME'] = 'Disabled'
  ENV['BUNDLE_WITHOUT'] = 'development:test:assets'
end
