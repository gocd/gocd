#!/usr/bin/env rake
require "bundler/gem_tasks"
require 'rake/testtask'

Rake::TestTask.new do |t|
  t.libs = ["test"]
  t.pattern = "test/**/*_test.rb"
  t.ruby_opts = ['-w']
end

task :default => :test

specname = "activerecord-deprecated_finders.gemspec"
deps = `git ls-files`.split("\n") - [specname]

task :gemspec => specname

file specname => deps do
  files       = `git ls-files`.split("\n") - Dir["gemfiles/*"]
  test_files  = `git ls-files -- {test,spec,features}/*`.split("\n")
  executables = `git ls-files -- bin/*`.split("\n").map{ |f| File.basename(f) }

  require 'erb'

  File.open specname, 'w:utf-8' do |f|
    f.write ERB.new(File.read("#{specname}.erb")).result(binding)
  end
end
