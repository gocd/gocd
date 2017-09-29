# coding: utf-8

require 'transpec/syntax/rspec_configure/framework'

module Transpec
  class Syntax
    class RSpecConfigure
      class Expectations < Framework
        def block_method_name
          :expect_with
        end
      end
    end
  end
end
