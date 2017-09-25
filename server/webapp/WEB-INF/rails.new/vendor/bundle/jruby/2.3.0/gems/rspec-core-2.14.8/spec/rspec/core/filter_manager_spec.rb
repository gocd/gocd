require 'spec_helper'

module RSpec::Core
  describe FilterManager do
    def opposite(name)
      name =~ /^in/ ? name.sub(/^(in)/,'ex') : name.sub(/^(ex)/,'in')
    end

    %w[include inclusions exclude exclusions].each_slice(2) do |name, type|
      describe "##{name}" do
        it "merges #{type}" do
          filter_manager = FilterManager.new
          filter_manager.exclusions.clear # defaults
          filter_manager.send name, :foo => :bar
          filter_manager.send name, :baz => :bam
          expect(filter_manager.send(type)).to eq(:foo => :bar, :baz => :bam)
        end

        it "overrides previous #{type} with (via merge)" do
          filter_manager = FilterManager.new
          filter_manager.exclusions.clear # defaults
          filter_manager.send name, :foo => 1
          filter_manager.send name, :foo => 2
          expect(filter_manager.send(type)).to eq(:foo => 2)
        end

        it "deletes matching opposites" do
          filter_manager = FilterManager.new
          filter_manager.exclusions.clear # defaults
          filter_manager.send opposite(name), :foo => 1
          filter_manager.send name, :foo => 2
          expect(filter_manager.send(type)).to eq(:foo => 2)
          expect(filter_manager.send(opposite(type))).to be_empty
        end

        if name == "include"
          [:locations, :line_numbers, :full_description].each do |filter|
            context "with :#{filter}" do
              it "clears previous inclusions" do
                filter_manager = FilterManager.new
                filter_manager.include :foo => :bar
                filter_manager.include filter => "value"
                expect(filter_manager.inclusions).to eq({filter => "value"})
              end

              it "does nothing when :#{filter} previously set" do
                filter_manager = FilterManager.new
                filter_manager.include filter => "a_value"
                filter_manager.include :foo => :bar
                expect(filter_manager.inclusions).to eq(filter => "a_value")
              end
            end
          end
        end
      end

      describe "##{name}!" do
        it "replaces existing #{type}" do
          filter_manager = FilterManager.new
          filter_manager.exclusions.clear # defaults
          filter_manager.send name, :foo => 1, :bar => 2
          filter_manager.send "#{name}!", :foo => 3
          expect(filter_manager.send(type)).to eq(:foo => 3)
        end

        it "deletes matching opposites" do
          filter_manager = FilterManager.new
          filter_manager.exclusions.clear # defaults
          filter_manager.send opposite(name), :foo => 1
          filter_manager.send "#{name}!", :foo => 2
          expect(filter_manager.send(type)).to eq(:foo => 2)
          expect(filter_manager.send(opposite(type))).to be_empty
        end
      end

      describe "##{name}_with_low_priority" do
        it "ignores new #{type} if same key exists" do
          filter_manager = FilterManager.new
          filter_manager.exclusions.clear # defaults
          filter_manager.send name, :foo => 1
          filter_manager.send "#{name}_with_low_priority", :foo => 2
          expect(filter_manager.send(type)).to eq(:foo => 1)
        end

        it "ignores new #{type} if same key exists in opposite" do
          filter_manager = FilterManager.new
          filter_manager.exclusions.clear # defaults
          filter_manager.send opposite(name), :foo => 1
          filter_manager.send "#{name}_with_low_priority", :foo => 1
          expect(filter_manager.send(type)).to be_empty
          expect(filter_manager.send(opposite(type))).to eq(:foo => 1)
        end

        it "keeps new #{type} if same key exists in opposite but values are different" do
          filter_manager = FilterManager.new
          filter_manager.exclusions.clear # defaults
          filter_manager.send opposite(name), :foo => 1
          filter_manager.send "#{name}_with_low_priority", :foo => 2
          expect(filter_manager.send(type)).to eq(:foo => 2)
          expect(filter_manager.send(opposite(type))).to eq(:foo => 1)
        end
      end
    end

    describe "#prune" do
      def filterable_object_with(args = {})
        object = double('a filterable object')
        object.stub(:any_apply?) { |f| Metadata.new(args).any_apply?(f) }
        object
      end

      it "includes objects with tags matching inclusions" do
        included = filterable_object_with({:foo => :bar})
        excluded = filterable_object_with
        filter_manager = FilterManager.new
        filter_manager.include :foo => :bar
        expect(filter_manager.prune([included, excluded])).to eq([included])
      end

      it "excludes objects with tags matching exclusions" do
        included = filterable_object_with
        excluded = filterable_object_with({:foo => :bar})
        filter_manager = FilterManager.new
        filter_manager.exclude :foo => :bar
        expect(filter_manager.prune([included, excluded])).to eq([included])
      end

      it "prefers exclusion when matches previously set inclusion" do
        included = filterable_object_with
        excluded = filterable_object_with({:foo => :bar})
        filter_manager = FilterManager.new
        filter_manager.include :foo => :bar
        filter_manager.exclude :foo => :bar
        expect(filter_manager.prune([included, excluded])).to eq([included])
      end

      it "prefers inclusion when matches previously set exclusion" do
        included = filterable_object_with({:foo => :bar})
        excluded = filterable_object_with
        filter_manager = FilterManager.new
        filter_manager.exclude :foo => :bar
        filter_manager.include :foo => :bar
        expect(filter_manager.prune([included, excluded])).to eq([included])
      end

      it "prefers previously set inclusion when exclusion matches but has lower priority" do
        included = filterable_object_with({:foo => :bar})
        excluded = filterable_object_with
        filter_manager = FilterManager.new
        filter_manager.include :foo => :bar
        filter_manager.exclude_with_low_priority :foo => :bar
        expect(filter_manager.prune([included, excluded])).to eq([included])
      end

      it "prefers previously set exclusion when inclusion matches but has lower priority" do
        included = filterable_object_with
        excluded = filterable_object_with({:foo => :bar})
        filter_manager = FilterManager.new
        filter_manager.exclude :foo => :bar
        filter_manager.include_with_low_priority :foo => :bar
        expect(filter_manager.prune([included, excluded])).to eq([included])
      end
    end

    describe "#inclusions#description" do
      it 'cleans up the description' do
        project_dir = File.expand_path('.')
        expect(lambda { }.inspect).to include(project_dir)
        expect(lambda { }.inspect).to include(' (lambda)') if RUBY_VERSION > '1.9'
        expect(lambda { }.inspect).to include('0x')

        filter_manager = FilterManager.new
        filter_manager.include :foo => lambda { }

        expect(filter_manager.inclusions.description).not_to include(project_dir)
        expect(filter_manager.inclusions.description).not_to include(' (lambda)')
        expect(filter_manager.inclusions.description).not_to include('0x')
      end
    end

    describe "#exclusions#description" do
      it 'cleans up the description' do
        project_dir = File.expand_path('.')
        expect(lambda { }.inspect).to include(project_dir)
        expect(lambda { }.inspect).to include(' (lambda)') if RUBY_VERSION > '1.9'
        expect(lambda { }.inspect).to include('0x')

        filter_manager = FilterManager.new
        filter_manager.exclude :foo => lambda { }

        expect(filter_manager.exclusions.description).not_to include(project_dir)
        expect(filter_manager.exclusions.description).not_to include(' (lambda)')
        expect(filter_manager.exclusions.description).not_to include('0x')
      end

      it 'returns `{}` when it only contains the default filters' do
        filter_manager = FilterManager.new
        expect(filter_manager.exclusions.description).to eq({}.inspect)
      end

      it 'includes other filters' do
        filter_manager = FilterManager.new
        filter_manager.exclude :foo => :bar
        expect(filter_manager.exclusions.description).to eq({ :foo => :bar }.inspect)
      end

      it 'deprecates an overridden :if filter' do
        expect(RSpec).to receive(:deprecate).with(/exclude\(:if/)
        filter_manager = FilterManager.new
        filter_manager.exclude :if => :custom_filter
      end

      it 'deprecates an :if filter overridden with low priority' do
        expect(RSpec).to receive(:deprecate).with(/exclude\(:if/)
        filter_manager = FilterManager.new
        filter_manager.exclude_with_low_priority :if => :custom_filter
      end

      it 'deprecates an overridden :unless filter' do
        expect(RSpec).to receive(:deprecate).with(/exclude\(:unless/)
        filter_manager = FilterManager.new
        filter_manager.exclude :unless => :custom_filter
      end

      it 'deprecates an :unless filter overridden with low priority' do
        expect(RSpec).to receive(:deprecate).with(/exclude\(:unless/)
        filter_manager = FilterManager.new
        filter_manager.exclude_with_low_priority :unless => :custom_filter
      end

      it 'includes an overriden :if filter' do
        allow(RSpec).to receive(:deprecate)
        filter_manager = FilterManager.new
        filter_manager.exclude :if => :custom_filter
        expect(filter_manager.exclusions.description).to eq({ :if => :custom_filter }.inspect)
      end

      it 'includes an overriden :unless filter' do
        allow(RSpec).to receive(:deprecate)
        filter_manager = FilterManager.new
        filter_manager.exclude :unless => :custom_filter
        expect(filter_manager.exclusions.description).to eq({ :unless => :custom_filter }.inspect)
      end
    end
  end
end
