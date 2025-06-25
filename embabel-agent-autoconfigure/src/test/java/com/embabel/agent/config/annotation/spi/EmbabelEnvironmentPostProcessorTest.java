/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.agent.config.annotation.spi;

import com.embabel.agent.config.annotation.AgentPlatform;
import com.embabel.agent.config.annotation.EnableAgents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbabelEnvironmentPostProcessorTest {

    private EmbabelEnvironmentPostProcessor processor;
    private MockEnvironment environment;
    private SpringApplication application;
    private String originalProfilesProperty;

    @BeforeEach
    void setUp() {
        processor = new EmbabelEnvironmentPostProcessor();
        environment = new MockEnvironment();
        application = mock(SpringApplication.class);

        // Save and clear system property
        originalProfilesProperty = System.getProperty("spring.profiles.active");
        System.clearProperty("spring.profiles.active");
    }

    @AfterEach
    void tearDown() {
        // Restore system property
        if (originalProfilesProperty != null) {
            System.setProperty("spring.profiles.active", originalProfilesProperty);
        } else {
            System.clearProperty("spring.profiles.active");
        }
    }

    @Test
    void testNoAnnotations() {
        // Given
        class TestApp {
        }
        when(application.getAllSources()).thenReturn(Set.of(TestApp.class));

        // When
        processor.postProcessEnvironment(environment, application);

        // Then
        assertThat(getAddedProfiles()).isEmpty();
        assertThat(System.getProperty("spring.profiles.active")).isNull();
    }

    @Test
    void testAgentPlatformAnnotation() {
        // Given
        @AgentPlatform("shell")
        class TestApp {
        }
        when(application.getAllSources()).thenReturn(Set.of(TestApp.class));

        // When
        processor.postProcessEnvironment(environment, application);

        // Then
        assertThat(getAddedProfiles()).containsExactly("shell");
        assertThat(System.getProperty("spring.profiles.active")).isEqualTo("shell");
    }

    @Test
    void testEnableAgentsWithLoggingTheme() {
        // Given
        @EnableAgents(loggingTheme = "starwars")
        class TestApp {
        }
        when(application.getAllSources()).thenReturn(Set.of(TestApp.class));

        // When
        processor.postProcessEnvironment(environment, application);

        // Then
        assertThat(getAddedProfiles()).containsExactly("starwars");
        assertThat(System.getProperty("spring.profiles.active")).isEqualTo("default,starwars");
    }

    @Test
    void testEnableAgentsWithLocalModels() {
        // Given
        @EnableAgents(localModels = {"ollama", "llamacpp"})
        class TestApp {
        }
        when(application.getAllSources()).thenReturn(Set.of(TestApp.class));

        // When
        processor.postProcessEnvironment(environment, application);

        // Then
        assertThat(getAddedProfiles()).containsExactlyInAnyOrder("ollama", "llamacpp", "starwars");
        assertThat(Arrays.stream(System.getProperty("spring.profiles.active").split(",")).collect(Collectors.toSet())
        ).isEqualTo(Set.of("default", "ollama", "llamacpp", "starwars"));
    }

    @Test
    void testEnableAgentsWithMcpClients() {
        // Given
        @EnableAgents(mcpServers = {"filesystem", "github"})
        class TestApp {
        }
        when(application.getAllSources()).thenReturn(Set.of(TestApp.class));

        // When
        processor.postProcessEnvironment(environment, application);

        // Then
        assertThat(getAddedProfiles()).containsExactlyInAnyOrder("filesystem", "github", "starwars");
        assertThat(Arrays.stream(System.getProperty("spring.profiles.active").split(",")).collect(Collectors.toSet())
        ).isEqualTo(Set.of("default", "filesystem", "github", "starwars"));
    }

    @Test
    void testCombinedAnnotations() {
        // Given
        @AgentPlatform("shell")
        @EnableAgents(
                loggingTheme = "starwars",
                localModels = {"ollama"},
                mcpServers = {"docker"}
        )
        class TestApp {
        }
        when(application.getAllSources()).thenReturn(Set.of(TestApp.class));

        // When
        processor.postProcessEnvironment(environment, application);

        // Then
        assertThat(getAddedProfiles())
                .containsExactlyInAnyOrder("shell", "starwars", "ollama", "docker");
        assertThat(System.getProperty("spring.profiles.active"))
                .isEqualTo("shell,starwars,ollama,docker");
    }

    @Test
    void testPreservesExistingProfiles() {
        // Given
        System.setProperty("spring.profiles.active", "existing,profiles");

        @EnableAgents(loggingTheme = "starwars")
        class TestApp {
        }
        when(application.getAllSources()).thenReturn(Set.of(TestApp.class));

        // When
        processor.postProcessEnvironment(environment, application);

        // Then
        assertThat(getAddedProfiles()).containsExactly("starwars");
        assertThat(System.getProperty("spring.profiles.active"))
                .isEqualTo("existing,profiles,default,starwars");
    }

    @Test
    void testEmptyEnableAgents() {
        // Given
        @EnableAgents(loggingTheme = "", localModels = {}, mcpServers = {})
        class TestApp {
        }
        when(application.getAllSources()).thenReturn(Set.of(TestApp.class));

        // When
        processor.postProcessEnvironment(environment, application);

        // Then
        assertThat(getAddedProfiles()).isEmpty();
        assertThat(System.getProperty("spring.profiles.active")).isEqualTo("default");
    }

    @Test
    void testHighestPrecedenceOrder() {
        assertThat(processor.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }

    /**
     * Helper method to get only the profiles added by our processor,
     * filtering out "default" profile that MockEnvironment might add.
     */
    private Set<String> getAddedProfiles() {
        return Arrays.stream(environment.getActiveProfiles())
                .filter(profile -> !"default".equals(profile))
                .collect(Collectors.toSet());
    }
}