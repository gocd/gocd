import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    private ZipEntryHandler zipEntryHandler = null;

    public ZipUtil() {
    }

    public ZipUtil(ZipEntryHandler zipEntryHandler) {
        this.zipEntryHandler = zipEntryHandler;
    }

    public File zip(File source, File destZipFile, int level) throws IOException {
        zipContents(source, new FileOutputStream(destZipFile), level, false);
        return destZipFile;
    }

    public File zipFolderContents(File source, File destZipFile, int level) throws IOException {
        zipContents(source, new FileOutputStream(destZipFile), level, true);
        return destZipFile;
    }

    public void zipFolderContents(File destDir, File destZipFile) throws IOException {
        zipFolderContents(destDir, destZipFile, Deflater.BEST_SPEED);
    }

    public void zip(File file, OutputStream output, int level) throws IOException {
        zipContents(file, output, level, false);
    }

    private void zipContents(File file, OutputStream output, int level, boolean excludeRootDir) throws IOException {
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(new BufferedOutputStream(output));
            zip.setLevel(level);
            addToZip(new ZipPath(), file, zip, excludeRootDir);
            zip.flush();
        } finally {
            if (zip != null) {
                zip.close();
            }
        }
    }

    private void addFolderToZip(ZipPath path, File source, ZipOutputStream zip, boolean excludeRootDir) throws IOException {
        ZipPath newPath = path.with(source);
        if (source.isFile()) {
            addToZip(newPath, source, zip, false);
        } else {
            addDirectory(path, source, zip, excludeRootDir);
        }
    }

    private void addDirectory(ZipPath path, File source, ZipOutputStream zip, boolean excludeRootDir) throws IOException {
        if (excludeRootDir) {
            addDirContents(path, source, zip);
            return;
        }
        ZipPath newPath = path.with(source);
        zip.putNextEntry(newPath.asZipEntryDirectory());
        addDirContents(newPath, source, zip);
    }

    private void addDirContents(ZipPath path, File source, ZipOutputStream zip) throws IOException {
        for (File file : source.listFiles()) {
            addToZip(path, file, zip, false);
        }
    }

    private void addToZip(ZipPath path, File srcFile, ZipOutputStream zip, boolean excludeRootDir) throws IOException {
        if (srcFile.isDirectory()) {
            addFolderToZip(path, srcFile, zip, excludeRootDir);
        } else {
            byte[] buff = new byte[4096];
            BufferedInputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(new FileInputStream(srcFile));
                zip.putNextEntry(path.with(srcFile).asZipEntry());
                int len;
                while ((len = inputStream.read(buff)) > 0) {
                    zip.write(buff, 0, len);
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
    }

    public void unzip(ZipInputStream zipInputStream, File destDir) throws IOException {
        destDir.mkdirs();
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            extractTo(zipEntry, zipInputStream, destDir);
            zipEntry = zipInputStream.getNextEntry();
        }
        IOUtils.closeQuietly(zipInputStream);
    }

    public void unzip(File zip, File destDir) throws IOException {
        unzip(new ZipInputStream(new BufferedInputStream(new FileInputStream(zip))), destDir);
    }

    private void extractTo(ZipEntry entry, InputStream entryInputStream, File toDir) throws IOException {
        String entryName = nonRootedEntryName(entry);

        File outputFile = new File(toDir, entryName);
        if (isDirectory(entryName)) {
            outputFile.mkdirs();
            return;
        }
        FileOutputStream os = null;
        try {
            outputFile.getParentFile().mkdirs();
            os = new FileOutputStream(outputFile);
            IOUtils.copyLarge(entryInputStream, os);
            if (zipEntryHandler != null) {
                FileInputStream stream = null;
                try {
                    stream = new FileInputStream(outputFile);
                    zipEntryHandler.handleEntry(entry, stream);
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private String nonRootedEntryName(ZipEntry entry) {
        String entryName = entry.getName();
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }
        return entryName;
    }

    private boolean isDirectory(String zipName) {
        return zipName.endsWith("/");
    }

    private class ZipPath {
        private final String path;

        ZipPath() {
            this.path = "";
        }

        private ZipPath(ZipPath old, File file) {
            String prefix = old.path.equals("") ? "" : old.path + "/";
            this.path = prefix + file.getName();
        }

        public ZipPath with(File file) {
            return new ZipPath(this, file);
        }

        public ZipEntry asZipEntry() {
            return new ZipEntry(path);
        }

        public ZipEntry asZipEntryDirectory() {
            return new ZipEntry(path + "/");
        }
    }

    public static interface ZipEntryHandler {
        public void handleEntry(ZipEntry entry, InputStream stream) throws IOException;
    }
}
