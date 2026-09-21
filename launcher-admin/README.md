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

`배포생성` 결과는 `pack/distribution.json`, `pack/repo`, `pack/servers`입니다.
기본 설정에서는 `런처빌드` 때 이 폴더를 설치 프로그램 안에 포함하므로 별도
호스팅이 필요하지 않습니다. 모드나 서버 주소를 변경한 뒤에는 새 설치 프로그램을
다시 배포해야 합니다. 원격 갱신을 원하면 HTTPS 호스팅 주소로 전환할 수 있습니다.

## 정식 배포 전에 반드시 설정할 값

1. `launcher.microsoftClientId`
2. `server.address` 및 `server.addressConfigured: true`
3. 원하는 경우 런처 아이콘과 배경 이미지

Microsoft 비밀번호는 런처가 직접 수집하거나 저장하지 않습니다.
Minecraft 원본 JAR·라이브러리·에셋은 배포 파일에 포함하지 않습니다.
