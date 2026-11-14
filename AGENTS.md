# faceswipe — 에이전트 작업 규칙
얼굴 제스처(고개 좌우·입 벌림)로 유튜브·밀리의 서재를 넘기는 Android 접근성 앱 · 설계 이유와 결정 기록: `ARCHITECTURE.md` (예정)
Source of Truth: `settings.gradle.kts`, `gradle/libs.versions.toml`, `build-logic/` · 이 문서는 사용자 승인을 받아서만 개정한다.

## 1. 명령
- 설치: (미정)
- 실행: `./gradlew :app:assembleDebug`
- 테스트: `./gradlew test`
- 정적 검사: `./gradlew lint`
- 출시 빌드: `./gradlew :app:assembleRelease` (R8 적용. 서명은 FACESWIPE_KEYSTORE_* 환경변수 4개가 있을 때만, README §출시 빌드)
- 빌드에는 JDK 21이 필요하다(JAVA_HOME). 클래스를 지우거나 옮긴 뒤 Hilt 생성 코드 오류가 나면 해당 모듈을 clean 후 다시 빌드한다.

## 2. 구조 지도
역할 이름으로 판단한다. 폴더 이름으로 역할을 추측하지 않는다.

| 역할 | 경로 | 들어가는 것 |
|---|---|---|
| 진입 | `feature/faceswipe/` | 화면(Compose Route/Screen)과 ViewModel |
| 유스케이스 | `domain/faceswipe/src/main/kotlin/com/peanutbutter1001/faceswipe/domain/faceswipe/usecase/` | 한 가지 목적의 흐름 조율 |
| 도메인 | `domain/faceswipe/` | 제스처 감지기, 모델, 기본 매핑. Android 의존성 없음 |
| 포트 | `domain/faceswipe/src/main/kotlin/com/peanutbutter1001/faceswipe/domain/faceswipe/repository/` | 저장소 인터페이스 |
| 인프라 | `data/faceswipe/` | DataStore, CameraX·ML Kit, 포그라운드·접근성·타일 서비스, 오버레이 |
| 조립 | `app/` | Application·MainActivity·NavHost. Hilt 모듈은 `data/faceswipe/src/main/kotlin/com/peanutbutter1001/faceswipe/data/faceswipe/di/` |
| 공유 | `core/ui/` | 테마(고정 팔레트), 설정 목록 행 컴포넌트 |
| 테스트 | `domain/faceswipe/src/test/` | 도메인·유스케이스 단위 테스트. 화면 판정 로직은 `feature/faceswipe/src/test/` |

## 3. 의존 규칙
규칙은 §2의 역할 이름으로 적는다. 경로는 §2에만 둔다.
- [목표] (R1) 의존은 진입 → 유스케이스 → 도메인 방향으로만 흐른다. 인프라는 포트를 구현하며, 도메인·포트는 진입·인프라·조립을 import하지 않는다.
- [강제] (R2) 파일·DB·네트워크·IPC·시계·환경변수 접근은 인프라와 조립에서만 한다. 도메인은 Android 의존성이 없는 Kotlin JVM 모듈이다.
- [목표] (R3) 구현체 생성과 연결은 조립에서만 한다. 그 밖의 코드는 협력자를 생성자 주입(Hilt)으로 받는다.
- [목표] (R4) 진입 코드는 입력 검증 → 유스케이스(또는 포트) 호출 → 출력 변환만 한다. 규칙과 계산을 두지 않는다.
- [목표] 화면 상태는 `sealed interface` UiState를 ViewModel의 단일 `StateFlow`로 노출하고, Route가 구독해 상태 없는 Screen에 넘긴다.

## 4. 무엇을 어디에
| 넣을 것 | 위치 |
|---|---|
| 사용자에게 보이는 문구 | 해당 모듈의 strings.xml (예: `feature/faceswipe/src/main/res/values/strings.xml`). 코드에 하드코딩 금지 |
| 제스처·동작·대상 앱 표시 이름 (서비스 오버레이와 화면 공용) | `data/faceswipe/src/main/kotlin/com/peanutbutter1001/faceswipe/data/faceswipe/label/GestureLabels.kt` |
| 새로 설치했을 때의 기본 제스처 매핑 | `domain/faceswipe/src/main/kotlin/com/peanutbutter1001/faceswipe/domain/faceswipe/model/DefaultGestureMappings.kt` |
| 제스처 감지 임계값 | `domain/faceswipe/src/main/kotlin/com/peanutbutter1001/faceswipe/domain/faceswipe/model/GestureConfig.kt` |
| 설정 목록 행·치수 토큰 | `core/ui/src/main/kotlin/com/peanutbutter1001/faceswipe/core/ui/component/SettingsList.kt` |
| 색 토큰 | `core/ui/src/main/kotlin/com/peanutbutter1001/faceswipe/core/ui/theme/Theme.kt` |
| UI 디자인 지침 | `docs/android-settings.md` (UI를 만들거나 고치기 전에 읽는다) |
| 라이브러리 버전·좌표 | `gradle/libs.versions.toml` (빌드 파일에 직접 쓰지 않음) |

## 5. 새 기능 레시피
- [목표] (R8) 작업 전에 관련 코드의 책임·의존성·기존 관례를 확인하고 아래 순서를 따른다. 일반적인 모범 사례를 이유로 현재 구조를 일괄 교체하지 않는다.
1. 도메인 모델과 규칙
2. 포트 (R7 조건을 충족할 때만)
3. 유스케이스 (흐름 조율이 필요할 때만)
4. 인프라 구현과 Hilt `@Binds`
5. 진입: UiState → ViewModel(`@HiltViewModel`) → Route/Screen, 다중 `@Preview`
6. 조립: NavHost에 목적지 등록
7. 테스트: 도메인은 I/O 없이, 포트는 테스트 대역으로

## 6. 알려진 예외
- [목표] (R9) 아래 예외는 늘리지 않는다. 수정하는 파일에 예외가 있어도 요청 범위 밖이면 고치지 않고 별도 작업으로 제안한다. 예외 추가는 §9에 따라 승인받는다.

| 규칙 | 파일 | 사유 |
|---|---|---|
| R1 | `feature/faceswipe` | 진입(ViewModel·Route)이 인프라(`AppStateManager`, `PermissionChecker`, `ServiceController`, `TileRequester`, `GestureLabels`)를 직접 사용 |

## 7. 하지 말 것
- [목표] (R7) 인터페이스·유스케이스·모듈·공통 컴포넌트는 구현이 2개 이상이거나 테스트 대역이 실제로 필요할 때만 만들고, 추가할 때는 해결할 문제(현재의 결합, 중복, 테스트 어려움, 변경 영향)를 밝힌다.
- [목표] (R11) 요청된 구현에 필요한 변경만 한다. 더 큰 리팩터링이 유익해 보여도 요청 범위 밖이면 같은 변경에 섞지 않고 먼저 제안으로 분리한다.
- 유스케이스 위에 서비스 계층을 한 겹 더 두지 않는다.
- "나중을 위한" 인터페이스·기반 클래스·범용 헬퍼를 만들지 않는다.
- XML 레이아웃·RxJava를 쓰지 않는다. UI는 Compose, 비동기는 Coroutines·Flow, DI는 Hilt만 쓴다.
- 윙크(눈 감음) 제스처를 다시 넣지 않는다. 오인식이 잦아 제거했다.
- `GestureConfig` 임계값은 실측 근거 없이 바꾸지 않는다. 현재 값은 사용자가 조정한 값이다.
- 프로젝트 고유 함정:
  - 접근성 이벤트의 `packageName`은 활성 창이 아닐 수 있다(백그라운드 유튜브가 이벤트를 보냄). 포그라운드 판정은 `rootInActiveWindow`로 한다. 런처를 무시 목록에 넣지 않는다.
  - 카메라 포그라운드 서비스는 백그라운드에서 시작할 수 없다. 타일 등 화면 밖 진입점은 보이는 Activity를 거쳐 시작한다.
  - ML Kit이 진단 통계를 전송한다(`INTERNET`). 개인정보 문구에 "아무것도 보내지 않음"이라고 쓰지 않는다.
  - 밀리의 서재는 USB 디버깅 중 실행을 막는다. 해당 앱 동작은 USB를 뺀 상태에서 확인한다.
  - (사람이 작성)

## 8. 완료 조건
- §1의 실행·테스트·정적 검사 명령이 모두 통과한다.
- 새 도메인 규칙에는 I/O 없는 단위 테스트가 있다.
- §2·§4에 없는 위치에 파일을 만들지 않았다.
- 모듈·의존성·명령·경로를 바꿨다면 AGENTS.md를 직접 고치지 않고 `/agents-md`로 개정을 요청한다.

## 9. 멈추고 물어볼 때
- [목표] (R10) §2에 없는 역할·위치나 새 계층·모듈이 필요하면 구현 전에 멈추고 보고한다.
- [목표] (R12) 설계나 리팩터링을 제안할 때는 어떤 책임·의존성이 어떻게 달라지는지와 어떤 테스트로 동작 보존을 확인할지를 함께 제시한다.
- 새 외부 의존성(라이브러리, 서비스)을 추가할 때
- 저장 형식(DataStore 키)·권한·접근성 서비스 설정·매니페스트 진입점을 바꿀 때
- 백그라운드 실행·포그라운드 서비스·알림 권한처럼 Android 버전별 제약이 있는 API를 쓸 때
- §6 예외를 추가할 때
- 데이터 삭제처럼 되돌릴 수 없는 작업을 할 때
