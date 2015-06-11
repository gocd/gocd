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

# To work-around a bug with gemcutter: http://stackoverflow.com/questions/4932881/gemcutter-rake-build-now-throws-undefined-method-write-for-syckemitter
require 'psych' if RUBY_VERSION >= '1.9.2' && !RUBY_PLATFORM[/java/]

# We need JAVA_HOME for most things (setup, spec, etc).
unless ENV['JAVA_HOME']
  if RUBY_PLATFORM[/java/]
    ENV['JAVA_HOME'] = Java.java.lang.System.getProperty('java.home')
  elsif RUBY_PLATFORM[/darwin/]
    ENV['JAVA_HOME'] = '/System/Library/Frameworks/JavaVM.framework/Home'
  else
    fail "Please set JAVA_HOME first (set JAVA_HOME=... or env JAVA_HOME=... rake ...)"
  end
end


# Load the Gem specification for the current platform (Ruby or JRuby).
def spec(platform = RUBY_PLATFORM[/java/] || 'ruby')
  @specs ||= ['ruby', 'java', 'x86-mswin32'].inject({}) { |hash, spec_platform|
    ENV['BUILDR_PLATFORM'] = spec_platform
    hash.update(spec_platform=> Gem::Specification.load('buildr.gemspec'))
    Gem::Specification._clear_load_cache
    ENV['BUILDR_PLATFORM'] = nil
    hash
  }
  @specs[platform]
end

desc 'Clean up all temporary directories used for running tests, creating documentation, packaging, etc.'
task :clobber
