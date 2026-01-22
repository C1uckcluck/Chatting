const normalizeBase = (value: string) => value.replace(/\/$/, "");

const defaultApiBase = "http://localhost:8080";
const apiBase = normalizeBase(
    process.env.NEXT_PUBLIC_API_BASE_URL || defaultApiBase,
);
const wsBase = normalizeBase(process.env.NEXT_PUBLIC_WS_BASE_URL || apiBase);

export const apiUrl = (path: string) =>
    `${apiBase}${path.startsWith("/") ? "" : "/"}${path}`;

export const wsUrl = (path: string) =>
    `${wsBase}${path.startsWith("/") ? "" : "/"}${path}`;
