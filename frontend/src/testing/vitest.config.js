import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, '..'),
    },
  },
  test: {
    include: ['src/**/*.test.{js,jsx}'],
    environment: 'jsdom',
    maxWorkers: 4,
    testTimeout: 10000,
    setupFiles: ['./src/testing/setup.js'],
    coverage: {
      provider: 'v8',
      reportsDirectory: './src/testing/coverage',
      include: ['src/**/*.{js,jsx}'],
      exclude: ['src/testing/**', 'src/**/*.test.{js,jsx}'],
      thresholds: {
        lines: 85,
        'src/app/routes/**/*.jsx': { lines: 85 },
      },
    },
  },
});
