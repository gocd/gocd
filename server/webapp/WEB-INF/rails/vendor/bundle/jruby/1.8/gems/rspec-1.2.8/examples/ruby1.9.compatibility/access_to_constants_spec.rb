# courtesy of Matthias Hennemeyer
#
# The following should pass against ruby 1.8 and 1.9. It currently only passes
# 1.8 (as of 1/2/2009)
#
# Once cucumber supports ruby 1.9, this should be moved to cucumber scenarios instead. 
module Foo 
  module Bar
    
    module ModuleInEnclosingModule;end
    class ClassInEnclosingModule;end 
    def method_in_enclosing_module;end
    CONSTANT_IN_ENCLOSING_MODULE = 0

    describe "Examples trying to access constants defined in an enclosing module" do

      it "can access Modules" do
        ModuleInEnclosingModule
      end
      it "can access Classes" do
        ClassInEnclosingModule.new
      end
      it "can access CONSTANTS" do
        CONSTANT_IN_ENCLOSING_MODULE
      end
      it "can NOT access methods" do
        lambda {method_in_enclosing_module}.should raise_error(/undefined/)
      end

      describe "from a nested example group" do

        it "can access Modules" do
          ModuleInEnclosingModule
        end
        it "can access Classes" do
          ClassInEnclosingModule.new
        end
        it "can access CONSTANTS" do
          CONSTANT_IN_ENCLOSING_MODULE
        end
        it "can NOT access methods" do
          lambda {method_in_enclosing_module}.should raise_error(/undefined/)
        end

      end 

    end
    
    describe "Examples trying to access constants defined in the example group" do
      
      module ModuleDefinedInGroup;end
      class ClassDefinedInGroup; end 
      def method_defined_in_group; end
      CONSTANT_DEFINED_IN_GROUP = 0

      it "can access Modules" do
        ModuleDefinedInGroup
      end
      it "can access Classes" do
        ClassDefinedInGroup.new
      end
      it "can access CONSTANTS" do
        CONSTANT_DEFINED_IN_GROUP
      end
      it "can access methods" do
        method_defined_in_group
      end
      
      describe "that live inside a nested group" do
        it "can access Modules" do
          ModuleDefinedInGroup
        end
        it "can access Classes" do
          ClassDefinedInGroup.new
        end
        it "can access CONSTANTS" do
          CONSTANT_DEFINED_IN_GROUP
        end
        it "can access methods" do
          method_defined_in_group
        end
      end
    end 
  end 
end
