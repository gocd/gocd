#!/usr/bin/env rake
# -*- Ruby -*-
require 'rubygems'
require 'rubygems/package_task'
require 'rdoc/task'
require 'rake/testtask'
require 'rake/extensiontask'
require 'rake/javaextensiontask'

lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require "ruby-debug-base/version"

namespace :test do
  desc "Test ruby-debug-base"
  Rake::TestTask.new(:base => :compile) do |t|
    t.libs += %w(./ext ./lib)
    t.test_files = FileList['test/base/*.rb']
    t.options = '--verbose' if $VERBOSE
    t.ruby_opts << "--debug" if defined?(JRUBY_VERSION)
  end

  desc "Test ruby-debug"
  Rake::TestTask.new(:cli => :compile) do |t|
    t.libs += %W(./lib ./cli ./ext)
    t.test_files = FileList['test/cli/commands/unit/*.rb',
                            'test/cli/commands/*_test.rb',
                            'test/cli/**/*_test.rb',
                            'test/test-*.rb']
    t.options = '--verbose' if $VERBOSE
    t.ruby_opts << "--debug" if defined?(JRUBY_VERSION)
  end
end

desc "Test everything"
task :test => %w(test:base test:cli)

desc "Create a GNU-style ChangeLog via svn2cl"
task :ChangeLog do
  system('git log --pretty --numstat --summary     | git2cl >     ChangeLog')
  system('git log --pretty --numstat --summary ext | git2cl > ext/ChangeLog')
  system('git log --pretty --numstat --summary lib | git2cl > lib/ChangeLog')
end

base_spec = Gem::Specification.load("ruby-debug-base.gemspec")
cli_spec  = Gem::Specification.load("ruby-debug.gemspec")

if defined?(JRUBY_VERSION)
  Rake::JavaExtensionTask.new('ruby_debug', base_spec) do |t|
    t.ext_dir = "src"
  end
else
  Rake::ExtensionTask.new('ruby_debug', base_spec) do |t|
    t.ext_dir = "ext"
  end
end

Gem::PackageTask.new(base_spec) {}
Gem::PackageTask.new(cli_spec) {}

task :default => :test

desc "Remove built files"
task :clean do
  cd "ext" do
    if File.exists?("Makefile")
      sh "make clean"
      rm  "Makefile"
    end
    derived_files = Dir.glob(".o") + Dir.glob("*.so")
    rm derived_files unless derived_files.empty?
  end
  rm 'lib/ruby_debug.jar' if File.exists?("lib/ruby_debug.jar")
end

desc "Generate rdoc documentation"
RDoc::Task.new("rdoc") do |rdoc|
  rdoc.rdoc_dir = 'doc/rdoc'
  rdoc.title    = "ruby-debug"
  # Show source inline with line numbers
  rdoc.options << "--inline-source" << "--line-numbers"
  # Make the readme file the start page for the generated html
  rdoc.options << '--main' << 'README'
  rdoc.rdoc_files.include('bin/**/*',
                          'cli/ruby-debug/commands/*.rb',
                          'cli/ruby-debug/*.rb',
                          'lib/**/*.rb',
                          'ext/**/ruby_debug.c',
                          'README',
                          'LICENSE')
end
