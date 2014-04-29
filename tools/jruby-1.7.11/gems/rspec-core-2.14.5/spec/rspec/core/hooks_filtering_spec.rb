require "spec_helper"

module RSpec::Core
  describe "config block hook filtering" do
    describe "unfiltered hooks" do
      it "is run" do
        filters = []
        RSpec.configure do |c|
          c.before(:all) { filters << "before all in config"}
          c.around(:each) {|example| filters << "around each in config"; example.run}
          c.before(:each) { filters << "before each in config"}
          c.after(:each) { filters << "after each in config"}
          c.after(:all) { filters << "after all in config"}
        end
        group = ExampleGroup.describe
        group.example("example") {}
        group.run
        expect(filters).to eq([
          "before all in config",
          "around each in config",
          "before each in config",
          "after each in config",
          "after all in config"
        ])
      end
    end

    describe "hooks with single filters" do

      context "with no scope specified" do
        it "is run around|before|after :each if the filter matches the example group's filter" do
          filters = []
          RSpec.configure do |c|
            c.around(:match => true) {|example| filters << "around each in config"; example.run}
            c.before(:match => true) { filters << "before each in config"}
            c.after(:match => true)  { filters << "after each in config"}
          end
          group = ExampleGroup.describe(:match => true)
          group.example("example") {}
          group.run
          expect(filters).to eq([
            "around each in config",
            "before each in config",
            "after each in config"
          ])
        end
      end

      it "is run if the filter matches the example group's filter" do
        filters = []
        RSpec.configure do |c|
          c.before(:all,  :match => true) { filters << "before all in config"}
          c.around(:each, :match => true) {|example| filters << "around each in config"; example.run}
          c.before(:each, :match => true) { filters << "before each in config"}
          c.after(:each,  :match => true) { filters << "after each in config"}
          c.after(:all,   :match => true) { filters << "after all in config"}
        end
        group = ExampleGroup.describe(:match => true)
        group.example("example") {}
        group.run
        expect(filters).to eq([
          "before all in config",
          "around each in config",
          "before each in config",
          "after each in config",
          "after all in config"
        ])
      end

      it "runs before|after :all hooks on matching nested example groups" do
        filters = []
        RSpec.configure do |c|
          c.before(:all, :match => true) { filters << :before_all }
          c.after(:all, :match => true)  { filters << :after_all }
        end

        example_1_filters = example_2_filters = nil

        group = ExampleGroup.describe "group" do
          it("example 1") { example_1_filters = filters.dup }
          describe "subgroup", :match => true do
            it("example 2") { example_2_filters = filters.dup }
          end
        end
        group.run

        expect(example_1_filters).to be_empty
        expect(example_2_filters).to eq([:before_all])
        expect(filters).to eq([:before_all, :after_all])
      end

      it "runs before|after :all hooks only on the highest level group that matches the filter" do
        filters = []
        RSpec.configure do |c|
          c.before(:all, :match => true) { filters << :before_all }
          c.after(:all, :match => true)  { filters << :after_all }
        end

        example_1_filters = example_2_filters = example_3_filters = nil

        group = ExampleGroup.describe "group", :match => true do
          it("example 1") { example_1_filters = filters.dup }
          describe "subgroup", :match => true do
            it("example 2") { example_2_filters = filters.dup }
            describe "sub-subgroup", :match => true do
              it("example 3") { example_3_filters = filters.dup }
            end
          end
        end
        group.run

        expect(example_1_filters).to eq([:before_all])
        expect(example_2_filters).to eq([:before_all])
        expect(example_3_filters).to eq([:before_all])

        expect(filters).to eq([:before_all, :after_all])
      end

      it "does not run if the filter doesn't match the example group's filter" do
        filters = []
        RSpec.configure do |c|
          c.before(:all,  :match => false) { filters << "before all in config"}
          c.around(:each, :match => false) {|example| filters << "around each in config"; example.run}
          c.before(:each, :match => false) { filters << "before each in config"}
          c.after(:each,  :match => false) { filters << "after each in config"}
          c.after(:all,   :match => false) { filters << "after all in config"}
        end
        group = ExampleGroup.describe(:match => true)
        group.example("example") {}
        group.run
        expect(filters).to eq([])
      end

      context "when the hook filters apply to individual examples instead of example groups" do
        let(:each_filters) { [] }
        let(:all_filters) { [] }

        let(:group) do
          md = example_metadata
          ExampleGroup.describe do
            it("example", md) { }
          end
        end

        def filters
          each_filters + all_filters
        end

        before(:each) do
          af, ef = all_filters, each_filters

          RSpec.configure do |c|
            c.before(:all,  :foo => :bar) { af << "before all in config"}
            c.around(:each, :foo => :bar) {|example| ef << "around each in config"; example.run}
            c.before(:each, :foo => :bar) { ef << "before each in config"}
            c.after(:each,  :foo => :bar) { ef << "after each in config"}
            c.after(:all,   :foo => :bar) { af << "after all in config"}
          end

          group.run
        end

        describe 'an example with matching metadata' do
          let(:example_metadata) { { :foo => :bar } }

          it "runs the `:each` hooks" do
            expect(each_filters).to eq([
              'around each in config',
              'before each in config',
              'after each in config'
            ])
          end

          it "does not run the `:all` hooks" do
            expect(all_filters).to be_empty
          end
        end

        describe 'an example without matching metadata' do
          let(:example_metadata) { { :foo => :bazz } }

          it "does not run any of the hooks" do
            expect(filters).to be_empty
          end
        end
      end
    end

    describe "hooks with multiple filters" do
      it "is run if all hook filters match the group's filters" do
        filters = []
        RSpec.configure do |c|
          c.before(:all,  :one => 1)                         { filters << "before all in config"}
          c.around(:each, :two => 2, :one => 1)              {|example| filters << "around each in config"; example.run}
          c.before(:each, :one => 1, :two => 2)              { filters << "before each in config"}
          c.after(:each,  :one => 1, :two => 2, :three => 3) { filters << "after each in config"}
          c.after(:all,   :one => 1, :three => 3)            { filters << "after all in config"}
        end
        group = ExampleGroup.describe(:one => 1, :two => 2, :three => 3)
        group.example("example") {}
        group.run
        expect(filters).to eq([
          "before all in config",
          "around each in config",
          "before each in config",
          "after each in config",
          "after all in config"
        ])
      end

      it "does not run if some hook filters don't match the group's filters" do
        filters = []
        RSpec.configure do |c|
          c.before(:all,  :one => 1, :four => 4)                         { filters << "before all in config"}
          c.around(:each, :two => 2, :four => 4)                         {|example| filters << "around each in config"; example.run}
          c.before(:each, :one => 1, :two => 2, :four => 4)              { filters << "before each in config"}
          c.after(:each,  :one => 1, :two => 2, :three => 3, :four => 4) { filters << "after each in config"}
          c.after(:all,   :one => 1, :three => 3, :four => 4)            { filters << "after all in config"}
        end
        group = ExampleGroup.describe(:one => 1, :two => 2, :three => 3)
        group.example("example") {}
        group.run
        expect(filters).to eq([])
      end
    end
  end
end
