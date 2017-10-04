require 'rspec/matchers'

module RSpec
  module Matchers
    # Passes if receiver is a collection with the submitted number of items OR
    # if the receiver OWNS a collection with the submitted number of items.
    #
    # If the receiver OWNS the collection, you must use the name of the
    # collection. So if a `Team` instance has a collection named `#players`,
    # you must use that name to set the expectation.
    #
    # If the receiver IS the collection, you can use any name you like for
    # `named_collection`. We'd recommend using either "elements", "members", or
    # "items" as these are all standard ways of describing the things IN a
    # collection.
    #
    # This also works for Strings, letting you set expectations about their
    # lengths.
    #
    # @example
    #
    #   # Passes if team.players.size == 11
    #   expect(team).to have(11).players
    #
    #   # Passes if [1,2,3].length == 3
    #   expect([1,2,3]).to have(3).items #"items" is pure sugar
    #
    #   # Passes if ['a', 'b', 'c'].count == 3
    #   expect([1,2,3]).to have(3).items #"items" is pure sugar
    #
    #   # Passes if "this string".length == 11
    #   expect("this string").to have(11).characters #"characters" is pure sugar
    def have(n)
      RSpec::CollectionMatchers::Have.new(n)
    end
    alias :have_exactly :have

    # Exactly like have() with >=.
    #
    # @example
    #   expect("this").to have_at_least(3).letters
    #
    # ### Warning:
    #
    # `expect(..).not_to have_at_least` is not supported
    def have_at_least(n)
      RSpec::CollectionMatchers::Have.new(n, :at_least)
    end

    # Exactly like have() with <=.
    #
    # @example
    #   expect("this").to have_at_most(4).letters
    #
    # ### Warning:
    #
    # `expect(..).not_to have_at_most` is not supported
    def have_at_most(n)
      RSpec::CollectionMatchers::Have.new(n, :at_most)
    end
  end
end
