# coding: utf-8

require 'bundler/gem_tasks'
require 'rspec/core/rake_task'
require 'rubocop/rake_task'

RSpec::Core::RakeTask.new(:spec)

desc 'Run benchmark specs'
RSpec::Core::RakeTask.new(:benchmark) do |task|
  task.pattern = 'benchmark/**/*_spec.rb'
  task.rspec_opts = '--format documentation'
end

RuboCop::RakeTask.new(:style)

task default: %w(spec style)

if RUBY_ENGINE == 'ruby'
  task ci: %w(spec style benchmark)
else
  # Benchmarks on JRuby and Rubinius are not as stable as CRuby...
  task ci: %w(spec style)
end
