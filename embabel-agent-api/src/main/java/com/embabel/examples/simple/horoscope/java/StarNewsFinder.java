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
package com.embabel.examples.simple.horoscope.java;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.config.models.OpenAiModels;
import com.embabel.agent.core.ToolGroup;
import com.embabel.agent.domain.library.PersonImpl;
import com.embabel.agent.domain.library.RelevantNewsStories;
import com.embabel.agent.domain.special.UserInput;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.model.ModelSelectionCriteria;
import com.embabel.examples.simple.horoscope.HoroscopeService;
import org.springframework.beans.factory.annotation.Value;

import java.util.stream.Collectors;

import static com.embabel.agent.api.annotation.WaitKt.fromForm;
import static com.embabel.agent.api.annotation.support.ActionReturnPromptRunnerKt.getUsingDefaultLlm;
import static com.embabel.agent.api.annotation.support.ActionReturnPromptRunnerKt.using;

/**
 * Find news based on a person's star sign
 */
//@Agent(
//        description = "Find news based on a person's star sign",
//        scan = false)
public class StarNewsFinder {

    private final HoroscopeService horoscopeService;
    private final int storyCount;

    public StarNewsFinder(
            HoroscopeService horoscopeService,
            @Value("${star-news-finder.story.count:5}") int storyCount) {
        this.horoscopeService = horoscopeService;
        this.storyCount = storyCount;
    }

    @Action
    public PersonImpl extractPerson(UserInput userInput) {
        // All prompts are typesafe
        return using(LlmOptions.fromModel(OpenAiModels.GPT_4o)).createObjectIfPossible(
                "Create a person from this user input, extracting their name:\n" +
                        userInput.getContent(),
                PersonImpl.class
        );
    }

    @Action(cost = 100.0) // Make it costly so it won't be used in a plan unless there's no other path
    public Starry makeStarry(PersonImpl person) {
        return fromForm("Let's get some astrological details for " + person.getName(),
                Starry.class);
    }

    // TODO should work with the Person interface rather than PersonImpl
    @Action
    public StarPerson assembleStarPerson(PersonImpl person, Starry starry) {
        return new StarPerson(
                person.getName(),
                starry.getSign()
        );
    }

    @Action
    public StarPerson extractStarPerson(UserInput userInput) {
        // All prompts are typesafe
        return using(LlmOptions.fromModel(OpenAiModels.GPT_4o)).createObjectIfPossible(
                "Create a person from this user input, extracting their name and star sign:\n" +
                        userInput.getContent(),
                StarPerson.class
        );
    }

    @Action
    public Horoscope retrieveHoroscope(StarPerson starPerson) {
        return new Horoscope(horoscopeService.dailyHoroscope(starPerson.getSign()));
    }

    // toolGroups specifies tools that are required for this action to run
    @Action(toolGroups = {ToolGroup.WEB})
    public RelevantNewsStories findNewsStories(StarPerson person, Horoscope horoscope) {
        return getUsingDefaultLlm().createObject(
                person.getName() + " is an astrology believer with the sign " + person.getSign() + ".\n" +
                        "Their horoscope for today is:\n" +
                        "    <horoscope>" + horoscope.getSummary() + "</horoscope>\n" +
                        "Given this, use web tools and generate search queries\n" +
                        "to find " + storyCount + " relevant news stories summarize them in a few sentences.\n" +
                        "Include the URL for each story.\n" +
                        "Do not look for another horoscope reading or return results directly about astrology;\n" +
                        "find stories relevant to the reading above.\n" +
                        "\n" +
                        "For example:\n" +
                        "- If the horoscope says that they may\n" +
                        "want to work on relationships, you could find news stories about\n" +
                        "novel gifts\n" +
                        "- If the horoscope says that they may want to work on their career,\n" +
                        "find news stories about training courses.",
                RelevantNewsStories.class
        );
    }

    // The @AchievesGoal annotation indicates that completing this action
    // achieves the given goal, so the agent can be complete
    @AchievesGoal(
            description = "Write an amusing writeup for the target person based on their horoscope and current news stories"
    )
    @Action
    public Writeup writeup(
            StarPerson person,
            RelevantNewsStories relevantNewsStories,
            Horoscope horoscope) {
        // Customize LLM call
        return using(
                LlmOptions.fromCriteria(
                        ModelSelectionCriteria.firstOf(
                                OpenAiModels.GPT_4o_MINI
                        )
                ).withTemperature(0.9)
        ).createObject(
                "Take the following news stories and write up something\n" +
                        "amusing for the target person.\n" +
                        "\n" +
                        "Begin by summarizing their horoscope in a concise, amusing way, then\n" +
                        "talk about the news. End with a surprising signoff.\n" +
                        "\n" +
                        person.getName() + " is an astrology believer with the sign " + person.getSign() + ".\n" +
                        "Their horoscope for today is:\n" +
                        "    <horoscope>" + horoscope.getSummary() + "</horoscope>\n" +
                        "Relevant news stories are:\n" +
                        relevantNewsStories.getItems().stream()
                                .map(item -> "- " + item.getUrl() + ": " + item.getSummary())
                                .collect(Collectors.joining("\n")) +
                        "\n" +
                        "Format it as Markdown with links.",
                Writeup.class
        );
    }
}