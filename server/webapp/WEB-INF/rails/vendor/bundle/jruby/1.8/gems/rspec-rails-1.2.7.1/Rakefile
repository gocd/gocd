# -*- ruby -*-
gem 'hoe', '>=2.0.0'
require 'hoe'
require './lib/spec/rails/version'
require 'cucumber/rake/task'

$:.unshift(File.join(File.dirname(__FILE__), "/../rspec/lib"))

require 'spec/rake/spectask'

Hoe.spec('rspec-rails') do |p|
  p.version = Spec::Rails::VERSION::STRING
  p.summary = Spec::Rails::VERSION::SUMMARY
  p.description = "Behaviour Driven Development for Ruby on Rails."
  p.rubyforge_name = 'rspec'
  p.developer('RSpec Development Team', 'rspec-devel@rubyforge.org')
  p.extra_deps = [["rspec",">=1.2.7"],["rack",">=0.4.0"]]
  p.extra_dev_deps = [["cucumber",">= 0.3.11"]]
  p.remote_rdoc_dir = "rspec-rails/#{Spec::Rails::VERSION::STRING}"
  p.history_file = 'History.rdoc'
  p.readme_file  = 'README.rdoc'
  p.post_install_message = <<-POST_INSTALL_MESSAGE
#{'*'*50}

  Thank you for installing rspec-rails-#{Spec::Rails::VERSION::STRING}
  
  If you are upgrading, do this in each of your rails apps
  that you want to upgrade:

    $ ruby script/generate rspec

  Please be sure to read History.rdoc and Upgrade.rdoc
  for useful information about this release.

#{'*'*50}
POST_INSTALL_MESSAGE
end

['audit','test','test_deps','default','post_blog', 'release'].each do |task|
  Rake.application.instance_variable_get('@tasks').delete(task)
end

task :post_blog do
  # no-op
end

task :release => [:clean, :package] do |t|
  version = ENV["VERSION"] or abort "Must supply VERSION=x.y.z"
  abort "Versions don't match #{version} vs #{Spec::Rails::VERSION::STRING}" unless version == Spec::Rails::VERSION::STRING
  pkg = "pkg/rspec-rails-#{version}"

  rubyforge = RubyForge.new.configure
  puts "Logging in to rubyforge ..."
  rubyforge.login

  puts "Releasing rspec-rails version #{version} ..."
  ["#{pkg}.gem", "#{pkg}.tgz"].each do |file|
    rubyforge.add_file('rspec', 'rspec', Spec::Rails::VERSION::STRING, file)
  end
end

Cucumber::Rake::Task.new

task :default => [:features]

namespace :update do
  desc "update the manifest"
  task :manifest do
    system %q[touch Manifest.txt; rake check_manifest | grep -v "(in " | patch]
  end
end
