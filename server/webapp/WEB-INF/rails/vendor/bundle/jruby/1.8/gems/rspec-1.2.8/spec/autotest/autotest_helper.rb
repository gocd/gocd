begin
  require 'autotest'
rescue LoadError
  require 'rubygems' unless ENV['NO_RUBYGEMS']
  require 'autotest'
end
dir = File.dirname(__FILE__)
require File.expand_path("#{dir}/../spec_helper")
require File.expand_path("#{dir}/../../lib/autotest/rspec")
require File.expand_path("#{dir}/autotest_matchers")
