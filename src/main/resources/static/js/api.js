const API = (() => {
    async function request(method, path, body) {
        const options = { method: method };
        if (body !== undefined && body !== null) {
            options.headers = { 'Content-Type': 'application/json' };
            options.body = JSON.stringify(body);
        }

        const res = await fetch(path, options);
        if (res.status === 204) return null;

        const text = await res.text();
        let data = null;
        if (text) {
            try { data = JSON.parse(text); } catch (e) { data = null; }
        }

        if (!res.ok) {
            const detail = data && (data.detail || data.message || data.error);
            throw new Error(detail || (res.status + ' ' + res.statusText));
        }
        return data;
    }

    return {
        get: (path) => request('GET', path),
        post: (path, body) => request('POST', path, body),
        put: (path, body) => request('PUT', path, body),
        patch: (path, body) => request('PATCH', path, body),
        del: (path) => request('DELETE', path)
    };
})();

window.API = API;