# coding: utf-8

require 'rspec/expectations'
require_relative 'project'

class Test < Project
  include RSpec::Matchers

  def run
    puts " Testing transpec on #{name} project ".center(80, '=')

    setup

    in_project_dir do
      transpec '--force'
      sh 'bundle exec rspec'
      compare_summary!('2.99.0')

      puts 'Rewriting `rspec` version in Gemfile as 3.0.0'
      add_rspec_3_to_gemfile
      sh 'bundle update'

      transpec '--force', '--convert', 'example_group,hook_scope'
      sh 'bundle exec rspec'
      compare_summary!('3.0.0')
    end
  end

  private

  def transpec(*args)
    with_clean_bundler_env do
      ENV['BUNDLE_GEMFILE'] = File.join(Transpec.root, 'Gemfile')
      sh 'bundle', 'exec', File.join(Transpec.root, 'bin', 'transpec'), *args
    end
  end

  def add_rspec_3_to_gemfile
    gemfile = File.read('Gemfile')

    pattern = /\bgem\s+['"]rspec.+/
    rspec_3_specification = "gem 'rspec', '~> 3.0.0.beta1'"

    if gemfile.match(pattern)
      gemfile.sub!(pattern, rspec_3_specification)
    else
      gemfile << rspec_3_specification
    end

    File.write('Gemfile', gemfile)
  end

  def compare_summary!(key)
    fixture_path = commit_message_fixture_path(key)

    if File.exist?(fixture_path)
      summary = summary_in_commit_message(File.read(commit_message_path))
      expected_summary = summary_in_commit_message(File.read(fixture_path))
      expect(summary).to eq(expected_summary)
    else
      warn "#{fixture_path} does not exist. Copying from #{commit_message_path}."
      FileUtils.mkdir_p(File.dirname(fixture_path))
      FileUtils.cp(commit_message_path, fixture_path)
    end
  end

  def summary_in_commit_message(message)
    message.lines.to_a[5..-1].join("\n")
  end

  def commit_message_path
    File.join(project_dir, '.git', 'COMMIT_EDITMSG')
  end

  def commit_message_fixture_path(key)
    path = File.join(Transpec.root, 'tasks', 'fixtures', name, key, 'COMMIT_EDITMSG')
    File.expand_path(path)
  end
end
