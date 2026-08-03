# Vercel Speed Insights Setup

This document describes the Vercel Speed Insights integration for HireNest.

## Overview

Vercel Speed Insights has been successfully integrated into the HireNest project to monitor and track real-time web performance metrics.

## Implementation Details

### Architecture

Since this project uses vanilla JavaScript/HTML (no Node.js build process or package.json), the implementation uses the CDN-based approach recommended by Vercel's documentation.

### Files Modified

1. **Created: `src/main/resources/static/js/speed-insights.js`**
   - Central initialization script for Speed Insights
   - Implements the Vercel-recommended pattern for vanilla JavaScript projects
   - Dynamically loads the Speed Insights script from `/_vercel/speed-insights/script.js`
   - Uses an IIFE (Immediately Invoked Function Expression) for clean global scope management

2. **Modified: All 8 HTML files**
   - `index.html` - Landing page
   - `login.html` - Login page
   - `register.html` - Registration page
   - `onboarding.html` - Onboarding flow
   - `forgot-password.html` - Password recovery
   - `reset-password.html` - Password reset
   - `dashboard/job-seeker.html` - Job seeker dashboard
   - `dashboard/recruiter.html` - Recruiter dashboard

Each HTML file now includes the Speed Insights script before the closing `</body>` tag:
```html
<script src="/js/speed-insights.js"></script>
```

## How It Works

1. When any page loads, the `speed-insights.js` script executes
2. It initializes the `window.si` function and `window.siq` queue
3. It dynamically creates and injects the Vercel Speed Insights script tag
4. The script loads asynchronously (deferred) to avoid blocking page rendering
5. Performance metrics are automatically collected and sent to Vercel

## Next Steps

### Enabling Speed Insights on Vercel

To complete the setup and start seeing performance data:

1. **Log in to your Vercel Dashboard**
   - Go to https://vercel.com/dashboard

2. **Navigate to your project**
   - Select the HireNest project

3. **Enable Speed Insights**
   - Go to the "Speed Insights" tab in the left sidebar
   - Click "Enable Speed Insights" button
   - This adds the necessary routes at `/_vercel/speed-insights/*`

4. **Deploy your changes**
   - The Speed Insights integration is now active in your code
   - Deploy this branch to Vercel
   - After deployment, visit your site to generate traffic

5. **View Performance Data**
   - Return to the Speed Insights tab in your Vercel dashboard
   - Data will appear after users visit your site
   - Metrics include:
     - First Contentful Paint (FCP)
     - Largest Contentful Paint (LCP)
     - First Input Delay (FID)
     - Cumulative Layout Shift (CLS)
     - Time to First Byte (TTFB)

## Technical Details

### Browser Support

Speed Insights works on all modern browsers that support:
- Dynamic script injection
- Performance APIs (Navigation Timing, Resource Timing)

### Performance Impact

- **Minimal overhead**: The script loads asynchronously and doesn't block page rendering
- **Small payload**: The Speed Insights script is highly optimized and cached
- **No user experience degradation**: Metrics are collected passively

### Privacy & Data Collection

- Vercel Speed Insights collects performance metrics only
- No personally identifiable information (PII) is collected
- Data is used solely for performance monitoring and analytics
- Complies with privacy regulations (GDPR, CCPA)

## Troubleshooting

### Script not loading?

1. Verify that Speed Insights is enabled in your Vercel project settings
2. Check browser console for any errors
3. Ensure the deployment includes the latest changes
4. Verify that `/_vercel/speed-insights/script.js` is accessible after deployment

### No data appearing in dashboard?

1. Wait 5-10 minutes after first visit for data to appear
2. Ensure you've deployed the changes to Vercel (not just running locally)
3. Generate some traffic by visiting different pages
4. Check that Speed Insights is properly enabled in project settings

### Local Development

Speed Insights will attempt to load but won't collect data in local development since the `/_vercel/speed-insights/*` routes are only available on Vercel's infrastructure. This is expected behavior and won't cause errors.

## References

- [Vercel Speed Insights Quickstart](https://vercel.com/docs/speed-insights/quickstart)
- [Vercel Speed Insights Documentation](https://vercel.com/docs/speed-insights)
- [Web Vitals Guide](https://web.dev/vitals/)
