package io.quarkus.documentation.rag.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.documentation.rag.SqlAggregator;

/**
 * Aggregates multiple quarkus-rag.sql fragments from a directory tree
 * into a single SQL file with the schema preamble.
 */
@Mojo(name = "aggregate-rag", defaultPhase = LifecyclePhase.PACKAGE)
public class AggregateRagMojo extends AbstractMojo {

    @Parameter(required = true)
    private File searchRoot;

    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/quarkus-rag-data.sql")
    private File outputFile;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping RAG aggregation");
            return;
        }

        Path searchPath = searchRoot.toPath();
        if (!searchRoot.isDirectory()) {
            throw new MojoExecutionException("Search root is not a directory: " + searchRoot);
        }

        getLog().info("Aggregating RAG SQL fragments from " + searchPath);

        SqlAggregator aggregator = new SqlAggregator();
        try {
            var fragments = aggregator.findFragments(searchPath);
            getLog().info("Found " + fragments.size() + " fragment(s)");

            aggregator.aggregate(fragments, outputFile.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to aggregate RAG SQL", e);
        }

        getLog().info("Aggregated RAG SQL written to " + outputFile);
    }
}
