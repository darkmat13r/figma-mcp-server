import React from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import './styles.css';

console.log('UI script loaded');

// Signal that ui.js has loaded
(window as any).uiJsLoaded = true;

const container = document.getElementById('root');
if (!container) {
  console.error('Root element not found!');
  throw new Error('Root element not found');
}

console.log('Root element found, rendering App...');

try {
  const root = createRoot(container);
  root.render(<App />);
  console.log('App rendered successfully');
} catch (error) {
  console.error('Error rendering app:', error);
  // Show error in UI
  container.innerHTML = `
    <div style="padding: 20px; color: white; background: #2C2C2C;">
      <h1 style="color: #ff6b6b;">Error Loading UI</h1>
      <pre style="color: #ffa500; margin-top: 10px;">${error}</pre>
    </div>
  `;
}
