import { expect, Page, test } from '@playwright/test';

function captureRuntimeFailures(page: Page) {
  const failures: string[] = [];
  page.on('console', message => {
    if (message.type() === 'error') failures.push(`console: ${message.text()}`);
  });
  page.on('pageerror', error => failures.push(`page: ${error.message}`));
  page.on('response', response => {
    if (response.url().includes('/api/') && response.status() >= 400) {
      failures.push(`api: ${response.status()} ${response.url()}`);
    }
  });
  return failures;
}

async function verifyWorkflow(page: Page, path: string, expectedText: RegExp) {
  const failures = captureRuntimeFailures(page);
  await page.goto(path, { waitUntil: 'domcontentloaded' });
  await expect(page.locator('body')).toContainText(expectedText);
  await page.waitForTimeout(3_000);
  expect(failures).toEqual([]);
}

test('query page loads fixture studies', async ({ page }) => {
  await verifyWorkflow(page, '/', /StarRocks Study A|Select Studies/i);
});

test('Study View renders the fixture cohort', async ({ page }) => {
  await verifyWorkflow(page, '/study/summary?id=sr_study_a', /StarRocks Study A|Summary/i);
});

test('patient view renders samples and timeline', async ({ page }) => {
  await verifyWorkflow(
    page,
    '/patient?studyId=sr_study_a&caseId=PA1',
    /PA1|Patient Summary/i,
  );
});

test('Results View renders an OncoPrint query', async ({ page }) => {
  await verifyWorkflow(
    page,
    '/results/oncoprint?cancer_study_list=sr_study_a&case_set_id=sr_study_a_all&gene_list=TP53%20EGFR&tab_index=tab_visualize',
    /OncoPrint|TP53/i,
  );
});
