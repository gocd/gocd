require 'action_view/testing/resolvers'

RSpec.configure do |config|
  # This allows us to expose `render_views` as a config option even though it
  # breaks the convention of other options by using `render_views` as a
  # command (i.e. render_views = true), where it would normally be used as a
  # getter. This makes it easier for rspec-rails users because we use
  # `render_views` directly in example groups, so this aligns the two APIs,
  # but requires this workaround:
  config.add_setting :rendering_views, :default => false

  def config.render_views=(val)
    self.rendering_views = val
  end

  def config.render_views
    self.rendering_views = true
  end

  def config.render_views?
    rendering_views
  end
end

module RSpec
  module Rails
    module ViewRendering
      extend ActiveSupport::Concern

      attr_accessor :controller

      module ClassMethods
        def metadata_for_rspec_rails
          metadata[:rspec_rails] = metadata[:rspec_rails] ? metadata[:rspec_rails].dup : {}
        end

        # @see RSpec::Rails::ControllerExampleGroup
        def render_views(true_or_false=true)
          metadata_for_rspec_rails[:render_views] = true_or_false
        end

        # @deprecated Use `render_views` instead.
        def integrate_views
          RSpec.deprecate("integrate_views", :replacement => "render_views")
          render_views
        end

        # @api private
        def render_views?
          metadata_for_rspec_rails.fetch(:render_views) do
            RSpec.configuration.render_views?
          end
        end
      end

      # @api private
      def render_views?
        self.class.render_views? || !controller.class.respond_to?(:view_paths)
      end

      # Delegates find_all to the submitted path set and then returns templates
      # with modified source
      class EmptyTemplatePathSetDecorator < ::ActionView::Resolver
        attr_reader :original_path_set

        def initialize(original_path_set)
          @original_path_set = original_path_set
        end

        # @api private
        def find_all(*args)
          original_path_set.find_all(*args).collect do |template|
            ::ActionView::Template.new(
              "",
              template.identifier,
              EmptyTemplateHandler,
              {
                :virtual_path => template.virtual_path,
                :format => template.formats
              }
            )
          end
        end
      end

      class EmptyTemplateHandler
        def self.call(template)
          %("")
        end
      end

      module EmptyTemplates
        # @api private
        def prepend_view_path(new_path)
          lookup_context.view_paths.unshift(*_path_decorator(new_path))
        end

        # @api private
        def append_view_path(new_path)
          lookup_context.view_paths.push(*_path_decorator(new_path))
        end

        private

        def _path_decorator(path)
          EmptyTemplatePathSetDecorator.new(ActionView::PathSet.new(Array.wrap(path)))
        end
      end

      included do
        before do
          unless render_views?
            @_empty_view_path_set_delegator = EmptyTemplatePathSetDecorator.new(controller.class.view_paths)
            controller.class.view_paths = ::ActionView::PathSet.new.push(@_empty_view_path_set_delegator)
            controller.extend(EmptyTemplates)
          end
        end

        after do
          unless render_views?
            controller.class.view_paths = @_empty_view_path_set_delegator.original_path_set
          end
        end
      end
    end
  end
end
