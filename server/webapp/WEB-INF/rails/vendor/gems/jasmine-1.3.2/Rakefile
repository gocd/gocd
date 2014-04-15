$LOAD_PATH.unshift File.expand_path(File.dirname(__FILE__))
$LOAD_PATH.unshift File.expand_path("#{File.dirname(__FILE__)}/lib")
require "bundler"
Bundler::GemHelper.install_tasks

require "jasmine"
if Jasmine::Dependencies.rspec2?
  require 'rspec'
  require 'rspec/core/rake_task'
else
  require 'spec'
  require 'spec/rake/spectask'
end

desc "Run all examples"
if Jasmine::Dependencies.rspec2?
  RSpec::Core::RakeTask.new(:spec) do |t|
    t.pattern = 'spec/**/*_spec.rb'
  end
else
  Spec::Rake::SpecTask.new('spec') do |t|
    t.spec_files = FileList['spec/**/*.rb']
  end
end

task :spec => ['jasmine:copy_examples_to_gem']

task :default => :spec

namespace :jasmine do
  require "jasmine-core"
  task :server do
    port = ENV['JASMINE_PORT'] || 8888
    Jasmine.configure do |config|
      root = File.expand_path(File.join(File.dirname(__FILE__), ".."))
      config.src_dir = File.join(root, 'src')
      config.spec_dir = Jasmine::Core.path
      config.spec_files = lambda { (Jasmine::Core.html_spec_files + Jasmine::Core.core_spec_files).map {|f| File.join(config.spec_dir, f) } }
    end

    config = Jasmine.config

    server = Jasmine::Server.new(8888, Jasmine::Application.app(config))
    server.start

    puts "your tests are here:"
    puts "  http://localhost:#{port}/"
  end

  desc "Copy examples from Jasmine JS to the gem"
  task :copy_examples_to_gem do
    require "fileutils"

    # copy jasmine's example tree into our generator templates dir
    FileUtils.rm_r('generators/jasmine/templates/jasmine-example', :force => true)
    FileUtils.cp_r(File.join(Jasmine::Core.path, 'example'), 'generators/jasmine/templates/jasmine-example', :preserve => true)
  end
end

desc "Run specs via server"
task :jasmine => ['jasmine:server']

