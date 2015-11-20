# -*- ruby encoding: utf-8 -*-

require 'rubygems'
require 'rspec'
require 'hoe'

Hoe.plugin :bundler
Hoe.plugin :doofus
Hoe.plugin :email
Hoe.plugin :gemspec2
Hoe.plugin :git
Hoe.plugin :rubyforge
Hoe.plugin :travis

Hoe.spec 'diff-lcs' do
  developer('Austin Ziegler', 'austin@rubyforge.org')

  self.remote_rdoc_dir = '.'
  self.rsync_args << ' --exclude=statsvn/'

  self.history_file = 'History.rdoc'
  self.readme_file = 'README.rdoc'
  self.extra_rdoc_files = FileList["*.rdoc"].to_a

  self.extra_dev_deps << ['hoe-bundler', '~> 1.2']
  self.extra_dev_deps << ['hoe-doofus', '~> 1.0']
  self.extra_dev_deps << ['hoe-gemspec2', '~> 1.1']
  self.extra_dev_deps << ['hoe-git', '~> 1.5']
  self.extra_dev_deps << ['hoe-rubygems', '~> 1.0']
  self.extra_dev_deps << ['hoe-travis', '~> 1.2']
  self.extra_dev_deps << ['rake', '~> 10.0']
  self.extra_dev_deps << ['rspec', '~> 2.0']
end

unless Rake::Task.task_defined? :test
  task :test => :spec
end

# vim: syntax=ruby
