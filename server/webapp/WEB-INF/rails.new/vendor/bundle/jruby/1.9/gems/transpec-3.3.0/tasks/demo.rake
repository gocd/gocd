# coding: utf-8

require_relative 'lib/demo'

namespace :demo do
  # rubocop:disable LineLength
  demos = [
    Demo.new('git@github.com:yujinakayama/twitter.git', 'transpec-test-rspec-2-99'),
    Demo.new('git@github.com:yujinakayama/guard.git', 'transpec-test-rspec-2-99', %w(--without development)),
    Demo.new('git@github.com:yujinakayama/mail.git', 'transpec-test-rspec-2-99')
  ]
  # rubocop:enable LineLength

  desc 'Publish conversion examples of all projects'
  task all: demos.map(&:name)

  demos.each do |demo|
    desc "Publish conversion example of #{demo.name} project"
    task demo.name do
      demo.run
    end
  end
end
