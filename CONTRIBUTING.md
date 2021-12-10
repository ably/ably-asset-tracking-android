# Contributing to the Ably Asset Tracking SDKs for Android

## Development

This repository is structured as a Gradle [Multi-Project Build](https://docs.gradle.org/current/userguide/multi_project_builds.html).

To run checks, tests and assemble all SDK and app projects from the command line use:

- macOS: `./gradlew check assemble`
- Windows: `gradle.bat check assemble`

These are the same Gradle tasks that we [run in CI](.github/workflows).

The recommended IDE for working on this project is [Android Studio](https://developer.android.com/studio).
From the dialog presented by `File` > `Open...` / `Open an Existing Project`, select the repository root folder and Studio's built-in support for Gradle projects will do the rest.

### MapBox SDK dependency

After cloning this repository for the first time, you will likely find that opening it in Android Studio or attempting to use Gradle from the command line (e.g. `./gradlew tasks`) will produce the following **FAILURE**:

    Could not get unknown property 'MAPBOX_DOWNLOADS_TOKEN' for project ':publishing-sdk' of type org.gradle.api.Project.

This is normal, and is easy to fix.

MapBox's Maps SDK for Android documentation [suggests](https://docs.mapbox.com/android/maps/overview/#configure-credentials) configuring your secret token in `~/.gradle/gradle.properties`, which makes sense as it keeps it well away from the repository itself to avoid accidental checkin.

There are, of course, [many other ways](https://docs.gradle.org/current/userguide/build_environment.html) to inject project properties into Gradle builds - all of which should work for this `MAPBOX_DOWNLOADS_TOKEN` property.

### API Keys and Access Tokens

The following secrets need configuring in a similar manner to that described above for the MapBox SDK Dependency `MAPBOX_DOWNLOADS_TOKEN`:

- `ABLY_API_KEY`
- `MAPBOX_ACCESS_TOKEN`
- `GOOGLE_MAPS_API_KEY`

### Debugging Gradle Task Dependencies

There isn't an out-of-the-box command provided by Gradle to provide a readable breakdown of which tasks in the build are configured to rely upon which other tasks. The `--dry-run` switch helps a bit, but it provides a flat view which doesn't provide the full picture.

We could have taken the option to include some Groovy code or a plugin in the root project configuration to provide a full task tree view, however it's strictly not needed to be part of the sources within this repository to build the projects as it's only a tool to help with debugging Gradle's configuration.

If such a view is required then we suggest installing [this Gradle-global, user-level init script](https://github.com/dorongold/gradle-task-tree#init-script-snippet), within `~/.gradle/init.gradle` as [described in the Gradle documentation](https://docs.gradle.org/current/userguide/init_scripts.html#sec:using_an_init_script). Once the init script is in place then, for example, the Gradle `check` task can be examined using:

    ./gradlew check taskTree --no-repeat

The `taskTree` task requires a preceding task name and can be run per project, a fact that's visible with:

    ./gradlew tasks --all | grep taskTree

## Release Process

Releases should always be made through a release pull request (PR), which needs to bump the version number and add to the [change log](CHANGELOG.md).
For an example of a previous release PR, see [#298](https://github.com/ably/ably-asset-tracking-android/pull/298).

The release process must include the following steps:

1. Ensure that all work intended for this release has landed to `main`
2. Create a release branch named like `release/1.2.3`
3. Add a commit to bump the version number - this commit should also include an update to the root `README.md`, where the latest version number is explicitly referenced
4. Add a commit to update the change log
5. Push the release branch to GitHub
6. Open a PR for the release against the release branch you just pushed
7. Gain approval(s) for the release PR from maintainer(s)
8. Land the release PR to `main`
9. Create a tag named like `v1.2.3` and push it to GitHub
10. Run the [publish workflow](https://github.com/ably/ably-asset-tracking-android/actions/workflows/publish.yml) from the `main` branch (manually triggered, where you supply the version number so the script publishes only up to that tag)

We tend to use [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator) to collate the information required for a change log update.
Your mileage may vary, but it seems the most reliable method to invoke the generator is something like:
`github_changelog_generator -u ably -p ably-asset-tracking-android --since-tag v1.0.0 --output delta.md`
and then manually merge the delta contents in to the main change log (where `v1.0.0` in this case is the tag for the previous release).
