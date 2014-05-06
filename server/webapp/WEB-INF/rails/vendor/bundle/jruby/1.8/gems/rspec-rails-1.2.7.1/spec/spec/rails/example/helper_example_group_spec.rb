require File.dirname(__FILE__) + '/../../../spec_helper'
Spec::Runner.configuration.global_fixtures = :people

describe ExplicitHelper, :type => :helper do
  include ExplicitHelper
  
  it "should not require naming the helper if describe is passed a type" do
    method_in_explicit_helper.should match(/text from a method/)
    helper.method_in_explicit_helper.should match(/text from a method/)
  end
end

module Spec
  module Rails
    module Example
      describe HelperExampleGroup, :type => :helper do
        helper_name :explicit

        accesses_configured_helper_methods

        it "DEPRECATED should have direct access to methods defined in helpers" do
          method_in_explicit_helper.should =~ /text from a method/
        end

        it "should expose the helper with the #helper method" do
          helper.method_in_explicit_helper.should =~ /text from a method/
        end

        it "should have access to named routes" do
          rspec_on_rails_specs_url.should == "http://test.host/rspec_on_rails_specs"
          rspec_on_rails_specs_path.should == "/rspec_on_rails_specs"

          helper.named_url.should == "http://test.host/rspec_on_rails_specs"
          helper.named_path.should == "/rspec_on_rails_specs"
        end

        it "should fail if the helper method deson't exist" do
          lambda { non_existent_helper_method }.should raise_error(NameError)
          lambda { helper.non_existent_helper_method }.should raise_error(NameError)
        end

        it "should have access to session" do
          session[:foo] = 'bar'
          session_foo.should == 'bar'
          helper.session_foo.should == 'bar'
        end
        
        it "should have access to params" do
          params[:foo] = 'bar'
          params_foo.should == 'bar'
          helper.params_foo.should == 'bar'
        end
        
        it "should have access to request" do
          request.stub!(:thing).and_return('bar')
          request_thing.should == 'bar'
          helper.request_thing.should == 'bar'
        end
        
        it "should have access to flash" do
          flash[:thing] = 'camera'
          flash_thing.should == 'camera'
          helper.flash_thing.should == 'camera'
        end
      end

      describe HelperExampleGroup, "#eval_erb", :type => :helper do
        helper_name :explicit

        it "should support methods that accept blocks" do
          eval_erb("<% prepend 'foo' do %>bar<% end %>").should == "foobar"
        end
      end

      describe HelperExampleGroup, ".fixtures", :type => :helper do
        helper_name :explicit
        fixtures :animals

        it "should load fixtures" do
          pig = animals(:pig)
          pig.class.should == Animal
        end

        it "should load global fixtures" do
          lachie = people(:lachie)
          lachie.class.should == Person
        end
      end
      
      describe "methods from standard helpers", :type => :helper do
        helper_name :explicit
        it "should be exposed to the helper" do
          helper.link_to("Foo","http://bar").should have_tag("a")
        end
      end

      describe HelperExampleGroup, "included modules", :type => :helper do
        helpers = [
          ActionView::Helpers::ActiveRecordHelper,
          ActionView::Helpers::AssetTagHelper,
          ActionView::Helpers::BenchmarkHelper,
          ActionView::Helpers::CacheHelper,
          ActionView::Helpers::CaptureHelper,
          ActionView::Helpers::DateHelper,
          ActionView::Helpers::DebugHelper,
          ActionView::Helpers::FormHelper,
          ActionView::Helpers::FormOptionsHelper,
          ActionView::Helpers::FormTagHelper,
          ActionView::Helpers::JavaScriptHelper,
          ActionView::Helpers::NumberHelper,
          ActionView::Helpers::PrototypeHelper,
          ActionView::Helpers::ScriptaculousHelper,
          ActionView::Helpers::TagHelper,
          ActionView::Helpers::TextHelper,
          ActionView::Helpers::UrlHelper
        ]
        helpers.each do |helper_module|
          it "should include #{helper_module}" do
            self.class.ancestors.should include(helper_module)
            helper.class.ancestors.should include(helper_module)
          end
        end
      end
      
      # TODO: BT - Helper Examples should proxy method_missing to a Rails View instance.
      # When that is done, remove this method
      describe HelperExampleGroup, "#protect_against_forgery?", :type => :helper do
        it "should return false" do
          protect_against_forgery?.should be_false
          helper.protect_against_forgery?.should be_false
        end
      end
      
      describe HelperExampleGroup, "#assigns", :type => :helper do
        helper_name :addition
        it "should expose variables to helper" do
          assigns[:addend] = 3
          helper.plus(4).should == 7
        end

        it "should make helper ivars available in example" do
          assigns[:addend] = 3
          assigns[:addend].should == 3
        end
      end
      
      describe HelperExampleGroup, "using a helper that uses output_buffer inside helper", :type => :helper do
        helper_name :explicit
        
        before(:each) do
          if Rails::VERSION::STRING <= "2.1"
            pending("need to get this new feature working against pre 2.2 versions of rails")
          end
        end

        it "should not raise an error" do
          lambda { method_using_output_buffer }.should_not raise_error
        end

        it "should put the output in the output_buffer" do
          method_using_output_buffer
          output_buffer.should == "the_text_from_concat"
        end
      end

      describe HelperExampleGroup, "using a helper that tries to access @template", :type => :helper do
        helper_name :explicit

        it "should not raise an error" do
          lambda { method_using_template }.should_not raise_error
        end

        it "should have the correct output" do
          method_using_template.should have_text(/#some_id/)
        end
      end

    end
  end
end

module Bug11223
  # see http://rubyforge.org/tracker/index.php?func=detail&aid=11223&group_id=797&atid=3149
  describe 'Accessing flash from helper spec', :type => :helper do
    it 'should not raise an error' do
      lambda { flash['test'] }.should_not raise_error
    end
  end
end

module Spec
  module Rails
    module Example
      describe HelperExampleGroup do
        it "should clear its name from the description" do
          group = describe("foo", :type => :helper) do
            $nested_group = describe("bar") do
            end
          end
          group.description.to_s.should == "foo"
          $nested_group.description.to_s.should == "foo bar"
        end
      end
    end
  end
end

module Bug719
  # see http://rspec.lighthouseapp.com/projects/5645/tickets/719
  # FIXME - helper and example provided in ticket. The example did
  # fail initially, so running it now shows that the bug is fixed,
  # but this doesn't serve as a good internal example.
  module ImagesHelper
    def hide_images_button
      content_tag :div, :class => :hide_images_button do
        button_to_function "Hide Images", :id => :hide_images_button do |page|
          page[:more_images_button].toggle
          page[:image_browser].toggle
        end
      end
    end
  end
  
  describe ImagesHelper, :type => :helper do
    it "should render a hide_images_button" do
      helper.hide_images_button.should have_tag('div[class=?]','hide_images_button') do
        with_tag('input[id=?][type=?][value=?][onclick^=?]',
                 'hide_images_button', 'button', 'Hide Images',
                 "$(&quot;more_images_button&quot;).toggle();\n$(&quot;image_browser&quot;).toggle();;")
        end
     end
  end
end
