const crypto = require('crypto')
const fs = require('fs')
const path = require('path')

const adminRoot = path.resolve(__dirname, '..')
const packRoot = path.join(adminRoot, 'pack')
const config = JSON.parse(fs.readFileSync(path.join(adminRoot, 'admin-config.json'), 'utf8'))
const distribution = JSON.parse(fs.readFileSync(path.join(packRoot, 'distribution.json'), 'utf8'))
const basePath = new URL(config.hosting.baseUrl).pathname.replace(/^\/+|\/+$/g, '')
const failures = []
let artifacts = 0

function verifyModule(module) {
  if (module.artifact?.url) {
    artifacts += 1
    const urlPath = decodeURIComponent(new URL(module.artifact.url).pathname).replace(/^\/+/, '')
    const relative = basePath && urlPath.startsWith(`${basePath}/`)
      ? urlPath.slice(basePath.length + 1)
      : urlPath
    const file = path.resolve(packRoot, ...relative.split('/'))
    const expectedRoot = `${path.resolve(packRoot)}${path.sep}`
    if (!file.startsWith(expectedRoot)) {
      failures.push(`${module.id}: 배포 루트 밖의 경로`)
    } else if (!fs.existsSync(file)) {
      failures.push(`${module.id}: 파일 없음 (${relative})`)
    } else {
      const data = fs.readFileSync(file)
      if (module.artifact.size !== data.length) failures.push(`${module.id}: 파일 크기 불일치`)
      if (module.artifact.MD5) {
        const actual = crypto.createHash('md5').update(data).digest('hex')
        if (actual.toLowerCase() !== module.artifact.MD5.toLowerCase()) failures.push(`${module.id}: MD5 불일치`)
      }
    }
  }
  for (const child of module.subModules || []) verifyModule(child)
}

for (const server of distribution.servers || []) {
  for (const module of server.modules || []) verifyModule(module)
}

if (failures.length) {
  for (const failure of failures) console.error(`[오류] ${failure}`)
  process.exit(1)
}
console.log(`배포 파일 검증 완료: 서버 ${distribution.servers.length}개, 아티팩트 ${artifacts}개, 오류 0개`)
