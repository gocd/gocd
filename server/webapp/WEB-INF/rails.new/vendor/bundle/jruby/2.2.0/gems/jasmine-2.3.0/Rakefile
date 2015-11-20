$LOAD_PATH.unshift File.expand_path(File.dirname(__FILE__))
$LOAD_PATH.unshift File.expand_path("#{File.dirname(__FILE__)}/lib")
require 'bundler'
Bundler::GemHelper.install_tasks

require 'jasmine'
require 'rspec'
require 'rspec/core/rake_task'

desc 'Run all examples'
RSpec::Core::RakeTask.new(:spec) do |t|
  t.pattern = 'spec/**/*_spec.rb'
  t.rspec_opts = '-t ~performance'
end

desc 'Run performance build'
RSpec::Core::RakeTask.new(:performance_specs) do |t|
  t.pattern = 'spec/**/*_spec.rb'
  t.rspec_opts = '-t performance'
end

task :spec => %w(jasmine:copy_examples_to_gem)
task :performance_specs => %w(jasmine:copy_examples_to_gem)

task :default => :spec

namespace :jasmine do
  require 'jasmine-core'

  desc 'Copy examples from Jasmine JS to the gem'
  task :copy_examples_to_gem do
    require 'fileutils'

    destination_root = File.expand_path(File.join('..', 'lib', 'generators', 'jasmine', 'examples', 'templates'), __FILE__)
    spec_path = File.join(destination_root, 'spec', 'javascripts', 'jasmine_examples')
    helpers_path = File.join(destination_root, 'spec', 'javascripts', 'helpers', 'jasmine_examples')
    source_code_path = File.join(destination_root, 'app', 'assets', 'javascripts', 'jasmine_examples')

    FileUtils.rm Dir.glob(File.join(spec_path, '*'))
    FileUtils.cp(Dir.glob(File.join(Jasmine::Core.path, 'example', 'spec', '*Spec.js')), spec_path, :preserve => true)

    FileUtils.rm Dir.glob(File.join(helpers_path, '*'))
    FileUtils.cp(Dir.glob(File.join(Jasmine::Core.path, 'example', 'spec', 'SpecHelper.js')), helpers_path, :preserve => true)

    FileUtils.rm Dir.glob(File.join(source_code_path, '*'))
    FileUtils.cp(Dir.glob(File.join(Jasmine::Core.path, 'example', 'src', '*')), source_code_path, :preserve => true)
  end
end

