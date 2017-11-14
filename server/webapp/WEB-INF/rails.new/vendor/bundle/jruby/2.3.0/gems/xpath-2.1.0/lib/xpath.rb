require 'nokogiri'

require 'xpath/dsl'
require 'xpath/expression'
require 'xpath/literal'
require 'xpath/union'
require 'xpath/renderer'
require 'xpath/html'

module XPath
  extend XPath::DSL
  include XPath::DSL

  def self.generate
    yield(self)
  end
end
