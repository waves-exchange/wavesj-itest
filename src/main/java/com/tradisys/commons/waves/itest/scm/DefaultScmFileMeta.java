package com.tradisys.commons.waves.itest.scm;

public class DefaultScmFileMeta implements ScmFileMeta {

    private String fileName;

    public DefaultScmFileMeta(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getFileName() {
        return fileName;
    }
}
