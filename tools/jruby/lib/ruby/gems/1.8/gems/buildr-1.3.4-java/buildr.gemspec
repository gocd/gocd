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


Gem::Specification.new do |spec|
  spec.name           = 'buildr'
  spec.version        = '1.3.4'
  spec.author         = 'Apache Buildr'
  spec.email          = "users@buildr.apache.org"
  spec.homepage       = "http://buildr.apache.org/"
  spec.summary        = "A build system that doesn't suck"
  spec.description    = <<-TEXT
Apache Buildr is a build system for Java-based applications, including support
for Scala, Groovy and a growing number of JVM languages and tools.  We wanted
something that's simple and intuitive to use, so we only need to tell it what
to do, and it takes care of the rest.  But also something we can easily extend
for those one-off tasks, with a language that's a joy to use.
  TEXT
  spec.rubyforge_project  = 'buildr'

  # Rakefile needs to create spec for both platforms (ruby and java), using the
  # $platform global variable.  In all other cases, we figure it out from RUBY_PLATFORM.
  spec.platform       = $platform || RUBY_PLATFORM[/java/] || 'ruby'
  
  spec.files          = Dir['{addon,bin,doc,etc,lib,rakelib,spec}/**/*', '*.{gemspec,buildfile}'] +
                        ['LICENSE', 'NOTICE', 'CHANGELOG', 'README.rdoc', 'Rakefile', '_buildr', '_jbuildr']
  spec.require_paths  = 'lib', 'addon'
  spec.bindir         = 'bin'                               # Use these for applications.
  spec.executable     = 'buildr'

  spec.has_rdoc         = true
  spec.extra_rdoc_files = 'README.rdoc', 'CHANGELOG', 'LICENSE', 'NOTICE'
  spec.rdoc_options     = '--title', 'Buildr', '--main', 'README.rdoc',
                          '--webcvs', 'http://svn.apache.org/repos/asf/buildr/trunk/'
  spec.post_install_message = "To get started run buildr --help"

  # Tested against these dependencies.
  spec.add_dependency 'rake',                 '0.8.4'
  spec.add_dependency 'builder',              '2.1.2'
  spec.add_dependency 'net-ssh',              '2.0.11'
  spec.add_dependency 'net-sftp',             '2.0.2'
  spec.add_dependency 'rubyzip',              '0.9.1'
  spec.add_dependency 'highline',             '1.5.0'
  spec.add_dependency 'rubyforge',            '1.0.3'
  spec.add_dependency 'hoe',                  '1.11.0'
  spec.add_dependency 'rjb',                  '1.1.6' if spec.platform.to_s == 'ruby' 
  spec.add_dependency 'Antwrap',              '0.7.0'
  spec.add_dependency 'rspec',                '1.2.2'
  spec.add_dependency 'xml-simple',           '1.0.12'
  spec.add_dependency 'archive-tar-minitar',  '0.5.2'
  spec.add_dependency 'jruby-openssl',        '0.3'   if spec.platform.to_s == 'java'
end
