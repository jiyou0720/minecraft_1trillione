# 1억 기부 프로젝트

Minecraft Java Edition 1.12.2 / Forge 14.23.5.2864 기반의 경제·농경·성장형 야생 서버 프로젝트입니다.

## 구성

- `server-mod/`: 경제, 기부, 튜토리얼, 거점 이동, NPC 상점, 영지, 스킨 랜덤박스를 구현한 Forge 코어 모드
- `launcher/`: Microsoft 계정 로그인과 모드팩 설치를 위한 전용 Electron 런처 소스
- `launcher-admin/`: 서버 주소, 모드팩, 배포 설정을 관리하는 운영자 도구
- `approval-site/`: 런처 및 Microsoft Minecraft Services 승인 안내 페이지
- `release/`: 최신 코어 모드 JAR과 설치·명령어 문서

현재 코어 모드 버전은 **0.2.11**입니다.

## 코어 모드 빌드

Java 8 JDK가 필요합니다.

```powershell
cd server-mod
.\gradlew.bat build
```

결과물은 `server-mod/build/libs/donationserver-0.2.11.jar`입니다. 서버와 모든 접속 PC의 `mods` 폴더에 같은 버전을 설치해야 합니다.

## 런처 개발

Node.js 22가 필요합니다.

```powershell
cd launcher
npm ci
npm start
```

운영 설정은 `launcher-admin/admin-config.json`에서 변경할 수 있습니다. Microsoft Client ID는 공개 식별자이며 Client Secret이나 사용자 토큰은 저장소에 넣지 않습니다.

## 배포 파일 안내

Minecraft 원본 파일, Java 런타임, 테스트 월드, 로그, 사용자 계정 데이터와 제3자 모드 JAR은 저장소에 포함하지 않습니다. 각 모드는 해당 배포처와 라이선스 조건에 따라 별도로 받아야 합니다.

최신 설치 방법은 `release/설치와_테스트.md`, 전체 명령어는 `release/명령어_전체목록_0.2.11.md`를 확인하세요.
