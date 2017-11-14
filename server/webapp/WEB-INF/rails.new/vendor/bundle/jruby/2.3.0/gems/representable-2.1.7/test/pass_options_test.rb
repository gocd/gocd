require 'test_helper'

class PassOptionsTest < BaseTest
  let (:format) { :hash }
  let (:song) { Struct.new(:title).new("Revolution") }
  let (:prepared) { representer.prepare song }

  describe "module" do
    representer! do
      property :title, :pass_options => true,
        :as => lambda { |args| [args.binding.name, args.user_options, args.represented, args.decorator]  }
    end

    it { render(prepared, :volume => 1).must_equal_document({["title", {:volume=>1}, prepared, prepared] => "Revolution"}) }
    # it { parse(prepared, {"args" => "Wie Es Geht"}).name.must_equal "Wie Es Geht" }
  end

  describe "decorator" do
    representer!(:decorator => true) do
      property :title, :pass_options => true,
        :as => lambda { |args| [args.binding.name, args.user_options, args.represented, args.decorator]  }
    end

    it { render(prepared, :volume => 1).must_equal_document({["title", {:volume=>1}, song, prepared] => "Revolution"}) }
    # it { parse(prepared, {"args" => "Wie Es Geht"}).name.must_equal "Wie Es Geht" }
  end
end
