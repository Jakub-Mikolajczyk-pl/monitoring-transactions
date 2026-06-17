import { defineConfig, devices } from '@playwright/test';

const slowMo = Number(process.env.DEMO_SLOW_MO_MS ?? 850);

export default defineConfig({
  testDir: './tests/demo',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 120_000,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'target/playwright-report', open: 'never' }],
  ],
  outputDir: 'target/playwright-results',
  use: {
    baseURL: process.env.DEMO_BASE_URL ?? 'http://localhost:8080',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off',
    viewport: { width: 1440, height: 1000 },
    launchOptions: { slowMo },
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1440, height: 1000 },
      },
    },
  ],
});
