# Change log

## [1.0.0-beta.1](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.1)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-preview.2...v1.0.0-beta.1)

**Closed issues:**

- Consider creating the Ably instance with the client ID included [\#138](https://github.com/ably/ably-asset-tracking-android/issues/138)
- Remember last sent locations for each of the Trackables independently [\#125](https://github.com/ably/ably-asset-tracking-android/issues/125)
- Consider whether DefaultResolutionSet should expose a getResolution\(Boolean, Boolean\) public API [\#101](https://github.com/ably/ably-asset-tracking-android/issues/101)
- Subscriber needs a means of telling the publisher it no longer wishes to express have influence over Resolution [\#98](https://github.com/ably/ably-asset-tracking-android/issues/98)
- Consider whether single-method listeners defined using Kotlin's typealias are appropriate for Java interoperability [\#91](https://github.com/ably/ably-asset-tracking-android/issues/91)
- Subscriber: Handle threading in SDK with a proper threading strategy [\#86](https://github.com/ably/ably-asset-tracking-android/issues/86)
- Re-encapsulate more information from ably-java when reporting exceptions from Ably [\#80](https://github.com/ably/ably-asset-tracking-android/issues/80)

**Merged pull requests:**

- Readme tweaks and corrections [\#148](https://github.com/ably/ably-asset-tracking-android/pull/148) ([QuintinWillison](https://github.com/QuintinWillison))
- Add status badge for the emulate workflow [\#146](https://github.com/ably/ably-asset-tracking-android/pull/146) ([QuintinWillison](https://github.com/QuintinWillison))
- Fix wrong ably state change events [\#143](https://github.com/ably/ably-asset-tracking-android/pull/143) ([KacperKluka](https://github.com/KacperKluka))
- Use our own LocationUpdate instead of Android's Location class [\#142](https://github.com/ably/ably-asset-tracking-android/pull/142) ([KacperKluka](https://github.com/KacperKluka))
- Asynchronous Stop APIs for Publisher and Subscriber [\#141](https://github.com/ably/ably-asset-tracking-android/pull/141) ([QuintinWillison](https://github.com/QuintinWillison))
- Fix updating subscriber's resolution without passing the client ID [\#140](https://github.com/ably/ably-asset-tracking-android/pull/140) ([KacperKluka](https://github.com/KacperKluka))
- Stop sending raw location updates from Publisher to Subscriber [\#139](https://github.com/ably/ably-asset-tracking-android/pull/139) ([KacperKluka](https://github.com/KacperKluka))
- Stop navigation trip session and cleanup Mapbox when stopping the Publisher [\#137](https://github.com/ably/ably-asset-tracking-android/pull/137) ([KacperKluka](https://github.com/KacperKluka))
- Remove a check that doesn't allow to call track() method multiple times [\#136](https://github.com/ably/ably-asset-tracking-android/pull/136) ([KacperKluka](https://github.com/KacperKluka))
- Add missing android coroutines dependency [\#130](https://github.com/ably/ably-asset-tracking-android/pull/130) ([KacperKluka](https://github.com/KacperKluka))
- Keep track of last location for each of the trackables individually [\#126](https://github.com/ably/ably-asset-tracking-android/pull/126) ([KacperKluka](https://github.com/KacperKluka))
- Add basic integration test for Publisher [\#123](https://github.com/ably/ably-asset-tracking-android/pull/123) ([KacperKluka](https://github.com/KacperKluka))
- Allow Subscriber to cancel its requested resolution [\#121](https://github.com/ably/ably-asset-tracking-android/pull/121) ([KacperKluka](https://github.com/KacperKluka))
- Change getResolution method from DefaultResoultionSet to an internal method [\#120](https://github.com/ably/ably-asset-tracking-android/pull/120) ([KacperKluka](https://github.com/KacperKluka))
- Add integration testing app [\#119](https://github.com/ably/ably-asset-tracking-android/pull/119) ([QuintinWillison](https://github.com/QuintinWillison))
- Create custom location engines that allow to change resolution while working [\#117](https://github.com/ably/ably-asset-tracking-android/pull/117) ([KacperKluka](https://github.com/KacperKluka))
- Replace Kotlin functions with custom interfaces in Publisher and Subscriber APIs [\#114](https://github.com/ably/ably-asset-tracking-android/pull/114) ([KacperKluka](https://github.com/KacperKluka))
- Handle routing profile \(transportation mode\) in Publisher SDK [\#113](https://github.com/ably/ably-asset-tracking-android/pull/113) ([KacperKluka](https://github.com/KacperKluka))
- Add jacoco tool for test coverage and update reports [\#112](https://github.com/ably/ably-asset-tracking-android/pull/112) ([KacperKluka](https://github.com/KacperKluka))
- Add tests for DefaultResolutionPolicy [\#111](https://github.com/ably/ably-asset-tracking-android/pull/111) ([KacperKluka](https://github.com/KacperKluka))
- Conform Readme [\#109](https://github.com/ably/ably-asset-tracking-android/pull/109) ([QuintinWillison](https://github.com/QuintinWillison))
- Add threading policy to Subscriber SDK [\#108](https://github.com/ably/ably-asset-tracking-android/pull/108) ([KacperKluka](https://github.com/KacperKluka))
- Treat warnings as errors [\#99](https://github.com/ably/ably-asset-tracking-android/pull/99) ([QuintinWillison](https://github.com/QuintinWillison))

## [1.0.0-preview.2](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-preview.2)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-preview.1...v1.0.0-preview.2)

**Closed issues:**

- Remove Asset prefix from Subscriber interfaces [\#72](https://github.com/ably/ably-asset-tracking-android/issues/72)
- Add Android OS support to readme [\#70](https://github.com/ably/ably-asset-tracking-android/issues/70)
- Remove Ably prefix from the AblyConfiguration interface [\#69](https://github.com/ably/ably-asset-tracking-android/issues/69)
- Bring in Amplify Configuration for S3 from repository secret [\#65](https://github.com/ably/ably-asset-tracking-android/issues/65)
- Handle threading in SDK with a proper threading strategy [\#22](https://github.com/ably/ably-asset-tracking-android/issues/22)
- Add proper error handling to AssetPublisher SDK [\#17](https://github.com/ably/ably-asset-tracking-android/issues/17)

**Merged pull requests:**

- Add resolution examples to subscriber and publisher example apps [\#105](https://github.com/ably/ably-asset-tracking-android/pull/105) ([KacperKluka](https://github.com/KacperKluka))
- Broadcast desired resolution from the Subscribers [\#100](https://github.com/ably/ably-asset-tracking-android/pull/100) ([KacperKluka](https://github.com/KacperKluka))
- Default resolution policy implementation [\#97](https://github.com/ably/ably-asset-tracking-android/pull/97) ([KacperKluka](https://github.com/KacperKluka))
- Remove misleading commentary from default Proximity API [\#96](https://github.com/ably/ably-asset-tracking-android/pull/96) ([QuintinWillison](https://github.com/QuintinWillison))
- Refine Proximity API [\#95](https://github.com/ably/ably-asset-tracking-android/pull/95) ([QuintinWillison](https://github.com/QuintinWillison))
- Conform subscriber API commentary [\#94](https://github.com/ably/ably-asset-tracking-android/pull/94) ([QuintinWillison](https://github.com/QuintinWillison))
- Resolution requests API in the subscriber SDK [\#93](https://github.com/ably/ably-asset-tracking-android/pull/93) ([QuintinWillison](https://github.com/QuintinWillison))
- Restore Java tests [\#90](https://github.com/ably/ably-asset-tracking-android/pull/90) ([QuintinWillison](https://github.com/QuintinWillison))
- Bring AWS Amplify configuration file in from repository secret [\#89](https://github.com/ably/ably-asset-tracking-android/pull/89) ([QuintinWillison](https://github.com/QuintinWillison))
- Refactor resolution computation callbacks [\#88](https://github.com/ably/ably-asset-tracking-android/pull/88) ([QuintinWillison](https://github.com/QuintinWillison))
- Handle remaining Publisher actions with events queue [\#85](https://github.com/ably/ably-asset-tracking-android/pull/85) ([KacperKluka](https://github.com/KacperKluka))
- Remove unnecessary Mapbox's location engine listener [\#83](https://github.com/ably/ably-asset-tracking-android/pull/83) ([KacperKluka](https://github.com/KacperKluka))
- Resolution Policy API additions [\#78](https://github.com/ably/ably-asset-tracking-android/pull/78) ([QuintinWillison](https://github.com/QuintinWillison))
- Implement new threading solution [\#77](https://github.com/ably/ably-asset-tracking-android/pull/77) ([KacperKluka](https://github.com/KacperKluka))
- Rename Ably configuration to connection configuration [\#76](https://github.com/ably/ably-asset-tracking-android/pull/76) ([KacperKluka](https://github.com/KacperKluka))
- Remove Asset prefix from Subscriber SDK [\#75](https://github.com/ably/ably-asset-tracking-android/pull/75) ([KacperKluka](https://github.com/KacperKluka))
- Change min SDK version to 21 and add README info [\#74](https://github.com/ably/ably-asset-tracking-android/pull/74) ([KacperKluka](https://github.com/KacperKluka))
- Add upload artifact steps to the assemble workflow [\#71](https://github.com/ably/ably-asset-tracking-android/pull/71) ([QuintinWillison](https://github.com/QuintinWillison))
- Add tracked asset destination support [\#67](https://github.com/ably/ably-asset-tracking-android/pull/67) ([KacperKluka](https://github.com/KacperKluka))
- Add support for multiple assets tracking [\#60](https://github.com/ably/ably-asset-tracking-android/pull/60) ([KacperKluka](https://github.com/KacperKluka))
