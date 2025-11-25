/**
 * Node.js test runner for Loading Indicator Timing Property Tests
 * Run with: node run-loading-indicator-tests.cjs
 */

const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

async function runTests() {
    console.log('Starting Loading Indicator Timing Property Tests...\n');
    
    const browser = await puppeteer.launch({
        headless: 'new',
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    
    try {
        const page = await browser.newPage();
        
        // Enable console logging from the page
        page.on('console', msg => {
            const type = msg.type();
            const text = msg.text();
            
            // Filter out noise
            if (text.includes('Download the React DevTools')) return;
            if (text.includes('Tailwind')) return;
            
            if (type === 'error') {
                console.error('Browser Error:', text);
            } else if (type === 'warning') {
                console.warn('Browser Warning:', text);
            }
        });
        
        // Load the test HTML file
        const testFile = path.join(__dirname, 'loading-indicator-timing-pbt.test.html');
        const fileUrl = 'file://' + testFile.replace(/\\/g, '/');
        
        console.log('Loading test file:', fileUrl);
        await page.goto(fileUrl, { waitUntil: 'networkidle0', timeout: 30000 });
        
        // Wait for QUnit to complete
        await page.waitForFunction(
            () => {
                const qunit = window.QUnit;
                return qunit && qunit.config && qunit.config.stats && qunit.config.stats.all !== undefined;
            },
            { timeout: 60000 }
        );
        
        // Get test results
        const results = await page.evaluate(() => {
            const qunit = window.QUnit;
            const config = qunit.config;
            const stats = config.stats;
            
            const tests = [];
            const modules = config.modules || [];
            
            modules.forEach(module => {
                module.tests.forEach(test => {
                    tests.push({
                        name: test.name,
                        module: module.name,
                        failed: test.failed,
                        passed: test.passed,
                        total: test.total,
                        assertions: test.assertions.map(a => ({
                            result: a.result,
                            message: a.message
                        }))
                    });
                });
            });
            
            return {
                total: stats.all,
                passed: stats.all - stats.bad,
                failed: stats.bad,
                runtime: stats.runtime,
                tests: tests
            };
        });
        
        // Print results
        console.log('\n' + '='.repeat(70));
        console.log('TEST RESULTS');
        console.log('='.repeat(70));
        
        results.tests.forEach(test => {
            const status = test.failed === 0 ? '✓ PASS' : '✗ FAIL';
            const color = test.failed === 0 ? '\x1b[32m' : '\x1b[31m';
            const reset = '\x1b[0m';
            
            console.log(`\n${color}${status}${reset} ${test.module} > ${test.name}`);
            console.log(`  Assertions: ${test.passed}/${test.total} passed`);
            
            if (test.failed > 0) {
                test.assertions.forEach(assertion => {
                    if (!assertion.result) {
                        console.log(`  ${color}✗${reset} ${assertion.message}`);
                    }
                });
            }
        });
        
        console.log('\n' + '='.repeat(70));
        console.log('SUMMARY');
        console.log('='.repeat(70));
        console.log(`Total Tests: ${results.total}`);
        console.log(`Passed: \x1b[32m${results.passed}\x1b[0m`);
        console.log(`Failed: \x1b[31m${results.failed}\x1b[0m`);
        console.log(`Runtime: ${results.runtime}ms`);
        console.log('='.repeat(70) + '\n');
        
        // Exit with appropriate code
        process.exit(results.failed > 0 ? 1 : 0);
        
    } catch (error) {
        console.error('Error running tests:', error);
        process.exit(1);
    } finally {
        await browser.close();
    }
}

// Run tests
runTests().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});
