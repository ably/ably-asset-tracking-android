# AWS Device Farm battery tests of Ably Asset Tracking SDKs for Android

## Overview

We wanted to create automated battery tests that will measure battery usage of the SDKs and check the impact of different resolution policies on that aspect.

[AWS Device Farm](https://aws.amazon.com/device-farm/) was chosen as the platform on which those battery tests will be run.
It allows to run tests on real devices which should produce meaningful battery usage data.
One issue is that those devices are always plugged in.
To overcome that we had to use a special command to make the devices think that they're unplugged and should use their battery.

Currently, the tests have been withheld due to the results being unreliable and insufficient.

## How to run tests

### Required files
To be able to run battery tests on AWS Device Farm we have to prepare 3 files:
- Application APK
- Application Android test APK
- Test specification file

### Preparing required files
The appropriate test specification file can be found [here](test_spec.yaml). To get the APKs you should run below shell command

```shell
./gradlew clean assembleDebug assembleAndroidTest
```

you can also specify a module name to make the assemble commands run quicker

```shell
./gradlew clean module-name:assembleDebug module-name:assembleAndroidTest
```

The generated APKs will be available in the `build` folder of the module.

### Running tests on AWS Device Farm
After you have all things needed you should go to the AWS Device Farm console and create a new test run.

When choosing a device you should consider its battery size.
The battery usage data is shown as a percentage value and it only has two decimal places (e.g. 15.08%).
Using a device with smaller battery will result in bigger values and more visible differences.

After the test is done you should download the `Customer Artifacts` from the device's "Test Suite" files.

Instead of using the console you can perform above actions in a [CLI script](https://docs.aws.amazon.com/cli/latest/reference/devicefarm/index.html)

### Analyzing battery data
Inside the downloaded `Customer Artifacts` files you should find the `bugreport.zip` file which can be analyzed with the [battery historian](https://developer.android.com/topic/performance/power/setup-battery-historian).
After uploading the file to the battery historian you need to filter the data by choosing the application's package name from the list.
Then look for a value described as "Device estimated power use" and you should find the battery usage value.

## Test results

For our tests we used a 1 hour long trip recording.
That recording was replayed with different resolutions to check how they affect the battery life.
GPS battery usage isn't affecting those results.
Only network transfer resolution is impacting the battery values.

### Resolutions used for tests
- **A** - Accuracy: Balanced, Desired interval: 1000ms, Minimal distance: 1.0m
- **B** - Accuracy: Balanced, Desired interval: 1ms, Minimal distance: 0.0m
- **C** - Accuracy: Balanced, Desired interval: 5000ms, Minimal distance: 5.0m
- **D** - Accuracy: Balanced, Desired interval: 5000ms, Minimal distance: 50000.0m

### Devices and test results
**Google Pixel 3 XL (battery size 3430 mAh)**
- A: 0.08%
- B: 0.08%
- C: 0.07%
- D: 0.07%

**Galaxy S6 Edge (battery size 2600 mAh)**
- A: 0.39%, 0.36%, 0.36%
- D: 0.35%, 0.36%, 0.37%

### Conclusions
Test results from the smaller battery device show that the data is unreliable.
The resolution D sends messages with at least 5 times slower rate than the resolution A and the battery usage results for resolution D were sometimes higher than for resolution A.
