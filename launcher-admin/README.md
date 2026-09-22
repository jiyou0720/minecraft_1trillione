# 1억 기부 프로젝트 전용 런처

Minecraft 1.12.2 / Forge 14.23.5.2864 서버용 자동 설치·업데이트 런처입니다.
사용자는 Java, Forge, 모드를 직접 설치하지 않아도 됩니다. 런처가 Microsoft 계정으로
로그인한 뒤 필요한 파일을 검사하고 변경된 파일만 내려받아 실행합니다.

## 관리자가 수정하는 곳

`admin-config.json`에서 다음 항목을 바꿀 수 있습니다.

- 런처 이름, 버전, 앱 ID
- Microsoft Entra 애플리케이션 Client ID
- `distribution.json` 주소와 업데이트 저장소
- 서버 이름, 주소, 자동 접속 여부
- Minecraft/Forge/팩 버전과 메모리
- 다운로드 호스팅 주소, 홈페이지·문의·SNS 링크
- 최초 실행 안내 문구

모드는 `mods` 폴더에서 관리합니다. 설정, 리소스팩 등은 `files` 폴더에서 관리합니다.

## 관리자 명령

Windows에서 `관리자도구.bat`을 실행하거나 아래 명령을 사용합니다.

```powershell
.\관리자도구.ps1 검사
.\관리자도구.ps1 설정적용
.\관리자도구.ps1 동기화
.\관리자도구.ps1 배포생성
.\관리자도구.ps1 런처빌드
```

`배포생성` 결과는 `pack/distribution.json`, `pack/repo`, `pack/servers`와
`publish/distribution.json`입니다. `pack`은 설치 프로그램에 포함되는 기본본이고,
`publish/distribution.json`은 온라인 업데이트 목록입니다. 런처는 실행 및 게임
시작 시 온라인 목록을 확인하고 크기/해시가 달라진 파일만 받습니다. 온라인 목록에
접속하지 못하면 마지막으로 저장된 목록, 그것도 없으면 내장 기본본으로 시작합니다. **기존 0.2.4 이하 런처는 0.2.5로
한 번 교체해야 합니다.**

## 코어 모드 업데이트 절차

1. 서버와 `mods/required`에서 오래된 `donationserver-*.jar`를 폴더 밖으로 옮기고 새 JAR을 넣습니다.
2. `admin-config.json`의 `server.packVersion`을 올립니다.
3. `./관리자도구.ps1 배포생성`과 `./관리자도구.ps1 배포검증`을 실행합니다.
4. 자체 제작 코어 JAR을 GitHub 저장소의 `release/`에 먼저 올리고, `publish/distribution.json`을 `launcher-feed/distribution.json`에 올립니다. 이전 JAR도 남겨 두세요.
5. 이후 사용자는 다음 게임 시작 시 변경된 모드만 받습니다. 서버와 클라이언트 코어 모드 버전을 함께 맞추세요.

기타 모드는 처음 배포한 설치 파일에 내장됩니다. 다른 모드를 온라인으로 새로
배포하려면 재배포 권한과 HTTPS 파일 주소를 확인한 뒤 `hosting.moduleUrls`에
`"모드파일.jar": "https://.../모드파일.jar"` 형식으로 지정합니다. 주소를
지정하지 않은 신규 모드는 기존 설치 파일에 없으므로 온라인 자동 설치가 되지 않습니다.

## 정식 배포 전에 반드시 설정할 값

1. `launcher.microsoftClientId`
2. `server.address` 및 `server.addressConfigured: true` (운영 서버 공개 시)
3. 원하는 경우 런처 아이콘과 배경 이미지

Microsoft 비밀번호는 런처가 직접 수집하거나 저장하지 않습니다.
Minecraft 원본 JAR·라이브러리·에셋은 배포 파일에 포함하지 않습니다.
