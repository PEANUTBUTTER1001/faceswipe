# Faceswipe

얼굴 제스처로 화면을 조작하는 Android 접근성 앱

전면 카메라로 고개 움직임, 윙크, 입 벌림을 인식해 지정된 앱에서 스와이프, 탭 등의 동작을 손 대지 않고 실행됩니다.


## 🗂️ 프로젝트 개요

- **앱 이름** : Faceswipe
- **구성원** : 1인 개발
- **작업 기간** : 2026.06 ~ 작업중(배포 준비 중)
- **플랫폼** : Android
- **개발 언어** : Kotlin
- **개발 환경** : Android Studio
- **외부API 및 서비스** : On-device AI Google ML Kit API

## 🎯 서비스 목표

손 사용이 불편한 사용자가 특정 반복 동작을 얼굴 움직임만으로 수행하기 위한 앱입니다.


## 🧰 기술 스택

| 분류           | 기술                                                            |
|--------------|---------------------------------------------------------------|
| Language     | Kotlin                                                        |
| Architecture | MVVM (Clean Architecture)                                     |
| Asynchronous | Coroutine, Flow                                               |
| UI           | Jetpack Compose (Material3), Navigation                       |
| On-device AI | Google ML Kit (Face Detection), CameraX                       |
| DataBase     | DataStore (Preferences)                                       |
| DI           | Hilt                                                          |
| BUILD        | Gradle Version Catalog(libs.versions.toml), Convention Plugin |
| ETC / Tools  | Android Studio, GitHub, SourceTree                            |


## ✨ 서비스 주요 기능

- 전면 카메라 기반 실시간 얼굴 인식 (CameraX + ML Kit Face Detection)
- 5가지 제스처 트리거 감지: 고개 왼쪽, 고개 오른쪽, 왼쪽 윙크, 오른쪽 윙크, 입 벌림
- 3가지 실행 동작: 아래로 스와이프, 위로 스와이프, 탭
- 앱별로 제스처와 동작을 자유롭게 매핑 (설정값은 DataStore에 영속 저장)
- 대상 앱이 포그라운드일 때만 카메라를 활성화하는 절전 동작
- 포그라운드 서비스로 백그라운드에서도 안정적으로 동작

## 📱 현재 제공되는 서비스 앱

- YouTube
- 밀리의 서재

## 📖 사용법

### 1. 필요한 권한 설정

- 카메라 권한 (앱 실행 시 자동으로 요청)

- 접근성 서비스 (시스템 설정 > 접근성 > 설치된 서비스 목록에서 faceswipe 활성화)

### 2. 제스처 매핑 설정

설정 화면에서 대상 앱(YouTube / 밀리의 서재)별로 각 제스처에 어떤 동작을 연결할지 지정합니다.

설정은 즉시 저장되고 실행 중인 서비스에도 바로 반영됩니다.

### 3. 실행

홈 화면에서 "faceswipe 시작"을 누르면 포그라운드 서비스가 시작됩니다.

이후 대상 앱을 열면 카메라가 활성화되고(상태 표시줄에 카메라 사용 아이콘 표시) 얼굴 제스처가
인식되며 대상 앱이 아닌 화면에서는 카메라가 절전 모드로 대기합니다.

중지하려면 홈 화면에서 "faceswipe 중지"를 누르거나 최근 앱 목록에서 앱을 제거합니다.

## 👆 제스처와 동작

| 제스처 트리거            | 설명            |
|--------------------|---------------|
| 고개 왼쪽 (HeadLeft)   | 고개를 왼쪽으로 회전   |
| 고개 오른쪽 (HeadRight) | 고개를 오른쪽으로 회전  |
| 왼쪽 윙크 (WinkLeft)   | 왼쪽 눈만 감았다 뜨기  |
| 오른쪽 윙크 (WinkRight) | 오른쪽 눈만 감았다 뜨기 |
| 입 벌림 (MouthOpen)   | 입을 벌렸다 다물기    |

| 실행 동작                     | 설명             |
|---------------------------|----------------|
| 세로 스와이프 (SwipeVertical)   | 위아래 방향 스와이프    |
| 가로 스와이프 (SwipeHorizontal) | 좌우 방향 스와이프     |
| 탭 (Tap)                   | 화면 하단 1/4 영역 탭 |

일반 깜빡임, 말하기, 하품 등과 구분하기 위해 윙크와 입 벌림에는 최소 유지 시간과 임계값이 적용됩니다.

## 🏗️ 프로젝트 아키텍쳐

Now in Android(NiA) 아키텍처를 참고한 멀티모듈 구성입니다.

```
faceswipe/
├── app/                  앱 진입점, MainActivity, 네비게이션 그래프
├── feature/faceswipe/    화면(Compose), Route/Screen, ViewModel
├── domain/faceswipe/     UseCase, 제스처 감지기, 도메인 모델, 인터페이스
├── data/faceswipe/       Repository 구현, 두 서비스, DataStore, 상태 관리
├── core/ui/              공통 UI 테마 및 컴포넌트
└── build-logic/          Convention Plugin (공통 빌드 설정)
```

레이어 의존 방향은 app → feature → domain 이며, data가 domain의 인터페이스를 구현하는 의존 역전 구조구성입니다.

## 🔐 필요 권한

| 권한                                                 | 용도                                     |
|----------------------------------------------------|----------------------------------------|
| `CAMERA`                                           | 전면 카메라로 얼굴 인식                          |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_CAMERA` | 백그라운드 카메라 유지                           |
| `POST_NOTIFICATIONS`                               | 포그라운드 서비스 알림 (Android 13+)             |
| 접근성 서비스                                            | 포그라운드 앱 감지 및 제스처 주입 (사용자가 설정에서 직접 활성화) |

