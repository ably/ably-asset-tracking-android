## Asset Tracking

This repository is structured as a Gradle [Multi-Project Build](https://docs.gradle.org/current/userguide/multi_project_builds.html).

We'll add more content here as the development evolves. For the time being this content will be focussed on those developing the code within this repository. Eventually it'll move elsewhere so that we can replace this root readme with something public facing.

### Coding Conventions and Style Guide

- Use best, current practice wherever possible.
- Kotlin is our primary development language for this project (in respect of SDK interfaces and implementation, as well as example app development):
    - We must keep in mind that some developers may choose to utilise the SDKs we build from a Java codebase (see [Calling Kotlin from Java](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html))
    - We should do our best to avoid "writing Kotlin with a Java accent":
        - published [Kotlin idioms](https://kotlinlang.org/docs/reference/idioms.html) should be utilised
        - strict linting and static analysis rules should be applied to all code, including unit and integration tests - Kotlin's Coding Conventions may be a starting point but all rules **must** fail the build when built from the command line (i.e. `./gradlew`, especially including CI / CD runs)
