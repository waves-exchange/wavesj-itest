package com.tradisys.commons.waves.itest.scm;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GitHubFileExtractor implements ScmFileExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubFileExtractor.class);

    static final String GITHUB_URL_TEMPLATE = "https://raw.githubusercontent.com/${project}/${branch}/${filePath}";

    private String project;
    private String branch;

    private Map<ScmFileMeta, List<ScmFileContentPostProcessor>> postProcessorsMap;
    private Map<ScmFileMeta, String> modifiedScriptsCache = new HashMap<>();

    public GitHubFileExtractor(String project, String branch) {
        this.project = project;
        this.branch = branch;
        this.postProcessorsMap = new HashMap<>();
    }

    public void addPostProcessor(ScmFileMeta scmFileMeta, ScmFileContentPostProcessor ... postProcessor) {
        List<ScmFileContentPostProcessor> postProcessorsList =
                postProcessorsMap.computeIfAbsent(scmFileMeta, k -> new ArrayList<>(10));
        postProcessorsList.addAll(Arrays.asList(postProcessor));
    }

    public void addPostProcessorToAll(ScmFileContentPostProcessor ... postProcessor) {
        postProcessorsMap.values()
                .forEach(list -> list.addAll(Arrays.asList(postProcessor)));
    }

    @Override
    public String read(ScmFileMeta scmFileMeta) throws IOException {
        return getPostProcessedFromCacheOrRead(scmFileMeta);
    }

    private String getPostProcessedFromCacheOrRead(ScmFileMeta scmFileMeta) throws IOException {
        String modifiedScript = modifiedScriptsCache.get(scmFileMeta);

        if (modifiedScript == null) {
            synchronized (this) {
                // reread
                modifiedScript = modifiedScriptsCache.get(scmFileMeta);
                if (modifiedScript == null) {
                    StringBuilder builder = new StringBuilder(getScriptOrDownload(branch, scmFileMeta));

                    for (ScmFileContentPostProcessor p: postProcessorsMap.getOrDefault(scmFileMeta, Collections.emptyList())) {
                        builder = p.modify(builder);
                    }
                    modifiedScript = builder.toString();
                    modifiedScriptsCache.put(scmFileMeta, modifiedScript);
                }
            }
        }

        return modifiedScript;
    }

    protected String getScriptOrDownload(String branch, ScmFileMeta scmFileMeta) throws IOException {
        return downloadScript(branch, scmFileMeta);
    }

    protected String downloadScript(String branch, ScmFileMeta scmFileMeta) throws IOException {
        String urlStr = GITHUB_URL_TEMPLATE.replace("${project}", project)
                .replace("${branch}", branch)
                .replace("${filePath}", scmFileMeta.getFileName());
        return IOUtils.toString(new URL(urlStr), StandardCharsets.UTF_8);
    }
}