require 'active_support'
require 'active_support/deprecation'
require 'active_support/core_ext'
require 'active_model'

module RSpec
  module Rails

    class IllegalDataAccessException < StandardError; end

    module Mocks

      module ActiveModelInstanceMethods
        # Stubs `persisted?` to return false and `id` to return nil
        # @return self
        def as_new_record
          RSpec::Mocks.allow_message(self, :persisted?).and_return(false)
          RSpec::Mocks.allow_message(self, :id).and_return(nil)
          self
        end

        # Returns true by default. Override with a stub.
        def persisted?
          true
        end

        # Returns false for names matching <tt>/_before_type_cast$/</tt>,
        # otherwise delegates to super.
        def respond_to?(message, include_private=false)
          message.to_s =~ /_before_type_cast$/ ? false : super
        end
      end

      # Starting with Rails 4.1, ActiveRecord associations are inversible
      # by default. This class represents an association from the mocked
      # model's perspective.
      #
      # @private
      class Association
        attr_accessor :target, :inversed

        def initialize(association_name)
          @association_name = association_name
        end
      end

      module ActiveRecordInstanceMethods
        # Stubs `persisted?` to return `false` and `id` to return `nil`.
        def destroy
          RSpec::Mocks.allow_message(self, :persisted?).and_return(false)
          RSpec::Mocks.allow_message(self, :id).and_return(nil)
        end

        # Transforms the key to a method and calls it.
        def [](key)
          send(key)
        end

        # Returns the opposite of `persisted?`
        def new_record?
          !persisted?
        end

        # Returns an object representing an association from the mocked
        # model's perspective. For use by Rails internally only.
        def association(association_name)
          @associations ||= Hash.new { |h, k| h[k] = Association.new(k) }
          @associations[association_name]
        end
      end

      # Creates a test double representing `string_or_model_class` with common
      # ActiveModel methods stubbed out. Additional methods may be easily
      # stubbed (via add_stubs) if `stubs` is passed. This is most useful for
      # impersonating models that don't exist yet.
      #
      # NOTE that only ActiveModel's methods, plus <tt>new_record?</tt>, are
      # stubbed out implicitly.  <tt>new_record?</tt> returns the inverse of
      # <tt>persisted?</tt>, and is present only for compatibility with
      # extension frameworks that have yet to update themselves to the
      # ActiveModel API (which declares <tt>persisted?</tt>, not
      # <tt>new_record?</tt>).
      #
      # `string_or_model_class` can be any of:
      #
      #   * A String representing a Class that does not exist
      #   * A String representing a Class that extends ActiveModel::Naming
      #   * A Class that extends ActiveModel::Naming
      def mock_model(string_or_model_class, stubs = {})
        if String === string_or_model_class
          if Object.const_defined?(string_or_model_class)
            model_class = Object.const_get(string_or_model_class)
          else
            model_class = Object.const_set(string_or_model_class, Class.new do
              extend ActiveModel::Naming
              def self.primary_key; :id; end
            end)
          end
        else
          model_class = string_or_model_class
        end

        unless model_class.kind_of? ActiveModel::Naming
          raise ArgumentError.new <<-EOM
The mock_model method can only accept as its first argument:
  * A String representing a Class that does not exist
  * A String representing a Class that extends ActiveModel::Naming
  * A Class that extends ActiveModel::Naming

It received #{model_class.inspect}
EOM
        end

        stubs = stubs.reverse_merge(:id => next_id)
        stubs = stubs.reverse_merge(:persisted? => !!stubs[:id],
                                    :destroyed? => false,
                                    :marked_for_destruction? => false,
                                    :valid? => true,
                                    :blank? => false)

        double("#{model_class.name}_#{stubs[:id]}", stubs).tap do |m|
          m.singleton_class.class_eval do
            include ActiveModelInstanceMethods
            include ActiveRecordInstanceMethods if defined?(ActiveRecord)
            include ActiveModel::Conversion
            include ActiveModel::Validations
          end
          if defined?(ActiveRecord)
            [:save, :update_attributes, :update].each do |key|
              if stubs[key] == false
                RSpec::Mocks.allow_message(m.errors, :empty?).and_return(false)
              end
            end
          end
          m.__send__(:__mock_proxy).instance_eval(<<-CODE, __FILE__, __LINE__)
            def @object.is_a?(other)
              #{model_class}.ancestors.include?(other)
            end unless #{stubs.has_key?(:is_a?)}

            def @object.kind_of?(other)
              #{model_class}.ancestors.include?(other)
            end unless #{stubs.has_key?(:kind_of?)}

            def @object.instance_of?(other)
              other == #{model_class}
            end unless #{stubs.has_key?(:instance_of?)}

            def @object.__model_class_has_column?(method_name)
              #{model_class}.respond_to?(:column_names) && #{model_class}.column_names.include?(method_name.to_s)
            end

            def @object.has_attribute?(attr_name)
              __model_class_has_column?(attr_name)
            end unless #{stubs.has_key?(:has_attribute?)}

            def @object.respond_to?(method_name, include_private=false)
              __model_class_has_column?(method_name) ? true : super
            end unless #{stubs.has_key?(:respond_to?)}

            def @object.method_missing(m, *a, &b)
              respond_to?(m) ? null_object? ? self : nil : super
            end

            def @object.class
              #{model_class}
            end unless #{stubs.has_key?(:class)}

            def @object.to_s
              "#{model_class.name}_#{to_param}"
            end unless #{stubs.has_key?(:to_s)}
          CODE
          yield m if block_given?
        end
      end

      module ActiveModelStubExtensions
        # Stubs `persisted` to return false and `id` to return nil
        def as_new_record
          RSpec::Mocks.allow_message(self, :persisted?).and_return(false)
          RSpec::Mocks.allow_message(self, :id).and_return(nil)
          self
        end

        # Returns `true` by default. Override with a stub.
        def persisted?
          true
        end
      end

      module ActiveRecordStubExtensions
        # Stubs `id` (or other primary key method) to return nil
        def as_new_record
          self.__send__("#{self.class.primary_key}=", nil)
          super
        end

        # Returns the opposite of `persisted?`.
        def new_record?
          !persisted?
        end

        # Raises an IllegalDataAccessException (stubbed models are not allowed to access the database)
        # @raises IllegalDataAccessException
        def connection
          raise RSpec::Rails::IllegalDataAccessException.new("stubbed models are not allowed to access the database")
        end
      end

      # Creates an instance of `Model` with `to_param` stubbed using a
      # generated value that is unique to each object. If `Model` is an
      # `ActiveRecord` model, it is prohibited from accessing the database*.
      #
      # For each key in `stubs`, if the model has a matching attribute
      # (determined by `respond_to?`) it is simply assigned the submitted values.
      # If the model does not have a matching attribute, the key/value pair is
      # assigned as a stub return value using RSpec's mocking/stubbing
      # framework.
      #
      # <tt>persisted?</tt> is overridden to return the result of !id.nil?
      # This means that by default persisted? will return true. If  you want
      # the object to behave as a new record, sending it `as_new_record` will
      # set the id to nil. You can also explicitly set :id => nil, in which
      # case persisted? will return false, but using `as_new_record` makes the
      # example a bit more descriptive.
      #
      # While you can use stub_model in any example (model, view, controller,
      # helper), it is especially useful in view examples, which are
      # inherently more state-based than interaction-based.
      #
      # @example
      #
      #     stub_model(Person)
      #     stub_model(Person).as_new_record
      #     stub_model(Person, :to_param => 37)
      #     stub_model(Person) {|person| person.first_name = "David"}
      def stub_model(model_class, stubs={})
        model_class.new.tap do |m|
          m.extend ActiveModelStubExtensions
          if defined?(ActiveRecord) && model_class < ActiveRecord::Base
            m.extend ActiveRecordStubExtensions
            primary_key = model_class.primary_key.to_sym
            stubs = stubs.reverse_merge(primary_key => next_id)
            stubs = stubs.reverse_merge(:persisted? => !!stubs[primary_key])
          else
            stubs = stubs.reverse_merge(:id => next_id)
            stubs = stubs.reverse_merge(:persisted? => !!stubs[:id])
          end
          stubs = stubs.reverse_merge(:blank? => false)

          stubs.each do |message, return_value|
            if m.respond_to?("#{message}=")
              m.__send__("#{message}=", return_value)
            else
              RSpec::Mocks.allow_message(m, message).and_return(return_value)
            end
          end

          yield m if block_given?
        end
      end

    private

      @@model_id = 1000

      def next_id
        @@model_id += 1
      end

    end
  end
end

RSpec.configuration.include RSpec::Rails::Mocks
