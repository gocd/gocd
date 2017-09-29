require 'bundler/setup'
require 'bundler/gem_tasks'
require 'rubocop/rake_task'
require 'rspec/core/version'

RuboCop::RakeTask.new(:style)

Dir['tasks/**/*.rake'].each do |path|
  load(path)
end

task default: %w(spec style readme)
