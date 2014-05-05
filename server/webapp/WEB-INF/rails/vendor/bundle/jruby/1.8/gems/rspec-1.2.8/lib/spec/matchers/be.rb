module Spec
  module Matchers
    
    class Be #:nodoc:
      include Spec::Matchers::Pretty
      
      def initialize(*args)
        @expected = args.empty? ? true : set_expected(args.shift)
        @args = args
        @comparison_method = nil
      end
      
      def matches?(actual)
        @actual = actual
        handling_predicate? ? run_predicate_on(actual) : match_or_compare(actual)
      end
      
      def run_predicate_on(actual)
        begin
          return @result = actual.__send__(predicate, *@args)
        rescue NameError => predicate_missing_error
          "this needs to be here or rcov will not count this branch even though it's executed in a code example"
        end

        begin
          return @result = actual.__send__(present_tense_predicate, *@args)
        rescue NameError
          raise predicate_missing_error
        end
      end
      
      def failure_message_for_should
        if handling_predicate?
          if predicate == :nil?
            "expected nil, got #{@actual.inspect}"
          else
            "expected #{predicate}#{args_to_s} to return true, got #{@result.inspect}"
          end
        else
          "expected #{@comparison_method} #{expected}, got #{@actual.inspect}".gsub('  ',' ')
        end
      end
      
      def failure_message_for_should_not
        if handling_predicate?
          if predicate == :nil?
            "expected not nil, got nil"
          else
          "expected #{predicate}#{args_to_s} to return false, got #{@result.inspect}"
          end
        else
          message = <<-MESSAGE
'should_not be #{@comparison_method} #{expected}' not only FAILED,
it is a bit confusing.
          MESSAGE
          
          raise message << ([:===,:==].include?(@comparison_method) ?
            "It might be more clearly expressed without the \"be\"?" :
            "It might be more clearly expressed in the positive?")
        end
      end
      
      def description
        "#{prefix_to_sentence}#{comparison} #{expected_to_sentence}#{args_to_sentence}".gsub(/\s+/,' ')
      end

      [:==, :<, :<=, :>=, :>, :===].each do |method|
        define_method method do |expected|
          compare_to(expected, :using => method)
          self
        end
      end

      private
        def match_or_compare(actual)
          TrueClass === @expected ? @actual : @actual.__send__(comparison_method, @expected)
        end
      
        def comparison_method
          @comparison_method || :equal?
        end
      
        def expected
          @expected
        end

        def compare_to(expected, opts)
          @expected, @comparison_method = expected, opts[:using]
        end

        def set_expected(expected)
          Symbol === expected ? parse_expected(expected) : expected
        end
        
        def parse_expected(expected)
          ["be_an_","be_a_","be_"].each do |prefix|
            handling_predicate!
            if expected.to_s =~ /^#{prefix}/
              set_prefix(prefix)
              expected = expected.to_s.sub(prefix,"")
              [true, false, nil].each do |val|
                return val if val.to_s == expected
              end
              return expected.to_sym
            end
          end
        end
        
        def set_prefix(prefix)
          @prefix = prefix
        end
        
        def prefix
          # FIXME - this is a bit goofy - but we get failures
          # if just defining @prefix = nil in initialize
          @prefix = nil unless defined?(@prefix)
          @prefix
        end

        def handling_predicate!
          @handling_predicate = true
        end
        
        def handling_predicate?
          return false if [true, false, nil].include?(expected)
          # FIXME - this is a bit goofy - but we get failures
          # if just defining @handling_predicate = nil or false in initialize
          return defined?(@handling_predicate) ? @handling_predicate : nil
        end

        def predicate
          "#{@expected.to_s}?".to_sym
        end
        
        def present_tense_predicate
          "#{@expected.to_s}s?".to_sym
        end
        
        def args_to_s
          @args.empty? ? "" : parenthesize(inspected_args.join(', '))
        end
        
        def parenthesize(string)
          return "(#{string})"
        end
        
        def inspected_args
          @args.collect{|a| a.inspect}
        end
        
        def comparison
          @comparison_method.nil? ? " " : "be #{@comparison_method.to_s} "
        end
        
        def expected_to_sentence
          split_words(expected)
        end
        
        def prefix_to_sentence
          split_words(prefix)
        end

        def args_to_sentence
          to_sentence(@args)
        end
        
    end
 
    # :call-seq:
    #   should be_true
    #   should be_false
    #   should be_nil
    #   should be_[arbitrary_predicate](*args)
    #   should_not be_nil
    #   should_not be_[arbitrary_predicate](*args)
    #
    # Given true, false, or nil, will pass if actual value is
    # true, false or nil (respectively). Given no args means
    # the caller should satisfy an if condition (to be or not to be). 
    #
    # Predicates are any Ruby method that ends in a "?" and returns true or false.
    # Given be_ followed by arbitrary_predicate (without the "?"), RSpec will match
    # convert that into a query against the target object.
    #
    # The arbitrary_predicate feature will handle any predicate
    # prefixed with "be_an_" (e.g. be_an_instance_of), "be_a_" (e.g. be_a_kind_of)
    # or "be_" (e.g. be_empty), letting you choose the prefix that best suits the predicate.
    #
    # == Examples 
    #
    #   target.should be_true
    #   target.should be_false
    #   target.should be_nil
    #   target.should_not be_nil
    #
    #   collection.should be_empty #passes if target.empty?
    #   target.should_not be_empty #passes unless target.empty?
    #   target.should_not be_old_enough(16) #passes unless target.old_enough?(16)
    def be(*args)
      Matchers::Be.new(*args)
    end

    # passes if target.kind_of?(klass)
    def be_a(klass)
      be_a_kind_of(klass)
    end
    
    alias_method :be_an, :be_a
  end
end
