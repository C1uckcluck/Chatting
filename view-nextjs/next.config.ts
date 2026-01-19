import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: '/auth/:path*',
        destination: 'http://localhost:8080/auth/:path*',
      },
      {
        source: '/chat/rooms/:path*',
        destination: 'http://localhost:8080/chat/rooms/:path*',
      },
      {
        source: '/members/:path*',
        destination: 'http://localhost:8080/members/:path*',
      },
      // Proxy websocket/stomp endpoint if needed, but usually SockJS/Stomp needs direct connection or specific proxying.
      // For now, let's proxy the API endpoints.
    ];
  },
};

export default nextConfig;
