require File.dirname(__FILE__) + '/spec_helper'

describe "pending example (which is fixed)" do
  it %Q|reports "FIXED ... Expected ... to fail.  No Error was raised."| do
    pending("for some reason") do
      # success
    end
  end
end
