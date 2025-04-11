# Embabel Agent programming model

Agent API

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

Embabel agent programming model.

## Environment variables

Required:

- `OPENAI_API_KEY`: For the OpenAI API

Optional:

Optional external services:

- `BRAVE_API_KEY`: For the Brave Search API. Web, news and video search is exposed if this key is available.

## Running via Spring Shell

You can run the application under your IDE or from the command line
using Maven.

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

You can run the tests using Maven. This will run both unit and integration tests
but will not require an internet connection or any external services.

```bash
mvn test
```

## Spring profiles

- `shell`: Runs agent in interactive shell.
- `severance`: [Severance](https://www.youtube.com/watch?v=xEQP4VVuyrY&ab_channel=AppleTV) specific logging.
- Praise Kier!