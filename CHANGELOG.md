# Change log

## [1.6.1](https://github.com/ably/ably-asset-tracking-android/tree/v1.6.1)

This release adds additional enhancements to operational stability, for publishers in particular, when running on a devices with unreliable network connectivity.

Please see:

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.6.0...v1.6.1)

**Implemented enhancements:**

- Wait for Ably connection to leave the "suspended" state before performing operations on Ably [\#973](https://github.com/ably/ably-asset-tracking-android/issues/973)
- Make publisher.track\(\) and publisher.add\(\) return instantly [\#966](https://github.com/ably/ably-asset-tracking-android/issues/966)

**Fixed bugs:**

- Handle presence.enter\(\) retries in a special way [\#972](https://github.com/ably/ably-asset-tracking-android/issues/972)
- Remove `connect()` and `attach()` timeouts from `DefaultAbly` [\#948](https://github.com/ably/ably-asset-tracking-android/issues/948)
- Non-fatal errors responses for `presence.enter()` cause Publisher to throw exceptions [\#907](https://github.com/ably/ably-asset-tracking-android/issues/907)
- Publisher can get into a bad state if offline for \> 2 minutes [\#906](https://github.com/ably/ably-asset-tracking-android/issues/906)
- Publisher apps reporting "Timeout was thrown when waiting for channel to attach" [\#859](https://github.com/ably/ably-asset-tracking-android/issues/859)

**Closed issues:**

- Flakey test: faultBeforeAddingTrackable\[DisconnectWithFailedResume\] [\#961](https://github.com/ably/ably-asset-tracking-android/issues/961)
- Presence operations are invalidly reattempted after a failed resume [\#951](https://github.com/ably/ably-asset-tracking-android/issues/951)
- Add `Publisher.start()` and `Publisher.stop()` coverage to `NetworkConnectivityTest` [\#939](https://github.com/ably/ably-asset-tracking-android/issues/939)
- Adding trackable just before fallback reconnection results in multiple exceptions [\#863](https://github.com/ably/ably-asset-tracking-android/issues/863)

**Merged pull requests:**

- Increase ably-java core version to 1.2.24 [\#982](https://github.com/ably/ably-asset-tracking-android/pull/982) ([ikbalkaya](https://github.com/ikbalkaya))
- 973 check channel state [\#981](https://github.com/ably/ably-asset-tracking-android/pull/981) ([davyskiba](https://github.com/davyskiba))
- Configure the Subscribing Example Project for Firebase App Distribution [\#979](https://github.com/ably/ably-asset-tracking-android/pull/979) ([QuintinWillison](https://github.com/QuintinWillison))
- Fix minor issue in the adhoc example app publishing workflow [\#978](https://github.com/ably/ably-asset-tracking-android/pull/978) ([QuintinWillison](https://github.com/QuintinWillison))
- Make some fixes and improvements to recent `DefaultAbly` test changes [\#977](https://github.com/ably/ably-asset-tracking-android/pull/977) ([lawrence-forooghian](https://github.com/lawrence-forooghian))
- Add pointers to sites that can be used to verify Maven Central releases [\#976](https://github.com/ably/ably-asset-tracking-android/pull/976) ([QuintinWillison](https://github.com/QuintinWillison))
- Add workflow to allow us to adhoc-publish the example apps [\#969](https://github.com/ably/ably-asset-tracking-android/pull/969) ([QuintinWillison](https://github.com/QuintinWillison))
- Remove timeouts from attach and connect [\#965](https://github.com/ably/ably-asset-tracking-android/pull/965) ([KacperKluka](https://github.com/KacperKluka))

## [1.6.0](https://github.com/ably/ably-asset-tracking-android/tree/v1.6.0)

This release enhances operational stability, for publishers in particular, when running on a devices with unreliable network connectivity.

Please see:
[Upgrade / Migration Guide from v.1.5.1](https://github.com/ably/ably-asset-tracking-android/blob/main/UPDATING.md#version-151-to-160)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.5.1...v1.6.0)

**Implemented enhancements:**

- Upgrade to Mapbox Nav SDK 2.10.0 [\#882](https://github.com/ably/ably-asset-tracking-android/issues/882)
- Consider supporting static token authentication [\#730](https://github.com/ably/ably-asset-tracking-android/issues/730)

**Fixed bugs:**

- Exceptions thrown for new publishing requests when offline [\#871](https://github.com/ably/ably-asset-tracking-android/issues/871)
- Retry behaviour improvements for Ably API calls [\#927](https://github.com/ably/ably-asset-tracking-android/issues/927)
- `Publisher.remove\(\)` fails during several connectivity faults [\#905](https://github.com/ably/ably-asset-tracking-android/issues/905)
- Adding a trackable stalls forever if presence.enter\(\) is interrupted by a disconnection [\#896](https://github.com/ably/ably-asset-tracking-android/issues/896)
- Publisher crashes when location data has NaN value [\#861](https://github.com/ably/ably-asset-tracking-android/issues/861)
- Subscriber, upon losing connectivity, continues to show the Publisher as online [\#835](https://github.com/ably/ably-asset-tracking-android/issues/835)
- Subscriber, upon losing connectivity, continues to show the Publisher as online [\#833](https://github.com/ably/ably-asset-tracking-android/issues/833)
- Unexpected exceptions fail the worker queue and silently break the SDK [\#830](https://github.com/ably/ably-asset-tracking-android/issues/830)
- Fix the logic responsible for deciding if an enhanced location update is predicted [\#828](https://github.com/ably/ably-asset-tracking-android/issues/828)
- Java users cannot build a publisher due to type issue [\#826](https://github.com/ably/ably-asset-tracking-android/issues/826)
- NPE from ably-java SDK [\#809](https://github.com/ably/ably-asset-tracking-android/issues/809)

**Closed issues:**

- Investigate TODO comment in `DropAction` fault regarding "limit" [\#934](https://github.com/ably/ably-asset-tracking-android/issues/934)
- Reduce publisher location check polling interval [\#946](https://github.com/ably/ably-asset-tracking-android/issues/946)
- Flakey test: com.ably.tracking.publisher.NetworkConnectivityTests \> faultDuringTracking\[NullTransportFault\] [\#943](https://github.com/ably/ably-asset-tracking-android/issues/943)
- Make `NetworkConnectivityTests` verify expected side-effects of operations publisher SDK claims was successful [\#925](https://github.com/ably/ably-asset-tracking-android/issues/925)
- Reduce complexity of state transition assertions in `NetworkConnectivityTests` [\#901](https://github.com/ably/ably-asset-tracking-android/issues/901)
- `connect - when channel fetched is in DETACHED state and attach fails` causes emulator to hang [\#900](https://github.com/ably/ably-asset-tracking-android/issues/900)
- `createAndStartPublisherAndSubscriberAndWaitUntilDataEnds` IndexOutOfBoundsException  [\#899](https://github.com/ably/ably-asset-tracking-android/issues/899)
- Flakey test: `when an unexpected exception is thrown by worker's async work, the queue should call worker's unexpected async exception method` [\#888](https://github.com/ably/ably-asset-tracking-android/issues/888)
- Investigate documented `ConnectionException` thrown by `Publisher.Builder.start` and how users are meant to handle it [\#876](https://github.com/ably/ably-asset-tracking-android/issues/876)
- Investigate `ConnectionException` thrown by `Publisher.stop` and whether we can remove it [\#873](https://github.com/ably/ably-asset-tracking-android/issues/873)
- Try simulating networking problems during core use cases [\#865](https://github.com/ably/ably-asset-tracking-android/issues/865)
- Sending presence leave event times out whilst still connected [\#862](https://github.com/ably/ably-asset-tracking-android/issues/862)
- `shouldNotEmitPublisherPresenceFalseIfPublisherIsPresentFromTheStart` \(`PublisherAndSubscriberTests`\) failing with "first publisherPresence value should be true" [\#845](https://github.com/ably/ably-asset-tracking-android/issues/845)
- `staticTokenAuthenticationShouldCreateWorkingConnectionBetweenPublisherAndSubscriber` \(`AuthenticationTests`\) failing with "Expectation 'subscriber received a location update' unfulfilled." [\#844](https://github.com/ably/ably-asset-tracking-android/issues/844)
- Replace deprecated `package` attribute in manifest file [\#837](https://github.com/ably/ably-asset-tracking-android/issues/837)
- Test issue for sync [\#834](https://github.com/ably/ably-asset-tracking-android/issues/834)
- Enable logging in the example apps by default [\#818](https://github.com/ably/ably-asset-tracking-android/issues/818)
- Update workflows to stop using the `set-output` command [\#817](https://github.com/ably/ably-asset-tracking-android/issues/817)
- Update workflows to stop using Node.js 12 actions [\#816](https://github.com/ably/ably-asset-tracking-android/issues/816)
- Refactor Publisher EventQueue to match Subscriber [\#781](https://github.com/ably/ably-asset-tracking-android/issues/781)
- Consider if we should allow to create multiple publisher instances [\#464](https://github.com/ably/ably-asset-tracking-android/issues/464)
- Flakey Test: `createAndStartPublisherAndSubscriberAndWaitUntilDataEnds` \(`PublisherAndSubscriberTests`\) [\#259](https://github.com/ably/ably-asset-tracking-android/issues/259)

**Merged pull requests:**

- 871 add exceptions [\#938](https://github.com/ably/ably-asset-tracking-android/pull/938) ([davyskiba](https://github.com/davyskiba))
- Use random trackable ids in integration tests [\#968](https://github.com/ably/ably-asset-tracking-android/pull/968) ([AndyTWF](https://github.com/AndyTWF))
- `NetworkingConnectivityTest` fixes [\#960](https://github.com/ably/ably-asset-tracking-android/pull/960) ([jaley](https://github.com/jaley))
- Example App Distribution documentation [\#959](https://github.com/ably/ably-asset-tracking-android/pull/959) ([QuintinWillison](https://github.com/QuintinWillison))
- Release/1.6.0 beta.1 [\#956](https://github.com/ably/ably-asset-tracking-android/pull/956) ([QuintinWillison](https://github.com/QuintinWillison))
- Upload Publishing Example App to Firebase for Distribution to Testers [\#955](https://github.com/ably/ably-asset-tracking-android/pull/955) ([QuintinWillison](https://github.com/QuintinWillison))
- Fix `publish.stop\(\)` hang when no trackables added [\#952](https://github.com/ably/ably-asset-tracking-android/pull/952) ([jaley](https://github.com/jaley))
- Increase back-off when querying history in test [\#949](https://github.com/ably/ably-asset-tracking-android/pull/949) ([AndyTWF](https://github.com/AndyTWF))
- Reenable disconnect failed resume faults [\#947](https://github.com/ably/ably-asset-tracking-android/pull/947) ([ikbalkaya](https://github.com/ikbalkaya))
- Change publisher and subscriber stop operation to always succeed [\#945](https://github.com/ably/ably-asset-tracking-android/pull/945) ([KacperKluka](https://github.com/KacperKluka))
- Fix flakey publisher test [\#944](https://github.com/ably/ably-asset-tracking-android/pull/944) ([AndyTWF](https://github.com/AndyTWF))
- Change publisher's remove operation to always succeed [\#942](https://github.com/ably/ably-asset-tracking-android/pull/942) ([KacperKluka](https://github.com/KacperKluka))
- Reset fault state for each test function [\#941](https://github.com/ably/ably-asset-tracking-android/pull/941) ([jaley](https://github.com/jaley))
- Upgrade core Ably SDK dependency to version `1.2.23` [\#940](https://github.com/ably/ably-asset-tracking-android/pull/940) ([QuintinWillison](https://github.com/QuintinWillison))
- Switch to CIO ktor engine [\#937](https://github.com/ably/ably-asset-tracking-android/pull/937) ([jaley](https://github.com/jaley))
- Improve testing in respect of JVM version underlying Gradle [\#936](https://github.com/ably/ably-asset-tracking-android/pull/936) ([QuintinWillison](https://github.com/QuintinWillison))
- setting log level for test location source AblyRealtime instance [\#935](https://github.com/ably/ably-asset-tracking-android/pull/935) ([davyskiba](https://github.com/davyskiba))
- Improve `NetworkingConnectivityTest` assertions [\#933](https://github.com/ably/ably-asset-tracking-android/pull/933) ([jaley](https://github.com/jaley))
- Fix swapped error codes when creating the malformed message exception [\#930](https://github.com/ably/ably-asset-tracking-android/pull/930) ([KacperKluka](https://github.com/KacperKluka))
- Add methods for detecting retriable and fatal Ably exceptions [\#929](https://github.com/ably/ably-asset-tracking-android/pull/929) ([KacperKluka](https://github.com/KacperKluka))
- 901 simplify transition assertions [\#916](https://github.com/ably/ably-asset-tracking-android/pull/916) ([davyskiba](https://github.com/davyskiba))
- Upgrade build tools to make Layer 7 proxy branch work [\#915](https://github.com/ably/ably-asset-tracking-android/pull/915) ([QuintinWillison](https://github.com/QuintinWillison))
- Write further unit tests for `DefaultAbly` [\#914](https://github.com/ably/ably-asset-tracking-android/pull/914) ([lawrence-forooghian](https://github.com/lawrence-forooghian))
- wait for publisher emissions before performing test assertion [\#911](https://github.com/ably/ably-asset-tracking-android/pull/911) ([AndyTWF](https://github.com/AndyTWF))
- Update workflow status badges [\#904](https://github.com/ably/ably-asset-tracking-android/pull/904) ([QuintinWillison](https://github.com/QuintinWillison))
- Add mapbox testing information to contributing guide [\#895](https://github.com/ably/ably-asset-tracking-android/pull/895) ([AndyTWF](https://github.com/AndyTWF))
- Wrap `ably-java`’s `ChannelStateChange` in our own type [\#894](https://github.com/ably/ably-asset-tracking-android/pull/894) ([lawrence-forooghian](https://github.com/lawrence-forooghian))
- Publisher and Subscriber builders start method throws exception descr… [\#893](https://github.com/ably/ably-asset-tracking-android/pull/893) ([davyskiba](https://github.com/davyskiba))
- Use different locations in test data file [\#892](https://github.com/ably/ably-asset-tracking-android/pull/892) ([AndyTWF](https://github.com/AndyTWF))
- Add timeout on verification in WorkerQueueTest [\#890](https://github.com/ably/ably-asset-tracking-android/pull/890) ([AndyTWF](https://github.com/AndyTWF))
- Document Java version requirements for running `./gradlew` [\#889](https://github.com/ably/ably-asset-tracking-android/pull/889) ([lawrence-forooghian](https://github.com/lawrence-forooghian))
- coroutines and coroutines-test dependency updated to 1.6.4 [\#887](https://github.com/ably/ably-asset-tracking-android/pull/887) ([davyskiba](https://github.com/davyskiba))
- Layer 7 Proxy for Network Connectivity Tests [\#886](https://github.com/ably/ably-asset-tracking-android/pull/886) ([jaley](https://github.com/jaley))
- Subscriber gets immediate publisher online state flakey test [\#884](https://github.com/ably/ably-asset-tracking-android/pull/884) ([QuintinWillison](https://github.com/QuintinWillison))
- Upgrade Mapbox Nav SDK to version 2.10.0 [\#881](https://github.com/ably/ably-asset-tracking-android/pull/881) ([KacperKluka](https://github.com/KacperKluka))
- Upgrade Ably core SDK dependency to improve testing capability [\#880](https://github.com/ably/ably-asset-tracking-android/pull/880) ([QuintinWillison](https://github.com/QuintinWillison))
- Write black-box tests for `DefaultAbly.connect` [\#878](https://github.com/ably/ably-asset-tracking-android/pull/878) ([lawrence-forooghian](https://github.com/lawrence-forooghian))
- Add dedicated unexpected error handling to certain workers [\#877](https://github.com/ably/ably-asset-tracking-android/pull/877) ([KacperKluka](https://github.com/KacperKluka))
- Static token auth flakey test [\#875](https://github.com/ably/ably-asset-tracking-android/pull/875) ([QuintinWillison](https://github.com/QuintinWillison))
- Remove `TimeoutCancellationException` from `Publisher.stop` [\#874](https://github.com/ably/ably-asset-tracking-android/pull/874) ([lawrence-forooghian](https://github.com/lawrence-forooghian))
- Handle unexpected exceptions in the worker queue [\#872](https://github.com/ably/ably-asset-tracking-android/pull/872) ([KacperKluka](https://github.com/KacperKluka))
- Networking connectivity integration test using local proxy [\#866](https://github.com/ably/ably-asset-tracking-android/pull/866) ([jaley](https://github.com/jaley))

## [1.5.1](https://github.com/ably/ably-asset-tracking-android/tree/v1.5.1)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.5.0...v1.5.1)

**Implemented enhancements:**

- Provide a fix/option for intermittent connection issue on publisher [\#803](https://github.com/ably/ably-asset-tracking-android/issues/803)

**Fixed bugs:**

- Fix crash when publisher tries to modify its internal state after being stopped [\#808](https://github.com/ably/ably-asset-tracking-android/issues/808)
- Fix not re-entering presence after connection is recovered via "Upgrade ably-java to 1.2.20" [\#822](https://github.com/ably/ably-asset-tracking-android/pull/822) ([KacperKluka](https://github.com/KacperKluka))

**Merged pull requests:**

- Check publisher state before emitting state on flow [\#820](https://github.com/ably/ably-asset-tracking-android/pull/820) ([ikbalkaya](https://github.com/ikbalkaya))
- Allow to specify the remainPresentFor Ably transport parameter [\#806](https://github.com/ably/ably-asset-tracking-android/pull/806) ([KacperKluka](https://github.com/KacperKluka))

## [1.5.0](https://github.com/ably/ably-asset-tracking-android/tree/v1.5.0)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.4.1...v1.5.0)

**Implemented enhancements:**

- Missing interface for tokenRequest Authentication instance creation in the Java facade [\#297](https://github.com/ably/ably-asset-tracking-android/issues/297), in [\#787](https://github.com/ably/ably-asset-tracking-android/pull/787) ([KacperKluka](https://github.com/KacperKluka))
- Add LocationAnimator for the Java API [\#790](https://github.com/ably/ably-asset-tracking-android/issues/790), in [\#791](https://github.com/ably/ably-asset-tracking-android/pull/791) ([KacperKluka](https://github.com/KacperKluka))
- Add missing methods to the Subscriber Java API [\#788](https://github.com/ably/ably-asset-tracking-android/issues/788), in [\#789](https://github.com/ably/ably-asset-tracking-android/pull/789) ([KacperKluka](https://github.com/KacperKluka))
- Update documentation regarding the Java verision of the AAT [\#707](https://github.com/ably/ably-asset-tracking-android/issues/707), in [\#793](https://github.com/ably/ably-asset-tracking-android/pull/793) ([KacperKluka](https://github.com/KacperKluka))
- Add logs connected to the lifecycle of publisher and subscriber SDKs [\#804](https://github.com/ably/ably-asset-tracking-android/issues/804), in [\#813](https://github.com/ably/ably-asset-tracking-android/pull/813) ([KacperKluka](https://github.com/KacperKluka))
- Add token-based auth configuration that do not require to specify client ID [\#768](https://github.com/ably/ably-asset-tracking-android/pull/768) ([KacperKluka](https://github.com/KacperKluka))

**Fixed bugs:**

- Fix NPE related to ably-java usage [\#805](https://github.com/ably/ably-asset-tracking-android/issues/805), in [\#807](https://github.com/ably/ably-asset-tracking-android/pull/807) ([KacperKluka](https://github.com/KacperKluka))
- Publisher failing with `IllegalStateException`: Already resumed, but proposed with update `kotlin.Unit` [\#799](https://github.com/ably/ably-asset-tracking-android/issues/799), in [\#800](https://github.com/ably/ably-asset-tracking-android/pull/800) ([KacperKluka](https://github.com/KacperKluka))
- Refactor the Ably wrapper to be more secure and error-proof [\#440](https://github.com/ably/ably-asset-tracking-android/issues/440), in [\#782](https://github.com/ably/ably-asset-tracking-android/pull/782) ([davyskiba](https://github.com/davyskiba))

## [1.4.1](https://github.com/ably/ably-asset-tracking-android/tree/v1.4.1)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.4.0...v1.4.1)

**Improved Subscriber behaviour:**

- Wait for the presence data sync before returning a subscriber instance [\#792](https://github.com/ably/ably-asset-tracking-android/issues/792),
  implemented in [\#794](https://github.com/ably/ably-asset-tracking-android/pull/794) ([KacperKluka](https://github.com/KacperKluka))

## [1.4.0](https://github.com/ably/ably-asset-tracking-android/tree/v1.4.0)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.4.0-rc.2...v1.4.0)

**Implemented enhancements:**

- Upgrade Mapbox SDK to 2.8.0 GA version [\#770](https://github.com/ably/ably-asset-tracking-android/issues/770)

**Fixed bugs:**

- Handle properly the error paths for the callback based auth [\#743](https://github.com/ably/ably-asset-tracking-android/issues/743)
- The publisher should close the Ably connection when it has no trackables [\#170](https://github.com/ably/ably-asset-tracking-android/issues/170)

**Closed issues:**

- Refactor the event queue in the CoreSubscriber [\#660](https://github.com/ably/ably-asset-tracking-android/issues/660)

## [1.4.0-rc.2](https://github.com/ably/ably-asset-tracking-android/tree/v1.4.0-rc.2)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.4.0-rc.1...v1.4.0-rc.2)

**Implemented enhancements:**

- Make the client ID an optional parameter if users use a token-based auth [\#766](https://github.com/ably/ably-asset-tracking-android/issues/766)

## [1.4.0-rc.1](https://github.com/ably/ably-asset-tracking-android/tree/v1.4.0-rc.1)

Version 1.4.0 of the Ably Asset Tracking SDKs for Android uses the Mapbox Navigation SDK in version 2.8.0
which enables users to use it together with the newest Mapbox Map SDK in version 10.8.0.

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.3.0...1.4.0-rc.1)

**Implemented enhancements:**

- Upgrade Mapbox to 2.8.0 [\#759](https://github.com/ably/ably-asset-tracking-android/issues/759)

**Merged pull requests:**

- Mapbox navigation dependency version updated to 2.8.0-rc.2 [\#761](https://github.com/ably/ably-asset-tracking-android/pull/761) ([davyskiba](https://github.com/davyskiba))

## [1.3.0](https://github.com/ably/ably-asset-tracking-android/tree/v1.3.0)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.2.0...1.3.0)

**Implemented enhancements:**

- Upgrade Mapbox Nav SDK to the GA 2.7.0 version [\#742](https://github.com/ably/ably-asset-tracking-android/issues/742)

**Fixed bugs:**

- CoreLocationAnimator progress may be bigger than 100% [\#751](https://github.com/ably/ably-asset-tracking-android/issues/751)
- Fix the waitForChannelReconnection\(\) method implementation [\#744](https://github.com/ably/ably-asset-tracking-android/issues/744)

**Merged pull requests:**

- Expose publisher presence flow from the subscriber SDK [\#754](https://github.com/ably/ably-asset-tracking-android/pull/754) ([KacperKluka](https://github.com/KacperKluka))
- Fix location animator progress exceeding 100% [\#752](https://github.com/ably/ably-asset-tracking-android/pull/752) ([KacperKluka](https://github.com/KacperKluka))
- Update builder examples for publisher and subscriber in the README [\#750](https://github.com/ably/ably-asset-tracking-android/pull/750) ([KacperKluka](https://github.com/KacperKluka))
- Fix the waitForChannelReconnection\(\) method implementation [\#749](https://github.com/ably/ably-asset-tracking-android/pull/749) ([KacperKluka](https://github.com/KacperKluka))
- Upgrade Mapbox to 2.7.0 [\#748](https://github.com/ably/ably-asset-tracking-android/pull/748) ([KacperKluka](https://github.com/KacperKluka))
- Add known limitations section to the README [\#747](https://github.com/ably/ably-asset-tracking-android/pull/747) ([KacperKluka](https://github.com/KacperKluka))

## [1.2.0](https://github.com/ably/ably-asset-tracking-android/tree/v1.2.0)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.1.1...v1.2.0)

**Implemented enhancements:**

- Add support for the "environment" Ably client option [\#739](https://github.com/ably/ably-asset-tracking-android/issues/739)

## [1.1.1](https://github.com/ably/ably-asset-tracking-android/tree/v1.1.1)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.1.0...v1.1.1)

**Fixed bugs:**

- Bad location updates when predictions are enabled [\#734](https://github.com/ably/ably-asset-tracking-android/issues/734)

**Merged pull requests:**

- Update camera less frequently in the subscriber example app [\#737](https://github.com/ably/ably-asset-tracking-android/pull/737) ([KacperKluka](https://github.com/KacperKluka))
- Always disable the predictions mechanism [\#733](https://github.com/ably/ably-asset-tracking-android/pull/733) ([KacperKluka](https://github.com/KacperKluka))

## [1.1.0](https://github.com/ably/ably-asset-tracking-android/tree/v1.1.0)

Version 1.1 of the Ably Asset Tracking SDKs for Android brings changes to both the publishing and subscribing SDKs,
and their corresponding example apps, that make the subscriber's marker animate much more smoothly.

[Full Changelog from version 1.1.0 RC 7 (last pre-release)](https://github.com/ably/ably-asset-tracking-android/compare/v1.1.0-rc.7...v1.1.0)

[Full Changelog from version 1.0.0 (last release)](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0...v1.1.0)

**Implemented enhancements:**

- Allow to enable and disable the cycling profile [\#726](https://github.com/ably/ably-asset-tracking-android/issues/726)

**Fixed bugs:**

- Validate messages received from Ably [\#691](https://github.com/ably/ably-asset-tracking-android/issues/691)
- Publisher service notification disappearing after stopping the publisher [\#528](https://github.com/ably/ably-asset-tracking-android/issues/528)

**Closed issues:**

- Add option to enable or disable the cycling profile in the publisher example app [\#728](https://github.com/ably/ably-asset-tracking-android/issues/728)

**Merged pull requests:**

- Upgrade Mapbox Nav SDK to 2.7.0 [\#727](https://github.com/ably/ably-asset-tracking-android/pull/727) ([KacperKluka](https://github.com/KacperKluka))
- Add documentation about location animator flows [\#725](https://github.com/ably/ably-asset-tracking-android/pull/725) ([KacperKluka](https://github.com/KacperKluka))

## [1.1.0-rc.7](https://github.com/ably/ably-asset-tracking-android/tree/v1.1.0-rc.7)

Version 1.1 of the Ably Asset Tracking SDKs for Android brings changes to both the publishing and subscribing SDKs,
and their corresponding example apps, that make the subscriber's marker animate much more smoothly.

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.1.0-rc.6...v1.1.0-rc.7)

**Implemented enhancements:**

- Request new auth token when channel attach fails due to permission failure [\#695](https://github.com/ably/ably-asset-tracking-android/issues/695)
- Change authentication callback functions to suspending functions [\#686](https://github.com/ably/ably-asset-tracking-android/issues/686)
- Allow to configure the interval of camera updates for the location animator [\#685](https://github.com/ably/ably-asset-tracking-android/issues/685)
- Allow to configure the intentional delay of the location animator [\#684](https://github.com/ably/ably-asset-tracking-android/issues/684)


## [1.1.0-rc.6](https://github.com/ably/ably-asset-tracking-android/tree/v1.1.0-rc.6)

Version 1.1 of the Ably Asset Tracking SDKs for Android brings changes to both the publishing and subscribing SDKs,
and their corresponding example apps, that make the subscriber's marker animate much more smoothly.

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.1.0-rc.5...v1.1.0-rc.6)

**Fixed bugs:**

- ConnectionException after waking up a device with subscriber running [\#702](https://github.com/ably/ably-asset-tracking-android/issues/702)

**Merged pull requests:**

- Wait for the suspended channel to reconnect before performing an operation [\#710](https://github.com/ably/ably-asset-tracking-android/pull/710) ([KacperKluka](https://github.com/KacperKluka))

## [1.1.0-rc.5](https://github.com/ably/ably-asset-tracking-android/tree/v1.1.0-rc.5)

Version 1.1 of the Ably Asset Tracking SDKs for Android brings changes to both the publishing and subscribing SDKs,
and their corresponding example apps, that make the subscriber's marker animate much more smoothly.

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.1.0-rc.4...v1.1.0-rc.5)

**Fixed bugs:**

- Removing and adding new trackable with the same name leads to an exception [\#701](https://github.com/ably/ably-asset-tracking-android/issues/701)
- Exception handling issues when connecting to Ably [\#700](https://github.com/ably/ably-asset-tracking-android/issues/700)
- EnhancedLocationUpdate equals returns true for different locations [\#694](https://github.com/ably/ably-asset-tracking-android/issues/694)
- Fix Java-WebSockets security issue [\#688](https://github.com/ably/ably-asset-tracking-android/issues/688)
- Fix gson dependency vulnerability [\#687](https://github.com/ably/ably-asset-tracking-android/issues/687)

**Merged pull requests:**

- Fix error when trying to connect to a previously closed channel [\#708](https://github.com/ably/ably-asset-tracking-android/pull/708) ([KacperKluka](https://github.com/KacperKluka))
- Fix exception throwing issues when connecting to Ably [\#699](https://github.com/ably/ably-asset-tracking-android/pull/699) ([KacperKluka](https://github.com/KacperKluka))
- Fix location updates equals implementation [\#697](https://github.com/ably/ably-asset-tracking-android/pull/697) ([KacperKluka](https://github.com/KacperKluka))
- Update ably-android version to 1.2.14 [\#693](https://github.com/ably/ably-asset-tracking-android/pull/693) ([KacperKluka](https://github.com/KacperKluka))

## [1.1.0-rc.4](https://github.com/ably/ably-asset-tracking-android/tree/v1.1.0-rc.4)

Version 1.1 of the Ably Asset Tracking SDKs for Android brings changes to both the publishing and subscribing SDKs,
and their corresponding example apps, that make the subscriber's marker animate much more smoothly.

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.1.0-rc.3...v1.1.0-rc.4)

**Fixed bugs:**

- Fatal error when adding a trackable after removing all previous ones [\#676](https://github.com/ably/ably-asset-tracking-android/issues/676)

**Closed issues:**

- Upgrade `ably-java` dependency to version 1.2.12 [\#669](https://github.com/ably/ably-asset-tracking-android/issues/669)

## [1.1.0-rc.3](https://github.com/ably/ably-asset-tracking-android/tree/v1.1.0-rc.3)

Version 1.1 of the Ably Asset Tracking SDKs for Android brings changes to both the publishing and subscribing SDKs,
and their corresponding example apps, that make the subscriber's marker animate much more smoothly.

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.1.0-rc.2...v1.1.0-rc.3)

**Fixed bugs:**

- Handle exception when getting presence messages but channel is in the suspended state [\#663](https://github.com/ably/ably-asset-tracking-android/issues/663)
- Handle the "Connection resume failed" exception from ably-java [\#662](https://github.com/ably/ably-asset-tracking-android/issues/662)

**Closed issues:**

- Refactor / simplify event queue that is implemented in CorePublisher [\#522](https://github.com/ably/ably-asset-tracking-android/issues/522)

**Merged pull requests:**

- Handle connection resume exception [\#665](https://github.com/ably/ably-asset-tracking-android/pull/665) ([KacperKluka](https://github.com/KacperKluka))
- Improve add trackable process [\#664](https://github.com/ably/ably-asset-tracking-android/pull/664) ([KacperKluka](https://github.com/KacperKluka))
- Add timeout to stopping the publisher [\#661](https://github.com/ably/ably-asset-tracking-android/pull/661) ([KacperKluka](https://github.com/KacperKluka))
- Integration: Refactor event queue [\#607](https://github.com/ably/ably-asset-tracking-android/pull/607) ([KacperKluka](https://github.com/KacperKluka))

## [1.1.0-rc.2](https://github.com/ably/ably-asset-tracking-android/tree/v1.1.0-rc.2)

Version 1.1 of the Ably Asset Tracking SDKs for Android brings changes to both the publishing and subscribing SDKs,
and their corresponding example apps, that make the subscriber's marker animate much more smoothly.

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.1.0-rc.1...v1.1.0-rc.2)

**Implemented enhancements:**

- Upgrade Mapbox dependencies [\#642](https://github.com/ably/ably-asset-tracking-android/issues/642)

**Closed issues:**

- Add a toggle in the example subscription app that would hide/show the raw locations marker on the map [\#649](https://github.com/ably/ably-asset-tracking-android/issues/649)

**Merged pull requests:**

- Add dialog with animation options in the subscriber example app [\#653](https://github.com/ably/ably-asset-tracking-android/pull/653) ([KacperKluka](https://github.com/KacperKluka))
- Upgrade Mapbox to version 2.3.0 and other dependencies to the newest versions [\#652](https://github.com/ably/ably-asset-tracking-android/pull/652) ([KacperKluka](https://github.com/KacperKluka))

## [1.1.0-rc.1](https://github.com/ably/ably-asset-tracking-android/tree/v1.1.0-rc.1)

Version 1.1 of the Ably Asset Tracking SDKs for Android brings changes to both the publishing and subscribing SDKs,
and their corresponding example apps, that make the subscriber's marker animate much more smoothly.

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0...v1.1.0-rc.1)

**Implemented enhancements:**

- Include the latest Mapbox profile to the build [\#631](https://github.com/ably/ably-asset-tracking-android/issues/631)
- Send batches of location updates [\#626](https://github.com/ably/ably-asset-tracking-android/issues/626)
- Update `ably-android` dependency to version 1.2.11 [\#624](https://github.com/ably/ably-asset-tracking-android/issues/624)

**Fixed bugs:**

- Trackable channel rewind doesn't work properly after sending resolution data from the Publisher [\#638](https://github.com/ably/ably-asset-tracking-android/issues/638)

**Closed issues:**

- Use the next location update intervals to make the marker movement smoother in the example app [\#634](https://github.com/ably/ably-asset-tracking-android/issues/634)
- Add constant location engine resolution to the publisher example app [\#633](https://github.com/ably/ably-asset-tracking-android/issues/633)

**Merged pull requests:**

- Add a migration guide to version 1.1.0 in the README file [\#646](https://github.com/ably/ably-asset-tracking-android/pull/646) ([KacperKluka](https://github.com/KacperKluka))
- Add animations module with location animators [\#645](https://github.com/ably/ably-asset-tracking-android/pull/645) ([KacperKluka](https://github.com/KacperKluka))
- Enable sending resolution from publisher example app by default [\#644](https://github.com/ably/ably-asset-tracking-android/pull/644) ([KacperKluka](https://github.com/KacperKluka))
- Make map marker animations smoother [\#643](https://github.com/ably/ably-asset-tracking-android/pull/643) ([KacperKluka](https://github.com/KacperKluka))
- Use the new asset tracking Mapbox profile snapshot [\#641](https://github.com/ably/ably-asset-tracking-android/pull/641) ([KacperKluka](https://github.com/KacperKluka))
- Send publisher resolution data via presence data [\#640](https://github.com/ably/ably-asset-tracking-android/pull/640) ([KacperKluka](https://github.com/KacperKluka))
- Update the Ably SDK to the version 1.2.11 [\#637](https://github.com/ably/ably-asset-tracking-android/pull/637) ([KacperKluka](https://github.com/KacperKluka))
- Allow to specify the constant location engine resolution in the publisher example app [\#636](https://github.com/ably/ably-asset-tracking-android/pull/636) ([KacperKluka](https://github.com/KacperKluka))
- Add next location update intervals to the Subscriber API [\#629](https://github.com/ably/ably-asset-tracking-android/pull/629) ([KacperKluka](https://github.com/KacperKluka))
- Send resolution data from publisher [\#628](https://github.com/ably/ably-asset-tracking-android/pull/628) ([KacperKluka](https://github.com/KacperKluka))
- Enable setting a constant location engine resolution [\#627](https://github.com/ably/ably-asset-tracking-android/pull/627) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0)

The Ably Asset Tracking SDKs for Android are out of Beta! :tada:

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.15...v1.0.0)

**Implemented enhancements:**

- Add timestamps to logs [\#530](https://github.com/ably/ably-asset-tracking-android/issues/530)

**Fixed bugs:**

- The destination from previously active trackable is not cleared if the new active trackable has no destination [\#588](https://github.com/ably/ably-asset-tracking-android/issues/588)

**Closed issues:**

- Store app preferences key in a non-translatable string resources file [\#502](https://github.com/ably/ably-asset-tracking-android/issues/502)
- Release to Maven Central [\#318](https://github.com/ably/ably-asset-tracking-android/issues/318)

**Merged pull requests:**

- Add timestamps to log statements [\#600](https://github.com/ably/ably-asset-tracking-android/pull/600) ([KacperKluka](https://github.com/KacperKluka))
- Add info about installing the AAT SDK from the Maven Central repository [\#598](https://github.com/ably/ably-asset-tracking-android/pull/598) ([KacperKluka](https://github.com/KacperKluka))
- Clear route when the new active trackable does not have a destination [\#594](https://github.com/ably/ably-asset-tracking-android/pull/594) ([KacperKluka](https://github.com/KacperKluka))
- Publish to the MavenCentral [\#592](https://github.com/ably/ably-asset-tracking-android/pull/592) ([KacperKluka](https://github.com/KacperKluka))
- Move untranslatable strings to a separate file [\#579](https://github.com/ably/ably-asset-tracking-android/pull/579) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.15](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.15)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.14...v1.0.0-beta.15)

**Fixed bugs:**

- API documentation is missing in the released SDK [\#571](https://github.com/ably/ably-asset-tracking-android/issues/571)

**Merged pull requests:**

- Add sources and javadoc JARs to the published library [\#580](https://github.com/ably/ably-asset-tracking-android/pull/580) ([KacperKluka](https://github.com/KacperKluka))
- Add callback for retrieving the raw history data from Mapbox [\#576](https://github.com/ably/ably-asset-tracking-android/pull/576) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.14](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.14)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.13...v1.0.0-beta.14)

**Implemented enhancements:**

- Migrate to Mapbox v2 [\#573](https://github.com/ably/ably-asset-tracking-android/issues/573)
- Allow to set a trackable destination in the publisher example app [\#534](https://github.com/ably/ably-asset-tracking-android/issues/534)
- Display the horizontal accuracy of locations in the subscriber example app [\#533](https://github.com/ably/ably-asset-tracking-android/issues/533)
- Add display of the SkippedLocations to the Example Subsriber app [\#490](https://github.com/ably/ably-asset-tracking-android/issues/490)

**Fixed bugs:**

- Zooming does not improve resolution: Resolution-type events does not get sent. [\#535](https://github.com/ably/ably-asset-tracking-android/issues/535)

**Closed issues:**

- Add option to select routing profile in the Publisher Example app [\#332](https://github.com/ably/ably-asset-tracking-android/issues/332)
- Make ResolutionConstraints an interface (right now it is a sealed class) [\#299](https://github.com/ably/ably-asset-tracking-android/issues/299)
- Decide from which location type should the last known location be chosen [\#79](https://github.com/ably/ably-asset-tracking-android/issues/79)

**Merged pull requests:**

- Migrate to Mapbox v2 [\#574](https://github.com/ably/ably-asset-tracking-android/pull/574) ([KacperKluka](https://github.com/KacperKluka))
- Show skipped locations in the subscriber example app [\#567](https://github.com/ably/ably-asset-tracking-android/pull/567) ([KacperKluka](https://github.com/KacperKluka))
- Add information about removing notification when stopping the publisher [\#566](https://github.com/ably/ably-asset-tracking-android/pull/566) ([KacperKluka](https://github.com/KacperKluka))
- Fix presence messages parsing if presence data is in JSON object format [\#565](https://github.com/ably/ably-asset-tracking-android/pull/565) ([KacperKluka](https://github.com/KacperKluka))
- Change ResolutionConstraints to an interface [\#562](https://github.com/ably/ably-asset-tracking-android/pull/562) ([KacperKluka](https://github.com/KacperKluka))
- Show accuracy circles for raw and enhanced markers in the subscriber app [\#560](https://github.com/ably/ably-asset-tracking-android/pull/560) ([KacperKluka](https://github.com/KacperKluka))
- Set routing profile in the publisher example app [\#553](https://github.com/ably/ably-asset-tracking-android/pull/553) ([KacperKluka](https://github.com/KacperKluka))
- Set trackable destination in the publisher example app [\#552](https://github.com/ably/ably-asset-tracking-android/pull/552) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.13](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.13)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.12...v1.0.0-beta.13)

**Implemented enhancements:**

- Clear the route when destination is reached [\#536](https://github.com/ably/ably-asset-tracking-android/issues/536)
- Allow to disable predictions in the Mapbox SDK [\#532](https://github.com/ably/ably-asset-tracking-android/issues/532)

**Merged pull requests:**

- Clear the route when the destination is reached [\#548](https://github.com/ably/ably-asset-tracking-android/pull/548) ([KacperKluka](https://github.com/KacperKluka))
- Allow to disable the enhanced location predictions in the publisher [\#547](https://github.com/ably/ably-asset-tracking-android/pull/547) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.12](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.12)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.11...v1.0.0-beta.12)

**Implemented enhancements:**

- Add log statements to both SDKs [\#524](https://github.com/ably/ably-asset-tracking-android/issues/524)
- Display resolution information in the example apps [\#510](https://github.com/ably/ably-asset-tracking-android/issues/510)
- Send resolution to subscriber [\#509](https://github.com/ably/ably-asset-tracking-android/issues/509)

**Fixed bugs:**

- Calling remove() should remove a trackable that isn't finished being added [\#450](https://github.com/ably/ably-asset-tracking-android/issues/450)

**Closed issues:**

- Information about using SDK as a dependency in Gradle [\#348](https://github.com/ably/ably-asset-tracking-android/issues/348)
- Update docs for DefaultResolutionConstraints [\#305](https://github.com/ably/ably-asset-tracking-android/issues/305)
- Document the Event classes that are used in both SDKs [\#260](https://github.com/ably/ably-asset-tracking-android/issues/260)
- Document the public interfaces exposed from LocationUpdateModels.kt in core-sdk [\#144](https://github.com/ably/ably-asset-tracking-android/issues/144)

**Merged pull requests:**

- Fix NPE when converting Ably's ChannelStateChange to AAT's one [\#527](https://github.com/ably/ably-asset-tracking-android/pull/527) ([KacperKluka](https://github.com/KacperKluka))
- Add log statements to publisher SDK [\#526](https://github.com/ably/ably-asset-tracking-android/pull/526) ([KacperKluka](https://github.com/KacperKluka))
- Add GitHub Packages installation instruction to the README [\#523](https://github.com/ably/ably-asset-tracking-android/pull/523) ([KacperKluka](https://github.com/KacperKluka))
- Add documentation for the core publisher and core subscriber events [\#521](https://github.com/ably/ably-asset-tracking-android/pull/521) ([KacperKluka](https://github.com/KacperKluka))
- Fix DefaultResolutionConstraints docs [\#519](https://github.com/ably/ably-asset-tracking-android/pull/519) ([KacperKluka](https://github.com/KacperKluka))
- Add documentation for the public location update classes [\#518](https://github.com/ably/ably-asset-tracking-android/pull/518) ([KacperKluka](https://github.com/KacperKluka))
- Show publisher's resolution in the subscriber example app [\#517](https://github.com/ably/ably-asset-tracking-android/pull/517) ([KacperKluka](https://github.com/KacperKluka))
- Send resolution data from publisher to subscribers [\#515](https://github.com/ably/ably-asset-tracking-android/pull/515) ([KacperKluka](https://github.com/KacperKluka))
- Remove unfinished trackable when remove called [\#506](https://github.com/ably/ably-asset-tracking-android/pull/506) ([ikbalkaya](https://github.com/ikbalkaya))

## [1.0.0-beta.11](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.11)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.10...v1.0.0-beta.11)

**Implemented enhancements:**

- Add raw location information for DEBUG capabilities [\#489](https://github.com/ably/ably-asset-tracking-android/issues/489)

**Fixed bugs:**

- Ensure only one active instance of MapboxNavigation is created [\#463](https://github.com/ably/ably-asset-tracking-android/issues/463)
- Project doesn't build under JDK 16 [\#462](https://github.com/ably/ably-asset-tracking-android/issues/462)
- Add and track methods called with an already added trackable repeat some of the adding logic [\#458](https://github.com/ably/ably-asset-tracking-android/issues/458)
- Detach from a channel when a trackable is removed [\#438](https://github.com/ably/ably-asset-tracking-android/issues/438)
- Code execution freezes when adding a trackable [\#403](https://github.com/ably/ably-asset-tracking-android/issues/403)

**Closed issues:**

- Remove code connected to trip metadata from the Publisher SDK [\#431](https://github.com/ably/ably-asset-tracking-android/issues/431)
- Conform ReadMe and Contributing Documents [\#393](https://github.com/ably/ably-asset-tracking-android/issues/393)

**Merged pull requests:**

- Add raw locations to the Subscriber SDK [\#495](https://github.com/ably/ably-asset-tracking-android/pull/495) ([KacperKluka](https://github.com/KacperKluka))
- Send raw locations from the Publisher [\#493](https://github.com/ably/ably-asset-tracking-android/pull/493) ([KacperKluka](https://github.com/KacperKluka))
- Start replaying recorded trip when the trip is started [\#428](https://github.com/ably/ably-asset-tracking-android/pull/428) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.10](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.10)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.9...v1.0.0-beta.10)

**Implemented enhancements:**

- Implement `RSC7d` \(Ably-Agent header\) [\#395](https://github.com/ably/ably-asset-tracking-android/issues/395)

**Fixed bugs:**

- Specify channel modes to limit the amount of unnecessary data being sent and received over Ably [\#412](https://github.com/ably/ably-asset-tracking-android/issues/412)
- EnhancedLocationUpdate timestamps do not represent real world time [\#387](https://github.com/ably/ably-asset-tracking-android/issues/387)

**Merged pull requests:**

- Update Ably SDK to 1.2.8 [\#420](https://github.com/ably/ably-asset-tracking-android/pull/420) ([KacperKluka](https://github.com/KacperKluka))
- Use real world timestamps for enhanced location updates [\#416](https://github.com/ably/ably-asset-tracking-android/pull/416) ([KacperKluka](https://github.com/KacperKluka))
- Use channel modes to avoid unnecessary channel attachements [\#413](https://github.com/ably/ably-asset-tracking-android/pull/413) ([KacperKluka](https://github.com/KacperKluka))
- Report AAT version through Ably Agent Header [\#410](https://github.com/ably/ably-asset-tracking-android/pull/410) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.9](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.9)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.8...v1.0.0-beta.9)

**Implemented enhancements:**

- Update ably-android library [\#404](https://github.com/ably/ably-asset-tracking-android/issues/404)
- App Obfuscation causes invalid keys to be serialised into transmitted JSON [\#396](https://github.com/ably/ably-asset-tracking-android/issues/396)

**Closed issues:**

- Prepare Proguard configuration [\#50](https://github.com/ably/ably-asset-tracking-android/issues/50)

**Merged pull requests:**

- Update Ably SDK to 1.2.7 [\#405](https://github.com/ably/ably-asset-tracking-android/pull/405) ([KacperKluka](https://github.com/KacperKluka))
- Add proguard configuration [\#399](https://github.com/ably/ably-asset-tracking-android/pull/399) ([KacperKluka](https://github.com/KacperKluka))
- Explicitly specify DTO field names during serialization [\#398](https://github.com/ably/ably-asset-tracking-android/pull/398) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.8](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.8)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.7...v1.0.0-beta.8)

**Implemented enhancements:**

- Add a map to the Android Publisher example  [\#377](https://github.com/ably/ably-asset-tracking-android/issues/377)
- Explore a way to not have 2 background notifications when SDK is used on Android 9+ [\#358](https://github.com/ably/ably-asset-tracking-android/issues/358)

**Fixed bugs:**

- Publishing app crashes on Android API 21 [\#359](https://github.com/ably/ably-asset-tracking-android/issues/359)
- Subscriber app tracking UI text overlapping [\#345](https://github.com/ably/ably-asset-tracking-android/issues/345)
- Missing emit to log from within MapException catch block [\#338](https://github.com/ably/ably-asset-tracking-android/issues/338)
- When an invalid Ably key is used when building an example app there is no log message explaining what is not working [\#335](https://github.com/ably/ably-asset-tracking-android/issues/335)
- Handle exception thrown when sending enhanced location update fails [\#213](https://github.com/ably/ably-asset-tracking-android/issues/213)

**Closed issues:**

- Send trip start and trip end events on a meta channel from Publisher SDK \(Android\) [\#374](https://github.com/ably/ably-asset-tracking-android/issues/374)
- Check if we should pass client ID when using the AblySimulationLocationEngine [\#232](https://github.com/ably/ably-asset-tracking-android/issues/232)
- Start using the new Kotlin JVM IR Backend [\#202](https://github.com/ably/ably-asset-tracking-android/issues/202)
- Improve CorePublisher active Trackable state management [\#195](https://github.com/ably/ably-asset-tracking-android/issues/195)

**Merged pull requests:**

- Show custom notification for Mapbox trip service [\#389](https://github.com/ably/ably-asset-tracking-android/pull/389) ([KacperKluka](https://github.com/KacperKluka))
- Remove previousState from ConnectionStateChange [\#385](https://github.com/ably/ably-asset-tracking-android/pull/385) ([KacperKluka](https://github.com/KacperKluka))
- Log mapbox exception when setting destination fails [\#384](https://github.com/ably/ably-asset-tracking-android/pull/384) ([KacperKluka](https://github.com/KacperKluka))
- Add a map to the Android Publisher example [\#382](https://github.com/ably/ably-asset-tracking-android/pull/382) ([KacperKluka](https://github.com/KacperKluka))
- Fix overlapping text in subscriber example app [\#381](https://github.com/ably/ably-asset-tracking-android/pull/381) ([KacperKluka](https://github.com/KacperKluka))
- Increase minSdk to 24 for example apps [\#380](https://github.com/ably/ably-asset-tracking-android/pull/380) ([KacperKluka](https://github.com/KacperKluka))
- Send failed location updates [\#379](https://github.com/ably/ably-asset-tracking-android/pull/379) ([KacperKluka](https://github.com/KacperKluka))
- Update Kotlin to 1.5.0 [\#370](https://github.com/ably/ably-asset-tracking-android/pull/370) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.7](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.7)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.6...v1.0.0-beta.7)

**Merged pull requests:**

- Downgrade Mapbox to 1.6.1 [\#369](https://github.com/ably/ably-asset-tracking-android/pull/369) ([KacperKluka](https://github.com/KacperKluka))
- Send trip start and trip end events on a meta channel from Publisher SDK [\#357](https://github.com/ably/ably-asset-tracking-android/pull/357) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.6](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.6)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.5...v1.0.0-beta.6)

**Implemented enhancements:**

- Channel Namespace [\#327](https://github.com/ably/ably-asset-tracking-android/issues/327)

**Fixed bugs:**

- Exceptions are not routed meaningfully [\#201](https://github.com/ably/ably-asset-tracking-android/issues/201)

**Closed issues:**

- Simple Log Handler [\#347](https://github.com/ably/ably-asset-tracking-android/issues/347)
- Replace Android Location type with our own location type [\#337](https://github.com/ably/ably-asset-tracking-android/issues/337)
- Make a decision regarding wether we expose portable type in addition to Android specific Location type via our APIs [\#287](https://github.com/ably/ably-asset-tracking-android/issues/287)
-  add capability to the SDK to support JWT flow + examples [\#285](https://github.com/ably/ably-asset-tracking-android/issues/285)
- Lint Gradle Groovy scripts [\#8](https://github.com/ably/ably-asset-tracking-android/issues/8)

**Merged pull requests:**

- Apply presence data hotfix to the main branch [\#356](https://github.com/ably/ably-asset-tracking-android/pull/356) ([KacperKluka](https://github.com/KacperKluka))
- Increase the distance value compensation added to mocked locations in tests [\#351](https://github.com/ably/ably-asset-tracking-android/pull/351) ([KacperKluka](https://github.com/KacperKluka))
- Use ScrollView in publisher kotlin app to fix entering tracking ID [\#344](https://github.com/ably/ably-asset-tracking-android/pull/344) ([ben-xD](https://github.com/ben-xD))
- Remove battery level from LocationUpdate [\#343](https://github.com/ably/ably-asset-tracking-android/pull/343) ([KacperKluka](https://github.com/KacperKluka))
- Replace Android location type with our own location type [\#341](https://github.com/ably/ably-asset-tracking-android/pull/341) ([KacperKluka](https://github.com/KacperKluka))
- Add JWT authentication support [\#340](https://github.com/ably/ably-asset-tracking-android/pull/340) ([KacperKluka](https://github.com/KacperKluka))
- Add simple log handler [\#339](https://github.com/ably/ably-asset-tracking-android/pull/339) ([KacperKluka](https://github.com/KacperKluka))
- Rename and refactor java example apps modules [\#333](https://github.com/ably/ably-asset-tracking-android/pull/333) ([KacperKluka](https://github.com/KacperKluka))
- Allow TokenParams and TokenRequest capability to be null [\#331](https://github.com/ably/ably-asset-tracking-android/pull/331) ([KacperKluka](https://github.com/KacperKluka))
- Bring back section about secrets to the readme. [\#330](https://github.com/ably/ably-asset-tracking-android/pull/330) ([kavalerov](https://github.com/kavalerov))
- Add prefix to Ably channel names [\#329](https://github.com/ably/ably-asset-tracking-android/pull/329) ([KacperKluka](https://github.com/KacperKluka))
- Redesign publisher example app [\#323](https://github.com/ably/ably-asset-tracking-android/pull/323) ([KacperKluka](https://github.com/KacperKluka))
- Redesign subscriber example app [\#322](https://github.com/ably/ably-asset-tracking-android/pull/322) ([KacperKluka](https://github.com/KacperKluka))
- Links to Asset Tracking content - ably-asset-tracking-android [\#321](https://github.com/ably/ably-asset-tracking-android/pull/321) ([ramiro-nd](https://github.com/ramiro-nd))
- Upgrade ably-android version [\#315](https://github.com/ably/ably-asset-tracking-android/pull/315) ([QuintinWillison](https://github.com/QuintinWillison))
- Add missing error routing [\#311](https://github.com/ably/ably-asset-tracking-android/pull/311) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.5](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.5)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.4...v1.0.0-beta.5)

**Merged pull requests:**

- Hotfix to add prefix to Ably channel names [\#361](https://github.com/ably/ably-asset-tracking-android/pull/361) ([kavalerov](https://github.com/kavalerov))

## [1.0.0-beta.4](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.4)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.3...v1.0.0-beta.4)

**Merged pull requests:**

- Fix crash when presence data is missing or in wrong format [\#354](https://github.com/ably/ably-asset-tracking-android/pull/354) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.3](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.3)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.2...v1.0.0-beta.3)

**Fixed bugs:**

- Keep zoom level when auto centering in the subscriber example app [\#306](https://github.com/ably/ably-asset-tracking-android/issues/306)
- Handle exception thrown when subscribing for presence messages fails in publisher [\#214](https://github.com/ably/ably-asset-tracking-android/issues/214)

**Closed issues:**

- Change names of client type values from presence data [\#300](https://github.com/ably/ably-asset-tracking-android/issues/300)
- Release to package manager [\#286](https://github.com/ably/ably-asset-tracking-android/issues/286)
- Add tests that check if different resolutions have impact on the number of sent locations [\#280](https://github.com/ably/ably-asset-tracking-android/issues/280)
- Move internal classes to the common module [\#271](https://github.com/ably/ably-asset-tracking-android/issues/271)
- Make DTO class definitions easier to identify in the codebase [\#165](https://github.com/ably/ably-asset-tracking-android/issues/165)

**Merged pull requests:**

- Publish to Maven GitHub Packages [\#317](https://github.com/ably/ably-asset-tracking-android/pull/317) ([QuintinWillison](https://github.com/QuintinWillison))
- Replace jCenter repository with maven Central [\#316](https://github.com/ably/ably-asset-tracking-android/pull/316) ([QuintinWillison](https://github.com/QuintinWillison))
- Replace interpolated string definitions with plain literals [\#314](https://github.com/ably/ably-asset-tracking-android/pull/314) ([QuintinWillison](https://github.com/QuintinWillison))
- Remove empty proguard configuration files to reduce noise [\#312](https://github.com/ably/ably-asset-tracking-android/pull/312) ([QuintinWillison](https://github.com/QuintinWillison))
- Change presence client types values to match naming convention [\#310](https://github.com/ably/ably-asset-tracking-android/pull/310) ([KacperKluka](https://github.com/KacperKluka))
- Handle error when subscribing for presence messages fails [\#308](https://github.com/ably/ably-asset-tracking-android/pull/308) ([KacperKluka](https://github.com/KacperKluka))
- Keep the zoom level when auto centering the camera position [\#307](https://github.com/ably/ably-asset-tracking-android/pull/307) ([KacperKluka](https://github.com/KacperKluka))
- Split out contributing guide [\#304](https://github.com/ably/ably-asset-tracking-android/pull/304) ([QuintinWillison](https://github.com/QuintinWillison))
- Add tests for publisher resolution and frequency of sent messages [\#303](https://github.com/ably/ably-asset-tracking-android/pull/303) ([KacperKluka](https://github.com/KacperKluka))
- Annotate DTO classes [\#302](https://github.com/ably/ably-asset-tracking-android/pull/302) ([KacperKluka](https://github.com/KacperKluka))
- Move shared classes that shouldn't be exposed to the common module [\#301](https://github.com/ably/ably-asset-tracking-android/pull/301) ([KacperKluka](https://github.com/KacperKluka))
- Remove the metadata field from the Trackable class [\#294](https://github.com/ably/ably-asset-tracking-android/pull/294) ([KacperKluka](https://github.com/KacperKluka))
- Create a test-common module for shared unit tests code [\#282](https://github.com/ably/ably-asset-tracking-android/pull/282) ([KacperKluka](https://github.com/KacperKluka))

## [1.0.0-beta.2](https://github.com/ably/ably-asset-tracking-android/tree/v1.0.0-beta.2)

[Full Changelog](https://github.com/ably/ably-asset-tracking-android/compare/v1.0.0-beta.1...v1.0.0-beta.2)

**Implemented enhancements:**

- Handle stopped state of the Subscriber [\#227](https://github.com/ably/ably-asset-tracking-android/issues/227)
- Update Mapbox dependency to Nav 2.0 [\#225](https://github.com/ably/ably-asset-tracking-android/issues/225)
- Token-based Ably Realtime Authentication [\#171](https://github.com/ably/ably-asset-tracking-android/issues/171)
- Unify approach of GPS location updates on iOS and Android [\#223](https://github.com/ably/ably-asset-tracking-android/issues/223)

**Fixed bugs:**

- Fix location history test files [\#258](https://github.com/ably/ably-asset-tracking-android/issues/258)
- Emit the current channel state when attaching a channel state listener [\#257](https://github.com/ably/ably-asset-tracking-android/issues/257)
- Don't modify Publisher state outside of it's events queue thread [\#256](https://github.com/ably/ably-asset-tracking-android/issues/256)
- Change the order of actions when closing a channel in the Ably wrapper [\#246](https://github.com/ably/ably-asset-tracking-android/issues/246)
- Use Ably and channel state when determining the trackable state in the Subscriber SDK [\#238](https://github.com/ably/ably-asset-tracking-android/issues/238)
- Handle errors when starting Subscriber and Publisher [\#237](https://github.com/ably/ably-asset-tracking-android/issues/237)
- Change the order of actions when removing a trackable from the Publisher [\#228](https://github.com/ably/ably-asset-tracking-android/issues/228)
- Unify the way of deciding if a location update should be sent in the Publisher [\#236](https://github.com/ably/ably-asset-tracking-android/issues/236)
- Fix calling suspend functions from Subscriber events queue [\#226](https://github.com/ably/ably-asset-tracking-android/issues/226)
- Handle exception thrown when connection to the channel on subscriber start [\#215](https://github.com/ably/ably-asset-tracking-android/issues/215)
- Android Publisher example app crashes when iOS Subscriber broadcasts resolution request [\#206](https://github.com/ably/ably-asset-tracking-android/issues/206)

**Closed issues:**

- Remove unused metadata field from the Trackable class [\#293](https://github.com/ably/ably-asset-tracking-android/issues/293)
- Allow to specify the default resolution and trackables resolution in the publisher example app [\#277](https://github.com/ably/ably-asset-tracking-android/issues/277)
- Don't expose GeoJsonMessage in the API [\#272](https://github.com/ably/ably-asset-tracking-android/issues/272)
- Add a button that will focus the map on the asset location in the Subscriber example app [\#242](https://github.com/ably/ably-asset-tracking-android/issues/242)
- Show the zoom level controls on the map in the Subscriber example app [\#241](https://github.com/ably/ably-asset-tracking-android/issues/241)
- Start method of Publisher and Subscriber SDKs should be async [\#240](https://github.com/ably/ably-asset-tracking-android/issues/240)
- Subscriber should be able to request no resolution after creation [\#239](https://github.com/ably/ably-asset-tracking-android/issues/239)
- Remove unnecessary isStopped check from handling StopEvent in the Publisher [\#231](https://github.com/ably/ably-asset-tracking-android/issues/231)
- Improve Publisher stop [\#230](https://github.com/ably/ably-asset-tracking-android/issues/230)
- Consider adding @Throws annotation to the API methods [\#229](https://github.com/ably/ably-asset-tracking-android/issues/229)
- Rename AblyException to ConnectionException [\#218](https://github.com/ably/ably-asset-tracking-android/issues/218)
- Fix comments in the README file [\#217](https://github.com/ably/ably-asset-tracking-android/issues/217)
- Decide whether we should use battery level when comparing LocationUpdate objects [\#210](https://github.com/ably/ably-asset-tracking-android/issues/210)
- Improve adding trackable flow in the publisher example app [\#209](https://github.com/ably/ably-asset-tracking-android/issues/209)
- Unify the way of parsing enums sent over Ably channels across all platforms [\#207](https://github.com/ably/ably-asset-tracking-android/issues/207)
- Simplify location history version field [\#198](https://github.com/ably/ably-asset-tracking-android/issues/198)
- Rename AssetStatus [\#197](https://github.com/ably/ably-asset-tracking-android/issues/197)
- Remove superfluous prefix from PublisherState inner class [\#196](https://github.com/ably/ably-asset-tracking-android/issues/196)
- Trackables should be comparable by id, not by all fields [\#187](https://github.com/ably/ably-asset-tracking-android/issues/187)
- Create shared location history file format that can be used in Android and iOS [\#184](https://github.com/ably/ably-asset-tracking-android/issues/184)
- Reflect tracked asset state in publisher example app [\#183](https://github.com/ably/ably-asset-tracking-android/issues/183)
- App crashes when replaying file from background [\#181](https://github.com/ably/ably-asset-tracking-android/issues/181)
- Android: Send elevation and battery level of the device as part of the update [\#176](https://github.com/ably/ably-asset-tracking-android/issues/176)
- Move Ably to common project and share between subscriber and publisher [\#167](https://github.com/ably/ably-asset-tracking-android/issues/167)
- Remove the need to keep a reference to the "native" publisher or subscriber instance for Java users [\#166](https://github.com/ably/ably-asset-tracking-android/issues/166)
- Introduce simpler state model for publisher and subscriber [\#164](https://github.com/ably/ably-asset-tracking-android/issues/164)
- Investigate upgrading to Kotlin 1.4 [\#163](https://github.com/ably/ably-asset-tracking-android/issues/163)
- Refactor our Java interfaces for callbacks to use CompletableFuture [\#162](https://github.com/ably/ably-asset-tracking-android/issues/162)
- Refactor our Kotlin interfaces for event streaming to use Asynchronous Flow [\#161](https://github.com/ably/ably-asset-tracking-android/issues/161)
- Consider renaming the locations method on the publisher builder [\#145](https://github.com/ably/ably-asset-tracking-android/issues/145)
- Only dispatch events to main queue when they're intended as direct callbacks to the SDK user [\#135](https://github.com/ably/ably-asset-tracking-android/issues/135)
- Channel state observability [\#133](https://github.com/ably/ably-asset-tracking-android/issues/133)
- Move DebugConfiguration interfaces out to become part of standard API [\#132](https://github.com/ably/ably-asset-tracking-android/issues/132)
- Create another module for shared utilities that are used only in tests [\#128](https://github.com/ably/ably-asset-tracking-android/issues/128)
- Generate API documentation [\#122](https://github.com/ably/ably-asset-tracking-android/issues/122)
- Consider whether clearer delineation is required between public and internal [\#118](https://github.com/ably/ably-asset-tracking-android/issues/118)
- Decide which builders fields are required to create Publisher and Subscriber objects [\#116](https://github.com/ably/ably-asset-tracking-android/issues/116)
- Implement proper strategy for publisher stop [\#110](https://github.com/ably/ably-asset-tracking-android/issues/110)
- Improve fallback logic in resolution policy's resolve method [\#104](https://github.com/ably/ably-asset-tracking-android/issues/104)
- Reduce amount of fields in DefaultPublisher [\#103](https://github.com/ably/ably-asset-tracking-android/issues/103)
- Consider making the delineation between publisher and resolution policy interfaces clearer [\#87](https://github.com/ably/ably-asset-tracking-android/issues/87)
- Reflect tracked asset state in publisher example app [\#82](https://github.com/ably/ably-asset-tracking-android/issues/82)
- Refine approach for error reporting [\#81](https://github.com/ably/ably-asset-tracking-android/issues/81)
- Decide on "filter out" vs "batched together" for location updates [\#66](https://github.com/ably/ably-asset-tracking-android/issues/66)
- Move code from core module "common" package to a separate gradle module [\#58](https://github.com/ably/ably-asset-tracking-android/issues/58)
- Revisit Ably / Connection Configuration Naming [\#55](https://github.com/ably/ably-asset-tracking-android/issues/55)
- Move shared interfaces and classes from both SDKs to the core module [\#54](https://github.com/ably/ably-asset-tracking-android/issues/54)
- Fill unit testing gaps for the publisher builder [\#46](https://github.com/ably/ably-asset-tracking-android/issues/46)
- Allow event dispatch Looper to be specified explicitly in the SDK API [\#31](https://github.com/ably/ably-asset-tracking-android/issues/31)
- Decide whether we want to keep the debug features in the final SDK version [\#19](https://github.com/ably/ably-asset-tracking-android/issues/19)
- Check if AssetPublisher is started from a main thread [\#18](https://github.com/ably/ably-asset-tracking-android/issues/18)

**Merged pull requests:**

- Remove the metadata field from the Trackable class [\#294](https://github.com/ably/ably-asset-tracking-android/pull/294) ([KacperKluka](https://github.com/KacperKluka))
- Add skipped location updates to the LocationUpdate [\#292](https://github.com/ably/ably-asset-tracking-android/pull/292) ([KacperKluka](https://github.com/KacperKluka))
- Automatically center the camera on the marker position in subscriber example app [\#289](https://github.com/ably/ably-asset-tracking-android/pull/289) ([KacperKluka](https://github.com/KacperKluka))
- Only run deployment fail step on docs workflow if needed [\#284](https://github.com/ably/ably-asset-tracking-android/pull/284) ([QuintinWillison](https://github.com/QuintinWillison))
- Replace hard-coded repository URL components [\#283](https://github.com/ably/ably-asset-tracking-android/pull/283) ([QuintinWillison](https://github.com/QuintinWillison))
- Add optional Firebase Crashlytics integration to the publisher example app [\#281](https://github.com/ably/ably-asset-tracking-android/pull/281) ([KacperKluka](https://github.com/KacperKluka))
- Setting resolution for publisher and trackables in the publisher example app [\#279](https://github.com/ably/ably-asset-tracking-android/pull/279) ([KacperKluka](https://github.com/KacperKluka))
- Allow to add trackable from the trackable id input keyboard [\#278](https://github.com/ably/ably-asset-tracking-android/pull/278) ([KacperKluka](https://github.com/KacperKluka))
- Show zoom buttons in the subscriber example app [\#275](https://github.com/ably/ably-asset-tracking-android/pull/275) ([KacperKluka](https://github.com/KacperKluka))
- Update Mapbox to version 2.0 [\#274](https://github.com/ably/ably-asset-tracking-android/pull/274) ([KacperKluka](https://github.com/KacperKluka))
- Run MapboxNavigation calls in the main thread [\#273](https://github.com/ably/ably-asset-tracking-android/pull/273) ([KacperKluka](https://github.com/KacperKluka))
- Update code present in the README [\#270](https://github.com/ably/ably-asset-tracking-android/pull/270) ([KacperKluka](https://github.com/KacperKluka))
- Use batteryLevel when comparing location updates [\#266](https://github.com/ably/ably-asset-tracking-android/pull/266) ([KacperKluka](https://github.com/KacperKluka))
- Upgrade to Kotlin 1.4 [\#265](https://github.com/ably/ably-asset-tracking-android/pull/265) ([KacperKluka](https://github.com/KacperKluka))
- Throw an exception if the publisher state is accessed after it is disposed [\#264](https://github.com/ably/ably-asset-tracking-android/pull/264) ([KacperKluka](https://github.com/KacperKluka))
- Add docs workflow [\#263](https://github.com/ably/ably-asset-tracking-android/pull/263) ([QuintinWillison](https://github.com/QuintinWillison))
- Change actions order when removing a trackable from the Publisher [\#262](https://github.com/ably/ably-asset-tracking-android/pull/262) ([KacperKluka](https://github.com/KacperKluka))
- Change actions order when disconnecting from Ably channel [\#261](https://github.com/ably/ably-asset-tracking-android/pull/261) ([KacperKluka](https://github.com/KacperKluka))
- Clear the Publisher state after it is stopped [\#255](https://github.com/ably/ably-asset-tracking-android/pull/255) ([KacperKluka](https://github.com/KacperKluka))
- Remove redundant isStopped check in the CorePublisher [\#254](https://github.com/ably/ably-asset-tracking-android/pull/254) ([KacperKluka](https://github.com/KacperKluka))
- Allow the Subscriber to request no resolution from the Publisher [\#253](https://github.com/ably/ably-asset-tracking-android/pull/253) ([KacperKluka](https://github.com/KacperKluka))
- Use Ably and channel state when determining the trackable state in Subscriber SDK [\#252](https://github.com/ably/ably-asset-tracking-android/pull/252) ([KacperKluka](https://github.com/KacperKluka))
- Handle subscriber in the stopped state [\#250](https://github.com/ably/ably-asset-tracking-android/pull/250) ([KacperKluka](https://github.com/KacperKluka))
- Handle errors when starting Publisher and Subscriber [\#249](https://github.com/ably/ably-asset-tracking-android/pull/249) ([KacperKluka](https://github.com/KacperKluka))
- Change start to be an asynchronous function for Subscriber [\#248](https://github.com/ably/ably-asset-tracking-android/pull/248) ([KacperKluka](https://github.com/KacperKluka))
- Unify approach of GPS location updates [\#247](https://github.com/ably/ably-asset-tracking-android/pull/247) ([KacperKluka](https://github.com/KacperKluka))
- Add @Throws annotation to inform Java users about thrown exceptions [\#245](https://github.com/ably/ably-asset-tracking-android/pull/245) ([KacperKluka](https://github.com/KacperKluka))
- Rename AblyException to ConnectionException [\#244](https://github.com/ably/ably-asset-tracking-android/pull/244) ([KacperKluka](https://github.com/KacperKluka))
- Require only one of the two resolution checks to be passed in order to send a location update [\#243](https://github.com/ably/ably-asset-tracking-android/pull/243) ([KacperKluka](https://github.com/KacperKluka))
- Fix modifying Publisher state from the Ably's disconnect callback [\#224](https://github.com/ably/ably-asset-tracking-android/pull/224) ([KacperKluka](https://github.com/KacperKluka))
- Add token based authentication [\#222](https://github.com/ably/ably-asset-tracking-android/pull/222) ([KacperKluka](https://github.com/KacperKluka))
- Fix location history test files [\#221](https://github.com/ably/ably-asset-tracking-android/pull/221) ([KacperKluka](https://github.com/KacperKluka))
- Fix emitting trackable state in the publisher SDK [\#220](https://github.com/ably/ably-asset-tracking-android/pull/220) ([KacperKluka](https://github.com/KacperKluka))
- Fix subscriber not receiving any location updates [\#219](https://github.com/ably/ably-asset-tracking-android/pull/219) ([KacperKluka](https://github.com/KacperKluka))
- Unify the way of parsing enums sent over Ably channels [\#208](https://github.com/ably/ably-asset-tracking-android/pull/208) ([KacperKluka](https://github.com/KacperKluka))
- Simplify location history version field [\#205](https://github.com/ably/ably-asset-tracking-android/pull/205) ([KacperKluka](https://github.com/KacperKluka))
- Move Ably wrapper to the common module [\#204](https://github.com/ably/ably-asset-tracking-android/pull/204) ([KacperKluka](https://github.com/KacperKluka))
- Stopping strategy for the Publisher [\#203](https://github.com/ably/ably-asset-tracking-android/pull/203) ([KacperKluka](https://github.com/KacperKluka))
- Renaming a few classes [\#200](https://github.com/ably/ably-asset-tracking-android/pull/200) ([KacperKluka](https://github.com/KacperKluka))
- Decide which builder fields are required [\#199](https://github.com/ably/ably-asset-tracking-android/pull/199) ([KacperKluka](https://github.com/KacperKluka))
- Observe state of each trackable channel [\#194](https://github.com/ably/ably-asset-tracking-android/pull/194) ([KacperKluka](https://github.com/KacperKluka))
- Add missing error handling for Ably [\#193](https://github.com/ably/ably-asset-tracking-android/pull/193) ([KacperKluka](https://github.com/KacperKluka))
- Fix failing check workflow due to a Jacoco issue [\#192](https://github.com/ably/ably-asset-tracking-android/pull/192) ([KacperKluka](https://github.com/KacperKluka))
- Redesign publisher example app to support adding multiple assets [\#190](https://github.com/ably/ably-asset-tracking-android/pull/190) ([KacperKluka](https://github.com/KacperKluka))
- Use only the Trackable ID when comparing trackables [\#189](https://github.com/ably/ably-asset-tracking-android/pull/189) ([KacperKluka](https://github.com/KacperKluka))
- Expose publisher trackables set to the SDK users [\#188](https://github.com/ably/ably-asset-tracking-android/pull/188) ([KacperKluka](https://github.com/KacperKluka))
- New history file format [\#186](https://github.com/ably/ably-asset-tracking-android/pull/186) ([KacperKluka](https://github.com/KacperKluka))
- Add battery level and altitude \(elevation\) [\#182](https://github.com/ably/ably-asset-tracking-android/pull/182) ([KacperKluka](https://github.com/KacperKluka))
- Move DebugConfiguration interfaces out to become part of standard API [\#180](https://github.com/ably/ably-asset-tracking-android/pull/180) ([KacperKluka](https://github.com/KacperKluka))
- Add missing exception handling for the Ably SDK code [\#179](https://github.com/ably/ably-asset-tracking-android/pull/179) ([KacperKluka](https://github.com/KacperKluka))
- Add native API methods to the facade interfaces [\#178](https://github.com/ably/ably-asset-tracking-android/pull/178) ([KacperKluka](https://github.com/KacperKluka))
- Implement Java facades for Publisher and Subscriber [\#177](https://github.com/ably/ably-asset-tracking-android/pull/177) ([KacperKluka](https://github.com/KacperKluka))
- Introduce simpler state model for publisher and subscriber [\#175](https://github.com/ably/ably-asset-tracking-android/pull/175) ([KacperKluka](https://github.com/KacperKluka))
- Replay last event in the flow when someone starts listening to it [\#172](https://github.com/ably/ably-asset-tracking-android/pull/172) ([KacperKluka](https://github.com/KacperKluka))
- Refactor Asynchronous Interfaces [\#159](https://github.com/ably/ably-asset-tracking-android/pull/159) ([QuintinWillison](https://github.com/QuintinWillison))
- Run publisher in a service in the example app [\#158](https://github.com/ably/ably-asset-tracking-android/pull/158) ([KacperKluka](https://github.com/KacperKluka))
- Improve fallback logic in resolution policy's resolve method [\#156](https://github.com/ably/ably-asset-tracking-android/pull/156) ([KacperKluka](https://github.com/KacperKluka))
- Integration test for Publisher and Subscriber interaction [\#150](https://github.com/ably/ably-asset-tracking-android/pull/150) ([KacperKluka](https://github.com/KacperKluka))

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
