# Weather Forecast

<div align="center">

  ![Created At](https://img.shields.io/github/created-at/untried-duck61/weather_android?style=flat-square&color=blue)
  ![Last Commit](https://img.shields.io/github/last-commit/untried-duck61/weather_android?style=flat-square&color=orange)
  ![Commit Activity](https://img.shields.io/github/commit-activity/t/untried-duck61/weather_android?style=flat-square&color=brightgreen)
  ![Issues](https://img.shields.io/github/issues/untried-duck61/weather_android?style=flat-square)
  ![Downloads](https://img.shields.io/github/downloads/untried-duck61/weather_android/total?style=flat-square&color=purple)
  ![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/untried-duck61/weather_android/android.yml?style=flat-square&logo=android)

</div>

<p align="center">
  <b>A modern, lightweight Android application to check weather forecasts, fully tailored with dynamic Material You styling.</b>
</p>

---

## Screenshots
<p align="center">
  <!-- Замените ссылки ниже на реальные скриншоты, когда загрузите их в свой репозиторий -->
  <img src="screenshots/first_setup.jpg" width="240" alt="First Setup Screen">  <img src="screenshots/main_screen.jpg" width="240" alt="Main Screen">
</p>

## Tech Stack & Specifications

* **OS & Hardware Compatibility:**

  <table>
    <tr>
      <td><b>OS Version</b></td>
      <td><img src="https://img.shields.io/badge/Android-14%2B-3DDC84?style=flat-square&logo=android&logoColor=3DDC84"/></td>
    </tr>
    <tr>
      <td><b>Architecture</b></td>
      <td><img src="https://img.shields.io/badge/Architecture-arm64--v8a-blue?style=flat-square" alt="arm64-v8a"/></td>
    </tr>
  </table>

* **SDK Configuration:**<br>
  ![compileSdk](https://img.shields.io/badge/compileSdk-36-orange?style=flat-square)<br>
  ![targetSdk](https://img.shields.io/badge/targetSdk-36-orange?style=flat-square)<br>
  ![minSdk](https://img.shields.io/badge/minSdk-34-brightgreen?style=flat-square)

* **Technologies Used:**<br>
  ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)<br>
  ![Android Views](https://img.shields.io/badge/UI_Framework-Android_Views-3DDC84?style=flat-square&logo=android&logoColor=3DDC84)

---

## Features & Stability

* [x] **Crash-free:** Fixed critical app crash after opening the application several times.

* [x] **Dynamic Themes:** Full support for Material You palette based on user's wallpaper.

## Supported languages

 * English (default)

 * Russian (ru)

## Roadmap & Tasks

* [ ] Add more detailed weather information on the main page (wind (WIP), humidity (done), pressure (WIP)).

* [ ] Add Settings page. (WIP)

* [ ] Implement in-app updates check.

* [x] **Fix Back Button:** Call `finish()` in `FirstRunActivity` after routing to prevent returning to the onboarding/splash screen.
