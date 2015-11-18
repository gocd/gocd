begin
  require 'rubygems'
  gem 'minitest'
rescue Gem::LoadError
  # do nothing
end

require 'minitest/unit'
require 'minitest/spec'
require 'minitest/mock'

MiniTest::Unit.autorun
