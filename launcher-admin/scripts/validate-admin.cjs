const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const config = JSON.parse(fs.readFileSync(path.join(root, 'admin-config.json'), 'utf8'))
const errors = []
const warnings = []

if (!config.launcher?.microsoftClientId) warnings.push('Microsoft Client ID 미설정: 정식 계정 로그인 불가')
if (!config.server?.addressConfigured) warnings.push('서버 주소 미설정: 자동 접속 비활성 상태로 배포해야 함')
if (config.launcher?.distributionUrl?.startsWith('http://127.0.0.1:38941/') && config.hosting?.baseUrl !== 'http://127.0.0.1:38941/') errors.push('내장 모드팩 URL과 파일 기본 URL이 서로 다름')
if (!/^[a-zA-Z0-9._-]+$/.test(config.server?.id || '')) errors.push('server.id에는 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있음')
if (/example\.invalid/i.test(config.launcher?.distributionUrl || '')) warnings.push('배포 매니페스트 URL이 예시 주소임')
if (/example\.invalid/i.test(config.hosting?.baseUrl || '')) warnings.push('파일 호스팅 기본 URL이 예시 주소임')
if (config.server?.minecraftVersion !== '1.12.2') errors.push('현재 프로젝트는 Minecraft 1.12.2용으로 준비됨')
if (config.server?.forgeVersion !== '14.23.5.2864') warnings.push('Forge 버전 변경 시 배포 매니페스트를 반드시 재생성해야 함')
if ((config.server?.minimumMemoryMb || 0) > (config.server?.recommendedMemoryMb || 0)) errors.push('최소 메모리가 권장 메모리보다 큼')

for (const warning of warnings) console.warn(`[경고] ${warning}`)
for (const error of errors) console.error(`[오류] ${error}`)
if (errors.length) process.exit(1)
console.log(`관리자 설정 검사 완료: 오류 0개, 경고 ${warnings.length}개`)
