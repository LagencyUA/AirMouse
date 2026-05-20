# AirMouse & Keyboard: Remote Control System / Система дистанційного керування

An open-source client-server system that transforms an Android smartphone into a fully customizable virtual mouse, keyboard, and multimedia controller for a Windows PC. Developed as a graduation (diploma) project.

Клієнт-серверна система, яка перетворює Android-смартфон на повністю кастомізовану віртуальну мишу, клавіатуру та мультимедійний контролер для ПК на базі Windows. Розроблено в рамках дипломного проєкту.

---

## 🌍 Language / Мова
*   [🇬🇧 English Description](#english-project-overview)
*   [🇺🇦 Український опис](#український-опис-проєкту)

---

## English: Project Overview

### 🚀 Architecture & Tech Stack
The project is built using a **monorepository** approach and consists of two main solutions communicating via low-latency **TCP Sockets** with custom **JSON packet validation**:

*   **Host (Server):** Windows desktop application built with **WPF (C# / .NET 10)**. It runs in the system tray, parses incoming JSON payloads, and emulates OS-level input events using native Windows API (`SendInput`).
*   **Client (Android App):** Native Android application built with **Java/Kotlin (Android Studio)**. It features a fully dynamic UI engine for layout management and real-time touch gesture processing.

### ✨ Key Features
*   **Dynamic Layout Engine:** Create, edit, rename, and delete custom control profiles. Buttons can be arranged freely using an adjustable **Snap Grid** system with support for custom sizes, icons, and z-index overlapping.
*   **Advanced Touchpad:** High-fidelity mouse simulation supporting single taps (LMB), two-finger taps (RMB), two-finger scrolling, and seamless **Drag-and-Drop / Selection** handling via state-machine touch tracking.
*   **Smooth Cursor Motion:** Integrated **adaptive Exponential Moving Average (EMA)** on the WPF host side to smooth out network jitter and Android touch discretization, resulting in fluid monitor-rate cursor movement.
*   **Rich Input Protocol:** Full support for three discrete button states (`down`, `up`, `click`) for both mouse and hardware keyboard keys, alongside dedicated **action modifiers**.
*   **Media & Keyboard Controls:** Built-in system text input bridging and expanded host key dictionaries (Play/Pause, Volume, Shortcuts).

### 🛠️ Installation & Build Guide 
> *Notice: Ready-to-use binaries (.exe for Windows and .apk for Android) are available under the [Releases](https://github.com/LagencyUA/AirMouse/releases) tab. You can download and run them instantly without manual compilation.*

1.  **Clone the repository:**
```bash
    git clone https://github.com/your-username/AirMouse-Project.git
```

2. **Build the Windows Host (Server):**
* Open `airmouse-server-wpf/AirMouseHost.sln` in **Visual Studio**.
* Change the build configuration from `Debug` to **`Release`** on the top panel.
* Right-click the project -> **Publish** -> Select **Folder** target.
* In Profile Settings, enable **Self-contained** deployment and check **Produce single file** to compile everything into a standalone `AirMouse Host.exe`.
* Click **Publish** and locate your executable in the output folder.


3. **Build the Android Client:**
* Open `airmouse-client-android/` in **Android Studio**.
* Let Gradle sync all dependencies.
* Navigate to **Build** -> **Generate APK Bundles or APKs** -> **Generate APKs**.
* Once finished, click *Locate* in the bottom-right popup to get your standalone `.apk` file.
---

## Український: Опис проєкту

### 🚀 Архітектура та Технологічний Стек

Проєкт реалізовано за принципом **монорепозиторію** та складається з двох основних частин, що взаємодіють через високошвидкісні **TCP Сокети** за допомогою валідації кастомних **JSON-пакетів**:

* **Хост (Сервер):** Десктопний додаток під Windows на базі **WPF (C# / .NET 8)**. Працює у системному треї, парсить вхідні JSON-пакети та емулює системні події введення за допомогою Windows API (`SendInput`).
* **Клієнт (Мобільний додаток):** Нативний Android-додаток, розроблений на **Java/Kotlin (Android Studio)**. Містить динамічний інтерфейс для конструювання індивідуальних робочих просторів.

### ✨ Головні можливості

* **Система Динамічних Лейаутів:** Можливість створювати, редагувати та видаляти кастомні пульти керування. Елементи розміщуються за допомогою системи вирівнювання по сітці (**Snap Grid**) із налаштуванням масштабу, іконок та шарів відображення (z-index).
* **Просунутий Тачпад:** Симуляція миші з підтримкою одиночних тапів (ЛКМ), тапів двома пальцями (ПКМ), скролу двома пальцями, а також коректного **затиснення та виділення (Drag-and-Drop)** завдяки вбудованій стейт-машині жестів.
* **Згладжування Руху:** Інтегрована **адаптивна Експоненційна ковзна середня (EMA)** на стороні WPF-хоста, що нівелює мережеві затримки (jitter) та дискретність сенсора смартфона, забезпечуючи плавний рух курсора.
* **Трирівневий Протокол Введення:** Підтримка трьох станів для кнопок (`натиснення`, `відпуск`, `клік`) для миші та клавіш хоста, а також підтримка **клавіш-модифікаторів** (Ctrl, Alt, Shift).
* **Клавіатура та Медіа-модуль:** Програмний виклик системної Android-клавіатури для введення тексту на ПК та підтримка гарячих мультимедійних клавіш.

### 🛠️ Розгортання та Запуск
> *Примітка: Готові до використання білди (.exe для Windows та .apk для Android) уже доступні у вкладці [Releases](https://github.com/LagencyUA/AirMouse/releases). Ви можете завантажити та запустити їх миттєво без ручної компіляції коду.*

1. **Склонуйте репозиторій:**
```bash
git clone [https://github.com/your-username/AirMouse-Project.git
```

2. **Збірка хоста Windows (Сервер):**
   * Відкрийте файл `airmouse-server-wpf/AirMouseHost.sln` у **Visual Studio**.
   * Змініть конфігурацію збірки з `Debug` на **`Release`** на верхній панелі.
   * Клікніть правою кнопкою миші на проєкт -> **Publish** -> Виберіть ціль **Folder** (Папка).
   * У налаштуваннях профілю увімкніть **Self-contained** режим та поставте галочку **Produce single file**, щоб скомпілювати все в один автономний файл `AirMouse Host.exe`.
   * Натисніть **Publish** та знайдіть готовий файл у вихідній папці.

3. **Збірка клієнта Android:**
   * Відкрийте папку `airmouse-client-android/` в **Android Studio**.
   * Зачекайте завершення синхронізації інструменту Gradle.
   * Перейдіть у меню **Build** -> **Generate APK Bundles or APKs** -> **Generate APKs**.
   * Після завершення натисніть *Locate* у спливаючому вікні, щоб отримати готовий `.apk` файл для встановлення.

---

## 📜 License / Ліцензія

This project is licensed under the MIT License - see the LICENSE file for details.
Цей проєкт поширюється під ліцензією MIT.
