# coding: utf-8

# Aliases by Capybara:
# https://github.com/jnicklas/capybara/blob/2.2.0/lib/capybara/rspec/features.rb

module Transpec
  module RSpecDSL
    # https://github.com/rspec/rspec-core/blob/77cc21e/lib/rspec/core/example_group.rb#L239-L265
    # https://github.com/rspec/rspec-core/blob/77cc21e/lib/rspec/core/shared_example_group.rb#L50-L61
    EXAMPLE_GROUP_METHODS = [
      :example_group,
      :describe, :context,
      :xdescribe, :xcontext,
      :fdescribe, :fcontext,
      :shared_examples, :shared_context, :share_examples_for, :shared_examples_for,
      :feature # Capybara
    ].freeze

    # https://github.com/rspec/rspec-core/blob/77cc21e/lib/rspec/core/example_group.rb#L130-L171
    EXAMPLE_METHODS = [
      :example, :it, :specify,
      :focus, :fexample, :fit, :fspecify,
      :focused, # TODO: Support conversion
      :xexample, :xit, :xspecify,
      :skip, :pending,
      :scenario, :xscenario # Capybara
    ].freeze

    HOOK_METHODS = [
      :before, :after, :around,
      :background # Capybara
    ].freeze

    HELPER_METHODS = [
      :subject, :subject!, :let, :let!,
      :given, :given! # Capybara
    ].freeze
  end
end
