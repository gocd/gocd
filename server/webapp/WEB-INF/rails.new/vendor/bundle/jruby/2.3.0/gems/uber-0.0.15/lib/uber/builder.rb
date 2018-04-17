require "uber/options"

module Uber
  # When included, allows to add builder on the class level.
  #
  #   class Operation
  #     include Uber::Builder
  #
  #     builds do |params|
  #       SignedIn if params[:current_user]
  #     end
  #
  #     class SignedIn
  #     end
  #
  # The class then has to call the builder to compute a class name using the build blocks you defined.
  #
  #     def self.build(params)
  #       class_builder.call(params).
  #       new(params)
  #     end
  module Builder
    def self.included(base)
      base.class_eval do
        def self.builders
          @builders ||= []
        end

        extend ClassMethods
      end
    end

    # Computes the concrete target class.
    class Constant
      def initialize(constant, context)
        @constant     = constant
        @context      = context
        @builders     = @constant.builders # only dependency, must be a Cell::Base subclass.
      end

      def call(*args)
        build_class_for(*args)
      end

    private
      def build_class_for(*args)
        @builders.each do |blk|
          klass = run_builder_block(blk, *args) and return klass
        end
        @constant
      end

      def run_builder_block(block, *args)
        block.(@context, *args) # Uber::Value.call()
      end
    end

    module ClassMethods
      # Adds a builder to the cell class. Builders are used in #cell to find out the concrete
      # class for rendering. This is helpful if you frequently want to render subclasses according
      # to different circumstances (e.g. login situations) and you don't want to place these deciders in
      # your view code.
      #
      # Passes the model and options from #cell into the block.
      #
      # Multiple build blocks are ORed, if no builder matches the building cell is used.
      #
      # Example:
      #
      # Consider two different user box cells in your app.
      #
      #   class AuthorizedUserBox < UserInfoBox
      #   end
      #
      #   class AdminUserBox < UserInfoBox
      #   end
      #
      # Now you don't want to have deciders all over your views - use a declarative builder.
      #
      #   UserInfoBox.build do |model, options|
      #     AuthorizedUserBox if options[:is_signed_in]
      #     AdminUserBox if model.admin?
      #   end
      #
      # In your view #cell will instantiate the right class for you now.
      def builds(proc=nil, &block)
        builders << Uber::Options::Value.new(proc.nil? ? block : proc) # TODO: provide that in Uber::O:Value.
      end

      # Call this from your classes' own ::build method to compute the concrete target class.
      # The class_builder is cached, you can't change the context once it's set.
      def class_builder(context=nil)
        @class_builder ||= Constant.new(self, context)
      end
    end # ClassMethods
  end
end
