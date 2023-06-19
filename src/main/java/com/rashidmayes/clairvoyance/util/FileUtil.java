package com.rashidmayes.clairvoyance.util;

import org.apache.commons.io.FileUtils;

import java.io.File;

public final class FileUtil {

    public static String prettyFileName(String fileName) {
        String pattern = "[^a-zA-Z0-9\\.\\-\\_]+";
        return fileName.replaceAll(pattern, "_").trim();
    }

    public static void clearCache() {
        try {
            ClairvoyanceLogger.logger.info("deleting tmp clairvoyance directory");
            var tmpRootDir = new File(System.getProperty("java.io.tmpdir"));
            var clairvoyanceTmpDir = new File(tmpRootDir, "clairvoyance");
            FileUtils.deleteDirectory(clairvoyanceTmpDir);
            ClairvoyanceLogger.logger.info("clairvoyance tmp directory has been deleted");
        } catch (Exception e) {
            ClairvoyanceLogger.logger.error(e.getMessage(), e);
        }
    }

}
