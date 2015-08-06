#!/usr/bin/env rake
begin
  require 'bundler/setup'
rescue LoadError
  puts 'You must `gem install bundler` and `bundle install` to run rake tasks'
  exit # bundler is required for testbeds
end

require 'testbeds/rake'
require 'rspec/core/rake_task'
Bundler::GemHelper.install_tasks

if File.directory?(File.expand_path('spec/testbeds', File.dirname(__FILE__)))
  all_test_tasks = []

  each_testbed do |testbed| # namespace 'testbed:rails-3.2'

    desc "run specs with phantomjs in #{testbed.name}"
    task :run_jasmine_rake_in_dummy do
      p testbed.gemfile
      ENV['BUNDLE_GEMFILE'] = testbed.gemfile
      cmd = 'bundle exec rake testbed:current:spec:javascript'
      raise 'specs failed' unless system cmd
    end

    desc "run specs with browser in #{testbed.name}"
    task :run_browser_spec_in_dummy do
      p testbed.gemfile
      ENV['BUNDLE_GEMFILE'] = testbed.gemfile
      unless Rake::Task.task_defined?('_in_browser')
        RSpec::Core::RakeTask.new(:_in_browser) do |t|
          deps = testbed.dependencies
          t.pattern = "spec/jasmine_spec.rb"
          t.rspec_opts = deps.collect { |d| [ '-r', d ] }.flatten
        end
      end
      Rake::Task['_in_browser'].invoke
      Rake::Task['_in_browser'].reenable
    end

    desc "run all tests in #{testbed.name}"
    task :all => [
                     "#{testbed.namespace}:run_jasmine_rake_in_dummy",
                     "#{testbed.namespace}:run_browser_spec_in_dummy"
                   ]

    all_test_tasks << "#{testbed.namespace}:all"
  end

  desc "run all tests"
  task :default => all_test_tasks
else
  desc "run all tests"
  task :default do
    puts "To run tests, you must generate testbeds:"
    puts
    puts "1. Install bundle for each file in gemfiles/*:"
    Dir['gemfiles/*'].each do |dir|
      next if dir['.lock']
      puts "    BUNDLE_GEMFILE=#{dir} bundle install"
    end
    puts
    puts "2. Execute `generate-testbeds` to generate testbed apps."
    puts "   (Don't use `bundle exec` for this.)"
    puts
    puts "3. Run `rake` again."
  end
end
