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


desc 'Check that source files contain the Apache license'
task 'license' => FileList['{addond,lib,doc,rakelib}/**/*.{rb,rake,java}', 'buildr.gemspec', 'Rakefile'] do |task|
  puts 'Checking that files contain the Apache license ... '
  required = task.prerequisites.select { |fn| File.file?(fn) }
  missing = required.reject { |fn|
    comments = File.read(fn).scan(/(\/\*(.*?)\*\/)|^#\s+(.*?)$|^-#\s+(.*?)$|<!--(.*?)-->/m).
      map { |match| match.compact }.flatten.join("\n")
    comments =~ /Licensed to the Apache Software Foundation/ && comments =~ /http:\/\/www.apache.org\/licenses\/LICENSE-2.0/
  }
  fail "#{missing.join(', ')} missing Apache License, please add it before making a release!" unless missing.empty?
  puts '[x] Source files contain the Apache license'
end
