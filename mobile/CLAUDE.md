# Subscription Manager Android App

## Project overview

Offline-first Kotlin Android application for tracking subscriptions and trials locally.

- No accounts
- No cloud sync
- No internet permission
- No analytics
- No ads
- No backend

All subscription data and settings are stored locally on the device.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- WorkManager
- Navigation Compose
- Hilt
- MVVM
- Repository pattern
- DataStore Preferences

## Architecture

Main package: `com.example.subscription_manager`

```text
data/
  database/
    AppDatabase.kt
    TypeConverters.kt
  dao/
    SubscriptionDao.kt
  entity/
    SubscriptionEntity.kt
  mapper/
    SubscriptionMappers.kt
  preferences/
    UserPreferencesStore.kt
  repository/
    SubscriptionRepositoryImpl.kt
  worker/
    NotificationCheckWorker.kt
    ReminderNotificationWorker.kt
    RescheduleRemindersWorker.kt
  di/
    DatabaseModule.kt
    RepositoryModule.kt

domain/
  model/
    Subscription.kt
    SubscriptionForm.kt
    SubscriptionType.kt
    Recurrence.kt
    ReminderTime.kt
    ThemeMode.kt
    HomeSubscriptionItem.kt
  repository/
    SubscriptionRepository.kt
  usecases/
  utils/
    DateCalculator.kt

presentation/
  ui/
    navigation/App.kt
    screens/home/
    screens/subscription/
    screens/settings/
    theme/
  viewmodels are colocated with their screens
```

## Key behavior

### Subscription model

Subscriptions support:

- `Subscription` and `Trial` types
- Monthly and annual recurrence
- Optional notes
- Optional start date
- Optional end date
- Payment day `1..31`
- Annual payment month `1..12`
- Renewal enabled/disabled
- Current-cycle paid tracking

Payment status is cycle-based:

- Monthly paid cycle key: `YYYY-MM`
- Annual paid cycle key: `YYYY`
- `isPaid` is computed from `paidCycleKey == currentCycleKey(nextPaymentDate)`
- When the cycle changes, the subscription automatically becomes unpaid again.

### Renewal and visibility

- Subscriptions remain visible even after `endDate`.
- Renewal can be disabled manually.
- `willRenew` is true only when renewal is enabled and there is a future payable cycle.

### Date edge cases

`DateCalculator` handles:

- February dates
- Leap years
- Payment day 31 on shorter months
- Annual renewals
- End dates in the past
- Timezone via `LocalDate` and system default zone conversion for UI date pickers

### Home sorting

Home cards are sorted into:

1. Due now
2. Upcoming
3. Paid

Status colors:

- Paid: primary
- Due soon: orange
- Overdue: error red
- Upcoming: surface variant

## Notifications

Notifications are local-only and scheduled with WorkManager.

Important classes:

- `NotificationScheduler`
- `NotificationPublisher`
- `ReminderNotificationWorker`
- `NotificationCheckWorker`
- `RescheduleRemindersWorker`
- `BootReceiver`
- `MarkPaidBroadcastReceiver`

Reminder schedule:

- Default reminder time: `09:00`
- Reminders are scheduled for 3, 2, 1, and 0 days before the next payment date.
- A daily WorkManager check worker keeps reminders alive after reboot or long periods without opening the app.
- Marking a subscription paid cancels current-cycle reminders and schedules the next cycle.
- Notification action: `Mark Paid`.

Android 13+ requires the user to grant `POST_NOTIFICATIONS`; Settings provides a permission button.

## Settings

Settings are stored in DataStore Preferences:

- Reminder time
- Theme mode:
  - System
  - Light
  - Dark

## Build files

Gradle version catalog is in:

```text
gradle/libs.versions.toml
```

App module:

```text
app/build.gradle.kts
```

Important plugins:

- Android Application
- Kotlin Compose
- Hilt
- KSP

Important dependencies:

- Compose BOM
- Material 3
- Navigation Compose
- Room
- WorkManager
- DataStore Preferences
- Hilt Android / Hilt Navigation Compose / Hilt Work

## Manifest

`app/src/main/AndroidManifest.xml` includes:

- `.SubscriptionApplication`
- `.MainActivity`
- `.notification.BootReceiver`
- `.notification.MarkPaidBroadcastReceiver`
- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `allowBackup="false"`

## Running

From the `mobile` directory:

```bash
./gradlew assembleDebug
```

On Windows PowerShell, use:

```powershell
.\gradlew assembleDebug
```
