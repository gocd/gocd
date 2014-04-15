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

unless defined?(Buildr::VERSION)
  require File.join(File.dirname(__FILE__), 'lib', 'buildr', 'version.rb')
  $LOADED_FEATURES << 'buildr/version.rb'
end

# Rakefile needs to create spec for both platforms (ruby and java), using the
# $platform global variable.  In all other cases, we figure it out from RUBY_PLATFORM.
$platform ||= RUBY_PLATFORM[/java/] || 'ruby'

Gem::Specification.new do |spec|
  spec.name           = 'buildr'
  spec.version        = Buildr::VERSION.dup
  spec.author         = 'Apache Buildr'
  spec.email          = "users@buildr.apache.org"
  spec.homepage       = "http://buildr.apache.org/"
  spec.summary        = "Build like you code"
  spec.description    = <<-TEXT
Apache Buildr is a build system for Java-based applications, including support
for Scala, Groovy and a growing number of JVM languages and tools.  We wanted
something that's simple and intuitive to use, so we only need to tell it what
to do, and it takes care of the rest.  But also something we can easily extend
for those one-off tasks, with a language that's a joy to use.
  TEXT
  spec.rubyforge_project  = 'buildr'

  spec.platform       = $platform

  spec.files          = Dir['{addon,bin,doc,etc,lib,rakelib,spec}/**/*', '*.{gemspec,buildfile}'] +
                        ['LICENSE', 'NOTICE', 'CHANGELOG', 'README.rdoc', 'Rakefile', '_buildr', '_jbuildr']
  spec.require_paths  = 'lib', 'addon'
  spec.bindir         = 'bin'                               # Use these for applications.
  spec.executable     = 'buildr'

  spec.extra_rdoc_files = 'README.rdoc', 'CHANGELOG', 'LICENSE', 'NOTICE'
  spec.rdoc_options     = '--title', 'Buildr', '--main', 'README.rdoc',
                          '--webcvs', 'http://svn.apache.org/repos/asf/buildr/trunk/'
  spec.post_install_message = "To get started run buildr --help"

  spec.required_rubygems_version = ">= 1.8.6"

  # Tested against these dependencies.
  spec.add_dependency 'rake',                 '0.9.2.2'
  spec.add_dependency 'builder',              '3.1.3'
  spec.add_dependency 'net-ssh',              '2.6.0'
  spec.add_dependency 'net-sftp',             '2.0.5'
  spec.add_dependency 'rubyzip',              '0.9.9'
  # Highline 1.6.15 does not seem to work on windows. We should
  # investigate the root cause at a later stage before upgrading
  spec.add_dependency 'highline',             '1.6.2'
  spec.add_dependency 'json_pure',            '1.7.5'
  spec.add_dependency 'rubyforge',            '2.0.4'
  spec.add_dependency 'hoe',                  '3.1.0'
  spec.add_dependency 'rjb',                  '1.4.2' if ($platform.to_s == 'x86-mswin32' || $platform.to_s == 'ruby')
  spec.add_dependency 'atoulme-Antwrap',      '~> 0.7.4'
  spec.add_dependency 'diff-lcs',             '1.1.3'
  spec.add_dependency 'rspec-expectations',   '2.11.3'
  spec.add_dependency 'rspec-mocks',          '2.11.3'
  spec.add_dependency 'rspec-core',           '2.11.1'
  spec.add_dependency 'rspec',                '2.11.0'
  spec.add_dependency 'xml-simple',           '1.1.1'
  spec.add_dependency 'minitar',              '0.5.3'
  spec.add_dependency 'jruby-openssl',        '~> 0.8.2' if $platform.to_s == 'java'
  spec.add_dependency 'bundler'

  # Unable to get this consistently working under jruby on windows
  unless $platform.to_s == 'java'
    spec.add_development_dependency 'jekyll', '0.11.2'
    spec.add_development_dependency 'RedCloth', '4.2.9'
    spec.add_development_dependency 'jekylltask', '1.1.0'
    spec.add_development_dependency 'rdoc', '3.12'
    spec.add_development_dependency 'rcov', '0.9.9'
  end

  spec.add_development_dependency 'ci_reporter', '1.7.2'

  # NOTE: Must update all-in-one.rake if this is updated
  spec.add_development_dependency 'ffi-ncurses', '0.4.0' if $platform.to_s == 'java'
  spec.add_development_dependency 'win32console' if $platform.to_s == 'x86-mswin32'
  # Ideally we would depend on psych when the platform has >= 1.9.2 support and jruby platform version > 1.6.6
  #spec.add_development_dependency 'psych' if RUBY_VERSION >= '1.9.2'
  spec.add_development_dependency 'pygmentize', '0.0.3'
  spec.add_development_dependency 'saikuro_treemap', '0.2.0'
  spec.add_development_dependency 'atoulme-Saikuro', '1.2.1'
end
