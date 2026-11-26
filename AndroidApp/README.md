# Accurate Account Services - Android Native App (Java + XML)

This is the native Android application for **Accurate Account Services**, connecting to the Node.js Express + Socket.IO backend and PostgreSQL database.

---

## Features
- **Authentication**: Sign in with role-based session caching (`admin`, `employee`, `client`) via `SessionManager`.
- **Task Management**: Real-time list of tasks with priority badges (`High`, `Medium`, `Low`), statuses, and due dates with pull-to-refresh.
- **Real-Time Chat**: Live messaging powered by `Socket.io-client` with distinct incoming and outgoing chat bubble layouts.
- **Network Layer**: Retrofit 2 + Gson converter + OkHttp logging.
- **Material UI**: Modern Material Design Components, ConstraintLayout, and RecyclerViews.

---

## How to Open and Run in Android Studio

### 1. Open the Project
1. Open **Android Studio**.
2. Click **Open** (or **File > Open**).
3. Browse and select the folder:
   `c:\Users\spectre\ACC APP\AccurateAccountServices\AndroidApp`
4. Android Studio will automatically recognize the Gradle files and download dependencies.

### 2. Configure Backend Host (Emulator vs Physical Phone)
The app is pre-configured in `com.accurateaccounts.app.utils.Constants.java`:
```java
public class Constants {
    // 10.0.2.2 connects to your computer's localhost from Android Emulator
    public static final String BASE_URL = "http://10.0.2.2:3000/";
}
```
* **If running on the Android Emulator**: Keep `http://10.0.2.2:3000/`.
* **If running on a physical Android phone**: Connect your phone to the same Wi-Fi network as your computer, find your computer's local IP (e.g. `ipconfig` -> `192.168.1.50`), and change the URL to `http://192.168.1.50:3000/`.

### 3. Start the Node.js Server
Before launching the app, start your Node.js backend:
```powershell
cd "c:\Users\spectre\ACC APP\AccurateAccountServices\Server"
node index.js
```

### 4. Run the App
1. In Android Studio, select your emulator or connected device from the device dropdown.
2. Click the green **Run (Play)** button (`Shift + F10`).
3. Log in with your admin credentials:
   - **Username**: `admin`
   - **Password**: `admin123`

