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


# We need JAVA_HOME for most things (setup, spec, etc).
unless ENV['JAVA_HOME']
  if RUBY_PLATFORM[/java/]
    ENV['JAVA_HOME'] = java.lang.System.getProperty('java.home')
  elsif RUBY_PLATFORM[/darwin/]
    ENV['JAVA_HOME'] = '/System/Library/Frameworks/JavaVM.framework/Home'
  else
    fail "Please set JAVA_HOME first (set JAVA_HOME=... or env JAVA_HOME=... rake ...)"
  end
end


# Load the Gem specification for the current platform (Ruby or JRuby).
def spec(platform = RUBY_PLATFORM[/java/] || 'ruby')
  @specs ||= ['ruby', 'java'].inject({}) { |hash, platform|
    $platform = platform
    hash.update(platform=>Gem::Specification.load('buildr.gemspec'))
  }
  @specs[platform]
end

# Tell us if we need sudo for various commands.
def sudo_needed?
  Config::CONFIG['host_os'] !~ /windows|cygwin|bccwin|cygwin|djgpp|mingw|mswin|wince/i && !ENV['GEM_HOME']
end


desc 'Clean up all temporary directories used for running tests, creating documentation, packaging, etc.'
task :clobber
