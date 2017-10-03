ENV['BUNDLE_GEMFILE'] = File.expand_path('../Gemfile', __FILE__)
require 'rubygems'
require 'bundler/setup'

require 'rake/testtask'
require 'rspec/core/rake_task'

desc "Build a gem file"
task :build do
  system "gem build mail.gemspec"
end

task :default => :spec

RSpec::Core::RakeTask.new(:spec) do |t|
  t.ruby_opts = '-w'
  t.rspec_opts = %w(--backtrace --color)
end

# load custom rake tasks
Dir["#{File.dirname(__FILE__)}/lib/tasks/**/*.rake"].sort.each { |ext| load ext }
