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


begin
  require 'spec/rake/spectask'
  directory '_reports'

  desc "Run all specs"
  Spec::Rake::SpecTask.new :spec=>'_reports' do |task|
    task.spec_files = FileList['spec/**/*_spec.rb']
    task.spec_files.exclude('spec/groovy/*') if RUBY_PLATFORM[/java/]
    task.spec_opts = %w{--format specdoc --format failing_examples:failed --format html:_reports/specs.html --loadby mtime --backtrace}    
    task.spec_opts << '--colour' if $stdout.isatty
  end
  file('_reports/specs.html') { task(:spec).invoke }

  desc 'Run all failed examples from previous run'
  Spec::Rake::SpecTask.new :failed do |task|
    task.spec_files = FileList['spec/**/*_spec.rb']
    task.spec_opts = %w{--format specdoc --format failing_examples:failed --example failed --backtrace}    
    task.spec_opts << '--colour' if $stdout.isatty
  end

  desc 'Run RSpec and generate Spec and coverage reports (slow)'
  Spec::Rake::SpecTask.new :coverage=>'_reports' do |task|
    task.spec_files = FileList['spec/**/*_spec.rb']
    task.spec_opts = %W{--format progress --format failing_examples:failed --format html:_reports/specs.html --backtrace}    
    task.spec_opts << '--colour' if $stdout.isatty
    task.rcov = true
    task.rcov_dir = '_reports/coverage'
    task.rcov_opts = %w{--exclude / --include-file ^lib --text-summary}
  end
  file('_reports/coverage') { task(:coverage).invoke }


  # Useful for testing with JRuby when using Ruby and vice versa.
  namespace :spec do
    desc "Run all specs specifically with Ruby"
    task :ruby do
      puts "Running test suite using Ruby ..."
      sh 'ruby -S rake spec'
    end

    desc "Run all specs specifically with JRuby"
    task :jruby do
      puts "Running test suite using JRuby ..."
      sh 'jruby -S rake spec'
    end
  end

  task :clobber do
    rm_f 'failed'
    rm_rf '_reports'
  end

rescue LoadError
  puts "Buildr uses RSpec. You can install it by running rake setup"
  task(:setup) { install_gem 'rcov', :version=>'~>0.8' }
  task(:setup) { install_gem 'win32console' if RUBY_PLATFORM[/win32/] } # Colors for RSpec, only on Windows platform.
end
