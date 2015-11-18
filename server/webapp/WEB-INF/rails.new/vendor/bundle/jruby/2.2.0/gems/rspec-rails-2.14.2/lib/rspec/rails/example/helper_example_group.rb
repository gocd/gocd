require 'rspec/rails/view_assigns'

module RSpec::Rails
  module HelperExampleGroup
    extend ActiveSupport::Concern
    include RSpec::Rails::RailsExampleGroup
    include ActionView::TestCase::Behavior
    include RSpec::Rails::ViewAssigns

    module ClassMethods
      # @api private
      def determine_default_helper_class(ignore)
        described_class
      end
    end

    # Returns an instance of ActionView::Base with the helper being specified
    # mixed in, along with any of the built-in rails helpers.
    def helper
      _view.tap do |v|
        v.extend(ApplicationHelper) if defined?(ApplicationHelper)
        v.assign(view_assigns)
      end
    end

    private

    def _controller_path
      example.example_group.described_class.to_s.sub(/Helper/,'').underscore
    end

    included do
      metadata[:type] = :helper

      before do
        controller.controller_path = _controller_path
      end
    end
  end
end
