# TaskSnap

This is a much healthier budget ($100) for an app involving system-level integrations and real-time
syncing. Spreading it over 6 Milestones allows for granular testing of the complex features (like
Call Triggers) before moving to the next step.
Here is the Updated Project Plan & Milestone Breakdown for TaskSnap.

Project: TaskSnap (Android Productivity)
Budget: $100.00 USD
Timeline: 6 Milestones
Milestone 1: Foundation & The Eisenhower Matrix ($10.00)
Goal: Setup the project structure and the unique visual interface.
Deliverables:
Android Setup: Kotlin project initialization with Firebase dependencies.
UI Implementation: The "Eisenhower Matrix" Home Screen (4-Quadrant Grid: Urgent/Important).
Task Data Model: Database structure (Title, Description, Quadrant ID, Timestamp).
Basic Entry: A floating "+" button to manually add a task inside the app.
Milestone 2: Local Data & Task Management ($15.00)
Goal: A fully functional offline task manager.
Deliverables:
CRUD Operations: Create, Read, Update, Delete tasks.
Logic: Toggles for "Urgent?" and "Important?" that auto-move tasks between the 4 quadrants.
Persistence: Setup Room Database (Local SQL) so tasks are saved on the phone even without internet.
Swipe Actions: Swipe to complete or delete a task.
Milestone 3: "Universal Capture" Part 1 (Text & Share) ($15.00)
Goal: Integrating with the Android System (Text Selection & WhatsApp).
Deliverables:
Process Text Intent: Add "TaskSnap" to the Android Copy/Paste menu (Select text in Chrome -> Create
Task).
Share Intent: Register app as a target for "Share via..." (Forward WhatsApp msg -> Create Task).
Quick Entry Dialog: A semi-transparent popup window that handles these inputs without needing to
fully open the main app.
Milestone 4: "Universal Capture" Part 2 (Call Triggers) ($20.00)
Goal: The complex "Call Ended" logic.
Deliverables:
Permissions Engine: Logic to request READ_PHONE_STATE and READ_CALL_LOG compliantly.
Broadcast Receiver: Detect when a call changes state to IDLE (Ended).
The Prompt: Trigger a "Bubble" or Notification Action: "Add task for [Caller Name]?"
Linkage: Auto-fill the contact name into the task description.
Milestone 5: Team Sync & Cloud Architecture ($20.00)
Goal: Moving from Local-only to Cloud-Collaborative.
Deliverables:
User Auth: Login via Phone Number (OTP).
Firebase Integration: Migrate local data to Firebase Realtime Database/Firestore.
Delegation UI: "Assign To" button (Select from Contacts).
Sync: When User A assigns a task, it instantly appears on User B's screen.
Milestone 6: Collaboration & Final Polish ($20.00)
Goal: Level 2 Collaboration and Launch readiness.
Deliverables:
Task Chat: Simple comment section inside each task.
Push Notifications: Alerts for "New Task Assigned" and "Task Completed".
Testing: Verify "Call Trigger" works on different Android versions (10, 11, 12, 13).
Final APK: Signed build ready for release.

Developer Note for Milestone 4 (Call Trigger)
Warning: Android 9+ restricts access to Call Logs strictly.
Requirement: During Milestone 4, the developer must implement a fallback. If Google Play rejects the
sensitive permission, the app should offer a "Manual Notification" toggle (a persistent notification
in the drawer that the user can tap quickly during a call).
