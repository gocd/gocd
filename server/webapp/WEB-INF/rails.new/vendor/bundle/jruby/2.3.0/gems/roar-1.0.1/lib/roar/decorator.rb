require 'roar/representer'
require 'representable/decorator'

class Roar::Decorator < Representable::Decorator
  module HypermediaConsumer
    def links=(arr)
      links = super
      represented.instance_variable_set :@links, links
    end
  end
end