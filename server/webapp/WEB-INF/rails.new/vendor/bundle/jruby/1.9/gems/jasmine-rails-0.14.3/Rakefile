#!/usr/bin/env rake

require 'rspec/core/rake_task'
Bundler::GemHelper.install_tasks

if Gem.ruby_version >= Gem::Version.new("2.2.2")
  require 'github_changelog_generator/task'
  GitHubChangelogGenerator::RakeTask.new :changelog
  task :changelog_commit do
    require_relative "lib/jasmine_rails/version"
    cmd = "git commit -m \"Changelog for #{JasmineRails::VERSION}\" -- CHANGELOG.md"
    puts "-------> #{cmd}"
    system cmd
  end
  Rake::Task["release:rubygem_push"].enhance([:changelog, :changelog_commit])
end

