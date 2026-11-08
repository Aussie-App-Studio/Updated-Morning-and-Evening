# Privacy Policy for Updated Morning and Evening

**Last Updated:** November 8, 2026

This Privacy Policy applies to the **Updated Morning and Evening** mobile application ("the App"), developed and maintained by **Aussie App Studio**.

---

### 1. Summary: 100% Offline & Zero Data Collection

**Updated Morning and Evening** is built with complete privacy by design:

- **No Internet Permission:** The App does **not** request the `android.permission.INTERNET` permission. It is technically incapable of transmitting your data to any external server.
- **No Personal Information Collected:** We do not collect, store, process, share, or sell any personal data, contact details, identifiers, or usage habits.
- **No Accounts or Sign-Ins:** The App requires no registration, login, or user profile.
- **No Analytics, Ads, or Trackers:** The App contains zero advertisements, zero analytics SDKs (such as Firebase or Google Analytics), and zero third-party tracking libraries.

---

### 2. Local On-Device Storage

All data created while you use the App is stored **100% locally on your device** (using Android's standard private application storage / SharedPreferences):

- Your reading appearance and schedule preferences (theme, font, layout, edition, and reading plan).
- Your completed reading checkmarks and bookmarked/saved devotions.
- Your personal reflection notes written on individual devotions.

#### Backups and EPUB Exports
- When you use the **Export backup** feature (`.json`) or **Download full book** feature (`.epub`), the file is written directly to the folder you select on your device via the Android system file picker.
- We never have access to your notes, backups, or exported files.

---

### 3. Device Permissions and How They Are Used

The App requests only the minimum system permissions required for optional local features:

- **Notifications (`android.permission.POST_NOTIFICATIONS`):**
  - **Purpose:** Requested *only* if you explicitly turn on Daily Reminders in Settings to receive a morning or evening devotion notification.
  - **Local Only:** Notifications are scheduled and generated 100% locally on your device using Android's `AlarmManager`. No remote push notification servers are used.
- **Run at Startup (`android.permission.RECEIVE_BOOT_COMPLETED`):**
  - **Purpose:** Used solely to reschedule your enabled local daily reminder alarms after your phone restarts.

---

### 4. External Scripture Links

When you tap a Scripture verse reference inside a devotion, the App uses a standard Android `ACTION_VIEW` intent to open that passage in your chosen external Bible provider (**YouVersion / Bible.com**, **Bible Gateway**, or **Blue Letter Bible**) using your device's web browser or installed Bible app. Once you leave the App to view an external website or app, that third party's own privacy policy governs your visit.

---

### 5. Children’s Privacy

The App is suitable for readers of all ages and does not knowingly or unknowingly collect personal information from anyone, including children under the age of 13.

---

### 6. Changes to This Privacy Policy

If we update this Privacy Policy in the future, any changes will be posted on this page with an updated "Last Updated" date.

---

### 7. Contact Us

If you have any questions or feedback regarding this Privacy Policy or **Updated Morning and Evening**, please contact **Aussie App Studio** by opening an issue on our GitHub repository:  
[https://github.com/Aussie-App-Studio/Updated-Morning-and-Evening](https://github.com/Aussie-App-Studio/Updated-Morning-and-Evening)
