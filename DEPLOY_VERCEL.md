# Deployment Guide for HireNest (Vercel & Render)

This guide explains how to fix the Vercel deployment issues for **HireNest**.

---

## 1. Why did you get a `404: NOT_FOUND` on Vercel?

Vercel is designed for static websites and JavaScript-based serverless functions. It **cannot run a Java (Spring Boot) backend**. 

Additionally, your web assets (HTML, CSS, JS) are nested inside the directory `src/main/resources/static/`. By default, Vercel looks for `index.html` at the **root** of the repository. Because there is no `index.html` at the root, Vercel fails to find your site.

To make the app work in production, we need a **hybrid deployment**:
1. **Backend**: Deploy the Java Spring Boot application to a host that supports Java/Docker (like **Render** or **Railway**).
2. **Frontend**: Deploy the static frontend to **Vercel**, pointing it to the `src/main/resources/static` directory.
3. **Connection**: Use a `vercel.json` rewrite file to proxy `/api/...` and `/uploads/...` requests from Vercel to your deployed backend.

---

## Step 2: Deploying the Backend on Render

Follow these steps to deploy your Java backend on [Render](https://render.com):

1. **Sign in to Render** using your GitHub account.
2. Click **New → Web Service** and connect your `HireNest` GitHub repository.
3. Configure the following service settings:
   - **Name**: `hirenest-backend`
   - **Runtime**: `Docker` (or Java/Native if you configure a build command)
   - **Start Command**: `java -jar target/hirenest-backend-0.0.1-SNAPSHOT.jar`
   - **Port**: `9090` (Make sure this matches the port configured in `application.properties`)
4. Add the following **Environment Variables** in Render's dashboard under **Environment**:
   - `APP_JWT_SECRET` = *(a long, random, secure string)*
   - `SPRING_DATASOURCE_URL` = *(your production database URL, e.g., hosted PostgreSQL, or stick to H2 file-backed if just testing, though file-backed databases reset on redeploys on Render)*
5. Once deployed, note down your Render service URL (e.g., `https://hirenest-backend.onrender.com`).

---

## Step 3: Configure Vercel to host the Frontend

Now we will configure Vercel to host your static frontend:

1. **Create a `vercel.json` file**:
   We have created a template file for you at `src/main/resources/static/vercel.json`. 
2. **Update the Render URL**:
   Open `src/main/resources/static/vercel.json` and replace `https://your-backend-render-url.onrender.com` with the actual URL of your deployed Render backend:
   ```json
   {
     "rewrites": [
       {
         "source": "/api/:path*",
         "destination": "https://your-backend-render-url.onrender.com/api/:path*"
       },
       {
         "source": "/uploads/:path*",
         "destination": "https://your-backend-render-url.onrender.com/uploads/:path*"
       }
     ]
   }
   ```
3. **Configure Project Settings in Vercel**:
   - Go to your Vercel Dashboard and click on the `hirenest` project.
   - Go to **Settings** → **General**.
   - Find the **Root Directory** setting. Change it from the root `.` to:
     `src/main/resources/static`
   - Click **Save**.
4. **Push the changes to GitHub**:
   Run the following commands in your terminal to commit and push the newly added files to GitHub:
   ```powershell
   git add src/main/resources/static/vercel.json DEPLOY_VERCEL.md
   git commit -m "Add Vercel config for frontend API proxying and deployment guide"
   git push origin main
   ```

Vercel will automatically detect the new commit, build the subfolder `src/main/resources/static`, and deploy it. Since the root directory is set to the static folder, `index.html` will load successfully as the homepage, and all `/api` requests will be proxied to your Render backend!
