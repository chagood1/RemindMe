# RemindMe

RemindMe is a simple Android reminder app built with Kotlin, Jetpack Compose, Material 3, and Room Database.

## Project Status

This project is currently a working MVP. Users can create reminders, select a date and time, view saved reminders, delete reminders, and keep reminders saved after closing and reopening the app.

## Features

- Add personal reminders
- Select reminder date and time
- Save reminders locally with Room Database
- Display saved reminders in a clean Compose UI
- Delete reminders
- Persist reminders after app restart

## Tech Stack

- Kotlin
- Android Studio
- Jetpack Compose
- Material 3
- Room Database
- Gradle Kotlin DSL

## Current Limitations

- Notifications are not implemented yet
- Reminders do not currently trigger alarms
- Reminders cannot be edited yet
- Recurring reminders are not implemented yet

## Planned Improvements

- Add reminder notifications
- Add edit reminder functionality
- Add completed reminder status
- Sort reminders by date and time
- Improve validation for past dates and empty fields
- Add recurring reminders

## Purpose

This app is a personal Android development project intended to become a portfolio-quality mobile app. The current milestone focused on replacing temporary in-memory reminder storage with persistent local storage using Room Database.

## Testing Persistence

To confirm Room persistence works:

1. Run the app.
2. Add a reminder.
3. Close the app completely.
4. Reopen the app.
5. Confirm the reminder is still visible.

## Author

Clay Hagood
