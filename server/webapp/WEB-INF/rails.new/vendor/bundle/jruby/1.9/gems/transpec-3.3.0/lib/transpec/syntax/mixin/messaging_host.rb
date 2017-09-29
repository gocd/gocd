# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/any_instance_block'
require 'transpec/syntax/mixin/no_message_allowance'
require 'transpec/syntax/mixin/useless_and_return'

module Transpec
  class Syntax
    module Mixin
      module MessagingHost
        extend ActiveSupport::Concern
        include Mixin::AnyInstanceBlock, Mixin::NoMessageAllowance, Mixin::UselessAndReturn
      end
    end
  end
end
