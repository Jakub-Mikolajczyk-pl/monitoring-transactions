import { expect, test, type Locator, type Page } from '@playwright/test';

const stepDelayMs = Number(process.env.DEMO_STEP_MS ?? 1200);

test.beforeEach(async ({ baseURL, request }) => {
  try {
    const health = await request.get(`${baseURL}/actuator/health`);
    expect(health.ok(), `Spring app at ${baseURL} must be running before the demo starts.`).toBeTruthy();
  } catch (error) {
    throw new Error(
      `Spring app is not reachable at ${baseURL}. Start it in another terminal with "mvnw.cmd spring-boot:run", then run "npm run demo".\n${error}`,
    );
  }
});

test('guided analyst walkthrough', async ({ page }) => {
  const runId = Date.now().toString().slice(-8);
  const businessId = `DEMO_${runId}`;
  const firstName = 'Demo';
  const lastName = `Analyst${runId}`;
  const normalAmount = '150.00';
  const suspiciousAmount = '2500.50';

  await test.step('Open the analyst UI', async () => {
    await page.goto('/');
    await expect(page.locator('app-shell')).toBeVisible();
    await showStep(page, '1. Analyst queue', 'The app opens on the AML alert queue. Start here when reviewing suspicious activity.');
  });

  await test.step('Register a customer', async () => {
    await page.locator('app-shell nav a[data-route="customers"]').click();
    await expect(page.locator('customers-view')).toBeVisible();
    await showStep(page, '2. Register a customer', 'Open Customers and create a customer inside a business context.');

    const form = page.locator('customer-form');
    await form.locator('input[name="businessId"]').fill(businessId);
    await form.locator('input[name="firstName"]').fill(firstName);
    await form.locator('input[name="lastName"]').fill(lastName);
    await form.locator('button[type="submit"]').click();

    await expect(form.locator('.banner:not([hidden])')).toBeVisible();
    await expect(page.locator('customers-view tbody tr', { hasText: businessId }).first()).toBeVisible();
    await showStep(page, 'Customer saved', 'The new customer appears in the table with a generated UUID.');
  });

  const customerId = await test.step('Read the generated customer id', async () => {
    const row = page.locator('customers-view tbody tr', { hasText: businessId }).first();
    const id = (await row.locator('td').nth(3).textContent())?.trim();
    expect(id, 'Customer id should be visible in the Customers table.').toBeTruthy();
    return id as string;
  });

  await test.step('Register a normal transaction', async () => {
    await page.locator('app-shell nav a[data-route="transactions"]').click();
    await expect(page.locator('transactions-view')).toBeVisible();
    await showStep(page, '3. Register a normal transaction', 'Use the transaction form to post activity for the selected customer.');

    const form = page.locator('transactions-view #register-form');
    await fillTransactionForm(form, businessId, customerId, normalAmount);
    await form.locator('button[type="submit"]').click();
    await expect(form.locator('#register-banner:not([hidden])')).toBeVisible();
    await showStep(page, 'Normal transaction saved', 'The backend accepts the transaction and starts AML analysis in the background.');
  });

  await test.step('Search transactions by businessId', async () => {
    const form = page.locator('transactions-view #search-form');
    await showStep(page, '4. Search transaction history', 'The search form requires businessId and can narrow by customer or date range.');
    await form.locator('input[name="businessId"]').fill(businessId);
    await form.locator('button[type="submit"]').click();

    await expect(page.locator('transactions-view #results tbody tr', { hasText: businessId }).first()).toBeVisible();
    await showStep(page, 'Transaction list', 'The table confirms what was registered for this business context.');
  });

  await test.step('Register a suspicious transaction', async () => {
    const form = page.locator('transactions-view #register-form');
    await showStep(page, '5. Trigger an AML alert', 'Post a transaction above the configured threshold to create an alert asynchronously.');
    await fillTransactionForm(form, businessId, customerId, suspiciousAmount);
    await form.locator('button[type="submit"]').click();
    await expect(form.locator('#register-banner:not([hidden])')).toBeVisible();
  });

  await test.step('Open the generated alert', async () => {
    await page.locator('app-shell nav a[data-route="alerts"]').click();
    await expect(page.locator('alerts-view')).toBeVisible();
    await showStep(page, '6. Review the alert queue', 'The newest suspicious transaction appears in the open AML alert queue.');

    const alertRow = page.locator('alerts-view tbody tr', { hasText: businessId }).first();
    await expect(alertRow).toBeVisible({ timeout: 15_000 });
    await alertRow.click();
    await expect(page.locator('alert-details')).toBeVisible();
  });

  await test.step('Inspect alert details', async () => {
    await expect(page.locator('alert-details')).toContainText(businessId);
    await expect(page.locator('alert-details')).toContainText('SUSPICIOUS_AMOUNT');
    await showStep(page, '7. Inspect alert details', 'The detail view joins alert, transaction, customer, reason codes, and the decision history.');
  });

  await test.step('Approve the alert', async () => {
    const details = page.locator('alert-details');
    await showStep(page, '8. Record the analyst decision', 'Add an audit comment and save the decision. The history is append-only.');
    await details.locator('textarea[name="comment"]').fill('Reviewed during the guided Playwright demo.');
    await details.locator('#decision-form button[type="submit"]').click();

    await expect(details.locator('#banner:not([hidden])')).toBeVisible();
    await expect(details).toContainText('APPROVED');
    await showStep(page, 'Demo complete', 'The alert status changed and the decision is visible in the history.');
  });
});

async function fillTransactionForm(form: Locator, businessId: string, customerId: string, amount: string) {
  await form.locator('input[name="businessId"]').fill(businessId);
  await form.locator('input[name="customerId"]').fill(customerId);
  await form.locator('input[name="amount"]').fill(amount);
  await form.locator('select[name="currency"]').selectOption('PLN');
  await form.locator('input[name="transactionDate"]').fill(localDateTimeValue());
}

function localDateTimeValue() {
  const now = new Date(Date.now() - 60_000);
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

async function showStep(page: Page, title: string, body: string) {
  await page.evaluate(
    ({ body, title }) => {
      const id = 'playwright-demo-step';
      const previous = document.getElementById(id);
      previous?.remove();

      const panel = document.createElement('aside');
      panel.id = id;
      panel.setAttribute('aria-live', 'polite');
      panel.innerHTML = `
        <div class="playwright-demo-kicker">Guided demo</div>
        <strong>${escapeHtml(title)}</strong>
        <p>${escapeHtml(body)}</p>
      `;

      const style = document.createElement('style');
      style.textContent = `
        #${id} {
          position: fixed;
          right: 24px;
          top: 24px;
          z-index: 2147483647;
          width: min(360px, calc(100vw - 48px));
          padding: 16px 18px;
          border: 1px solid rgb(15 23 42 / 0.18);
          border-radius: 8px;
          background: rgb(255 255 255 / 0.97);
          color: #0f172a;
          box-shadow: 0 18px 48px rgb(15 23 42 / 0.22);
          font: 15px/1.45 system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
          pointer-events: none;
        }
        #${id} .playwright-demo-kicker {
          margin-bottom: 4px;
          color: #475569;
          font-size: 12px;
          font-weight: 700;
          letter-spacing: 0.08em;
          text-transform: uppercase;
        }
        #${id} strong {
          display: block;
          margin-bottom: 4px;
          font-size: 18px;
        }
        #${id} p {
          margin: 0;
          color: #334155;
        }
      `;
      panel.append(style);
      document.body.append(panel);

      function escapeHtml(value: string) {
        return value
          .replaceAll('&', '&amp;')
          .replaceAll('<', '&lt;')
          .replaceAll('>', '&gt;')
          .replaceAll('"', '&quot;')
          .replaceAll("'", '&#039;');
      }
    },
    { body, title },
  );
  await page.waitForTimeout(stepDelayMs);
}
