require 'active_support/deprecation'

module ActiveRecord
  module DynamicMatchers
    module DeprecatedFinder
      def body
        <<-CODE
          result = #{super}
          result && block_given? ? yield(result) : result
        CODE
      end

      def result
        "all.apply_finder_options(options, true).#{super}"
      end

      def signature
        "#{super}, options = {}"
      end
    end

    module DeprecationWarning
      def body
        "#{deprecation_warning}\n#{super}"
      end

      def deprecation_warning
        %{ActiveSupport::Deprecation.warn("This dynamic method is deprecated. Please use e.g. #{deprecation_alternative} instead.")}
      end
    end

    module FindByDeprecationWarning
      def body
        <<-CODE
          if block_given?
            ActiveSupport::Deprecation.warn("Calling find_by or find_by! methods with a block is deprecated with no replacement.")
          end

          unless options.empty?
            ActiveSupport::Deprecation.warn(
              "Calling find_by or find_by! methods with options is deprecated. " \
              "Build a scope instead, e.g. User.where('age > 21').find_by_name('Jon')."
            )
          end

          #{super}
        CODE
      end
    end

    class FindBy
      include DeprecatedFinder
      include FindByDeprecationWarning
    end

    class FindByBang
      include DeprecatedFinder
      include FindByDeprecationWarning
    end

    class FindAllBy < Method
      Method.matchers << self
      include Finder
      include DeprecatedFinder
      include DeprecationWarning

      def self.prefix
        "find_all_by"
      end

      def finder
        "where"
      end

      def result
        "#{super}.to_a"
      end

      def deprecation_alternative
        "Post.where(...).all"
      end
    end

    class FindLastBy < Method
      Method.matchers << self
      include Finder
      include DeprecatedFinder
      include DeprecationWarning

      def self.prefix
        "find_last_by"
      end

      def finder
        "where"
      end

      def result
        "#{super}.last"
      end

      def deprecation_alternative
        "Post.where(...).last"
      end
    end

    class ScopedBy < Method
      Method.matchers << self
      include Finder
      include DeprecationWarning

      def self.prefix
        "scoped_by"
      end

      def body
        "#{deprecation_warning} \n where(#{attributes_hash})"
      end

      def deprecation_alternative
        "Post.where(...)"
      end
    end

    class Instantiator < Method
      include DeprecationWarning

      def self.dispatch(klass, attribute_names, instantiator, args, block)
        if args.length == 1 && args.first.is_a?(Hash)
          attributes = args.first.stringify_keys
          conditions = attributes.slice(*attribute_names)
          rest       = [attributes.except(*attribute_names)]
        else
          raise ArgumentError, "too few arguments" unless args.length >= attribute_names.length

          conditions = Hash[attribute_names.map.with_index { |n, i| [n, args[i]] }]
          rest       = args.drop(attribute_names.length)
        end

        klass.where(conditions).first ||
          klass.create_with(conditions).send(instantiator, *rest, &block)
      end

      def signature
        "*args, &block"
      end

      def body
        <<-CODE
          #{deprecation_warning}
          #{self.class}.dispatch(self, #{attribute_names.inspect}, #{instantiator.inspect}, args, block)
        CODE
      end

      def instantiator
        raise NotImplementedError
      end

      def deprecation_alternative
        "Post.#{self.class.prefix}#{self.class.suffix}(name: 'foo')"
      end
    end

    class FindOrInitializeBy < Instantiator
      Method.matchers << self

      def self.prefix
        "find_or_initialize_by"
      end

      def instantiator
        "new"
      end
    end

    class FindOrCreateBy < Instantiator
      Method.matchers << self

      def self.prefix
        "find_or_create_by"
      end

      def instantiator
        "create"
      end
    end

    class FindOrCreateByBang < Instantiator
      Method.matchers << self

      def self.prefix
        "find_or_create_by"
      end

      def self.suffix
        "!"
      end

      def instantiator
        "create!"
      end
    end
  end
end
