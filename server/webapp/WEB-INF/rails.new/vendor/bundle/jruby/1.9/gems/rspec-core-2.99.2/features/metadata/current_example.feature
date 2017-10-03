Feature: current example

  You can reference the example object, and access its metadata, using
  a block argument to it, before and after hooks, let, and subject.

  Scenario: access the example object from within an example
    Given a file named "spec/example_spec.rb" with:
      """ruby
      describe "example as block arg to it, before, and after" do
        before do |example|
          expect(example.description).to eq("is the example object")
        end

        after do |example|
          expect(example.description).to eq("is the example object")
        end

        it "is the example object" do |example|
          expect(example.description).to eq("is the example object")
        end
      end

      describe "example as block arg to let" do
        let(:the_description) do |example|
          example.description
        end

        it "is the example object" do |example|
          expect(the_description).to eq("is the example object")
        end
      end

      describe "example as block arg to subject" do
        subject do |example|
          example.description
        end

        it "is the example object" do |example|
          expect(subject).to eq("is the example object")
        end
      end

      describe "example as block arg to subject with a name" do
        subject(:the_subject) do |example|
          example.description
        end

        it "is the example object" do |example|
          expect(the_subject).to eq("is the example object")
          expect(subject).to eq("is the example object")
        end
      end
      """
    When I run `rspec spec/example_spec.rb`
    Then the example should pass

