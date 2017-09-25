# Rakefile for rake        -*- ruby -*-

# Copyright 2003, 2004, 2005 by Jim Weirich (jim@weirichhouse.org)
# All rights reserved.

# This file may be distributed under an MIT style license.  See
# MIT-LICENSE for details.

lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)

require 'bundler/gem_tasks'
require 'rake/testtask'
require 'rdoc/task'

Rake::TestTask.new(:test) do |t|
  t.libs << "test"
  t.verbose = true
  t.test_files = FileList['test/**/test_*.rb']
end

RDoc::Task.new do |doc|
  doc.main   = 'README.rdoc'
  doc.title  = 'Rake -- Ruby Make'
  doc.rdoc_files = FileList.new %w[lib MIT-LICENSE doc/**/*.rdoc *.rdoc]
  doc.rdoc_dir = 'html'
end

task :default => :test
