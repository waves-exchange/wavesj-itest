package com.tradisys.commons.waves.itest.scm;

public interface ScmFileContentPostProcessor {
    StringBuilder modify(StringBuilder fileContent);

    static void replaceLine(String oldLinePrefix, String newLine, StringBuilder in) {
        int start = in.indexOf(oldLinePrefix);
        int end = in.indexOf("\n", start);

        if (start > 0) {
            in.replace(start, end, newLine);
        }
    }
}