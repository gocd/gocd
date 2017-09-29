# coding: utf-8

require 'rspec/core/rake_task'
require 'rspec/core/version'

RSpec::Core::RakeTask.new(:spec) do |task|
  task.verbose = false
end

Rake::Task[:spec].enhance(['spec:convert', 'spec:setup'])

namespace :spec do
  task :convert do
    next if RSpec::Core::Version::STRING.start_with?('2.')

    puts "Converting specs before running them on RSpec #{RSpec::Core::Version::STRING}..."

    require 'fileutils'
    Dir.mkdir('tmp') unless Dir.exist?('tmp')
    FileUtils.mv('Gemfile.lock', 'tmp')

    begin
      Bundler.with_clean_env do
        ENV['RSPEC_VERSION'] = '2.99'
        sh 'bundle', 'install', '--retry', '3'

        ENV['TRANSPEC_TEST'] = 'true'
        ENV['CI'] = ENV['JENKINS_URL'] = ENV['COVERALLS_RUN_LOCALLY'] = nil # Disable Coveralls.
        sh 'bundle', 'exec', 'transpec'
      end
    ensure
      puts 'Reverting to the original Gemfile.lock...'
      FileUtils.mv('tmp/Gemfile.lock', '.')
    end
  end

  task :setup do
    ENV['TRANSPEC_TEST'] = 'true'
    puts "Running specs on RSpec #{RSpec::Core::Version::STRING}..."
  end
end
