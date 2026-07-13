FROM node:20-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_STRIPE_PUBLISHABLE_KEY=
ARG VITE_ENABLE_MOCK_PAYMENT=false
ENV VITE_STRIPE_PUBLISHABLE_KEY=$VITE_STRIPE_PUBLISHABLE_KEY
ENV VITE_ENABLE_MOCK_PAYMENT=$VITE_ENABLE_MOCK_PAYMENT
RUN npm run build

FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ /usr/share/nginx/html/

EXPOSE 80
