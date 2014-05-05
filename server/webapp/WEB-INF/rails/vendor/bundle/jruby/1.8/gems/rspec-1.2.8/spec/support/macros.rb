module Macros
  def treats_method_missing_as_private(options = {:noop => true, :subject => nil})
    it "should have method_missing as private" do
      with_ruby 1.8 do
        described_class.private_instance_methods.should include("method_missing")
      end
      with_ruby 1.9 do
        described_class.private_instance_methods.should include(:method_missing)
      end
    end

    it "should not respond_to? method_missing (because it's private)" do
      formatter = options[:subject] || described_class.new({ }, StringIO.new)
      formatter.should_not respond_to(:method_missing)
    end

    if options[:noop]
      it "should respond_to? all messages" do
        formatter = described_class.new({ }, StringIO.new)
        formatter.should respond_to(:just_about_anything)
      end

      it "should respond_to? anything, when given the private flag" do
        formatter = described_class.new({ }, StringIO.new)
        formatter.respond_to?(:method_missing, true).should be_true
      end
    end
  end
end
