# syntax=docker/dockerfile:1

# сборка
FROM node:20-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# nginx раздаёт статику и проксирует /api на бэкенд
FROM nginx:1.27-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html

# API_UPSTREAM - адрес бэкенда, на Render его нужно поменять
ENV API_UPSTREAM=http://backend:3000 \
    PORT=80
EXPOSE 80
