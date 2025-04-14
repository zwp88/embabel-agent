# Embabel Agent Framework

![Build](https://github.com/embabel/agent-api/actions/workflows/maven.yml/badge.svg)

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![Jinja](https://img.shields.io/badge/jinja-white.svg?style=for-the-badge&logo=jinja&logoColor=black)
![JSON](https://img.shields.io/badge/JSON-000?logo=json&logoColor=fff)
![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![SonarQube](https://img.shields.io/badge/SonarQube-black?style=for-the-badge&logo=sonarqube&logoColor=4E9BCD)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

<img align="left" src="https://github.com/embabel/agent-api/blob/main/images/315px-Meister_der_Weltenchronik_001.jpg?raw=true" width="180">

&nbsp;&nbsp;&nbsp;&nbsp;

Embabel agent programming model. Framework for authoring agentic flows that seamlessly mix LLM-prompted interactions
with manual code and domain models.

## Key Concepts

Models agentic flows in terms of:

- **Actions**: Steps an agent takes
- **Goals**: What the agent is trying to achieve
- **Conditions**: Conditions to assess before executing an action or determining that a goal has been achieved
- **Domain model**: Objects underpinning the flow and informing Actions, Goals and Conditions.
- **Plan**: A sequence of actions to achieve a goal. Plans are dynamically formulated by the system, not the programmer.

These concepts deliver the following differentiators versus other agentic systems:

- **Sophisticated planning.** Goes beyond a finite state machine or sequential execution
  with nesting by introducing a true planning step, using a
  non-LLM AI algorithm. This enables the system to perform tasks it wasn’t programmed to do by combining known
  steps in
  a novel order, as well as make decisions about parallelization and other runtime behavior.
- **Superior extensibility and reuse**: Because of dynamic planning, adding more domain objects, actions, goals and
  conditions
  can extend the capability of the system, _without editing FSM definitions._
- **Strong typing and the benefits of object orientation**: Actions, goals and conditions are informed by a domain
  model, which can
  include behavior. Everything is strongly typed and prompts and
  manually authored code interact cleanly. No more magic maps. Enjoy full refactoring support.

Other benefits:

- **Platform abstraction**: Clean separation between programming model and platform concept allow running locally while
  potentially offering higher QoS changing application code.
- **Designed for LLM mixing**: It is easy to build applications that mix LLMs, ensuring the most cost effective yet
  capable solution.
  models. This enables the system to leverage the strengths of different models for different tasks.
- **Built on Spring and the JVM,** making it easy to access existing enterprise functionality and capabilities.
  For example:
    - Spring can inject and manage agents, including using Spring AOP to decorate functions.
    - Robust persistence and transaction management solutions are available.
- **Designed for testability** from the ground up. Both unit testing and agent end to end testing are easy.

Flows can be authored in one of two ways:

- Annotation-based model similar to Spring MVC, with types annotated with `@Agent` with `@Goal`, `@Condition` and
  `@Action` methods.
- Kotlin DSL.

Either way, flows are backed by a domain model of objects that can defined behavior.

> We are working toward allowing natural language actions and goals to be deployed.

The planning step is pluggable.

The default planning approach is
GOAP: [Goal Oriented Action Planning](https://medium.com/@vedantchaudhari/goal-oriented-action-planning-34035ed40d0b).
GOAP is a popular AI planning algorithm used in gaming. It allows for dynamic decision-making and action selection based
on the current state of the world and the goals of the agent.

Goals, actions and plans are independent of GOAP. Future planning options include:

- Plans created by a reasoning model such as OpenAI o1 or DeepSeek R1.

## Show Me The Code

```kotlin
@Agent(description = "Find news based on a person's star sign")
class StarNewsFinder(
    // Services such as Horoscope are injected using Spring
    private val horoscopeService: HoroscopeService,
    private val storyCount: Int = 5,
) {

    @Action
    fun extractPerson(userInput: UserInput): StarPerson =
        // All prompts are typesafe
        PromptRunner().createObject("Create a person from this user input, extracting their name and star sign: $userInput")

    @Action
    fun retrieveHoroscope(starPerson: StarPerson) =
        Horoscope(horoscopeService.dailyHoroscope(starPerson.sign))

    // toolGroups specifies tools that are required for this action to run
    @Action(toolGroups = [ToolGroup.WEB])
    fun findNewsStories(person: StarPerson, horoscope: Horoscope): RelevantNewsStories =
        PromptRunner().createObject(
            """
            ${person.name} is an astrology believer with the sign ${person.sign}.
            Their horoscope for today is:
                <horoscope>${horoscope.summary}</horoscope>
            Given this, use web tools and generate search queries
            to find $storyCount relevant news stories summarize them in a few sentences.
            Include the URL for each story.
            Do not look for another horoscope reading or return results directly about astrology;
            find stories relevant to the reading above.

            For example:
            - If the horoscope says that they may
            want to work on relationships, you could find news stories about
            novel gifts
            - If the horoscope says that they may want to work on their career,
            find news stories about training courses.
        """.trimIndent()
        )

    // The @AchievesGoal annotation indicates that completing this action
    // achieves the given goal, so the agent can be complete
    @AchievesGoal(
        description = "Write an amusing writeup for the target person based on their horoscope and current news stories",
    )
    @Action
    fun writeup(
        person: StarPerson,
        relevantNewsStories: RelevantNewsStories,
        horoscope: Horoscope,
    ): Writeup =
        // Customize LLM call
        PromptRunner().withTemperature(1.2).createObject(
            """
            Take the following news stories and write up something
            amusing for the target person.

            Begin by summarizing their horoscope in a concise, amusing way, then
            talk about the news. End with a surprising signoff.

            ${person.name} is an astrology believer with the sign ${person.sign}.
            Their horoscope for today is:
                <horoscope>${horoscope.summary}</horoscope>
            Relevant news stories are:
            ${relevantNewsStories.items.joinToString("\n") { "- ${it.url}: ${it.summary}" }}

            Format it as Markdown with links.
        """.trimIndent()
        )

}
```

The following domain classes ensure type safety:

```kotlin
data class RelevantNewsStories(
    val items: List<NewsStory>
)

data class NewsStory(
    val url: String,

    val summary: String,
)

data class Subject(
    val name: String,
    val sign: String,
)

data class Horoscope(
    val summary: String,
)

data class FunnyWriteup(
    override val text: String,
) : HasContent
```

## Getting Started

## Environment variables

Required:

- `OPENAI_API_KEY`: For the OpenAI API

Optional:

Optional external services:

- `BRAVE_API_KEY`: For the Brave Search API. Web, news and video search is exposed if this key is available.

## Running via Spring Shell

The easiest way to run the application using using one of the scripts in the
`scripts` directory. You can start the shell with:

```bash
cd scripts
./shell.sh
````

This script will also warn of any missing environment variables.

You can also run the shell under your IDE or from the command line
using Maven directly.

This will run the application in interactive shell mode:

```bash
export SPRING_PROFILES_ACTIVE=shell,severance
mvn spring-boot:run
```

> The export is separate from the mvn command to avoid Spring Shell trying to process the profiles as a command.

Type `help` to see the available commands.

An example:

```
execute "Lynda is a Scorpio, find news for her" -p -r
```

This will look for a goal, find the star finder goal and
run the flow. `-p` will log prompts `-r` will log LLM responses.
Omit these for less verbose logging.

## Running Tests

Run the tests via Maven.

```bash
mvn test
```

This will run both unit and integration tests
but will not require an internet connection or any external services.

## Spring profiles

- `shell`: Runs agent in interactive shell.
- `severance`: [Severance](https://www.youtube.com/watch?v=xEQP4VVuyrY&ab_channel=AppleTV) specific logging. Praise
  Kier!
-

## Miscellaneous

- _Why the name Embabel?_
  The "babel" part is inspired by the story of the Tower of Babel. Embabel is intended to bring many languages to
  applications.
  "embabel" also sounds like "enable."
- Milestone names are Australian animals. Mythical animals such as "bunyip" and "yowie" are used for futures that may or
  not be implemented.
- README badges come from [here](https://github.com/Ileriayo/markdown-badges).

--------------------
(c) Embabel Software Inc 2024-2025.
