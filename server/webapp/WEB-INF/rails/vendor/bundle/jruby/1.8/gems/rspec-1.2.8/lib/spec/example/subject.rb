module Spec
  module Example
    module Subject
      module ExampleGroupMethods
        # Defines an explicit subject for an example group which can then be the
        # implicit receiver (through delegation) of calls to +should+.
        #
        # == Examples
        #
        #   describe CheckingAccount, "with $50" do
        #     subject { CheckingAccount.new(:amount => 50, :currency => :USD) }
        #     it { should have_a_balance_of(50, :USD) }
        #     it { should_not be_overdrawn }
        #   end
        #
        # See +ExampleMethods#should+ for more information about this approach.
        def subject(&block)
          block.nil? ?
            explicit_subject || implicit_subject : @explicit_subject_block = block
        end

        attr_reader :explicit_subject_block # :nodoc:

      private

        def explicit_subject
          group = self
          while group.respond_to?(:explicit_subject_block)
            return group.explicit_subject_block if group.explicit_subject_block
            group = group.superclass
          end
        end

        def implicit_subject
          (described_class ? lambda {described_class.new} : lambda {description_args.first})
        end
      end

      module ExampleMethods

        alias_method :__should_for_example_group__,     :should
        alias_method :__should_not_for_example_group__, :should_not

        # Returns the subject defined in ExampleGroupMethods#subject. The
        # subject block is only executed once per example, the result of which
        # is cached and returned by any subsequent calls to +subject+.
        #
        # If a class is passed to +describe+ and no subject is explicitly
        # declared in the example group, then +subject+ will return a new
        # instance of that class.
        #
        # == Examples
        #
        #   # explicit subject defined by the subject method
        #   describe Person do
        #     subject { Person.new(:birthdate => 19.years.ago) }
        #     it "should be eligible to vote" do
        #       subject.should be_eligible_to_vote
        #     end
        #   end
        #
        #   # implicit subject => { Person.new }
        #   describe Person do
        #     it "should be eligible to vote" do
        #       subject.should be_eligible_to_vote
        #     end
        #   end
        def subject
          @subject ||= instance_eval(&self.class.subject)
        end

        # When +should+ is called with no explicit receiver, the call is
        # delegated to the object returned by +subject+. Combined with
        # an implicit subject (see +subject+), this supports very concise
        # expressions.
        #
        # == Examples
        #
        #   describe Person do
        #     it { should be_eligible_to_vote }
        #   end
        def should(matcher=nil, message=nil)
          self == subject ? self.__should_for_example_group__(matcher) : subject.should(matcher,message)
        end

        # Just like +should+, +should_not+ delegates to the subject (implicit or
        # explicit) of the example group.
        #
        # == Examples
        #
        #   describe Person do
        #     it { should_not be_eligible_to_vote }
        #   end
        def should_not(matcher=nil, message=nil)
          self == subject ? self.__should_not_for_example_group__(matcher) : subject.should_not(matcher,message)
        end
      end
    end
  end
end
