module Spec
  module Mocks
    module ExampleMethods
      include Spec::Mocks::ArgumentMatchers

      # Shortcut for creating an instance of Spec::Mocks::Mock.
      #
      # +name+ is used for failure reporting, so you should use the
      # role that the mock is playing in the example.
      #
      # +stubs_and_options+ lets you assign options and stub values
      # at the same time. The only option available is :null_object.
      # Anything else is treated as a stub value.
      #
      # == Examples
      #
      #   stub_thing = mock("thing", :a => "A")
      #   stub_thing.a == "A" => true
      #
      #   stub_person = stub("thing", :name => "Joe", :email => "joe@domain.com")
      #   stub_person.name => "Joe"
      #   stub_person.email => "joe@domain.com"
      def mock(*args)
        Spec::Mocks::Mock.new(*args)
      end

      alias :stub :mock

      # DEPRECATED - use mock('name').as_null_object instead
      #
      # Shortcut for creating a mock object that will return itself in response
      # to any message it receives that it hasn't been explicitly instructed
      # to respond to.
      def stub_everything(name = 'stub')
        Spec.warn(<<-WARNING)

DEPRECATION: stub_everything('#{name}') is deprecated and will be removed
from a future version of rspec. Please use mock('#{name}').as_null_object
or stub('#{name}').as_null_object instead.

WARNING
        mock(name, :null_object => true)
      end

      # Disables warning messages about expectations being set on nil.
      #
      # By default warning messages are issued when expectations are set on nil.  This is to
      # prevent false-positives and to catch potential bugs early on.
      def allow_message_expectations_on_nil
        Proxy.allow_message_expectations_on_nil
      end

    end
  end
end
