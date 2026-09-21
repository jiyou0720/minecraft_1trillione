const { DistributionAPI } = require('helios-core/common')

const ConfigManager = require('./configmanager')

// Generated from launcher-admin/admin-config.json.
// Development can override this without rebuilding the app.
exports.REMOTE_DISTRO_URL = process.env.LAUNCHER_DISTRIBUTION_URL || 'http://127.0.0.1:38941/distribution.json'

const api = new DistributionAPI(
    ConfigManager.getLauncherDirectory(),
    null,
    null,
    exports.REMOTE_DISTRO_URL,
    false
)

exports.DistroAPI = api
