/**
 * Vercel Speed Insights Integration
 * This script initializes Vercel Speed Insights for performance monitoring
 */

(function() {
  'use strict';
  
  // Initialize Speed Insights queue
  window.si = window.si || function() {
    (window.siq = window.siq || []).push(arguments);
  };
  
  // Load the Speed Insights script
  var script = document.createElement('script');
  script.defer = true;
  script.src = '/_vercel/speed-insights/script.js';
  
  // Append script to document
  if (document.head) {
    document.head.appendChild(script);
  } else {
    // Fallback if head is not available yet
    document.addEventListener('DOMContentLoaded', function() {
      document.head.appendChild(script);
    });
  }
})();
