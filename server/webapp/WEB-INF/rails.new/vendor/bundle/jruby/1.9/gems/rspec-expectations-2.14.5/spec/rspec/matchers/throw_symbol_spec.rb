require 'spec_helper'

module RSpec::Matchers::BuiltIn
  describe ThrowSymbol do
    it_behaves_like("an RSpec matcher", :valid_value => lambda { throw :foo },
                                        :invalid_value => lambda { }) do
      let(:matcher) { throw_symbol(:foo) }
    end

    describe "with no args" do
      before(:each) { @matcher = throw_symbol }

      it "matches if any Symbol is thrown" do
        expect(@matcher.matches?(lambda{ throw :sym })).to be_true
      end
      it "matches if any Symbol is thrown with an arg" do
        expect(@matcher.matches?(lambda{ throw :sym, "argument" })).to be_true
      end
      it "does not match if no Symbol is thrown" do
        expect(@matcher.matches?(lambda{ })).to be_false
      end
      it "provides a failure message" do
        @matcher.matches?(lambda{})
        expect(@matcher.failure_message_for_should).to eq "expected a Symbol to be thrown, got nothing"
      end
      it "provides a negative failure message" do
        @matcher.matches?(lambda{ throw :sym})
        expect(@matcher.failure_message_for_should_not).to eq "expected no Symbol to be thrown, got :sym"
      end
    end

    describe "with a symbol" do
      before(:each) { @matcher = throw_symbol(:sym) }

      it "matches if correct Symbol is thrown" do
        expect(@matcher.matches?(lambda{ throw :sym })).to be_true
      end
      it "matches if correct Symbol is thrown with an arg" do
        expect(@matcher.matches?(lambda{ throw :sym, "argument" })).to be_true
      end
      it "does not match if no Symbol is thrown" do
        expect(@matcher.matches?(lambda{ })).to be_false
      end
      it "does not match if correct Symbol is thrown" do
        expect(@matcher.matches?(lambda{ throw :other_sym })).to be_false
      end
      it "provides a failure message when no Symbol is thrown" do
        @matcher.matches?(lambda{})
        expect(@matcher.failure_message_for_should).to eq "expected :sym to be thrown, got nothing"
      end
      it "provides a failure message when wrong Symbol is thrown" do
        @matcher.matches?(lambda{ throw :other_sym })
        expect(@matcher.failure_message_for_should).to eq "expected :sym to be thrown, got :other_sym"
      end
      it "provides a negative failure message" do
        @matcher.matches?(lambda{ throw :sym })
        expect(@matcher.failure_message_for_should_not).to eq "expected :sym not to be thrown, got :sym"
      end
      it "only matches NameErrors raised by uncaught throws" do
        expect {
          expect(@matcher.matches?(lambda{ sym })).to be_false
        }.to raise_error(NameError)
      end
    end

    describe "with a symbol and an arg" do
      before(:each) { @matcher = throw_symbol(:sym, "a") }

      it "matches if correct Symbol and args are thrown" do
        expect(@matcher.matches?(lambda{ throw :sym, "a" })).to be_true
      end
      it "does not match if nothing is thrown" do
        expect(@matcher.matches?(lambda{ })).to be_false
      end
      it "does not match if other Symbol is thrown" do
        expect(@matcher.matches?(lambda{ throw :other_sym, "a" })).to be_false
      end
      it "does not match if no arg is thrown" do
        expect(@matcher.matches?(lambda{ throw :sym })).to be_false
      end
      it "does not match if wrong arg is thrown" do
        expect(@matcher.matches?(lambda{ throw :sym, "b" })).to be_false
      end
      it "provides a failure message when no Symbol is thrown" do
        @matcher.matches?(lambda{})
        expect(@matcher.failure_message_for_should).to eq %q[expected :sym with "a" to be thrown, got nothing]
      end
      it "provides a failure message when wrong Symbol is thrown" do
        @matcher.matches?(lambda{ throw :other_sym })
        expect(@matcher.failure_message_for_should).to eq %q[expected :sym with "a" to be thrown, got :other_sym]
      end
      it "provides a failure message when wrong arg is thrown" do
        @matcher.matches?(lambda{ throw :sym, "b" })
        expect(@matcher.failure_message_for_should).to eq %q[expected :sym with "a" to be thrown, got :sym with "b"]
      end
      it "provides a failure message when no arg is thrown" do
        @matcher.matches?(lambda{ throw :sym })
        expect(@matcher.failure_message_for_should).to eq %q[expected :sym with "a" to be thrown, got :sym with no argument]
      end
      it "provides a negative failure message" do
        @matcher.matches?(lambda{ throw :sym })
        expect(@matcher.failure_message_for_should_not).to eq %q[expected :sym with "a" not to be thrown, got :sym with no argument]
      end
      it "only matches NameErrors raised by uncaught throws" do
        expect {
          expect(@matcher.matches?(lambda{ sym })).to be_false
        }.to raise_error(NameError)
      end
      it "raises other errors" do
        expect {
          @matcher.matches?(lambda { raise "Boom" })
        }.to raise_error(/Boom/)
      end
    end
  end
end
