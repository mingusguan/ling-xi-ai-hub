const publicPath = process.env.VUE_APP_PUBLIC_PATH || '/'

export function withPublicPath(path = '') {
  const normalizedBase = publicPath.endsWith('/') ? publicPath : `${publicPath}/`
  const normalizedPath = String(path).replace(/^\/+/, '')
  return normalizedPath ? `${normalizedBase}${normalizedPath}` : normalizedBase
}

export function filePreviewUrl(path = '') {
  if (!path) {
    return path
  }
  const value = String(path).trim()
  if (/^(https?:|data:|blob:)/i.test(value)) {
    return value
  }
  if (value.startsWith('/profile/')) {
    return value
  }
  return withBackendPath(encodePathSegments(toPreviewPath(value)))
}

function withBackendPath(path = '') {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  if (process.env.NODE_ENV === 'production') {
    return normalizedPath
  }
  const baseApi = process.env.VUE_APP_BASE_API || ''
  return `${baseApi}${normalizedPath}`
}

function toPreviewPath(path = '') {
  const baseApi = process.env.VUE_APP_BASE_API || ''
  if (baseApi && path.startsWith(`${baseApi}/file/preview/`)) {
    return path.slice(baseApi.length)
  }
  if (path.startsWith('/file/preview/')) {
    return path
  }
  if (path.startsWith('file/preview/')) {
    return `/${path}`
  }
  return `/file/preview/${path.replace(/^\/+/, '')}`
}

function encodePathSegments(path = '') {
  const [pathname, query] = path.split('?')
  const encodedPathname = pathname.split('/').map(segment => {
    if (!segment) {
      return segment
    }
    try {
      return encodeURIComponent(decodeRepeatedly(segment))
    } catch (e) {
      return encodeURIComponent(segment)
    }
  }).join('/')
  return query ? `${encodedPathname}?${query}` : encodedPathname
}

function decodeRepeatedly(value = '') {
  let decoded = value
  for (let i = 0; i < 3; i++) {
    const next = decodeURIComponent(decoded)
    if (next === decoded) {
      return next
    }
    decoded = next
  }
  return decoded
}
