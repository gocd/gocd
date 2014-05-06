Feature: explicit subject

  You can override the implicit subject using the subject() method.
  
  Scenario: subject in top level group
    Given a file named "top_level_subject_spec.rb" with:
      """
      describe Array, "with some elements" do
        subject { [1,2,3] }
        it "should have the prescribed elements" do
          subject.should == [1,2,3]
        end
      end
      """
    When I run "spec top_level_subject_spec.rb"
    Then the stdout should match "1 example, 0 failures"

  Scenario: subject in a nested group
    Given a file named "nested_subject_spec.rb" with:
      """
      describe Array do
        subject { [1,2,3] }
        describe "with some elements" do
          it "should have the prescribed elements" do
            subject.should == [1,2,3]
          end
        end
      end
      """
    When I run "spec nested_subject_spec.rb"
    Then the stdout should match "1 example, 0 failures"
