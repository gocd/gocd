# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.


require 'jruby' if RUBY_PLATFORM[/java/]
require 'rubygems/source_info_cache'


# Install the specified gem. Options include:
# - :version -- Version requirement, e.g. '1.2' or '~> 1.2'
# - :source  -- Gem repository, e.g. 'http://gems.github.com'
def install_gem(name, options = {})
  dep = Gem::Dependency.new(name, options[:version] || '>0')
  if Gem::SourceIndex.from_installed_gems.search(dep).empty?
    puts "Installing #{name} ..."
    rb_bin = File.join(Config::CONFIG['bindir'], Config::CONFIG['ruby_install_name'])
    args = []
    args << 'sudo' << 'env' << "JAVA_HOME=#{ENV['JAVA_HOME']}" if sudo_needed?
    args << rb_bin << '-S' << 'gem' << 'install' << name
    args << '--version' << dep.version_requirements.to_s
    args << '--source' << options[:source] if options[:source]
    args << '--source' << 'http://gems.rubyforge.org'
    args << '--install-dir' << ENV['GEM_HOME'] if ENV['GEM_HOME']
    sh *args
  end
end


# Setup environment for running this Rakefile (RSpec, Jekyll, etc).
desc "If you're building from sources, run this task first to setup the necessary dependencies."
task :setup do
  missing = spec.dependencies.select { |dep| Gem::SourceIndex.from_installed_gems.search(dep).empty? }
  missing.each do |dep|
    install_gem dep.name, :version=>dep.version_requirements
  end
end
