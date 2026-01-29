FROM oven/bun:1

WORKDIR /app

# Copy root workspace configuration
COPY package.json bun.lock* pnpm-workspace.yaml ./

# Copy package descriptors for workspace members
COPY services/visualizer/package.json ./services/visualizer/
COPY shared/ts/package.json ./shared/ts/
COPY libs/ts/logging/package.json ./libs/ts/logging/
COPY libs/ts/config/package.json ./libs/ts/config/

# Install dependencies
RUN bun install

# Copy source code (explicitly to avoid node_modules conflict)
COPY shared/ts/src ./shared/ts/src
COPY shared/ts/tsconfig.json ./shared/ts/

COPY services/visualizer/src ./services/visualizer/src
COPY services/visualizer/server ./services/visualizer/server
COPY services/visualizer/tsconfig.json ./services/visualizer/
COPY services/visualizer/vite.config.ts ./services/visualizer/
COPY services/visualizer/index.html ./services/visualizer/
COPY services/visualizer/tailwind.config.js ./services/visualizer/
COPY services/visualizer/postcss.config.js ./services/visualizer/

WORKDIR /app/services/visualizer

# Expose ports: 3000 for Vite (frontend), 3001 for Bun server (SSE)
EXPOSE 3000
EXPOSE 3001

# Default to running the SSE bridge server
CMD ["bun", "run", "server/index.ts"]
