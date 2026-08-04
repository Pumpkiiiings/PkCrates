package com.pumpkings.pkcrates;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves the plugin's runtime libraries instead of shading them into the jar.
 *
 * <p>Paper runs this before the plugin is constructed. Each artifact is looked up in the
 * server's shared {@code libraries/} cache first and only downloaded when missing, so the
 * cost is paid once per server rather than once per jar — and a second plugin depending
 * on the same artifact reuses the same file.</p>
 *
 * <p>Downloads go through the Maven resolver, which validates the published checksums.
 * A hand-rolled downloader would have to reimplement that; fetching an unverified jar and
 * putting it on the classpath is a supply-chain hole.</p>
 *
 * <h3>Adding a dependency</h3>
 * <p>Declare it here <em>and</em> as {@code compileOnly} in {@code build.gradle.kts},
 * keeping the coordinates identical. A mismatch compiles cleanly and fails at runtime
 * with {@code NoClassDefFoundError}.</p>
 *
 * <h3>Offline servers</h3>
 * <p>If the host has no outbound network access and the artifact is not already cached,
 * the plugin fails to load with an explicit resolver error. Pre-seed {@code libraries/}
 * or switch the dependency back to {@code implementation} to shade it.</p>
 */
public class PkCratesLoader implements PluginLoader {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";

    /** Must stay in sync with the compileOnly coordinates in build.gradle.kts. */
    private static final String[] RUNTIME_LIBRARIES = {
            "com.zaxxer:HikariCP:5.1.0",
            "org.xerial:sqlite-jdbc:3.46.1.3"
    };

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL).build());

        for (String coordinates : RUNTIME_LIBRARIES) {
            resolver.addDependency(new Dependency(new DefaultArtifact(coordinates), null));
        }

        classpathBuilder.addLibrary(resolver);
    }
}
