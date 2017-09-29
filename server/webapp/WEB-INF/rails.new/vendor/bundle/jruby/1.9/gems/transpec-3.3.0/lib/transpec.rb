# coding: utf-8

require 'transpec/rspec_version'

module Transpec
  def self.root
    File.expand_path('..', File.dirname(__FILE__))
  end

  def self.required_rspec_version
    @required_rspec_version ||= RSpecVersion.new('2.14')
  end
end
