require 'rubygems'
require 'bundler'
require 'stringio'
require 'tmpdir'

envs = [:default, :development]
envs << :debug if ENV['DEBUG']
Bundler.setup(*envs)

$:.unshift(File.expand_path(File.join(File.dirname(__FILE__), '../lib')))
require 'jasmine'
require 'rspec'

def rails_available?
  if Gem::Specification.respond_to?(:find_by_name)
    Gem::Specification.find_by_name('railties', '>= 3')
  elsif Gem.respond_to?(:available?)
    Gem.available?('railties', '>= 3')
  end
rescue Gem::LoadError
  false
end

def create_temp_dir
  tmp = File.join(Dir.tmpdir, "jasmine-gem-test_#{Time.now.to_f}")
  FileUtils.rm_r(tmp, :force => true)
  FileUtils.mkdir(tmp)
  tmp
end

def temp_dir_before
  @root = File.expand_path(File.join(File.dirname(__FILE__), '..'))
  @old_dir = Dir::pwd
  @tmp = create_temp_dir
end

def temp_dir_after
  Dir::chdir @old_dir
  FileUtils.rm_r @tmp
end

def custom_jasmine_config(prefix, &block)
  require 'yaml'
  support_path = File.join('spec', 'javascripts', 'support')
  jasmine_config = YAML.load_file(File.join(support_path, 'jasmine.yml'))
  block.call(jasmine_config)
  custom_path = File.join(support_path, "#{prefix}_jasmine.yml")
  File.open(custom_path, 'w') do |f|
    f.write YAML.dump(jasmine_config)
    f.flush
  end
  custom_path
end

module Kernel
  def capture_stdout
    out = StringIO.new
    $stdout = out
    yield
    return out.string
  ensure
    $stdout = STDOUT
  end
end

def passing_raw_result
  {'id' => 123, 'status' => 'passed', 'fullName' => 'Passing test', 'description' => 'Passing', 'failedExpectations' => []}
end

def pending_raw_result
  {'id' => 123, 'status' => 'pending', 'fullName' => 'Passing test', 'description' => 'Pending', 'failedExpectations' => [], 'pendingReason' => 'I pend because'}
end

def disabled_raw_result
  {'id' => 123, 'status' => 'disabled', 'fullName' => 'Disabled test', 'description' => 'Disabled', 'failedExpectations' => []}
end

def failing_raw_result
  {
    'status' => 'failed',
    'id' => 124,
    'description' => 'a failing spec',
    'fullName' => 'a suite with a failing spec',
    'failedExpectations' => [
      {
        'message' => 'a failure message',
        'stack' => 'a stack trace'
      }
    ]
  }
end

def failing_result_no_stack_trace
  {
    'status' => 'failed',
    'id' => 124,
    'description' => 'a failing spec',
    'fullName' => 'a suite with a failing spec',
    'failedExpectations' => [
      {
        'message' => 'a failure message, but no stack',
        'stack' => nil
      }
    ]
  }
end

