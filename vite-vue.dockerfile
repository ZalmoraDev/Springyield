FROM node:20-alpine
WORKDIR /app

# Install dependencies listed in package.json & package-lock.json
COPY frontend/package*.json ./
RUN npm install

# Copy the rest of the frontend files (EXCEPT node_modules, which is ignored by .dockerignore)
COPY frontend/ .

# Internally, Vite uses port 5173 for development
EXPOSE 5173

# TOOD: REPLACE WITH `npm run build` which builds to /dist and is read directly by nginx
CMD ["npm", "run", "dev"]
