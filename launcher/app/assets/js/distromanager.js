const { DistributionAPI } = require('helios-core/common')

const ConfigManager = require('./configmanager')

// Generated from launcher-admin/admin-config.json.
// Development can override this without rebuilding the app.
exports.REMOTE_DISTRO_URL = process.env.LAUNCHER_DISTRIBUTION_URL || 'https://raw.githubusercontent.com/jiyou0720/minecraft_1trillione/main/launcher-feed/distribution.json'
const LOCAL_DISTRO_URL = 'http://127.0.0.1:38941/distribution.json'

const api = new DistributionAPI(
    ConfigManager.getLauncherDirectory(),
    null,
    null,
    exports.REMOTE_DISTRO_URL,
    false
)

// Prefer the last online index when offline; use the bundled index only on first run.
const localApi = new DistributionAPI(ConfigManager.getLauncherDirectory(), null, null, LOCAL_DISTRO_URL, false)
const pullRemote = api.pullRemote.bind(api)
api.pullRemote = async () => {
    const result = await pullRemote()
    if (result.data != null && (!Array.isArray(result.data.servers) || result.data.servers.length === 0)) result.data = null
    return result
}
const pullLocal = api.pullLocal.bind(api)
api.pullLocal = async () => {
    const cached = await pullLocal()
    if (cached != null) return cached
    const bundled = await localApi.pullRemote()
    return bundled.data
}

exports.DistroAPI = api
