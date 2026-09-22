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

// The packaged index remains available if the online feed is temporarily unreachable.
const localApi = new DistributionAPI(ConfigManager.getLauncherDirectory(), null, null, LOCAL_DISTRO_URL, false)
const pullRemote = api.pullRemote.bind(api)
api.pullRemote = async () => {
    const result = await pullRemote()
    if (Array.isArray(result.data?.servers) && result.data.servers.length > 0) return result
    return localApi.pullRemote()
}

exports.DistroAPI = api
