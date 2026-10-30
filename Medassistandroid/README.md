# MedAssist for Android

One app, two modes, picked by who logs in:

- **Pharmacy mode** (owner and staff): GST billing with barcode scan, stock and expiry, the online-order queue, reports, staff and plan.
- **Customer mode** (everyone else): find pharmacies nearby, see who has a medicine in stock, order for pickup or delivery with a prescription photo, track the order, and get AI help identifying a medicine or checking interactions.

It talks to the Spring Boot API in [`../backend`](../backend). The API contract is in [`../docs/API.md`](../docs/API.md).

## Screens

| Pharmacy | Customer |
|---|---|
| **Home:** today's sales, open orders, low stock, expiring stock value, 7-day chart, branch switcher | **Explore:** pharmacies near you sorted by distance, medicine search, directions in Google Maps |
| **Sell:** search or scan, cart, customer, prescription photo for Rx medicines, cash/UPI/card/credit, refill reminder, idempotent checkout → GST invoice (shareable) | **Pharmacy:** catalogue with in-stock / few left / out of stock, basket, pickup or delivery, prescription photo, place order |
| **Orders:** New / Preparing / Ready / All; accept, mark ready, reject with reason, hand over and bill; view the customer's prescription | **Medicine:** details, which pharmacies within 15 km have it now, same-salt alternatives |
| **Stock:** levels, low stock, expiring in 90 days with value at cost; receive a batch; add products from the medicine master | **Orders:** history and live tracking (placed → accepted → ready → completed), cancel before acceptance |
| **More:** sales history, reports (sales, profit, payment split, top products, GST), staff invites and roles, plan and usage, notifications, account | **Scan:** identify a medicine from a photo (Gemini), interaction checker |

Both modes have notifications, light/dark theme, and switching between pharmacies (or into customer mode) for people who work at more than one.

## Architecture

Single module, the same layout as VenueSync:

```
app/src/main/java/com/medassist/app/
  core/            pure Kotlin, no android.* imports (portable to KMP)
    model/         @Serializable DTOs matching the API, ApiError, BigDecimal money serializer
    network/       Ktor client: retries only safe requests, single-origin guard, bearer auth
    auth/          SessionManager (single refresh under concurrent 401s), TokenStore interface
    repository/    one class per area; every call goes through apiCall() → Result<T> with a typed ApiError
  data/            Android side: Keystore-encrypted token store, preferences, location, photo scaling, current branch
  di/              Hilt module
  ui/              Compose + Material 3, MVVM with StateFlow/UiState
    auth/          welcome, login (with demo accounts), patient signup, pharmacy signup
    pharmacy/      dashboard, pos/, orders/, stock/, more/
    patient/       explore, pharmacy, medicine, my orders + tracking, scan
    account/       session, account, notifications
    common/        UiState, components, formatting (₹ with Indian grouping), theme
```

Decisions worth knowing:

- **Mode switch at the root.** `AppRoot` swaps the whole navigation tree when the session changes, so a pharmacy screen can never survive into the customer app after logout or a switch.
- **Money is `BigDecimal` end to end.** A custom serializer reads `105.00` exactly. The cart total is an estimate; the server's invoice (batch MRP, GST split) is the truth.
- **Checkout is idempotent.** Each checkout gets a UUID `Idempotency-Key`, reused on retry and replaced when the cart changes. A lost response followed by "Try again" returns the same invoice.
- **Retries only where safe:** GETs, and POSTs with an idempotency key, on 502/503/504 or network errors (Render's free tier sleeps). A plain POST is never repeated.
- **Tokens:** 15-minute access token, rotating refresh token. `SessionManager` holds a mutex, so five requests failing at once cause one refresh, not five. Reusing a rotated refresh token makes the server revoke the whole session. Tokens are stored AES-GCM-encrypted with a key in the Android Keystore.
- **No Maps SDK.** Pharmacies are a list sorted by distance, and "Directions" hands off to Google Maps with a `geo:` intent, so there's no API key or billing account.
- **No camera permission.** Photos come from the system camera (`TakePicture` + `FileProvider`) or the photo picker, and barcodes from Google's code scanner. Photos are scaled to 1600 px JPEG (~300 KB) before upload.

## Run it

1. Start the backend with demo data: `docker compose up --build` in the repo root (see the root README).
2. Open this folder in Android Studio and run on an emulator. The debug build calls `http://10.0.2.2:8080/api/v1`, which is the computer as seen from the emulator.
   - On a USB phone: `adb reverse tcp:8080 tcp:8080`, and add `medassist.apiBaseUrl=http://localhost:8080/api/v1` to `local.properties`.
   - Against a deployed API: `medassist.apiBaseUrl=https://<your-api>/api/v1`.
3. On the login screen tap **Pharmacy demo** or **Customer demo** (password `Demo@12345`).

Release builds use `medassist.releaseApiBaseUrl` from `local.properties` (or the `MEDASSIST_API_BASE_URL` env var), defaulting to `https://medassist-api.onrender.com/api/v1`.

## Tests

```bash
./gradlew testDebugUnitTest          # JVM: contract, error mapping, retries, session refresh, bill maths
./gradlew connectedDebugAndroidTest  # emulator + backend running: the end-to-end flow
```

- **Contract tests** decode real responses captured from the backend (`app/src/test/resources/fixtures/`), so a server change that breaks a model fails here first.
- **The end-to-end test** (`EndToEndTest`): the owner logs in, bills a sale and opens the invoice, then browses orders, stock and reports. A customer logs in, finds the demo pharmacy, orders and sees the tracking screen. CI ([`.github/workflows/android.yml`](../.github/workflows/android.yml)) runs it on an emulator against the real backend and uploads a screenshot of each screen.

## Play Store checklist

- [x] `targetSdk 36`, `versionName 1.0.0`, R8 shrinking (release APK ~2.5 MB)
- [x] HTTPS only (cleartext allowed only to the emulator host and localhost), backups off for the token store
- [x] Release signing reads `keystore.properties` or env vars; the keystore is never committed
- [ ] Create the upload keystore and `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`)
- [ ] Deploy the backend and set `medassist.releaseApiBaseUrl`
- [ ] Privacy policy URL (the app handles prescriptions, which are health data) and the Data safety form
- [ ] Store listing: screenshots (CI produces them), feature graphic, a demo login for reviewers
- [ ] `./gradlew bundleRelease` → upload the AAB to a closed-testing track
