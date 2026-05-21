FROM node:16-alpine AS build

WORKDIR /app

COPY lingxi-ui/package*.json ./
RUN npm ci --registry=https://registry.npmmirror.com

COPY lingxi-ui/ ./
RUN NODE_OPTIONS=--max_old_space_size=8192 ./node_modules/.bin/vue-cli-service build --mode production

FROM nginx:1.27-alpine

COPY lingxi-ui/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
