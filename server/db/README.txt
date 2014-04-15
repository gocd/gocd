Cruise Development Database Deployment QuickStart
--------------------------------------------------

Start HSQLDB (NB: Do this in a different terminal window)
    ant hsqldb


Create the baseline schema and apply migrations
    ant rebuild-db

    Note that this target can also be invoked from the top-level ANT script.


Query the HSQLDB instance with the HSQLDB manager [optional]
    ant dbmgr

    Use the following connection settings in the DatabaseManager

    Setting Name: cruise (this will persist the data below so you won't have to enter it again)
    Driver: org.hsqldb.jdbcDriver
    URL: jdbc:hsqldb:hsql://localhost:9002
    User: sa


Add a new data migration: Any changes that need to evolve the original schema
    Add a new file to db/migrate/deltas where the first character of the file name is the next sequential integer
    and the contents of the file is the SQL needed to perform the migration. DbDeploy will read these delta scripts,
    compare the number of the delta to the schema version in the changelog table in the database, and generate an
    appropriate migration and undo script in the db/migrate/generated folder.

    NB: java.sql.SQLException: Table already exists:
        There appears to be a weird issue with HSQLDB not completely dropping tables when it drops the schema during a
        rebuild. As a workaround until someone determines the root cause, add drop table statements to
        db/schema/drop_schema.sql to ensure these tables are properly removed.


Generate and apply new data migrations
    ant apply-deltas


Undo the migration
    ant execute-undo

For more information about this style of data migration, see http://www.dbdeploy.com

Changing the database during development
    Run ant db.rebuild to create the new schema to check in