# Build and Test Commands

## Lint
```bash
./gradlew lint
```

## Type Check (Kotlin)
```bash
./gradlew compileDebugKotlin compileReleaseKotlin
```

## Unit Tests
```bash
./gradlew test
```

## Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## All Tests
```bash
./gradlew test connectedAndroidTest
```

## Build Debug
```bash
./gradlew assembleDebug
```

## Build Release
```bash
./gradlew assembleRelease
```

## Clean
```bash
./gradlew clean
```

## Code Coverage
```bash
./gradlew testDebugUnitTestCoverage
```
