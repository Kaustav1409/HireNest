# Vercel — seedha setup

**Site:** https://hire-nest-virid.vercel.app

---

## Pehle Render (ek baar)

1. [render.com](https://render.com) → GitHub → `HireNest` → Deploy  
2. URL copy karo: `https://xxxx.onrender.com`

---

## Phir Vercel

1. [vercel.com](https://vercel.com) → project **HireNest**  
2. **Settings → Environment Variables** → add:

   `HIRENEST_BACKEND_URL` = `https://xxxx.onrender.com`

3. **Deployments** → latest → **⋯** → **Redeploy**  
   (ya GitHub par push — auto deploy)

4. Register: https://hire-nest-virid.vercel.app/register.html

---

## Redeploy par "500" aaye?

- 2–3 minute wait, dubara try  
- Ya **Git push** karo — naya deploy start hoga (zyada reliable)

---

## Abhi kyun fail ho raha tha?

GitHub par purana code tha — `/api` proxy missing thi → **404 Not Found**.  
Naya code push ke baad + `HIRENEST_BACKEND_URL` set = theek.
