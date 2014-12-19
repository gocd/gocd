require 'rubygems'

$:.unshift File.expand_path("#{File.dirname(__FILE__)}/../vendor/bundle")

# Set up gems listed in the Gemfile.
ENV['BUNDLE_GEMFILE'] ||= File.expand_path('../../Gemfile', __FILE__)

require 'bundler/setup' if File.exists?(ENV['BUNDLE_GEMFILE'])
