# AudioEQ Development Phases

## Phase 0: Environment Setup (Prerequisite)

### 0.1 JDK Installation
```powershell
# Install JDK 17 (required for Android Gradle Plugin 8.x)
# Option A: Using Chocolatey (recommended)
choco install microsoft-openjdk17 -y

# Option B: Manual download
# Download from: https://learn.microsoft.com/en-us/java/openjdk/download

# Set JAVA_HOME environment variable
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Microsoft\jdk-17", "User")

# Add to PATH
[System.Environment]::SetEnvironmentVariable("PATH", "$env:PATH;$env:JAVA_HOME\bin", "User")
```

### 0.2 Android SDK Setup
```powershell
# Install Android Studio (includes SDK)
# Download from: https://developer.android.com/studio

# Set ANDROID_HOME
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")

# Accept SDK licenses
$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager --licenses
```

### 0.3 Verify Environment
```powershell
# Verify Java
java -version          # Should show 17.x.x

# Verify Android SDK
$env:ANDROID_HOME\platform-tools\adb version

# Verify Gradle wrapper
cd AudioEQ
.\gradlew.bat --version
```

### 0.4 Environment Validation Tests
```powershell
# Test 1: Gradle sync
.\gradlew.bat tasks --quiet

# Test 2: Compile check
.\gradlew.bat compileDebugKotlin compileReleaseKotlin

# Test 3: Lint check
.\gradlew.bat lint
```

**Acceptance Criteria:**
- JAVA_HOME points to JDK 17
- ANDROID_HOME points to Android SDK
- `gradlew --version` succeeds
- Project compiles without errors

---

## Phase 1: Core DSP Foundation

### 1.1 Development Tasks
| Component | Status | File |
|-----------|--------|------|
| BiquadFilter | DONE | `equalizer/BiquadFilter.kt` |
| FilterBand data class | DONE | `equalizer/BiquadFilter.kt` |
| FilterType enum | DONE | `equalizer/BiquadFilter.kt` |
| ParametricEqualizer | DONE | `equalizer/ParametricEqualizer.kt` |
| EqualizerPreset | DONE | `equalizer/ParametricEqualizer.kt` |

### 1.2 Unit Test Files
| Test File | Status | Coverage |
|-----------|--------|----------|
| `BiquadFilterTest.kt` | DONE | Filter types, reset, zero gain |
| `ParametricEqualizerTest.kt` | DONE | Band operations, presets, buffer processing |
| `FilterBandTest.kt` | DONE | Data class operations |
| `FrequencyResponseTest.kt` | DONE | Frequency response validation |
| `EdgeCaseTest.kt` | DONE | Boundary conditions |
| `PerformanceBenchmarkTest.kt` | DONE | Speed benchmarks |

### 1.3 Running Unit Tests
```powershell
# Run all Phase 1 unit tests
.\gradlew.bat test --tests "com.audioeq.equalizer.*"

# Run specific test class
.\gradlew.bat test --tests "com.audioeq.BiquadFilterTest"

# Run with verbose output
.\gradlew.bat test --tests "com.audioeq.equalizer.*" --info

# Generate coverage report
.\gradlew.bat testDebugUnitTestCoverage
```

### 1.4 Validation Commands
```powershell
# Compile check
.\gradlew.bat compileDebugKotlin compileReleaseKotlin

# Run all unit tests
.\gradlew.bat test

# Check test results
cat app\build\reports\tests\testDebugUnitTest\index.html
```

**Acceptance Criteria:**
- [ ] All 6 filter types working (PEAKING, LOW_SHELF, HIGH_SHELF, LOW_PASS, HIGH_PASS, NOTCH)
- [ ] Frequency response validated against reference implementations
- [ ] Unit test coverage > 90%
- [ ] All tests pass

---

## Phase 2: Audio Pipeline

### 2.1 Development Tasks
| Component | Status | File |
|-----------|--------|------|
| CircularBuffer | DONE | `audio/CircularBuffer.kt` |
| AudioCapture | DONE | `audio/AudioCapture.kt` |
| AudioOutput | DONE | `audio/AudioOutput.kt` |
| AudioPipeline | DONE | `audio/AudioPipeline.kt` |

### 2.2 Unit Test Files
| Test File | Status | Coverage |
|-----------|--------|----------|
| `CircularBufferTest.kt` | DONE | Thread safety, wraparound, partial read/write |
| `AudioPipelineTest.kt` | DONE | Start/stop, processing, buffer management |

### 2.3 Instrumented Test Files
| Test File | Status | Device Required |
|-----------|--------|-----------------|
| `CircularBufferInstrumentedTest.kt` | DONE | Yes |

### 2.4 Running Tests
```powershell
# Unit tests only
.\gradlew.bat test --tests "com.audioeq.audio.*"

# Instrumented tests (requires connected device/emulator)
.\gradlew.bat connectedAndroidTest --tests "com.audioeq.audio.*"

# Both unit and instrumented
.\gradlew.bat test connectedAndroidTest
```

**Acceptance Criteria:**
- [ ] CircularBuffer thread-safe under concurrent access
- [ ] AudioPipeline processes audio with < 50ms latency
- [ ] No audio dropouts during processing
- [ ] All unit tests pass

---

## Phase 3: Service Integration

### 3.1 Development Tasks
| Component | Status | File |
|-----------|--------|------|
| AudioProcessingService | DONE | `service/AudioProcessingService.kt` |
| PermissionManager | DONE | `service/PermissionManager.kt` |
| ProcessingState enum | DONE | `service/AudioProcessingService.kt` |

### 3.2 Service Features
- Foreground service with media projection
- Notification display
- Service lifecycle management
- MediaProjection callback handling

### 3.3 Running Tests
```powershell
# Instrumented tests (requires device)
.\gradlew.bat connectedAndroidTest --tests "com.audioeq.service.*"

# Manual testing checklist:
# 1. Start service from MainActivity
# 2. Grant MediaProjection permission
# 3. Verify notification appears
# 4. Play audio from another app (Spotify, YouTube Music)
# 5. Verify EQ affects audio output
# 6. Stop service cleanly
```

**Acceptance Criteria:**
- [ ] Service starts/stops cleanly
- [ ] Handles permission denial gracefully
- [ ] Survives configuration changes
- [ ] Notification displays correctly

---

## Phase 4: UI Implementation

### 4.1 Development Tasks
| Component | Status | File |
|-----------|--------|------|
| MainActivity | DONE | `ui/MainActivity.kt` |
| MainViewModel | DONE | `ui/MainViewModel.kt` |
| EqualizerView | DONE | `ui/EqualizerView.kt` |
| activity_main.xml | DONE | `res/layout/activity_main.xml` |

### 4.2 UI Features
- Start/Stop button
- Preset dropdown
- Touch-interactive equalizer visualization
- Status display

### 4.3 Running UI Tests
```powershell
# UI instrumented tests
.\gradlew.bat connectedAndroidTest --tests "com.audioeq.ui.*"

# Manual testing:
# 1. Install debug APK
.\gradlew.bat installDebug

# 2. Launch app
adb shell am start -n com.audioeq/.ui.MainActivity

# 3. Test touch interaction on equalizer bands
# 4. Test preset selection
# 5. Test start/stop functionality
```

**Acceptance Criteria:**
- [ ] UI responsive under load
- [ ] Smooth 60fps animation
- [ ] Touch interaction updates bands
- [ ] Preset selection works

---

## Phase 5: Integration Testing

### 5.1 End-to-End Test Scenarios
```powershell
# Run all instrumented tests
.\gradlew.bat connectedAndroidTest

# Run all unit tests
.\gradlew.bat test

# Combined
.\gradlew.bat test connectedAndroidTest
```

### 5.2 Manual Integration Tests
| Scenario | Steps | Expected Result |
|----------|-------|-----------------|
| Basic Flow | Start → Grant Permission → Play Music | Audio processed through EQ |
| Preset Change | Start → Select "Bass Boost" | Bass frequencies boosted |
| Band Adjustment | Start → Drag band slider | Frequency curve updates |
| Stop/Restart | Start → Stop → Start | Service restarts cleanly |
| Permission Deny | Start → Deny Permission | Graceful error message |

### 5.3 Performance Testing
```powershell
# Run performance benchmarks
.\gradlew.bat test --tests "com.audioeq.equalizer.PerformanceBenchmarkTest"

# Check results for:
# - Processing time < buffer duration
# - Memory usage < 100MB
# - CPU usage < 10% when idle
```

**Acceptance Criteria:**
- [ ] All instrumented tests pass
- [ ] Memory usage < 100MB
- [ ] CPU usage < 10% idle
- [ ] No ANRs in production scenarios

---

## Phase 6: Release Validation

### 6.1 Pre-Release Checklist
```powershell
# 1. Clean build
.\gradlew.bat clean

# 2. Full test suite
.\gradlew.bat test connectedAndroidTest

# 3. Lint check
.\gradlew.bat lint
cat app\build\reports\lint-results-debug.html

# 4. Coverage report
.\gradlew.bat testDebugUnitTestCoverage
cat app\build\reports\coverage\debug\index.html

# 5. Release build
.\gradlew.bat assembleRelease
```

### 6.2 Code Quality Gates
| Metric | Threshold | Command |
|--------|-----------|---------|
| Unit Test Coverage | > 80% | `.\gradlew.bat testDebugUnitTestCoverage` |
| Lint Errors | 0 | `.\gradlew.bat lint` |
| Lint Warnings | < 10 | `.\gradlew.bat lint` |
| Compilation | Success | `.\gradlew.bat compileReleaseKotlin` |

### 6.3 Release Build
```powershell
# Generate signed release APK
.\gradlew.bat assembleRelease

# Output location
ls app\build\outputs\apk\release\

# Verify APK
$env:ANDROID_HOME\build-tools\34.0.0\aapt dump badging app\build\outputs\apk\release\app-release.apk
```

### 6.4 Final Validation
```powershell
# Install release APK on test device
adb install app\build\outputs\apk\release\app-release.apk

# Run monkey test for stability
adb shell monkey -p com.audioeq -v 10000

# Check for crashes
adb logcat -d | Select-String "AndroidRuntime: FATAL"
```

**Release Criteria:**
- [ ] Code coverage > 80%
- [ ] Zero critical bugs
- [ ] Zero lint errors
- [ ] All tests pass
- [ ] Release APK builds successfully
- [ ] No crashes in monkey test

---

## Quick Reference Commands

```powershell
# Environment Setup
java -version                                    # Verify JDK
.\gradlew.bat --version                         # Verify Gradle

# Development
.\gradlew.bat compileDebugKotlin                # Compile only
.\gradlew.bat assembleDebug                     # Build debug APK
.\gradlew.bat installDebug                      # Install debug APK

# Testing
.\gradlew.bat test                              # All unit tests
.\gradlew.bat test --tests "com.audioeq.equalizer.*"  # Specific package
.\gradlew.bat connectedAndroidTest              # All instrumented tests
.\gradlew.bat test connectedAndroidTest         # All tests

# Quality
.\gradlew.bat lint                              # Lint check
.\gradlew.bat testDebugUnitTestCoverage         # Coverage report

# Release
.\gradlew.bat assembleRelease                   # Build release APK
.\gradlew.bat clean assembleRelease             # Clean release build

# Troubleshooting
.\gradlew.bat clean                             # Clean build
.\gradlew.bat --info test                       # Verbose test output
.\gradlew.bat --stacktrace assembleDebug       # Stack trace on error
```

---

## Test Coverage Summary

### Unit Tests (JVM)
| Package | Test Files | Status |
|---------|------------|--------|
| `equalizer` | 6 files | READY |
| `audio` | 2 files | READY |

### Instrumented Tests (Android)
| Package | Test Files | Status |
|---------|------------|--------|
| `equalizer` | 1 file | READY |
| `audio` | 1 file | READY |

### Total Test Count
- Unit Tests: ~40 test methods
- Instrumented Tests: ~4 test methods
