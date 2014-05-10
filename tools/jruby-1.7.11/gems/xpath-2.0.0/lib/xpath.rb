require 'nokogiri'

require 'xpath/dsl'
require 'xpath/expression'
require 'xpath/literal'
require 'xpath/union'
require 'xpath/renderer'
require 'xpath/html'

module XPath

  extend XPath::DSL::TopLevel
  include XPath::DSL::TopLevel

  def self.generate
    yield(self)
  end
end
