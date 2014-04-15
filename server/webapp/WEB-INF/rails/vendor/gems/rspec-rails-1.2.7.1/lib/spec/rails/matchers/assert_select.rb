# This is a wrapper of assert_select for rspec.

module Spec # :nodoc:
  module Rails
    module Matchers

      class AssertSelect #:nodoc:
        attr_reader :options

        def initialize(selector_assertion, spec_scope, *args, &block)
          @args, @options = args_and_options(args)
          @spec_scope = spec_scope
          @selector_assertion = selector_assertion
          @block = block
        end
        
        def matches?(response_or_text, &block)
          @block = block if block

          if doc = doc_from(response_or_text)
            @args.unshift(doc)
          end

          begin
            @spec_scope.__send__(@selector_assertion, *@args, &@block)
            true
          rescue ::Test::Unit::AssertionFailedError => @error
            false
          end
        end
        
        def failure_message_for_should; @error.message; end
        def failure_message_for_should_not; "should not #{description}, but did"; end

        def description
          {
            :assert_select => "have tag#{format_args(*@args)}",
            :assert_select_email => "send email#{format_args(*@args)}",
          }[@selector_assertion]
        end

      private

        module TestResponseOrString
          def test_response?
            ActionController::TestResponse === self and
                                               !self.headers['Content-Type'].blank? and
                                               self.headers['Content-Type'].to_sym == :xml
          end
        
          def string?
            String === self
          end
        end

        def doc_from(response_or_text)
          response_or_text.extend TestResponseOrString
          if response_or_text.test_response?
            HTML::Document.new(response_or_text.body, @options[:strict], @options[:xml]).root
          elsif response_or_text.string?
            HTML::Document.new(response_or_text, @options[:strict], @options[:xml]).root
           end
        end

        def format_args(*args)
          args.empty? ? "" : "(#{arg_list(*args)})"
        end

        def arg_list(*args)
          args.map do |arg|
            arg.respond_to?(:description) ? arg.description : arg.inspect
          end.join(", ")
        end
        
        def args_and_options(args)
          opts = {:xml => false, :strict => false}
          if args.last.is_a?(::Hash)
            opts[:strict] = args.last.delete(:strict) unless args.last[:strict].nil?
            opts[:xml]    = args.last.delete(:xml)    unless args.last[:xml].nil?
            args.pop if args.last.empty?
          end
          return [args, opts]
        end
        
      end
      
      # :call-seq:
      #   response.should have_tag(*args, &block)
      #   string.should have_tag(*args, &block)
      #
      # wrapper for assert_select with additional support for using
      # css selectors to set expectation on Strings. Use this in
      # helper specs, for example, to set expectations on the results
      # of helper methods. Also allow specification of how the 
      # response is parsed using the options :xml and :strict options.
      # By default, these options are set to false.
      #
      # == Examples
      #
      #   # in a controller spec
      #   response.should have_tag("div", "some text")
      #
      #   # to force xml and/or strict parsing of the response
      #   response.should have_tag("div", "some text", :xml => true)
      #   response.should have_tag("div", "some text", :strict => true)
      #   response.should have_tag("div", "some text", :xml => true, :strict => false)
      #
      #   # in a helper spec (person_address_tag is a method in the helper)
      #   person_address_tag.should have_tag("input#person_address")
      #
      # see documentation for assert_select at http://api.rubyonrails.org/
      def have_tag(*args, &block)
        @__current_scope_for_assert_select = AssertSelect.new(:assert_select, self, *args, &block)
      end
    
      # wrapper for a nested assert_select
      #
      #   response.should have_tag("div#form") do
      #     with_tag("input#person_name[name=?]", "person[name]")
      #   end
      #
      # see documentation for assert_select at http://api.rubyonrails.org/
      def with_tag(*args, &block)
        args = prepare_args(args, @__current_scope_for_assert_select)
        @__current_scope_for_assert_select.should have_tag(*args, &block)
      end
    
      # wrapper for a nested assert_select with false
      #
      #   response.should have_tag("div#1") do
      #     without_tag("span", "some text that shouldn't be there")
      #   end
      #
      # see documentation for assert_select at http://api.rubyonrails.org/
      def without_tag(*args, &block)
        args = prepare_args(args, @__current_scope_for_assert_select)
        @__current_scope_for_assert_select.should_not have_tag(*args, &block)
      end
    
      # :call-seq:
      #   response.should have_rjs(*args, &block)
      #
      # wrapper for assert_select_rjs
      #
      # see documentation for assert_select_rjs at http://api.rubyonrails.org/
      def have_rjs(*args, &block)
        AssertSelect.new(:assert_select_rjs, self, *args, &block)
      end
      
      # :call-seq:
      #   response.should send_email(*args, &block)
      #
      # wrapper for assert_select_email
      #
      # see documentation for assert_select_email at http://api.rubyonrails.org/
      def send_email(*args, &block)
        AssertSelect.new(:assert_select_email, self, *args, &block)
      end
      
      # wrapper for assert_select_encoded
      #
      # see documentation for assert_select_encoded at http://api.rubyonrails.org/
      def with_encoded(*args, &block)
        should AssertSelect.new(:assert_select_encoded, self, *args, &block)
      end

    private
    
      def prepare_args(args, current_scope = nil)
        return args if current_scope.nil?
        defaults = current_scope.options || {:strict => false, :xml => false}
        args << {} unless args.last.is_a?(::Hash)
        args.last[:strict] = defaults[:strict] if args.last[:strict].nil?
        args.last[:xml] = defaults[:xml] if args.last[:xml].nil?
        args
      end

    end
  end
end
