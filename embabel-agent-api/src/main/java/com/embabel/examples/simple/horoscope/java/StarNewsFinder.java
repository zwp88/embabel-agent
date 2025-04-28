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

import com.embabel.agent.api.annotation.*;
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


/**
 * Find news based on a person's star sign
 */
@Agent(
        description = "Find news based on a person's star sign",
        beanName = "javaStarNewsFinder",
        scan = true)
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
        return using.llm(LlmOptions.fromModel(OpenAiModels.GPT_4o)).createObjectIfPossible(
                """
                        Create a person from this user input, extracting their name:
                        %s""".formatted(userInput.getContent()),
                PersonImpl.class
        );
    }

    @Action(cost = 100.0) // Make it costly so it won't be used in a plan unless there's no other path
    public Starry makeStarry(PersonImpl person) {
        return WaitFor.formSubmission("Let's get some astrological details for " + person.getName(),
                Starry.class);
    }

    // TODO should work with the Person interface rather than PersonImpl
    @Action
    public StarPerson assembleStarPerson(PersonImpl person, Starry starry) {
        return new StarPerson(
                person.getName(),
                starry.sign()
        );
    }

    @Action
    public StarPerson extractStarPerson(UserInput userInput) {
        return using.llm(LlmOptions.fromModel(OpenAiModels.GPT_4o)).createObjectIfPossible(
                """
                        Create a person from this user input, extracting their name and star sign:
                        %s""".formatted(userInput.getContent()),
                StarPerson.class
        );
    }

    @Action
    public Horoscope retrieveHoroscope(StarPerson starPerson) {
        return new Horoscope(horoscopeService.dailyHoroscope(starPerson.sign()));
    }

    // toolGroups specifies tools that are required for this action to run
    @Action(toolGroups = {ToolGroup.WEB})
    public RelevantNewsStories findNewsStories(StarPerson person, Horoscope horoscope) {
        var prompt = """
                %s is an astrology believer with the sign %s.
                Their horoscope for today is:
                    <horoscope>%s</horoscope>
                Given this, use web tools and generate search queries
                to find %d relevant news stories summarize them in a few sentences.
                Include the URL for each story.
                Do not look for another horoscope reading or return results directly about astrology;
                find stories relevant to the reading above.
                
                For example:
                - If the horoscope says that they may
                want to work on relationships, you could find news stories about
                novel gifts
                - If the horoscope says that they may want to work on their career,
                find news stories about training courses.""".formatted(
                person.name(), person.sign(), horoscope.summary(), storyCount);

        return using.DEFAULT_LLM.createObject(prompt, RelevantNewsStories.class);
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
        var llm = LlmOptions.fromCriteria(
                ModelSelectionCriteria.firstOf(OpenAiModels.GPT_4o_MINI)
        ).withTemperature(0.9);

        var newsItems = relevantNewsStories.getItems().stream()
                .map(item -> "- " + item.getUrl() + ": " + item.getSummary())
                .collect(Collectors.joining("\n"));

        var prompt = """
                Take the following news stories and write up something
                amusing for the target person.
                
                Begin by summarizing their horoscope in a concise, amusing way, then
                talk about the news. End with a surprising signoff.
                
                %s is an astrology believer with the sign %s.
                Their horoscope for today is:
                    <horoscope>%s</horoscope>
                Relevant news stories are:
                %s
                
                Format it as Markdown with links.""".formatted(
                person.name(), person.sign(), horoscope.summary(), newsItems);
        return using.llm(llm).createObject(prompt, Writeup.class);
    }
}