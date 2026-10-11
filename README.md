# 폴드 애니메이션 (갤럭시 Z 폴드7용 데모)

힌지 각도 센서 값에 맞춰 스크린샷 두 장(커버 화면 / 내부 화면)을 블러 + 입체 효과로 전환하는 데모 앱입니다.
앱을 켜둔 상태에서만 동작하며, 실제 홈 화면이나 다른 앱 위에는 적용되지 않습니다.

## APK 만들기 (PC 필요)

1. Android Studio 설치: https://developer.android.com/studio
2. Android Studio에서 `Open` → 이 `FoldAnim` 폴더 선택
3. 처음 열면 필요한 파일을 자동으로 내려받습니다 (몇 분 소요). 업그레이드를 권하는 창이 뜨면 무시해도 됩니다.
4. 둘 중 하나:
   - 폰을 USB로 연결(개발자 옵션 → USB 디버깅 켜기)하고 상단의 ▶ Run 버튼 → 바로 설치됨
   - 메뉴 `Build` → `Build App Bundle(s) / APK(s)` → `Build APK(s)` → `app/build/outputs/apk/debug/app-debug.apk`를 폰으로 옮겨 설치

## 사용법

1. 커버 홈 화면과 내부 홈 화면을 각각 스크린샷으로 찍습니다.
2. 앱을 열고 `커버 화면 스크린샷 선택`, `내부 화면 스크린샷 선택`으로 두 장을 지정합니다.
3. 화면을 탭해 메뉴를 숨기고 폰을 접었다 펴 봅니다.
4. 접을 때도 효과를 보려면: 설정 → 디스플레이 → `커버 화면에서 앱 계속 사용`에서 이 앱을 켭니다.

## 느낌 조절

`app/src/main/java/com/foldanim/demo/FoldView.java` 맨 위 상수 4개를 바꾸면 됩니다.

- `COVER_END_ANGLE`: 커버 화면에서 블러가 최대가 되는 각도
- `INNER_START_ANGLE`: 내부 화면에서 애니메이션이 시작되는 각도
- `MAX_TILT_DEG`: 양쪽 절반이 기울어지는 정도
- `FOLLOW`: 각도 변화를 따라가는 속도
