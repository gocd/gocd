module RSpec
  module Matchers
    module BuiltIn
      class Exist < BaseMatcher
        def initialize(*expected)
          @expected = expected
        end

        def matches?(actual)
          @actual = actual
          predicates = [:exist?, :exists?].select { |p| @actual.respond_to?(p) }
          existence_values = predicates.map { |p| @actual.send(p, *@expected) }
          uniq_truthy_values = existence_values.map { |v| !!v }.uniq

          case uniq_truthy_values.size
          when 0; raise NoMethodError.new("#{@actual.inspect} does not respond to either #exist? or #exists?")
          when 1; existence_values.first
          else raise "#exist? and #exists? returned different values:\n\n" +
            " exist?: #{existence_values.first}\n" +
            "exists?: #{existence_values.last}"
          end
        end
      end
    end
  end
end
