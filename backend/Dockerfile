# Use Node.js 20 on Alpine Linux for a lightweight image
FROM node:20-alpine

# Set working directory inside the container
WORKDIR /app

# Copy dependency files and install production deps
COPY package.json package-lock.json* ./
RUN npm install --only=production && npm cache clean --force

# Copy source code into container
COPY src ./src

# Expose app port
EXPOSE 3000

# Environment defaults (can be overridden by docker-compose or .env)
ENV PORT=3000
ENV DATA_DIR=/data

# Run the server
CMD ["node", "src/server.js"]

