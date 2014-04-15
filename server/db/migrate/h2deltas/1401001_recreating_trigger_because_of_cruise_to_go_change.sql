DROP TRIGGER lastTransitionedTimeUpdate;
CREATE TRIGGER lastTransitionedTimeUpdate AFTER INSERT ON buildStateTransitions FOR EACH ROW CALL "com.thoughtworks.go.server.sqlmigration.Migration_230007";

--//@UNDO

DROP TRIGGER lastTransitionedTimeUpdate;
CREATE TRIGGER lastTransitionedTimeUpdate AFTER INSERT ON buildStateTransitions FOR EACH ROW CALL "com.thoughtworks.go.server.sqlmigration.Migration_230007";