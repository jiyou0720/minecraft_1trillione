const fs = require('fs')
const http = require('http')
const path = require('path')
const { app } = require('electron')

const LOCAL_PORT = 38941
const LOCAL_URL = `http://127.0.0.1:${LOCAL_PORT}/distribution.json`
const contentTypes = {
    '.json': 'application/json; charset=utf-8',
    '.jar': 'application/java-archive',
    '.cfg': 'text/plain; charset=utf-8',
    '.png': 'image/png',
    '.zip': 'application/zip'
}

function packRoot() {
    return app.isPackaged
        ? path.join(process.resourcesPath, 'pack')
        : path.resolve(__dirname, '..', 'launcher-admin', 'pack')
}

function resolveFile(root, requestUrl) {
    const pathname = new URL(requestUrl, LOCAL_URL).pathname
    const decoded = decodeURIComponent(pathname)
    const relative = decoded.replace(/^\/+/, '').replace(/\//g, path.sep)
    const full = path.resolve(root, relative)
    const rootPrefix = `${path.resolve(root)}${path.sep}`
    if(!full.startsWith(rootPrefix)) return null
    return full
}

function start(root = packRoot(), port = LOCAL_PORT) {
    const manifest = path.join(root, 'distribution.json')
    if(!fs.existsSync(manifest)) {
        throw new Error(`Bundled distribution index not found: ${manifest}`)
    }

    return new Promise((resolve, reject) => {
        const server = http.createServer((request, response) => {
            if(request.method !== 'GET' && request.method !== 'HEAD') {
                response.writeHead(405)
                response.end()
                return
            }
            let file
            try {
                file = resolveFile(root, request.url)
            } catch {
                response.writeHead(400)
                response.end()
                return
            }
            if(!file) {
                response.writeHead(403)
                response.end()
                return
            }
            fs.stat(file, (error, stat) => {
                if(error || !stat.isFile()) {
                    response.writeHead(404)
                    response.end()
                    return
                }
                response.writeHead(200, {
                    'Content-Type': contentTypes[path.extname(file).toLowerCase()] || 'application/octet-stream',
                    'Content-Length': stat.size,
                    'Cache-Control': 'no-store',
                    'X-Content-Type-Options': 'nosniff'
                })
                if(request.method === 'HEAD') {
                    response.end()
                } else {
                    fs.createReadStream(file).pipe(response)
                }
            })
        })
        server.once('error', reject)
        server.listen(port, '127.0.0.1', () => {
            server.removeListener('error', reject)
            resolve(server)
        })
    })
}

module.exports = { start, LOCAL_URL, resolveFile }
