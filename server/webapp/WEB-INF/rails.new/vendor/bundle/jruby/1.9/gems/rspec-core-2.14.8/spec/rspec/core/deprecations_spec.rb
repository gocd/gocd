require "spec_helper"

describe "deprecated methods" do
  describe "Spec" do
    it "is deprecated" do
      RSpec.should_receive(:deprecate)
      Spec
    end

    it "returns RSpec" do
      RSpec.stub(:deprecate)
      expect(Spec).to eq(RSpec)
    end

    it "doesn't include backward compatibility in const_missing backtrace" do
      RSpec.stub(:deprecate)
      exception = nil
      begin
        ConstantThatDoesNotExist
      rescue Exception => exception
      end
      expect(exception.backtrace.find { |l| l =~ /lib\/rspec\/core\/backward_compatibility/ }).to be_nil
    end
  end

  describe RSpec::Core::ExampleGroup do
    describe 'running_example' do
      it 'is deprecated' do
        RSpec.should_receive(:deprecate).at_least(:once)
        self.running_example
      end

      it "delegates to example" do
        RSpec.stub(:deprecate)
        expect(running_example).to eq(example)
      end
    end
  end

  describe RSpec::Core::SharedExampleGroup do
    describe 'share_as' do
      it 'is deprecated' do
        RSpec.should_receive(:deprecate).at_least(:once)
        RSpec::Core::SharedExampleGroup.share_as(:DeprecatedSharedConst) {}
      end
    end
  end

  describe "Spec::Runner.configure" do
    it "is deprecated" do
      RSpec.should_receive(:deprecate).at_least(:once)
      Spec::Runner.configure
    end
  end

  describe "Spec::Rake::SpecTask" do
    it "is deprecated" do
      RSpec.should_receive(:deprecate).at_least(:once)
      Spec::Rake::SpecTask
    end

    it "doesn't include backward compatibility in const_missing backtrace" do
      RSpec.stub(:deprecate)
      exception = nil
      begin
        Spec::Rake::ConstantThatDoesNotExist
      rescue Exception => exception
      end
      expect(exception.backtrace.find { |l| l =~ /lib\/rspec\/core\/backward_compatibility/ }).to be_nil
    end
  end

end
