# 클라이언트 파일 관리 폴더

게임 인스턴스에 배포할 파일을 상대 경로 그대로 넣습니다.

예시:

- `config/donationserver.cfg`
- `resourcepacks/server-resourcepack.zip`
- `options.txt`

사용자가 변경한 설정을 보존해야 한다면 `pack/servers/.../servermeta.json`의
`untrackedFiles` 규칙에 경로를 추가하세요.
