# -*- ruby -*-
gem 'hoe', '>=2.0.0'
require 'hoe'

$:.unshift(File.join(File.dirname(__FILE__), 'lib'))

require 'spec/version'
require 'spec/rake/spectask'
require 'cucumber/rake/task'

Hoe.spec('rspec') do |hoe|
  hoe.version = Spec::VERSION::STRING
  hoe.summary = Spec::VERSION::SUMMARY
  hoe.description = "Behaviour Driven Development for Ruby."
  hoe.rubyforge_name = 'rspec'
  hoe.developer('RSpec Development Team', 'rspec-devel@rubyforge.org')
  hoe.extra_dev_deps = [["cucumber",">= 0.2.2"]]
  hoe.remote_rdoc_dir = "rspec/#{Spec::VERSION::STRING}"
  hoe.rspec_options = ['--options', 'spec/spec.opts']
  hoe.history_file = 'History.rdoc'
  hoe.readme_file  = 'README.rdoc'
  hoe.post_install_message = <<-POST_INSTALL_MESSAGE
#{'*'*50}

  Thank you for installing rspec-#{Spec::VERSION::STRING}

  Please be sure to read History.rdoc and Upgrade.rdoc
  for useful information about this release.

#{'*'*50}
POST_INSTALL_MESSAGE
end

['audit','test','test_deps','default','post_blog'].each do |task|
  Rake.application.instance_variable_get('@tasks').delete(task)
end

task :post_blog do
  # no-op
end

# Some of the tasks are in separate files since they are also part of the website documentation
load File.dirname(__FILE__) + '/resources/rake/examples.rake'
load File.dirname(__FILE__) + '/resources/rake/examples_with_rcov.rake'
load File.dirname(__FILE__) + '/resources/rake/failing_examples_with_html.rake'
load File.dirname(__FILE__) + '/resources/rake/verify_rcov.rake'

if RUBY_VERSION =~ /^1.8/
  task :default => [:verify_rcov, :features]
else
  task :default => [:spec, :features]
end

namespace :spec do
  desc "Run all specs with rcov"
  Spec::Rake::SpecTask.new('rcov') do |t|
    t.spec_files = FileList['spec/**/*_spec.rb']
    t.spec_opts = ['--options', 'spec/spec.opts']
    t.rcov = true
    t.rcov_dir = 'coverage'
    t.rcov_opts = ['--exclude', "kernel,load-diff-lcs\.rb,instance_exec\.rb,lib/spec.rb,lib/spec/runner.rb,^spec/*,bin/spec,examples,/gems,/Library/Ruby,\.autotest,#{ENV['GEM_HOME']}"]
  end
end

desc "Run Cucumber features"
task :features do
  sh(RUBY_VERSION =~ /^1.8/ ? "cucumber" : "cucumber --profile no_heckle")
end

desc "Run failing examples (see failure output)"
Spec::Rake::SpecTask.new('failing_examples') do |t|
  t.spec_files = FileList['failing_examples/**/*_spec.rb']
  t.spec_opts = ['--options', 'spec/spec.opts']
end

def egrep(pattern)
  Dir['**/*.rb'].each do |fn|
    count = 0
    open(fn) do |f|
      while line = f.gets
        count += 1
        if line =~ pattern
          puts "#{fn}:#{count}:#{line}"
        end
      end
    end
  end
end

desc "Look for TODO and FIXME tags in the code"
task :todo do
  egrep /(FIXME|TODO|TBD)/
end

desc "verify_committed, verify_rcov, post_news, release"
task :complete_release => [:verify_committed, :verify_rcov, :post_news, :release]

desc "Verifies that there is no uncommitted code"
task :verify_committed do
  IO.popen('git status') do |io|
    io.each_line do |line|
      raise "\n!!! Do a git commit first !!!\n\n" if line =~ /^#\s*modified:/
    end
  end
end

namespace :update do
  desc "update the manifest"
  task :manifest do
    system %q[touch Manifest.txt; rake check_manifest | grep -v "(in " | patch]
  end
end

task :clobber => :clobber_tmp

task :clobber_tmp do
  cmd = %q[rm -r tmp]
  puts cmd
  system cmd if test ?d, 'tmp'
end
