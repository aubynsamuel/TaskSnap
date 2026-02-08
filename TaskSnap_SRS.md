# Software Requirements Specification (SRS)

**Project Name:** TaskSnap

---

## 1. Introduction

### 1.1 Purpose

The purpose of this document is to define the functional and non-functional requirements for *
*TaskSnap**, an Android productivity application. TaskSnap distinguishes itself by combining the *
*Eisenhower Matrix** prioritization method with **Universal Capture** capabilities (System Call &
Text triggers), allowing users to capture tasks immediately from any context on their phone.

### 1.2 Scope

TaskSnap is a native Android application that serves two primary functions:

1. **Task Capture**: Intercepting system events (Phone Calls, Text Selection, Share Intents) to
   prompt immediate task creation.
2. **Task Management**: Organizing tasks into a 4-Quadrant Eisenhower Matrix (Urgent/Important) with
   offline-first capabilities and cloud synchronization for team collaboration.

### 1.3 Definitions & Acronyms

* **Eisenhower Matrix**: A productivity framework dividing tasks into four quadrants: Do First (
  Urgent/Important), Schedule (Important/Not Urgent), Delegate (Urgent/Not Important), and Delete (
  Not Urgent/Not Important).
* **Universal Capture**: The ability to create a task from "outside" the app (e.g., from a web
  browser or after a phone call) without manually launching the app first.
* **Quick Entry Overlay**: A floating window or dialog that allows task input over other
  applications.

---

## 2. Overall Description

### 2.1 Product Perspective

TaskSnap works as a standalone Android application with deep system integrations.

* **Client**: Native Android (Kotlin/Jetpack Compose).
* **Local Storage**: Room Database (SQLite) for offline-first data persistence.
* **Backend**: Firebase (Firestore, Authentication, Cloud Functions) for sync and collaboration.

### 2.2 User Characteristics

* **Target Audience**: Professionals, extensive phone users, and teams who need to organize tasks
  rapidly.
* **Key Need**: Reducing friction between "having a thought/receiving a call" and "recording the
  task."

### 2.3 Operating Environment

* **OS**: Android 10 (API Level 29) and above.
* **Permissions**: Requires sensitive permissions for `READ_PHONE_STATE`, `READ_CALL_LOG`, and
  `SYSTEM_ALERT_WINDOW`.

---

## 3. Functional Requirements

### 3.1 Feature: The Eisenhower Matrix (Core UI)

The home screen shall display a 2x2 grid representing the four quadrants.

* **FR-1.1**: User shall view tasks categorized into:
    * **Q1**: Urgent & Important
    * **Q2**: Not Urgent & Important
    * **Q3**: Urgent & Not Important
    * **Q4**: Not Urgent & Not Important
* **FR-1.2**: User shall be able to manually create a task via a floating action button ("+").
* **FR-1.3**: When creating a task, the user shall toggle "Urgent?" and "Important?" switches, which
  automatically assigns the task to the correct quadrant.
* **FR-1.4**: Users shall be able to swipe tasks to complete (Done) or delete them.

### 3.2 Feature: Universal Capture - Text & Share

* **FR-2.1 Process Text**: The app shall appear in the Android System Text Selection Menu (
  Copy/Paste/TaskSnap). Selecting this shall open the Quick Entry Overlay with the selected text
  pre-filled.
* **FR-2.2 Share Intent**: The app shall accept `ACTION_SEND` intents (text/plain) from other apps (
  e.g., WhatsApp, Chrome). Sharing to TaskSnap shall open the Quick Entry Overlay.

### 3.3 Feature: Universal Capture - Call Triggers

* **FR-3.1 Call Detection**: The system shall detect when a phone call (incoming or outgoing) ends.
* **FR-3.2 VoIP Support**: The system shall attempt to detect VoIP calls (e.g., WhatsApp) that route
  through the system telecom manager.
* **FR-3.3 Overlay Prompt**: Upon call completion, a transparent Overlay Window (Truecaller-style)
  shall appear immediately.
* **FR-3.4 Context Pre-fill**: The overlay shall pre-fill the task description with the Contact Name
  or Phone Number of the other party.
* **FR-3.5 Reliability**: The call detection service must self-heal if the application process is
  killed by the OS (using Manifest Broadcast Receivers as a watchdog).

### 3.4 Feature: Data Persistence (Offline First)

* **FR-4.1**: All tasks must be saved locally to a Room Database immediately upon creation.
* **FR-4.2**: The app must be fully functional without an internet connection.

### 3.5 Feature: Cloud Sync & Collaboration (Milestones 5-6)

* **FR-5.1 Authentication**: Users shall log in using Phone Number Authentication (Firebase Auth).
* **FR-5.2 Cloud Sync**: Local data shall synchronize with Firestore when online.
* **FR-5.3 Delegation**: Users shall be able to "Assign" a task to another registered user via their
  phone number.
* **FR-5.4 Real-time Updates**: Assigned tasks shall appear on the assignee's device in real-time.

---

## 4. Non-Functional Requirements

### 4.1 System Reliability

* **NFR-1**: The Call Monitor Service must utilize a Foreground Service with a persistent
  notification to prevent OS termination.
* **NFR-2**: The system must employ a "Hybrid Receiver" strategy (Dynamic + Manifest receivers) to
  ensure compatibility across Android versions (Android 9 through 14).

### 4.2 Performance

* **NFR-3**: The Call Ended Overlay must appear within 2 seconds of the call disconnecting.
* **NFR-4**: App launch time (Cold Start) should be under 1.5 seconds.

### 4.3 Privacy & Security

* **NFR-5**: The app must check for permissions at runtime and gracefully degrade (e.g., fallback to
  manual notifications) if sensitive permissions (Call Log) are denied or restricted by Google Play
  policy.
* **NFR-6**: User data (Tasks) must be encrypted in transit (Firebase SDK handles SSL) and access
  controlled via security rules (users can only access their own or shared tasks).

---

## 5. Interface Requirements

### 5.1 Quick Entry Overlay

* Semi-transparent background (dimmed).
* Card-based input form centered on screen.
* "Save" and "Dismiss" buttons prominent.
* Must function over the Lock Screen (if permitted by OS).

---

## 6. Data Model (Schema)

### 6.1 Task Entity

```kotlin
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val isUrgent: Boolean,     // Determines Q1/Q3 vs Q2/Q4
    val isImportant: Boolean,  // Determines Q1/Q2 vs Q3/Q4
    val createdTimestamp: Long,
    val source: TaskSource,    // MANUAL, CALL, SHARED_TEXT
    val relatedContact: String?, // Phone number or Name from call
    val assignedTo: String?,   // User ID of assignee (Cloud only)
    val isSynced: Boolean = false
)
```
