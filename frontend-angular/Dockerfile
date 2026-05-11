FROM node:24-alpine AS builder
WORKDIR /app

COPY frontend-angular/package.json frontend-angular/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY frontend-angular/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /app/dist/frontend-angular/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

