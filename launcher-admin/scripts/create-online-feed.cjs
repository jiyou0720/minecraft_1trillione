const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const config = JSON.parse(fs.readFileSync(path.join(root, 'admin-config.json'), 'utf8'))
const manifest = JSON.parse(fs.readFileSync(path.join(root, 'pack', 'distribution.json'), 'utf8'))
const baseUrl = config.hosting?.coreModBaseUrl
if (!baseUrl || !/^https:\/\//i.test(baseUrl)) {
  throw new Error('hosting.coreModBaseUrl에 HTTPS 주소를 설정해 주세요.')
}

const server = manifest.servers.find(item => item.id === `${config.server.id}-${config.server.minecraftVersion}`)
if (!server) throw new Error('온라인 목록에 넣을 서버를 찾을 수 없습니다.')
const coreMods = server.modules.filter(item => item.type === 'ForgeMod' && /^kr\.co:donationserver:/.test(item.id))
if (coreMods.length !== 1) throw new Error('코어 모드가 정확히 하나여야 합니다. mods/required를 확인해 주세요.')

const core = coreMods[0]
const version = core.id.match(/^kr\.co:donationserver:([^@]+)@jar$/)?.[1]
if (!version || !/^\d+\.\d+\.\d+$/.test(version)) throw new Error(`알 수 없는 코어 모드 버전: ${core.id}`)
const fileName = `donationserver-${version}.jar`
core.artifact.url = new URL(encodeURIComponent(fileName), baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`).href

// Some legacy mods validate their own exact JAR file name. Helios normally
// derives that name from the Maven artifact id, so allow an operator override.
for (const [name, id] of Object.entries(config.hosting.moduleIds || {})) {
  if (!/^[^:]+:[^:]+:[^@]+@jar$/.test(id)) throw new Error(`잘못된 모듈 ID입니다: ${name} -> ${id}`)
  const matches = server.modules.filter(item => item.type === 'ForgeMod' &&
    decodeURIComponent(new URL(item.artifact.url).pathname).split('/').pop() === name)
  if (matches.length !== 1) throw new Error(`모듈 ID를 변경할 모드를 찾지 못했거나 중복입니다: ${name}`)
  matches[0].id = id
}

// Other mods stay in the one-time installer until the operator supplies a
// redistributable HTTPS file URL. This avoids publishing third-party JARs by default.
for (const [name, url] of Object.entries(config.hosting.moduleUrls || {})) {
  if (!/^https:\/\//i.test(url)) throw new Error(`HTTPS 주소가 아닙니다: ${name}`)
  const matches = server.modules.filter(item => item.type === 'ForgeMod' &&
    decodeURIComponent(new URL(item.artifact.url).pathname).split('/').pop() === name)
  if (matches.length !== 1) throw new Error(`모드를 찾지 못했거나 중복입니다: ${name}`)
  matches[0].artifact.url = url
}

const output = path.join(root, 'publish', 'distribution.json')
fs.mkdirSync(path.dirname(output), { recursive: true })
fs.writeFileSync(output, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8')
console.log(`온라인 배포 목록 생성: ${output}`)
console.log(`코어 모드 다운로드 주소: ${core.artifact.url}`)
