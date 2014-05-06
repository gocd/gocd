require File.expand_path(File.dirname(__FILE__) + '<%= '/..' * class_nesting_depth %>/../spec_helper')

describe <%= controller_class_name %>Controller do
  describe "route generation" do
    it "maps #index" do
      route_for(:controller => "<%= table_name %>", :action => "index").should == "/<%= table_name %>"
    end

    it "maps #new" do
      route_for(:controller => "<%= table_name %>", :action => "new").should == "/<%= table_name %>/new"
    end

    it "maps #show" do
      route_for(:controller => "<%= table_name %>", :action => "show", :id => "1").should == "/<%= table_name %>/1"
    end

    it "maps #edit" do
      route_for(:controller => "<%= table_name %>", :action => "edit", :id => "1").should == "/<%= table_name %>/1/edit"
    end

    it "maps #create" do
      route_for(:controller => "<%= table_name %>", :action => "create").should == {:path => "/<%= table_name %>", :method => :post}
    end

    it "maps #update" do
      route_for(:controller => "<%= table_name %>", :action => "update", :id => "1").should == {:path =>"/<%= table_name %>/1", :method => :put}
    end

    it "maps #destroy" do
      route_for(:controller => "<%= table_name %>", :action => "destroy", :id => "1").should == {:path =>"/<%= table_name %>/1", :method => :delete}
    end
  end

  describe "route recognition" do
    it "generates params for #index" do
      params_from(:get, "/<%= table_name %>").should == {:controller => "<%= table_name %>", :action => "index"}
    end

    it "generates params for #new" do
      params_from(:get, "/<%= table_name %>/new").should == {:controller => "<%= table_name %>", :action => "new"}
    end

    it "generates params for #create" do
      params_from(:post, "/<%= table_name %>").should == {:controller => "<%= table_name %>", :action => "create"}
    end

    it "generates params for #show" do
      params_from(:get, "/<%= table_name %>/1").should == {:controller => "<%= table_name %>", :action => "show", :id => "1"}
    end

    it "generates params for #edit" do
      params_from(:get, "/<%= table_name %>/1/edit").should == {:controller => "<%= table_name %>", :action => "edit", :id => "1"}
    end

    it "generates params for #update" do
      params_from(:put, "/<%= table_name %>/1").should == {:controller => "<%= table_name %>", :action => "update", :id => "1"}
    end

    it "generates params for #destroy" do
      params_from(:delete, "/<%= table_name %>/1").should == {:controller => "<%= table_name %>", :action => "destroy", :id => "1"}
    end
  end
end
