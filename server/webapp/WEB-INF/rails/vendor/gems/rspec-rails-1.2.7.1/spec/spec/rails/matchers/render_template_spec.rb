require File.dirname(__FILE__) + '/../../../spec_helper'

[:response, :controller].each do |subject_method|
  ['isolation','integration'].each do |mode|
    describe "#{subject_method}.should render_template (in #{mode} mode)",
      :type => :controller do
      controller_name :render_spec
      if mode == 'integration'
        integrate_views
      end

      subject { send(subject_method) }

      it "matches an action (using a string)" do
        post 'some_action'
        should render_template('some_action')
      end

      it "does not match an action that is a truncated version of the actual action" do
        post 'some_action'
        should_not render_template('some_actio')
      end

      if ::Rails::VERSION::STRING >= '2.3'
        it "matches an action with specified extenstions (implicit format)" do
          post 'some_action'
          should render_template('some_action.html.erb')
        end

        it "matches an action with specified extenstions (explicit format)" do
          post 'some_action', :format => 'js'
          should render_template('some_action.js.rjs')
        end
      end

      it "matches an action (using a symbol)" do
        post 'some_action'
        should render_template(:some_action)
      end
    
      it "matches an action on a specific controller" do
        post 'some_action'
        should render_template('render_spec/some_action')
      end
    
      it "matches an action on a non-default specific controller" do
        post 'action_which_renders_template_from_other_controller'
        should render_template('controller_spec/action_with_template')
      end
    
      it "matches an rjs template" do
        xhr :post, 'some_action'
        should render_template('render_spec/some_action')
      end
    
      it "matches a partial template (simple path)" do
        get 'action_with_partial'
        should render_template("_a_partial")
      end
    
      it "matches a partial template (complex path)" do
        get 'action_with_partial'
        should render_template("render_spec/_a_partial")
      end
    
      it "fails when the wrong template is rendered" do
        post 'some_action'
        lambda do
          should render_template('non_existent_template')
        end.should fail_with(/expected \"non_existent_template\", got \"render_spec\/some_action(\.html\.erb)?\"/)
      end
    
      it "fails when template is associated with a different controller but controller is not specified" do
        post 'action_which_renders_template_from_other_controller'
        lambda do
          should render_template('action_with_template')
        end.should fail_with(/expected \"action_with_template\", got \"controller_spec\/action_with_template(\.html\.erb)?\"/)
      end
    
      it "fails with incorrect full path when template is associated with a different controller" do
        post 'action_which_renders_template_from_other_controller'
        lambda do
          should render_template('render_spec/action_with_template')
        end.should fail_with(/expected \"render_spec\/action_with_template\", got \"controller_spec\/action_with_template(\.html\.erb)?\"/)
      end
    
      it "fails on the wrong extension" do
        get 'some_action'
        lambda {
          should render_template('render_spec/some_action.js.rjs')
        }.should fail_with(/expected \"render_spec\/some_action\.js\.rjs\", got \"render_spec\/some_action(\.html\.erb)?\"/)
      end
    
      it "faild when TEXT is rendered" do
        post 'text_action'
        lambda do
          should render_template('some_action')
        end.should fail_with(/expected \"some_action\", got (nil|\"\")/)
      end
    
      describe "with an alternate layout" do
        it "says it rendered the action's layout" do
          pending("record rendering of layouts") do
            get 'action_with_alternate_layout'
            should render_template('layouts/simple')
          end
        end
      end
      
      it "provides a description" do
        render_template("foo/bar").description.should == %q|render template "foo/bar"|
      end
    end
    
    describe "#{subject_method}.should_not render_template (in #{mode} mode)",
      :type => :controller do
      controller_name :render_spec
      if mode == 'integration'
        integrate_views
      end
      
      subject { send(subject_method) }

      it "passes when the action renders nothing" do
        post 'action_that_renders_nothing'
        should_not render_template('action_that_renders_nothing')
      end
      
      it "passes when the action renders nothing (symbol)" do
        post 'action_that_renders_nothing'
        should_not render_template(:action_that_renders_nothing)
      end
      
      it "passes when the action does not render the template" do
        post 'some_action'
        should_not render_template('some_other_template')
      end
      
      it "passes when the action does not render the template (symbol)" do
        post 'some_action'
        should_not render_template(:some_other_template)
      end
      
      it "passes when the action does not render the template (named with controller)" do
        post 'some_action'
        should_not render_template('render_spec/some_other_template')
      end
      
      it "passes when the action renders the template with a different controller" do
        post 'action_which_renders_template_from_other_controller'
        should_not render_template('action_with_template')
      end
      
      it "passes when the action renders the template (named with controller) with a different controller" do
        post 'action_which_renders_template_from_other_controller'
        should_not render_template('render_spec/action_with_template')
      end
      
      it "passes when TEXT is rendered" do
        post 'text_action'
        should_not render_template('some_action')
      end
      
      it "fails when the action renders the template" do
        post 'some_action'
        lambda do
          should_not render_template('some_action')
        end.should fail_with("expected not to render \"some_action\", but did")
      end
      
      it "fails when the action renders the template (symbol)" do
        post 'some_action'
        lambda do
          should_not render_template(:some_action)
        end.should fail_with("expected not to render \"some_action\", but did")
      end
      
      it "fails when the action renders the template (named with controller)" do
        post 'some_action'
        lambda do
          should_not render_template('render_spec/some_action')
        end.should fail_with("expected not to render \"render_spec/some_action\", but did")
      end
      
      it "fails when the action renders the partial" do
        post 'action_with_partial'
        lambda do
          should_not render_template('_a_partial')
        end.should fail_with("expected not to render \"_a_partial\", but did")
      end
      
      it "fails when the action renders the partial (named with controller)" do
        post 'action_with_partial'
        lambda do
          should_not render_template('render_spec/_a_partial')
        end.should fail_with("expected not to render \"render_spec/_a_partial\", but did")
      end
          
    end
  end
end
