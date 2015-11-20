require 'test/unit'
require 'test/unit/assertions'
require 'rake/file_list'

module Rake
  include Test::Unit::Assertions

  def run_tests(pattern='test/test*.rb', log_enabled=false)
    FileList.glob(pattern).each do |fn|
      $stderr.puts fn if log_enabled
      begin
        require fn
      rescue Exception => ex
        $stderr.puts "Error in #{fn}: #{ex.message}"
        $stderr.puts ex.backtrace
        assert false
      end
    end
  end

  extend self
end
