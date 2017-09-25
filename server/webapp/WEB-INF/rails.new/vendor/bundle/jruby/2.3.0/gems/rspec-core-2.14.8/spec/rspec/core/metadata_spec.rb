require 'spec_helper'

module RSpec
  module Core
    describe Metadata do

      describe '.relative_path' do
        let(:here) { File.expand_path(".") }
        it "transforms absolute paths to relative paths" do
          expect(Metadata.relative_path(here)).to eq "."
        end
        it "transforms absolute paths to relative paths anywhere in its argument" do
          expect(Metadata.relative_path("foo #{here} bar")).to eq "foo . bar"
        end
        it "returns nil if passed an unparseable file:line combo" do
          expect(Metadata.relative_path("-e:1")).to be_nil
        end
        # I have no idea what line = line.sub(/\A([^:]+:\d+)$/, '\\1') is supposed to do
        it "gracefully returns nil if run in a secure thread" do
          safely do
            value = Metadata.relative_path(".")
            # on some rubies, File.expand_path is not a security error, so accept "." as well
            expect([nil, "."]).to include(value)
          end
        end

      end

      describe "#process" do
        Metadata::RESERVED_KEYS.each do |key|
          it "prohibits :#{key} as a hash key" do
            m = Metadata.new
            expect do
              m.process('group', key => {})
            end.to raise_error(/:#{key} is not allowed/)
          end
        end

        it "uses :caller if passed as part of the user metadata" do
          m = Metadata.new
          m.process('group', :caller => ['example_file:42'])
          expect(m[:example_group][:location]).to eq("example_file:42")
        end
      end

      describe "#filter_applies?" do
        let(:parent_group_metadata) { Metadata.new.process('parent group', :caller => ["foo_spec.rb:#{__LINE__}"]) }
        let(:group_metadata) { Metadata.new(parent_group_metadata).process('group', :caller => ["foo_spec.rb:#{__LINE__}"]) }
        let(:example_metadata) { group_metadata.for_example('example', :caller => ["foo_spec.rb:#{__LINE__}"], :if => true) }
        let(:next_example_metadata) { group_metadata.for_example('next_example', :caller => ["foo_spec.rb:#{example_line_number + 2}"]) }
        let(:world) { World.new }

        before { RSpec.stub(:world) { world } }

        shared_examples_for "matching by line number" do
          let(:preceeding_declaration_lines) {{
            parent_group_metadata[:example_group][:line_number] => parent_group_metadata[:example_group][:line_number],
            group_metadata[:example_group][:line_number] => group_metadata[:example_group][:line_number],
            example_metadata[:line_number] => example_metadata[:line_number],
            (example_metadata[:line_number] + 1) => example_metadata[:line_number],
            (example_metadata[:line_number] + 2) => example_metadata[:line_number] + 2,
          }}
          before do
            world.should_receive(:preceding_declaration_line).at_least(:once).and_return do |v|
              preceeding_declaration_lines[v]
            end
          end

          it "matches the group when the line_number is the example group line number" do
            # this call doesn't really make sense since filter_applies? is only called
            # for example metadata not group metadata
            expect(group_metadata.filter_applies?(condition_key, group_condition)).to be_true
          end

          it "matches the example when the line_number is the grandparent example group line number" do
            expect(example_metadata.filter_applies?(condition_key, parent_group_condition)).to be_true
          end

          it "matches the example when the line_number is the parent example group line number" do
            expect(example_metadata.filter_applies?(condition_key, group_condition)).to be_true
          end

          it "matches the example when the line_number is the example line number" do
            expect(example_metadata.filter_applies?(condition_key, example_condition)).to be_true
          end

          it "matches when the line number is between this example and the next" do
            expect(example_metadata.filter_applies?(condition_key, between_examples_condition)).to be_true
          end

          it "does not match when the line number matches the next example" do
            expect(example_metadata.filter_applies?(condition_key, next_example_condition)).to be_false
          end
        end

        context "with a single line number" do
          let(:condition_key){ :line_numbers }
          let(:parent_group_condition) { [parent_group_metadata[:example_group][:line_number]] }
          let(:group_condition) { [group_metadata[:example_group][:line_number]] }
          let(:example_condition) { [example_metadata[:line_number]] }
          let(:between_examples_condition) { [group_metadata[:example_group][:line_number] + 1] }
          let(:next_example_condition) { [example_metadata[:line_number] + 2] }

          it_has_behavior "matching by line number"
        end

        context "with multiple line numbers" do
          let(:condition_key){ :line_numbers }
          let(:parent_group_condition) { [-1, parent_group_metadata[:example_group][:line_number]] }
          let(:group_condition) { [-1, group_metadata[:example_group][:line_number]] }
          let(:example_condition) { [-1, example_metadata[:line_number]] }
          let(:between_examples_condition) { [-1, group_metadata[:example_group][:line_number] + 1] }
          let(:next_example_condition) { [-1, example_metadata[:line_number] + 2] }

          it_has_behavior "matching by line number"
        end

        context "with locations" do
          let(:condition_key){ :locations }
          let(:parent_group_condition) do
            {File.expand_path(parent_group_metadata[:example_group][:file_path]) => [parent_group_metadata[:example_group][:line_number]]}
          end
          let(:group_condition) do
            {File.expand_path(group_metadata[:example_group][:file_path]) => [group_metadata[:example_group][:line_number]]}
          end
          let(:example_condition) do
            {File.expand_path(example_metadata[:file_path]) => [example_metadata[:line_number]]}
          end
          let(:between_examples_condition) do
            {File.expand_path(group_metadata[:example_group][:file_path]) => [group_metadata[:example_group][:line_number] + 1]}
          end
          let(:next_example_condition) do
            {File.expand_path(example_metadata[:file_path]) => [example_metadata[:line_number] + 2]}
          end

          it_has_behavior "matching by line number"

          it "ignores location filters for other files" do
            expect(example_metadata.filter_applies?(:locations, {"/path/to/other_spec.rb" => [3,5,7]})).to be_true
          end
        end

        it "matches a proc with no arguments that evaluates to true" do
          expect(example_metadata.filter_applies?(:if, lambda { true })).to be_true
        end

        it "matches a proc that evaluates to true" do
          expect(example_metadata.filter_applies?(:if, lambda { |v| v })).to be_true
        end

        it "does not match a proc that evaluates to false" do
          expect(example_metadata.filter_applies?(:if, lambda { |v| !v })).to be_false
        end

        it "matches a proc with an arity of 2" do
          example_metadata[:foo] = nil
          expect(example_metadata.filter_applies?(:foo, lambda { |v, m| m == example_metadata })).to be_true
        end

        it "raises an error when the proc has an incorrect arity" do
          expect {
            example_metadata.filter_applies?(:if, lambda { |a,b,c| true })
          }.to raise_error(ArgumentError)
        end

        context "with an Array" do
          let(:metadata_with_array) {
            group_metadata.for_example('example_with_array', :tag => [:one, 2, 'three', /four/])
          }

          it "matches a symbol" do
            expect(metadata_with_array.filter_applies?(:tag, 'one')).to be_true
            expect(metadata_with_array.filter_applies?(:tag, :one)).to be_true
            expect(metadata_with_array.filter_applies?(:tag, 'two')).to be_false
          end

          it "matches a string" do
            expect(metadata_with_array.filter_applies?(:tag, 'three')).to be_true
            expect(metadata_with_array.filter_applies?(:tag, :three)).to be_true
            expect(metadata_with_array.filter_applies?(:tag, 'tree')).to be_false
          end

          it "matches an integer" do
            expect(metadata_with_array.filter_applies?(:tag, '2')).to be_true
            expect(metadata_with_array.filter_applies?(:tag, 2)).to be_true
            expect(metadata_with_array.filter_applies?(:tag, 3)).to be_false
          end

          it "matches a regexp" do
            expect(metadata_with_array.filter_applies?(:tag, 'four')).to be_true
            expect(metadata_with_array.filter_applies?(:tag, 'fourtune')).to be_true
            expect(metadata_with_array.filter_applies?(:tag, 'fortune')).to be_false
          end

          it "matches a proc that evaluates to true" do
            expect(metadata_with_array.filter_applies?(:tag, lambda { |values| values.include? 'three' })).to be_true
          end

          it "does not match a proc that evaluates to false" do
            expect(metadata_with_array.filter_applies?(:tag, lambda { |values| values.include? 'nothing' })).to be_false
          end
        end
      end

      describe "#for_example" do
        let(:metadata)           { Metadata.new.process("group description") }
        let(:mfe)                { metadata.for_example("example description", {:arbitrary => :options}) }
        let(:line_number)        { __LINE__ - 1 }

        it "stores the description args" do
          expect(mfe.fetch(:description_args)).to eq ["example description"]
          expect(mfe[:description_args]).to eq ["example description"]
        end

        it "ignores nil description args" do
          expect(metadata.for_example(nil, {}).fetch(:description_args)).to eq []
          expect(metadata.for_example(nil, {})[:description_args]).to eq []
        end

        it "stores the full_description (group description + example description)" do
          expect(mfe.fetch(:full_description)).to eq("group description example description")
          expect(mfe[:full_description]).to eq("group description example description")
        end

        it "creates an empty execution result" do
          expect(mfe.fetch(:execution_result)).to eq({})
          expect(mfe[:execution_result]).to eq({})
        end

        it "extracts file path from caller" do
          expect(mfe.fetch(:file_path)).to eq(relative_path(__FILE__))
          expect(mfe[:file_path]).to eq(relative_path(__FILE__))
        end

        it "extracts line number from caller" do
          expect(mfe.fetch(:line_number)).to eq(line_number)
          expect(mfe[:line_number]).to eq(line_number)
        end

        it "extracts location from caller" do
          expect(mfe.fetch(:location)).to eq("#{relative_path(__FILE__)}:#{line_number}")
          expect(mfe[:location]).to eq("#{relative_path(__FILE__)}:#{line_number}")
        end

        it "uses :caller if passed as an option" do
          example_metadata = metadata.for_example('example description', {:caller => ['example_file:42']})
          expect(example_metadata[:location]).to eq("example_file:42")
        end

        it "merges arbitrary options" do
          expect(mfe.fetch(:arbitrary)).to eq(:options)
          expect(mfe[:arbitrary]).to eq(:options)
        end

        it "points :example_group to the same hash object" do
          a = metadata.for_example("foo", {})[:example_group]
          b = metadata.for_example("bar", {})[:example_group]
          a[:description] = "new description"
          expect(b[:description]).to eq("new description")
        end
      end

      [:described_class, :describes].each do |key|
        describe key do
          context "with a String" do
            it "returns nil" do
              m = Metadata.new
              m.process('group')

              expect(m[:example_group][key]).to be_nil
            end
          end

          context "with a Symbol" do
            it "returns nil" do
              m = Metadata.new
              m.process(:group)

              expect(m[:example_group][key]).to be_nil
            end
          end

          context "with a class" do
            it "returns the class" do
              m = Metadata.new
              m.process(String)

              expect(m[:example_group][key]).to be(String)
            end
          end

          context "in a nested group" do
            it "returns the parent group's described class" do
              sm = Metadata.new
              sm.process(String)

              m = Metadata.new(sm)
              m.process(Array)

              expect(m[:example_group][key]).to be(String)
            end

            it "returns own described class if parent doesn't have one" do
              sm = Metadata.new
              sm.process("foo")

              m = Metadata.new(sm)
              m.process(Array)

              expect(m[:example_group][key]).to be(Array)
            end

            it "can override a parent group's described class" do
              parent = Metadata.new
              parent.process(String)

              child = Metadata.new(parent)
              child.process(Fixnum)
              child[:example_group][key] = Hash

              grandchild = Metadata.new(child)
              grandchild.process(Array)

              expect(grandchild[:example_group][key]).to be(Hash)
              expect(child[:example_group][key]).to be(Hash)
              expect(parent[:example_group][key]).to be(String)
            end
          end
        end
      end

      describe ":description" do
        it "just has the example description" do
          m = Metadata.new
          m.process("group")

          m = m.for_example("example", {})
          expect(m[:description]).to eq("example")
        end

        context "with a string" do
          it "provides the submitted description" do
            m = Metadata.new
            m.process("group")

            expect(m[:example_group][:description]).to eq("group")
          end
        end

        context "with a non-string" do
          it "provides the submitted description" do
            m = Metadata.new
            m.process("group")

            expect(m[:example_group][:description]).to eq("group")
          end
        end

        context "with a non-string and a string" do
          it "concats the args" do
            m = Metadata.new
            m.process(Object, 'group')

            expect(m[:example_group][:description]).to eq("Object group")
          end
        end

        context "with empty args" do
          it "returns empty string for [:example_group][:description]" do
            m = Metadata.new
            m.process()

            expect(m[:example_group][:description]).to eq("")
          end
        end
      end

      describe ":full_description" do
        it "concats example group name and description" do
          group_metadata = Metadata.new
          group_metadata.process('group')

          example_metadata = group_metadata.for_example("example", {})
          expect(example_metadata[:full_description]).to eq("group example")
        end

        it "concats nested example group descriptions" do
          parent = Metadata.new
          parent.process('parent')

          child = Metadata.new(parent)
          child.process('child')

          expect(child[:example_group][:full_description]).to eq("parent child")
          expect(child.for_example('example', child)[:full_description]).to eq("parent child example")
        end

        it "concats nested example group descriptions three deep" do
          grandparent = Metadata.new
          grandparent.process('grandparent')

          parent = Metadata.new(grandparent)
          parent.process('parent')

          child = Metadata.new(parent)
          child.process('child')

          expect(grandparent[:example_group][:full_description]).to eq("grandparent")
          expect(parent[:example_group][:full_description]).to eq("grandparent parent")
          expect(child[:example_group][:full_description]).to eq("grandparent parent child")
          expect(child.for_example('example', child)[:full_description]).to eq("grandparent parent child example")
        end

        %w[# . ::].each do |char|
          context "with a 2nd arg starting with #{char}" do
            it "removes the space" do
              m = Metadata.new
              m.process(Array, "#{char}method")
              expect(m[:example_group][:full_description]).to eq("Array#{char}method")
            end
          end

          context "with a description starting with #{char} nested under a module" do
            it "removes the space" do
              parent = Metadata.new
              parent.process(Object)
              child = Metadata.new(parent)
              child.process("#{char}method")
              expect(child[:example_group][:full_description]).to eq("Object#{char}method")
            end
          end

          context "with a description starting with #{char} nested under a context string" do
            it "does not remove the space" do
              grandparent = Metadata.new
              grandparent.process(Array)
              parent = Metadata.new(grandparent)
              parent.process("with 2 items")
              child = Metadata.new(parent)
              child.process("#{char}method")
              expect(child[:example_group][:full_description]).to eq("Array with 2 items #{char}method")
            end
          end
        end
      end

      describe ":file_path" do
        it "finds the first non-rspec lib file in the caller array" do
          m = Metadata.new
          m.process(:caller => [
                    "./lib/rspec/core/foo.rb",
                    "#{__FILE__}:#{__LINE__}"
          ])
          expect(m[:example_group][:file_path]).to eq(relative_path(__FILE__))
        end
      end

      describe ":line_number" do
        it "finds the line number with the first non-rspec lib file in the backtrace" do
          m = Metadata.new
          m.process({})
          expect(m[:example_group][:line_number]).to eq(__LINE__ - 1)
        end

        it "finds the line number with the first spec file with drive letter" do
          m = Metadata.new
          m.process(:caller => [ "C:/path/to/file_spec.rb:#{__LINE__}" ])
          expect(m[:example_group][:line_number]).to eq(__LINE__ - 1)
        end

        it "uses the number after the first : for ruby 1.9" do
          m = Metadata.new
          m.process(:caller => [ "#{__FILE__}:#{__LINE__}:999" ])
          expect(m[:example_group][:line_number]).to eq(__LINE__ - 1)
        end
      end

      describe "child example group" do
        it "nests the parent's example group metadata" do
          parent = Metadata.new
          parent.process(Object, 'parent')

          child = Metadata.new(parent)
          child.process()

          expect(child[:example_group][:example_group]).to eq(parent[:example_group])
        end
      end
    end
  end
end
