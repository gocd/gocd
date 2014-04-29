require 'spec_helper'

describe "extensions" do
  describe "debugger" do
    it "is defined on Kernel" do
      expect(Kernel).to respond_to(:debugger)
    end
  end
end
