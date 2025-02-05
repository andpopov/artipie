/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.cactoos.text.Base64Encoded;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.BindMode;

/**
 * Integration tests for Gem repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @since 0.13
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class GemITCase {

    /**
     * Rails gem.
     */
    private static final String RAILS = "rails-6.0.2.2.gem";

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("gem/gem.yml", "my-gem")
            .withRepoConfig("gem/gem-port.yml", "my-gem-port")
            .withExposedPorts(8081),
        () -> new TestDeployment.ClientContainer("ruby:2.7.2")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "gem/rails-6.0.2.2.gem", "/w/rails-6.0.2.2.gem", BindMode.READ_ONLY
            )
    );

    @ParameterizedTest
    @CsvSource({
        "8080,my-gem",
        "8081,my-gem-port"
    })
    void gemPushAndInstallWorks(final String port, final String repo) throws IOException {
        this.containers.assertExec(
            "Packages was not pushed",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(
                    new ListOf<String>(
                        String.format("POST http://artipie:%s/%s/api/v1/gems", port, repo),
                        "201 Created"
                    )
                )
            ),
            "env", String.format("GEM_HOST_API_KEY=%s", new Base64Encoded("any:any").asString()),
            "gem", "push", "-v", "/w/rails-6.0.2.2.gem", "--host",
            String.format("http://artipie:%s/%s", port, repo)
        );
        this.containers.assertArtipieContent(
            "Package was not added to storage",
            String.format("/var/artipie/data/%s/gems/%s", repo, GemITCase.RAILS),
            new IsEqual<>(new TestResource(String.format("gem/%s", GemITCase.RAILS)).asBytes())
        );
        this.containers.assertExec(
            "rubygems.org was not removed from sources",
            new ContainerResultMatcher(),
            "gem", "sources", "--remove", "https://rubygems.org/"
        );
        this.containers.assertExec(
            "Package was not installed",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(
                    new ListOf<String>(
                        String.format(
                            "GET http://artipie:%s/%s/quick/Marshal.4.8/%sspec.rz",
                            port, repo, GemITCase.RAILS
                        ),
                        "200 OK",
                        "Successfully installed rails-6.0.2.2",
                        "1 gem installed"
                    )
                )
            ),
            "gem", "install", GemITCase.RAILS,
            "--source", String.format("http://artipie:%s/%s", port, repo),
            "--ignore-dependencies", "-V"
        );
    }

}
