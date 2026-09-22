const fs = require('fs')
const path = require('path')

const adminRoot = path.resolve(__dirname, '..')
const workspaceRoot = path.resolve(adminRoot, '..')
const launcherRoot = path.join(workspaceRoot, 'launcher')
const configPath = path.join(adminRoot, 'admin-config.json')

function fail(message) {
  console.error(`[설정 오류] ${message}`)
  process.exit(1)
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'))
}

function writeJson(file, value) {
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

function tomlString(value) {
  return JSON.stringify(String(value ?? ''))
}

function jsSingleQuoted(value) {
  return `'${String(value).replaceAll('\\', '\\\\').replaceAll("'", "\\'")}'`
}

const config = readJson(configPath)
const launcher = config.launcher || {}
const server = config.server || {}

for (const [name, value] of Object.entries({
  'launcher.name': launcher.name,
  'launcher.shortName': launcher.shortName,
  'launcher.appId': launcher.appId,
  'launcher.version': launcher.version,
  'launcher.distributionUrl': launcher.distributionUrl,
  'server.id': server.id,
  'server.minecraftVersion': server.minecraftVersion,
  'server.forgeVersion': server.forgeVersion
})) {
  if (!value) fail(`${name} 값을 입력하세요.`)
}

if (!/^https:\/\//i.test(launcher.distributionUrl) && !/^http:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?\//i.test(launcher.distributionUrl)) {
  fail('launcher.distributionUrl은 HTTPS 주소여야 합니다(내장 모드팩용 loopback만 HTTP 허용).')
}
if (!/^[a-zA-Z0-9._-]+$/.test(server.id)) {
  fail('server.id에는 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.')
}

const packagePath = path.join(launcherRoot, 'package.json')
const packageJson = readJson(packagePath)
packageJson.name = launcher.shortName.toLowerCase().replace(/[^a-z0-9._-]/g, '-')
packageJson.version = launcher.version
packageJson.productName = launcher.name
packageJson.description = launcher.description
packageJson.author = 'Server Operator'
packageJson.license = 'MIT'
packageJson.homepage = config.links?.home || '#'
packageJson.scripts['apply-config'] = 'node ../launcher-admin/scripts/apply-config.cjs'
packageJson.scripts['admin:validate'] = 'node ../launcher-admin/scripts/validate-admin.cjs'
writeJson(packagePath, packageJson)

const customLanguage = `# Generated from launcher-admin/admin-config.json. Do not edit directly.\n\n[ejs.app]\ntitle = ${tomlString(launcher.name)}\n\n[ejs.landing]\nmediaGitHubURL = ${tomlString(config.links?.home || '#')}\nmediaXURL = ${tomlString(config.links?.x || '#')}\nmediaInstagramURL = ${tomlString(config.links?.instagram || '#')}\nmediaYouTubeURL = ${tomlString(config.links?.youtube || '#')}\nmediaDiscordURL = ${tomlString(config.links?.discord || '#')}\nlaunchButton = \"게임 시작\"\nlaunchButtonPlaceholder = \"&#8226; 서버 설정을 확인하세요\"\nserverStatus = \"서버\"\nserverStatusPlaceholder = \"주소 미설정\"\nsettingsTooltip = \"설정\"\nusernameEditButton = \"계정\"\nnewsButton = \"공지\"\n\n[ejs.settings]\nsourceGithubLink = ${tomlString(config.links?.home || '#')}\nsupportLink = ${tomlString(config.links?.support || '#')}\n\n[ejs.welcome]\nwelcomeHeader = ${tomlString(config.welcome?.title)}\nwelcomeDescription = ${tomlString(config.welcome?.description)}\nwelcomeDescCTA = ${tomlString(config.welcome?.cta)}\ncontinueButton = \"시작하기\"\n\n[ejs.loginOptions]\nloginOptionsTitle = \"로그인 방식\"\nloginWithMicrosoft = \"Microsoft 계정으로 로그인\"\ncancelButton = \"취소\"\n\n[js.index]\nmicrosoftLoginTitle = \"Microsoft 로그인\"\nmicrosoftLogoutTitle = \"Microsoft 로그아웃\"\n\n[js.landing.discord]\nloading = \"게임을 준비하는 중\"\njoining = \"서버에 접속하는 중\"\njoined = \"게임 실행 완료\"\n`
fs.writeFileSync(path.join(launcherRoot, 'app', 'assets', 'lang', '_custom.toml'), customLanguage, 'utf8')

const ipcConstants = path.join(launcherRoot, 'app', 'assets', 'js', 'ipcconstants.js')
let ipcSource = fs.readFileSync(ipcConstants, 'utf8')
const clientId = launcher.microsoftClientId || '00000000-0000-0000-0000-000000000000'
ipcSource = ipcSource.replace(/exports\.AZURE_CLIENT_ID\s*=\s*['"][^'"]*['"]/, `exports.AZURE_CLIENT_ID = '${clientId}'`)
fs.writeFileSync(ipcConstants, ipcSource, 'utf8')

const distroManagerPath = path.join(launcherRoot, 'app', 'assets', 'js', 'distromanager.js')
const distroManager = [
  "const { DistributionAPI } = require('helios-core/common')",
  '',
  "const ConfigManager = require('./configmanager')",
  '',
  '// Generated from launcher-admin/admin-config.json.',
  '// Development can override this without rebuilding the app.',
  `exports.REMOTE_DISTRO_URL = process.env.LAUNCHER_DISTRIBUTION_URL || ${jsSingleQuoted(launcher.distributionUrl)}`,
  "const LOCAL_DISTRO_URL = 'http://127.0.0.1:38941/distribution.json'",
  '',
  'const api = new DistributionAPI(',
  '    ConfigManager.getLauncherDirectory(),',
  '    null,',
  '    null,',
  '    exports.REMOTE_DISTRO_URL,',
  '    false',
  ')',
  '',
  '// The packaged index remains available if the online feed is temporarily unreachable.',
  'const localApi = new DistributionAPI(ConfigManager.getLauncherDirectory(), null, null, LOCAL_DISTRO_URL, false)',
  'const pullRemote = api.pullRemote.bind(api)',
  'api.pullRemote = async () => {',
  '    const result = await pullRemote()',
  '    if (Array.isArray(result.data?.servers) && result.data.servers.length > 0) return result',
  '    return localApi.pullRemote()',
  '}',
  '',
  'exports.DistroAPI = api',
  ''
].join('\r\n')
fs.writeFileSync(distroManagerPath, distroManager, 'utf8')

const builderPath = path.join(launcherRoot, 'electron-builder.yml')
let builder = fs.readFileSync(builderPath, 'utf8')
builder = builder
  .replace(/^appId:.*$/m, `appId: '${launcher.appId}'`)
  .replace(/^productName:.*$/m, `productName: '${launcher.name.replaceAll("'", "''")}'`)
  .replace(/^copyright:.*$/m, "copyright: 'Copyright © 2026 Server Operator; Helios Launcher © Daniel Scalzi'")
fs.writeFileSync(builderPath, builder, 'utf8')

console.log('런처 설정 적용 완료')
if (!launcher.microsoftClientId) {
  console.warn('주의: Microsoft Client ID가 비어 있어 실제 계정 로그인은 아직 사용할 수 없습니다.')
}
if (!server.addressConfigured) {
  console.warn('주의: 서버 주소가 미설정 상태입니다. 배포 전에 address와 addressConfigured를 수정하세요.')
}
