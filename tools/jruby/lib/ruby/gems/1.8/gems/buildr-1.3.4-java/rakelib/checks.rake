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


desc "Check that source files contain the Apache license"
task :license=>FileList["**/*.{rb,rake,java,gemspec,buildfile}", 'Rakefile'] do |task|
  puts "Checking that files contain the Apache license ... "
  required = task.prerequisites.select { |fn| File.file?(fn) }
  missing = required.reject { |fn| 
    comments = File.read(fn).scan(/(\/\*(.*?)\*\/)|^#\s+(.*?)$|^-#\s+(.*?)$|<!--(.*?)-->/m).
      map { |match| match.compact }.flatten.join("\n")
    comments =~ /Licensed to the Apache Software Foundation/ && comments =~ /http:\/\/www.apache.org\/licenses\/LICENSE-2.0/
  }
  fail "#{missing.join(', ')} missing Apache License, please add it before making a release!" unless missing.empty?
  puts "[x] Source files contain the Apache license"
end


desc "Look for new dependencies, check transitive dependencies"
task :dependency do
  puts "Checking that all dependencies are up to date ..."
  # Find if anything has a more recent dependency.  These are not errors, just reports.
  spec.dependencies.each do |dep|
    current = Gem::SourceInfoCache.search(dep).last
    latest = Gem::SourceInfoCache.search(Gem::Dependency.new(dep.name, '>0')).last
    puts "A new version of #{dep.name} is available, #{latest.version} replaces #{current.version}" if (current && latest && latest.version > current.version)
  end

  # Returns orderd list of transitive dependencies for the given dependency.
  transitive = lambda { |depend|
    dep_spec = Gem::SourceIndex.from_installed_gems.search(depend).last
    fail "No specification for dependency #{depend}" unless dep_spec
    dep_spec.runtime_dependencies.map { |trans| transitive[trans].push(trans) }.flatten.uniq }
  # For each dependency, make sure *all* its transitive dependencies are listed
  # as a Buildr dependency, and order is preserved.
  spec.dependencies.each_with_index do |dep, index|
    puts "checking #{dep.name}"
    transitive[dep].each do |trans|
      matching = spec.dependencies.find { |existing| trans =~ existing }
      fail "#{trans} required by #{dep} and missing from spec" unless matching
      fail "#{trans} must come before #{dep} in dependency list" unless spec.dependencies.index(matching) < index
    end
  end
  puts "[X] Checked all dependencies are up to date and transitive dependencies are correctly ordered"
end
