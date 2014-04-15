import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class UpdateGoJar {
    private static final String INIT_2013_0156_FILE_NAME = "cve_2013_0156.rb";
    private static final String INIT_2013_1856_FILE_NAME = "cve_2013_1856.rb";
    private static final String YAML_FILE_NAME = "yaml.rb";
    private static final String DECODING_FILE_NAME = "decoding.rb";
    private static final String OKJSON_FILE_NAME = "okjson.rb";
    private static final String SANITIZER_FILE_NAME = "sanitizer.rb";
    private File gojar;
    private File parent;
    private File goJarBackUpFile;
    private ZipUtil zipUtil;
    private File goJarExplodeDir;

    public UpdateGoJar(String goJarPath) throws IOException {
        gojar = validateJarAndGetFile(goJarPath);
        parent = gojar.getParentFile() == null ? new File(".") : gojar.getParentFile();
        goJarBackUpFile = new File(parent, "go.backup.before.patch." + System.currentTimeMillis() + ".jar");
        goJarExplodeDir = new File(parent, "go_explode_dir");
        FileUtils.deleteDirectory(goJarExplodeDir);
        zipUtil = new ZipUtil();
    }

    private File validateJarAndGetFile(String goJarPath) {
        File jarFile = new File(goJarPath);
        if (!jarFile.exists()) {
            logToConsoleAndExit("Jar File does not exist: " + goJarPath);
        }

        if (!jarFile.isFile()) {
            logToConsoleAndExit("Jar File is not a valid file: " + goJarPath);
        }
        if (!jarFile.canRead() || !jarFile.canWrite()) {
            logToConsoleAndExit("Jar File is not readable or writable: " + goJarPath);
        }
        File parent = jarFile.getParentFile();
        if (parent == null) {
            parent = new File(".");
        }
        if (!parent.canRead() || !parent.canWrite()) {
            logToConsoleAndExit("Jar File's parent directory is not readable or writable: " + parent.getAbsolutePath());
        }
        return jarFile;
    }


    private void executeUpdate() throws IOException {
        backupJarFile();

        File cruiseWarFile = unzipJarFileAndGetCruiseWarFile();
        File warExplodeDir = unzipAndGetCruiseWarFile(cruiseWarFile);

        copyInitFileToRailsInitDirectory(warExplodeDir);

        deleteOldWarAndCreateNewWarFile(cruiseWarFile, warExplodeDir);
        deleteOldJarAndCreateNewJarFile(gojar, goJarExplodeDir);

        System.err.println("Jar file has been successfully updated.");
    }

    private void deleteOldJarAndCreateNewJarFile(File goJarFile, File jarExplodeDir) throws IOException {
        System.err.println("Deleting Old Go Jar File: " + goJarFile);

        if (!FileUtils.deleteQuietly(goJarFile)) {
            logToConsoleAndExit("Failed to delete go jar file: " + goJarFile);
        }

        System.err.println("Compressing File: " + jarExplodeDir + " in directory: " + parent);
        zipUtil.zipFolderContents(jarExplodeDir, new File(parent, "go.jar"));

        System.err.println("Deleting the exploded directory: " + jarExplodeDir);
        FileUtils.deleteDirectory(jarExplodeDir);
    }

    private void deleteOldWarAndCreateNewWarFile(File cruiseWarFile, File warExplodeDir) throws IOException {
        System.err.println("Deleting Old Cruise War File: " + cruiseWarFile);
        if (!FileUtils.deleteQuietly(cruiseWarFile)) {
            logToConsoleAndExit("Failed to delete war file: " + cruiseWarFile);
        }

        File warParentDirectory = cruiseWarFile.getParentFile();
        System.err.println("Compressing File: " + warExplodeDir + " in directory: " + warParentDirectory);
        zipUtil.zipFolderContents(warExplodeDir, new File(warParentDirectory, "cruise.war"));

        System.err.println("Deleting the exploded directory: " + warExplodeDir);
        FileUtils.deleteDirectory(warExplodeDir);
    }

    private void copyInitFileToRailsInitDirectory(File warExplodeDir) throws IOException {
        File railsJsonBackendsDir = new File(warExplodeDir, "WEB-INF/rails/vendor/rails/activesupport/lib/active_support/json/backends");

        if (new File(railsJsonBackendsDir, OKJSON_FILE_NAME).exists()) {
            System.err.println("This patch has already been applied. No more changes will be made.");
        } else {
            System.err.println(String.format("Copying %s to %s", OKJSON_FILE_NAME, railsJsonBackendsDir));
            FileUtils.copyURLToFile(this.getClass().getResource("/" + OKJSON_FILE_NAME), new File(railsJsonBackendsDir, OKJSON_FILE_NAME));

            System.err.println(String.format("Copying %s to %s", YAML_FILE_NAME, railsJsonBackendsDir));
            FileUtils.copyURLToFile(this.getClass().getResource("/" + YAML_FILE_NAME), new File(railsJsonBackendsDir, YAML_FILE_NAME));

            File railsJsonDir = new File(warExplodeDir, "WEB-INF/rails/vendor/rails/activesupport/lib/active_support/json");
            System.err.println(String.format("Copying %s to %s", DECODING_FILE_NAME, railsJsonDir));
            FileUtils.copyURLToFile(this.getClass().getResource("/" + DECODING_FILE_NAME), new File(railsJsonDir, DECODING_FILE_NAME));
        }

        File railsInitDirectory = new File(warExplodeDir, "WEB-INF/rails/config/initializers");
        System.err.println(String.format("Copying %s to %s", INIT_2013_0156_FILE_NAME, railsInitDirectory));
        FileUtils.copyURLToFile(this.getClass().getResource("/" + INIT_2013_0156_FILE_NAME), new File(railsInitDirectory, INIT_2013_0156_FILE_NAME));

        System.err.println(String.format("Copying %s to %s", INIT_2013_1856_FILE_NAME, railsInitDirectory));
        FileUtils.copyURLToFile(this.getClass().getResource("/" + INIT_2013_1856_FILE_NAME), new File(railsInitDirectory, INIT_2013_1856_FILE_NAME));

        File railsHtmlScannerDirectory = new File(warExplodeDir, "WEB-INF/rails/vendor/rails/actionpack/lib/action_controller/vendor/html-scanner/html");
        System.err.println(String.format("Copying %s to %s", SANITIZER_FILE_NAME, railsHtmlScannerDirectory));
        FileUtils.copyURLToFile(this.getClass().getResource("/" + SANITIZER_FILE_NAME), new File(railsHtmlScannerDirectory, SANITIZER_FILE_NAME));
    }

    private File unzipAndGetCruiseWarFile(File cruiseWarFile) throws IOException {
        File warExplodeDir = new File(cruiseWarFile.getParentFile(), "cruise_war_explode");
        System.err.println(String.format("Unzipping %s to %s", cruiseWarFile, warExplodeDir));
        zipUtil.unzip(cruiseWarFile, warExplodeDir);
        return warExplodeDir;
    }

    private File unzipJarFileAndGetCruiseWarFile() throws IOException {
        System.err.println(String.format("Unzipping %s to %s", gojar, goJarExplodeDir));
        zipUtil.unzip(gojar, goJarExplodeDir);
        return new File(goJarExplodeDir, "defaultFiles/cruise.war");
    }

    private void backupJarFile() throws IOException {
        System.err.println(String.format("Backing up %s to %s", gojar, goJarBackUpFile));
        FileUtils.copyFile(gojar, goJarBackUpFile);
    }


    public static void main(String[] args) throws Exception {
        String goJarPath = validateAndGetArguments(args);
        new UpdateGoJar(goJarPath).executeUpdate();
    }

    private static String validateAndGetArguments(String[] args) {
        if (args.length != 1) {
            logToConsoleAndExit("Usage: java -jar go-patch-19mar2013.jar <path-to-go-jar>");
        }
        return args[0];
    }

    private static void logToConsoleAndExit(final String message) {
        System.err.println(message);
        System.exit(-1);
    }
}
