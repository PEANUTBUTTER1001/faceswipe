# Daykit Coding Skills & Patterns

이 문서는 Daykit 프로젝트에서 실제로 사용되는 구현 패턴과 코드 템플릿을 정리한 레퍼런스입니다.
새로운 기능을 추가할 때 이 문서의 패턴을 그대로 따르세요.

---

## 1. 모듈 구조 & 패키지 경로

```
:domain:<feature>   → 순수 Kotlin, 안드로이드 의존성 없음
:data:<feature>     → Room / 스케줄러 / 리시버 / 서비스 구현체
:feature:<feature>  → Jetpack Compose UI + ViewModel
:core:ui            → 공통 컴포넌트 / 테마 / 미리보기 어노테이션
:app                → DI 진입점, Navigation, Scaffold
```

패키지 루트: `com.peanutbutter1001.daykit`

| 모듈 | 패키지 예시 |
|---|---|
| `:domain:alarm` | `…daykit.domain.alarm.model`, `…usecase`, `…repository` |
| `:data:alarm` | `…daykit.data.alarm.local`, `…repository`, `…di`, `…receiver` |
| `:feature:alarm` | `…daykit.feature.alarm.screen`, `…navigation`, `…model` |
| `:core:ui` | `…daykit.core.ui.component`, `…theme`, `…preview` |

---

## 2. Domain 레이어 패턴

### 2.1 Entity (Model)

```kotlin
// domain/alarm/model/Alarm.kt
data class Alarm(
    val id: Long = 0L,
    val title: String = "",
    val time: LocalTime,
    val isActive: Boolean = true,
    val activeDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    // ...
)
```

- `Long` ID는 기본값 `0L` — Room이 auto-generate
- `Set<DayOfWeek>` 등 Java Time API 적극 활용
- 안드로이드 타입(`Context`, `Uri` 등) **절대 포함 금지**

### 2.2 Repository Interface

```kotlin
// domain/alarm/repository/AlarmRepository.kt
interface AlarmRepository {
    fun getAlarms(): Flow<List<Alarm>>
    suspend fun insertAlarm(alarm: Alarm): Long
    suspend fun updateAlarm(alarm: Alarm)
    suspend fun deleteAlarm(alarmId: Long)
}
```

- 읽기 스트림 → `Flow<T>`
- 쓰기 작업 → `suspend fun`
- 반환값이 필요한 경우 (`insertAlarm`) 명시적으로 반환

### 2.3 UseCase

```kotlin
// domain/alarm/usecase/AddAlarmUseCase.kt
class AddAlarmUseCase @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarm: Alarm) {
        val newId = repository.insertAlarm(alarm)
        scheduler.schedule(alarm.copy(id = newId))
    }
}
```

- 클래스 1개 = 책임 1개 (단일 책임 원칙)
- `operator fun invoke`로 호출 지점을 간결하게: `addAlarmUseCase(alarm)`
- `@Inject constructor` 사용, Hilt 모듈 별도 등록 불필요

---

## 3. Data 레이어 패턴

### 3.1 Room Entity & Mapper

```kotlin
// data/alarm/local/AlarmEntity.kt
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val timeHour: Int,
    val timeMinute: Int,
    // ...
)

// data/alarm/local/AlarmMapper.kt
fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    title = title,
    time = LocalTime.of(timeHour, timeMinute),
    // ...
)

fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    title = title,
    timeHour = time.hour,
    timeMinute = time.minute,
    // ...
)
```

- Entity ↔ Domain 변환은 반드시 Mapper 함수로 분리 (`toDomain()` / `toEntity()`)
- `LocalTime`, `Set<DayOfWeek>` 등 복합 타입은 `TypeConverter`로 직렬화

### 3.2 Hilt 모듈 (interface 바인딩)

```kotlin
// data/alarm/di/AlarmDataModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmDataModule {

    @Binds @Singleton
    abstract fun bindAlarmRepository(
        impl: AlarmRepositoryImpl
    ): AlarmRepository

    @Binds @Singleton
    abstract fun bindAlarmScheduler(
        impl: AlarmSchedulerImpl
    ): AlarmScheduler
}
```

- interface 바인딩은 `@Binds` (abstract 함수), 생성자가 필요하면 `@Provides`
- 스코프는 원칙적으로 `@Singleton`
- 모듈 파일은 `di/` 하위에 기능별로 분리

---

## 4. Feature 레이어 패턴

### 4.1 UiState 정의

```kotlin
sealed interface AlarmUiState {
    object Loading : AlarmUiState
    data class Success(val alarms: List<Alarm>) : AlarmUiState
    data class Error(val message: String) : AlarmUiState
}
```

- `sealed interface` 사용 (sealed class 아님)
- 상태는 **Loading / Success / Error** 3종 기본 세트
- 상태별 필요 데이터는 `data class` 서브타입에 포함

### 4.2 ViewModel

```kotlin
@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val toggleAlarmUseCase: ToggleAlarmUseCase,
    // ...
) : ViewModel() {

    val uiState: StateFlow<AlarmUiState> = someFlow
        .map<List<Alarm>, AlarmUiState> { AlarmUiState.Success(it) }
        .catch { emit(AlarmUiState.Error(it.message ?: "오류")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlarmUiState.Loading
        )

    fun toggleAlarm(alarmId: Long, isActive: Boolean) {
        viewModelScope.launch {
            toggleAlarmUseCase(alarmId, isActive)
        }
    }
}
```

- `@HiltViewModel` + `@Inject constructor` 필수
- 단일 `StateFlow<UiState>` 원칙 (상태 분산 금지)
- `SharingStarted.WhileSubscribed(5000)` 표준 사용
- 쓰기 함수는 `viewModelScope.launch { useCase(...) }` 패턴

### 4.3 Composable 3단 구조

```
AlarmRoute          ← ViewModel 연결, 플랫폼 Side Effect (권한 요청 등)
  └─ AlarmRouteContent  ← (선택) 상태 전달 브릿지 레이어
       └─ AlarmScreen   ← Stateless, UiState를 분기하여 렌더링
```

```kotlin
// 1단: Route — ViewModel 구독 & Side Effect
@Composable
fun AlarmRoute(
    viewModel: AlarmViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { /* 권한 요청, 초기화 등 */ }
    AlarmScreen(
        uiState = uiState,
        onToggleAlarm = viewModel::toggleAlarm,
        // ...
    )
}

// 2단: Screen — Stateless, when(uiState) 분기
@Composable
fun AlarmScreen(
    uiState: AlarmUiState,
    onToggleAlarm: (Long, Boolean) -> Unit,
    // ...
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AlarmUiState.Loading -> { /* ... */ }
        is AlarmUiState.Success -> { /* LazyColumn 등 */ }
        is AlarmUiState.Error   -> { /* ... */ }
    }
}
```

- `modifier: Modifier = Modifier`는 모든 Composable의 마지막 파라미터
- Route에서만 `hiltViewModel()` 호출, Screen은 순수 함수
- `collectAsState()`는 Route에서만 사용

---

## 5. Navigation 패턴

```kotlin
// feature/alarm/navigation/AlarmNavigation.kt
fun NavGraphBuilder.alarmScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    composable<AlarmListRoute> {
        AlarmRoute(
            onNavigateToAdd = onNavigateToAdd,
            onNavigateToEditAlarm = onNavigateToEdit
        )
    }
}

fun NavController.navigateToAlarm() {
    navigate(AlarmListRoute)
}
```

- Navigation 목적지는 `@Serializable` 오브젝트/데이터 클래스 (Type-safe Navigation)
- `NavGraphBuilder` 확장 함수로 각 feature 모듈이 자체 등록
- `NavController` 확장 함수로 이동 로직 캡슐화

---

## 6. Preview 규칙

```kotlin
// core/ui/preview/DevicePreviews.kt 의 @DevicePreviews 사용
@DevicePreviews
@Composable
fun AlarmScreenPreview() {
    DaykitTheme {
        AlarmScreen(
            uiState = AlarmUiState.Success(
                listOf(
                    Alarm(id = 1L, title = "기상", time = LocalTime.of(7, 0)),
                    Alarm(id = 2L, title = "주말", time = LocalTime.of(8, 30), isActive = false)
                )
            ),
            onToggleAlarm = { _, _ -> },
            onDeleteAlarm = {},
            onAddAlarm = {},
            onEditAlarm = {},
            onSortTypeSelected = {}
        )
    }
}
```

- 모든 Screen 레벨 Composable은 파일 하단에 Preview 필수
- `@DevicePreviews`로 다양한 기기 폼팩터 동시 확인
- 반드시 `DaykitTheme { }` 래핑
- Loading / Success / Error 상태별 별도 Preview 권장

---

## 7. Gradle 의존성 추가 절차

새 라이브러리 추가 시 아래 순서를 반드시 지킵니다.

```toml
# 1단계: gradle/libs.versions.toml 에 버전 및 라이브러리 정의
[versions]
newLibrary = "1.0.0"

[libraries]
new-library = { module = "com.example:new-library", version.ref = "newLibrary" }

[plugins]
new-plugin = { id = "com.example.plugin", version.ref = "newLibrary" }
```

```kotlin
// 2단계: 해당 모듈의 build.gradle.kts 에서 참조
dependencies {
    implementation(libs.new.library)
}
```

- `build.gradle` 파일에 버전 문자열 하드코딩 **금지**
- 라이브러리 이름은 kebab-case, 참조 시 `.`으로 변환 (`new-library` → `libs.new.library`)

---

## 8. 신규 Feature 추가 체크리스트

새 기능(예: `timer`)을 추가할 때의 작업 순서입니다.

```
[ ] settings.gradle.kts 에 모듈 include 추가
      :domain:timer / :data:timer / :feature:timer

[ ] domain:timer
      model/Timer.kt              — 도메인 엔티티
      repository/TimerRepository  — 인터페이스
      usecase/StartTimerUseCase   — 유스케이스

[ ] data:timer
      local/TimerEntity.kt        — Room Entity
      local/TimerMapper.kt        — toDomain() / toEntity()
      local/TimerDao.kt
      repository/TimerRepositoryImpl.kt
      di/TimerDataModule.kt       — @Binds 모듈

[ ] feature:timer
      screen/TimerViewModel.kt    — @HiltViewModel
      screen/TimerScreen.kt       — Route + Screen
      navigation/TimerNavigation  — NavGraphBuilder 확장

[ ] app
      navigation/DaykitNavHost    — alarmNavigation() 패턴으로 등록
      ui/TopLevelDestination      — 바텀바 탭 추가 시
```
