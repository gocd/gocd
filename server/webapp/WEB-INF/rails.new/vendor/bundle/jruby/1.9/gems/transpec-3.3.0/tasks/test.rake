# coding: utf-8

require_relative 'lib/test'

namespace :test do
  # On Travis CI, reuse system gems to speed up build.
  bundler_args = if ENV['TRAVIS']
                   []
                 else
                   %w(--path vendor/bundle)
                 end

  # rubocop:disable LineLength
  tests = [
    Test.new('https://github.com/yujinakayama/twitter.git', 'transpec-test-rspec-2-99', bundler_args),
    Test.new('https://github.com/yujinakayama/mail.git', 'transpec-test-rspec-2-99', bundler_args),
    # Test.new('https://github.com/yujinakayama/guard.git', 'transpec-test-rspec-2-99', bundler_args + %w(--without development))
  ]
  # rubocop:enable LineLength

  desc 'Test transpec on all projects'
  task all: tests.map(&:name)

  tests.each do |test|
    desc "Test transpec on #{test.name} project"
    task test.name do
      test.run
    end
  end
end
