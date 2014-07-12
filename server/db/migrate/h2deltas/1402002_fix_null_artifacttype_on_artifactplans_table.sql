UPDATE ARTIFACTPLANS SET ARTIFACTTYPE='file' WHERE ARTIFACTTYPE IS NULL;
COMMIT;

-- No undo as adding default value to mandatory field

