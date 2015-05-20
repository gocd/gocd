require 'spec_helper'
require 'ostruct'

describe "exist matcher" do
  it_behaves_like "an RSpec matcher", :valid_value => OpenStruct.new(:exist? => true),
                                      :invalid_value => OpenStruct.new(:exist? => false) do
    let(:matcher) { exist }
  end

  context "when the object does not respond to #exist? or #exists?" do
    subject { double }

    [:to, :not_to].each do |expect_method|
      describe "expect(...).#{expect_method} exist" do
        it "raises an error" do
          expect {
            expect(subject).send(expect_method, exist)
          }.to raise_error(NoMethodError)
        end
      end
    end
  end

  [:exist?, :exists?].each do |predicate|
    context "when the object responds to ##{predicate}" do
      describe "expect(...).to exist" do
        it "passes if #{predicate}" do
          expect(double(predicate => true)).to exist
        end

        it "fails if not #{predicate}" do
          expect {
            expect(double(predicate => false)).to exist
          }.to fail_with(/expected .* to exist/)
        end

        it 'works when the object overrides `send`' do
          klass = Struct.new(:message) do
            def send
              :message_sent
            end

            define_method predicate do
              true
            end
          end

          expect(klass.new("msg")).to exist
        end
      end

      describe "expect(...).not_to exist" do
        it "passes if not #{predicate}" do
          expect(double(predicate => false)).not_to exist
        end

        it "fails if #{predicate}" do
          expect {
            expect(double(predicate => true)).not_to exist
          }.to fail_with(/expected .* not to exist/)
        end
      end
    end
  end

  context "when the object responds to #exist? and #exists?" do
    context "when they both return falsey values" do
      subject { double(:exist? => false, :exists? => nil) }

      describe "expect(...).not_to exist" do
        it "passes" do
          expect(subject).not_to exist
        end
      end

      describe "expect(...).to exist" do
        it "fails" do
          expect {
            expect(subject).to exist
          }.to fail_with(/expected .* to exist/)
        end
      end
    end

    context "when they both return truthy values" do
      subject { double(:exist? => true, :exists? => "something true") }

      describe "expect(...).not_to exist" do
        it "fails" do
          expect {
            expect(subject).not_to exist
          }.to fail_with(/expected .* not to exist/)
        end
      end

      describe "expect(...).to exist" do
        it "passes" do
          expect(subject).to exist
        end
      end
    end

    context "when they return values with different truthiness" do
      subject { double(:exist? => true, :exists? => false) }

      [:to, :not_to].each do |expect_method|
        describe "expect(...).#{expect_method} exist" do
          it "raises an error" do
            expect {
              expect(subject).send(expect_method, exist)
            }.to raise_error(/#exist\? and #exists\? returned different values/)
          end
        end
      end
    end
  end

  it 'passes any provided arguments to the call to #exist?' do
    object = double
    object.should_receive(:exist?).with(:foo, :bar) { true }

    expect(object).to exist(:foo, :bar)
  end
end
