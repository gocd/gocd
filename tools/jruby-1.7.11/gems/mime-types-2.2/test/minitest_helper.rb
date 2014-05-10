# -*- ruby encoding: utf-8 -*-

require 'mime/type'
require 'fileutils'

gem 'minitest'
require 'minitest/autorun'

module MIME
  @__deprecated = Hash.new { |h, k| h[k] = true }

  class << self
    attr_reader :__deprecated
  end
end

def assert_deprecated(name, message = "and will be removed")
  MIME.__deprecated[name] = false
  assert_output(nil, /#{Regexp.escape(name)} is deprecated #{Regexp.escape(message)}./) { yield }
ensure
  MIME.__deprecated[name] = true
end
