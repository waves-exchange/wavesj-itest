package com.tradisys.commons.waves.itest.scm;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubFileExtractor implements ScmFileExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubFileExtractor.class);

    static final String GITHUB_URL_TEMPLATE = "https://raw.githubusercontent.com/${project}/${branch}/${filePath}";
    static final String GITHUB_WEB_TEMPLATE = "https://github.com/${project}/blob/${branch}/${filePath}";
    static final String COMMIT_TEMPLATE = "href=\"/%1$s/blob/([0-9,a-f]{40})/%2$s";
    public static final Pattern REVISION_PATTERN = Pattern.compile("\\s*let\\s+revisionNum\\s*=\\s*\".*\"");

    private String project;
    private String branch;
    private String hash;

    private Map<ScmFileMeta, List<ScmFileContentPostProcessor>> postProcessorsMap;
    private Map<ScmFileMeta, String> modifiedScriptsCache = new HashMap<>();

    public GitHubFileExtractor(String project, String branch) {
        this.project = project;
        this.branch = branch;
        this.postProcessorsMap = new HashMap<>();
    }

    /**
     * Add a list of postprocessors to single file
     * @param scmFileMeta File to postprocess
     * @param postProcessor Array of postprocessors
     */
    public void addPostProcessor(ScmFileMeta scmFileMeta, ScmFileContentPostProcessor ... postProcessor) {
        List<ScmFileContentPostProcessor> postProcessorsList =
                postProcessorsMap.computeIfAbsent(scmFileMeta, k -> new ArrayList<>(10));
        postProcessorsList.addAll(Arrays.asList(postProcessor));
    }

    /**
     * Add the same list of postprocessors to every file
     * @param postProcessor Array of postprocessors
     */
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
                    builder = replaceRevision(builder);
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

    private StringBuilder replaceRevision(StringBuilder content) {
        ScmFileContentPostProcessor.replaceLine("let revisionNum", String.format("let revisionNum = \"%s\"", hash), content);
        return content;
    }

    protected String getScriptOrDownload(String branch, ScmFileMeta scmFileMeta) throws IOException {
        hash = getCommitId(branch, scmFileMeta);
        return downloadScript(branch, scmFileMeta);
    }

    protected String downloadScript(String branch, ScmFileMeta scmFileMeta) throws IOException {
        String urlStr = GITHUB_URL_TEMPLATE.replace("${project}", project)
                .replace("${branch}", branch)
                .replace("${filePath}", scmFileMeta.getFileName());
        return IOUtils.toString(new URL(urlStr), StandardCharsets.UTF_8);
    }

    private String getCommitId(String branch, ScmFileMeta scmFileMeta) throws IOException {
        String urlStr = GITHUB_WEB_TEMPLATE.replace("${project}", project)
                .replace("${branch}", branch)
                .replace("${filePath}", scmFileMeta.getFileName());
        String s = IOUtils.toString(new URL(urlStr), StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile(String.format(COMMIT_TEMPLATE, project, scmFileMeta.getFileName()));
        Matcher matcher = pattern.matcher(s);
        return matcher.find() ? matcher.group(1) : "";
    }
}