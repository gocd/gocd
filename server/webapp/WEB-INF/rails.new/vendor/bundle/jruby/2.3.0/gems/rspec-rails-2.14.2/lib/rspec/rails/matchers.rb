require 'rspec/core/deprecation'
require 'rspec/expectations'

module RSpec::Rails
  module Matchers
  end
end

begin
  require 'test/unit/assertionfailederror'
rescue LoadError
  module Test
    module Unit
      class AssertionFailedError < StandardError
      end
    end
  end
end

require 'rspec/rails/matchers/have_rendered'
require 'rspec/rails/matchers/redirect_to'
require 'rspec/rails/matchers/routing_matchers'
require 'rspec/rails/matchers/be_new_record'
require 'rspec/rails/matchers/be_a_new'
require 'rspec/rails/matchers/have_extension'
require 'rspec/rails/matchers/relation_match_array'
require 'rspec/rails/matchers/be_valid'
