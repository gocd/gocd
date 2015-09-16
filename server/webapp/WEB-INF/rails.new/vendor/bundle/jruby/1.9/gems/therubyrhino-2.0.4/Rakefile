require 'bundler/setup'

require 'rspec/core/rake_task'
RSpec::Core::RakeTask.new

task :default => :spec

desc "Remove all build artifacts"
task :clean do
  sh "rm -rf pkg/"
end

require 'bundler/gem_helper'

['therubyrhino', 'therubyrhino_jar'].each do |name|
  gem_helper = Bundler::GemHelper.new(Dir.pwd, name)
  def gem_helper.version_tag
    "#{name}-#{version}" # override "v#{version}"
  end
  version = gem_helper.send(:version)
  version_tag = gem_helper.version_tag
  namespace name do
    desc "Build #{name}-#{version}.gem into the pkg directory"
    task('build') { gem_helper.build_gem }

    desc "Build and install #{name}-#{version}.gem into system gems"
    task('install') { gem_helper.install_gem }

    desc "Create tag #{version_tag} and build and push #{name}-#{version}.gem to Rubygems"
    task('release') { gem_helper.release_gem }
  end
end

task :build   => 'therubyrhino:build'
task :install => 'therubyrhino:install'
task :release => 'therubyrhino:release'
