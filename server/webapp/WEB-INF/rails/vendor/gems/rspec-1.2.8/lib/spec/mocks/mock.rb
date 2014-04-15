module Spec
  module Mocks
    class Mock
      include Methods

      # Creates a new mock with a +name+ (that will be used in error messages
      # only) == Options:
      # * <tt>:null_object</tt> - if true, the mock object acts as a forgiving
      #   null object allowing any message to be sent to it.
      def initialize(name='mock', stubs_and_options={})
        if name.is_a?(Hash) && stubs_and_options.empty?
          stubs_and_options = name
          build_name_from_options stubs_and_options
        else
          @name = name
        end
        @options = parse_options(stubs_and_options)
        assign_stubs(stubs_and_options)
      end

      # This allows for comparing the mock to other objects that proxy such as
      # ActiveRecords belongs_to proxy objects. By making the other object run
      # the comparison, we're sure the call gets delegated to the proxy
      # target.
      def ==(other)
        other == __mock_proxy
      end

      def inspect
        "#<#{self.class}:#{sprintf '0x%x', self.object_id} @name=#{@name.inspect}>"
      end

      def to_s
        inspect.gsub('<','[').gsub('>',']')
      end

    private

      def method_missing(sym, *args, &block)
        __mock_proxy.record_message_received(sym, args, block)
        begin
          return self if __mock_proxy.null_object?
          super(sym, *args, &block)
        rescue NameError
          __mock_proxy.raise_unexpected_message_error sym, *args
        end
      end

      def parse_options(options)
        options.has_key?(:null_object) ? {:null_object => options.delete(:null_object)} : {}
      end

      def assign_stubs(stubs)
        stubs.each_pair do |message, response|
          stub!(message).and_return(response)
        end
      end

      def build_name_from_options(options)
        vals = options.inject([]) {|coll, pair| coll << "#{pair.first}: #{pair.last.inspect}"}
        @name = '{' + vals.join(', ') + '}'
      end
    end
  end
end
