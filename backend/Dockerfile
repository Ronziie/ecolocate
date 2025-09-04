FROM node:20-alpine

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install --only=production && npm cache clean --force

COPY src ./src

ENV PORT=3000
ENV DATA_DIR=/data

EXPOSE 3000

CMD ["node", "src/server.js"]


