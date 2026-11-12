# 협업 규칙 (PassHUFS)

BE / FE 레포 공통 규칙입니다. 개발 시작 전에 한 번 읽고, 헷갈리면 이 문서를 먼저 확인해주세요.

## 0. 흐름 한 줄 요약

```
이슈 생성 → 브랜치 생성 → 코드 작성 → add / commit / push → PR(Closes #번호) → 리뷰 → develop에 Squash 머지 → (팀장/배포 담당) main 머지 → 자동배포
```

- 모든 작업은 **GitHub 이슈에서 시작**합니다. 이슈 없는 작업은 하지 않습니다.
- 진행 상태는 **Projects 보드**(Todo / In Progress / Done)로 봅니다.
- 기획 문서와 회의록은 노션, 코드 규칙은 이 문서를 기준으로 합니다.
- **API 명세서가 기준**입니다. 명세와 다르게 구현해야 한다면 임의로 바꾸지 말고 이슈에 남긴 뒤 단톡에 공유해주세요.

---

## 1. 브랜치 구조

| 브랜치 | 역할 | 머지 권한 |
|---|---|---|
| `main` | 배포 전용. 머지되면 서버에 자동배포됩니다 | BE: 팀장(채원) / FE: 배포 담당(윤지) |
| `develop` | 개발 통합. 팀원들의 작업이 모이는 곳 (기본 브랜치) | 작성자 (리뷰 승인 후) |
| `feature/...` 등 | 개인 작업 브랜치. develop에서 분기합니다 | - |

- **main, develop에는 직접 push하지 않습니다.** 항상 PR로만 들어갑니다.
- 배포는 시연이나 점검이 필요할 때 `develop → main` PR로 합니다. 머지 전에 단톡에 먼저 알려주세요.
- `develop → main`은 Squash가 아니라 **`Create a merge commit`** 으로 머지합니다. (Squash로 하면 develop과 main의 기록이 어긋나서 다음 배포 때 충돌이 납니다.)
- 엔티티(테이블) 변경이 포함된 배포는 머지 전에 단톡에 알리고, 배포 후 서버가 정상인지 확인합니다.

---

## 2. 이슈 만들기

### 2-1. 어디서 만들까
레포 상단 `Projects` → 본인 파트 보드(BE Board / FE Board) → 원하는 컬럼에서 `Add item` → `Create new issue`

### 2-2. 보드 사용 규칙
- `Todo` : 진행 예정인 작업. 미리 여러 개 만들어둬도 됩니다.
- `In Progress` : **지금 실제로 작업 중인 것만**. 한 사람당 1개를 원칙으로 합니다.
- `Done` : 완료된 작업. PR이 머지되면 자동으로 이동합니다.

### 2-3. 이슈 작성
- 제목: `[파트] 작업 내용` (예: `[BE] 회원가입 API 구현`)
- 내용(마크다운): 이슈 생성 시 나오는 템플릿(현재 상황 / 목표 / 작업 내용 체크리스트 / 스크린샷 / 참고자료)을 수정해서 작성합니다. 참고자료에는 API 명세서 해당 항목 링크를 넣어주세요.
- 오른쪽 메뉴 설정
    - **Assignees**: 작업할 사람 본인
    - **Labels**: 아래 표에서 선택 (여러 개 가능)
    - **Type**: `feature` / `bug` / `task` (둘 다 아니면 task)

### 2-4. 이슈 번호를 꼭 기억하세요
이슈를 만들면 `#5` 같은 번호가 붙습니다. 브랜치 이름과 PR 제목에 그대로 씁니다.

### 라벨 / 작업 종류

| 이름 | 설명 |
|---|---|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| hotfix | 급하게 고쳐야 하는 치명적 버그 |
| design | CSS 등 UI 디자인 변경 |
| refactor | 코드 리팩터링 (동작 변경 없음) |
| style | 코드 포맷 변경 등 (로직 변경 없음) |
| docs | 문서 수정 |
| test | 테스트 추가/수정 |
| chore | 빌드, 설정, 패키지 관리 등 |
| rename | 파일/폴더 이름 변경 또는 이동만 한 경우 |
| remove | 파일 삭제만 한 경우 |
| BE / FE | 파트 구분 |

---

## 3. 브랜치 만들기

반드시 **develop 최신 상태에서** 분기합니다.

```bash
git checkout develop
git pull origin develop
git checkout -b feature/BE-#5/signup-api
```

**브랜치명 양식**: `작업종류/파트-#이슈번호/작업내용`

- 작업종류: `feature` · `fix` · `hotfix` · `refactor` · `chore` · `docs` 등
- 파트: `BE` 또는 `FE`
- 작업내용: 영어 소문자, 단어는 하이픈(`-`)으로 연결
- 예: `feature/BE-#5/signup-api`, `fix/FE-#12/login-button-style`

작업 전에 현재 브랜치가 내가 만든 브랜치인지 꼭 확인하세요 (터미널 또는 IDE 상단에 표시됩니다).

---

## 4. 매일 develop 최신화 (충돌 방지)

아래 두 경우에는 **반드시** 진행합니다.

- 매일 개발을 시작하기 전
- 팀원이 develop에 PR을 머지했다고 알려줬을 때

**지금 작업 중인 브랜치가 있을 때** (커밋하지 않은 변경이 있어도 이 순서대로 하면 됩니다)

```bash
git stash                  # 작업 중인 내용을 임시 저장
git checkout develop       # 로컬 develop으로 이동
git pull origin develop    # 다른 팀원이 머지한 내용을 받아옴
git checkout 내-브랜치명    # 작업 중이던 브랜치로 복귀
git merge develop          # develop 내용을 내 브랜치에 반영
git stash pop              # 임시 저장한 내용 복구 (충돌이 나면 해결 후 commit)
```

- `git stash`할 변경이 없다면(`No local changes to save`) `git stash`와 `git stash pop`은 건너뛰세요.

**작업 중인 브랜치가 없을 때** (새 작업을 시작할 때)

```bash
git checkout develop
git pull origin develop
git checkout -b "새로-작업할-브랜치명"   # 3번 규칙대로 이름 짓기
```

**충돌이 났을 때**

```bash
git status                  # 충돌 파일 확인
# 충돌 난 파일을 열어서 <<<<<<< ~ >>>>>>> 구간을 원하는 코드로 정리
git add 충돌-해결한-파일명
git commit -m "chore: Merge develop into 내-브랜치명"
```

충돌이 없으면 별도 작업 없이 개발을 이어가면 됩니다. 충돌 해결이 어렵다면 **혼자 붙잡지 말고 바로 단톡에 올려주세요.**

---

## 5. add / commit / push

```bash
git status                          # 바뀐 파일 확인
git add src/main/java/.../Service.java
git commit -m "feat: 회원가입 서비스 로직 구현"
git push -u origin feature/BE-#5/signup-api   # 이 브랜치의 첫 push
git push                                       # 이후 push
```

- 🚫 **`git add .` 금지.** 의도하지 않은 파일(`.env`, 설정 파일 등)이 올라갈 수 있습니다. 파일을 하나씩 add 하며 본인이 수정한 파일이 맞는지 확인하세요.
- **기능 또는 파일 단위로** add → commit 합니다. 한 번에 몰아서 커밋하지 마세요. push는 자유롭게 해도 됩니다.
- 커밋 메시지: `종류: 작업 내용` (종류는 위 라벨 표의 이름을 사용)
    - 예: `feat: 회원가입 API 추가`, `fix: 로그인 시 토큰 만료 오류 수정`, `chore: 배포 스크립트 설정`
- 🚫 **비밀값 커밋 금지.** DB 비밀번호, 키, `.env`, 로컬 `application-*.yml`은 올리지 않습니다. 배포용 값은 GitHub Secrets에서 관리합니다.

---

## 6. PR 올리기 / 리뷰 / 머지

### 6-1. PR 작성
1. push 후 GitHub에서 `Compare & pull request` 클릭
2. **base는 develop**인지 확인
3. 제목: `[파트-#이슈번호] 작업 내용` (예: `[BE-#5] 회원가입 API 구현`)
4. 본문: PR 템플릿 양식에 맞춰 작성하고, 관련 이슈를 **`Closes #5`** 로 적습니다 (머지되면 이슈가 자동으로 닫히고 Done으로 이동합니다)
5. 오른쪽 메뉴 설정
    - **Reviewers**: 같은 파트 팀원 (BE는 서로 리뷰, FE는 1명 이상)
    - **Assignees**: 작업한 본인
    - **Labels**: 이슈와 동일
    - **Projects**: BE는 BE Board, FE는 FE Board

### 6-2. 리뷰와 머지
- PR을 올린 뒤 **단톡에 "[API명] PR 올렸습니다! 리뷰 부탁드립니다!"** 라고 남기고, 리뷰어를 태그해주세요.
- **리뷰어 승인 1개 이상**을 받은 뒤 작성자가 직접 머지합니다.
- develop과 충돌이 있으면 먼저 해결해야 머지 버튼이 활성화됩니다.
- 작업 브랜치 → develop 머지 방식은 **Squash and merge** (초록 버튼 옆 화살표에서 선택). `develop → main`만 예외이니 1번을 확인하세요.
- 머지 후에는 `Delete branch`로 작업 브랜치를 삭제합니다.
- 머지한 사람은 단톡에 **"develop에 머지했습니다! 풀 받아주세요!"** 라고 알려주세요. 다른 팀원들은 4번(develop 최신화)을 진행합니다.
- 이슈가 Done으로 옮겨졌는지, 이슈의 체크리스트가 체크되었는지 확인합니다.

---

## 7. 로컬 환경 세팅

처음 한 번만 하면 됩니다. 로컬 DB는 Docker의 MySQL을 쓰고, 서버는 IntelliJ에서 실행합니다.

### 7-1. 준비물
- **Docker Desktop** (설치 후 실행 상태로 둡니다)
- **JDK 21** (IntelliJ의 `Project Structure > SDK`와 `Settings > Build Tools > Gradle > Gradle JVM` 둘 다 21로 맞춰주세요)
- IntelliJ IDEA

### 7-2. 처음 세팅 순서
1. 레포를 클론하고 `develop` 브랜치로 이동합니다.
2. 프로젝트 **루트 폴더**(`docker-compose.yml`이 있는 곳)에 `.env` 파일을 만들고 아래 내용을 그대로 붙여넣습니다.
   ```
   MYSQL_PORT=3307
   MYSQL_ROOT_PASSWORD=root
   MYSQL_DATABASE=team5
   MYSQL_USER=team5
   MYSQL_PASSWORD=team5
   ```
    - 서버 기본 접속값(`team5` / `team5`)과 맞춰져 있으니 **값을 바꾸지 마세요.**
    - `.env`는 Git에 올라가지 않습니다 (`.gitignore` 대상). 절대 커밋하지 마세요.
3. 터미널(프로젝트 루트)에서 DB를 실행합니다.
   ```bash
   docker compose up -d
   docker compose ps        # STATUS가 healthy면 준비 완료
   ```
4. IntelliJ에서 `Application` 클래스를 실행합니다.
5. 브라우저에서 Swagger가 열리면 성공입니다: http://localhost:8080/swagger-ui.html

### 7-3. 알아두면 좋은 것
- `.env`는 **Docker(MySQL)만** 읽습니다. 서버는 `application.yaml`의 기본값(`localhost:3307/team5`, `team5`/`team5`)으로 접속하므로 IntelliJ에 환경 변수를 따로 넣지 않아도 됩니다.
- 귀국보고서 항목 추출(외부 AI)은 **로컬 기본값이 mock**(`AI_MODE=mock`)이라 API 키 없이 고정 응답이 나옵니다. 실제 Gemini 호출을 확인할 때만 IntelliJ 실행 환경 변수에 `AI_MODE=real`, `GEMINI_API_KEY=...`를 넣고, 키는 절대 커밋하지 마세요.
- 서버 실행 전에 **Docker와 DB가 켜져 있어야** 합니다. `Connection refused` 오류가 나면 `docker compose ps`부터 확인하세요.
- DB 컨테이너가 시작되지 않으면 `.env`가 루트 폴더에 있는지, 파일 이름이 정확히 `.env`인지 확인하세요.
- 3307 포트를 이미 쓰고 있으면 충돌합니다. 이 경우 단톡에 물어봐주세요.

### 7-4. 스키마(테이블) 관리
- 현재 `ddl-auto: update`라서 **엔티티를 수정하고 서버를 켜면 로컬 DB에 자동 반영**됩니다.
- 단, 컬럼 이름 변경이나 삭제는 반영되지 않아 옛 컬럼이 남고 오류가 날 수 있습니다. 이때는 로컬 DB를 초기화하세요.
  ```bash
  docker compose down -v   # DB 데이터까지 삭제 (로컬 데이터가 모두 사라집니다)
  docker compose up -d
  ```
- **엔티티를 바꾸는 PR은 단톡에도 알려주세요.** 팀원마다 로컬 DB가 따로라서, 모르면 "내 컴퓨터에서는 되는데"가 생깁니다.

### 7-5. 자주 쓰는 명령어
| 명령어 | 설명 |
|---|---|
| `docker compose up -d` | DB 실행 |
| `docker compose ps` | 상태 확인 |
| `docker compose stop` | DB 중지 (데이터 유지) |
| `docker compose down` | DB 컨테이너 삭제 (데이터 유지) |
| `docker compose down -v` | DB 컨테이너와 데이터 모두 삭제 (초기화) |

---

## 8. 막혔을 때

- 명세가 이상하거나 모호하면 이슈에 남기고 팀장에게 알려주세요.
- 실수로 잘못 push하거나 머지했다면 숨기지 말고 바로 공유해주세요. 되돌리는 방법이 있습니다.