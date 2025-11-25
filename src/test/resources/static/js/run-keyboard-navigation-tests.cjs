/**
 * Node.js test runner for keyboard navigation property-based tests
 * Feature: file-preview-system, Property 19
 */

const puppeteer = require('puppeteer');
const path = require('path');

async function runTests() {
    console.log('🎹 Starting Keyboard Navigation Property-Based Tests...\n');
    
    const browser = await puppeteer.launch({
        headless: 'new',
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    
    try {
        const page = await browser.newPage();
        
        // Collect console messages
        const messages = [];
        page.on('console', msg => {
            const text = msg.text();
            messages.push(text);
            console.log(text);
        });
        
        // Collect errors
        page.on('pageerror', error => {
            console.error('❌ Page Error:', error.message);
        });
        
        // Navigate to test page
        const testFile = path.join(__dirname, 'keyboard-navigation-pbt.test.html');
        await page.goto(`file://${testFile}`, {
            waitUntil: 'networkidle0',
            timeout: 30000
        });
        
        // Wait for tests to complete
        await page.waitForFunction(
            () => {
                const statusDiv = document.getElementById('test-status');
                return statusDiv && 
                       (statusDiv.classList.contains('passed') || 
                        statusDiv.classList.contains('failed'));
            },
            { timeout: 30000 }
        );
        
        // Get test results
        const results = await page.evaluate(() => {
            const stats = document.querySelector('.mocha-stats');
            const failures = document.querySelectorAll('.test.fail');
            
            return {
                passed: stats ? parseInt(stats.querySelector('.passes em')?.textContent || '0') : 0,
                failed: stats ? parseInt(stats.querySelector('.failures em')?.textContent || '0') : 0,
                duration: stats ? stats.querySelector('.duration em')?.textContent || '0ms' : '0ms',
                failures: Array.from(failures).map(f => ({
                    title: f.querySelector('h2')?.textContent || 'Unknown test',
                    error: f.querySelector('.error')?.textContent || 'Unknown error'
                }))
            };
        });
        
        // Print summary
        console.log('\n' + '='.repeat(60));
        console.log('📊 Test Summary');
        console.log('='.repeat(60));
        console.log(`✅ Passed: ${results.passed}`);
        console.log(`❌ Failed: ${results.failed}`);
        console.log(`⏱️  Duration: ${results.duration}`);
        
        if (results.failures.length > 0) {
            console.log('\n❌ Failed Tests:');
            results.failures.forEach((failure, index) => {
                console.log(`\n${index + 1}. ${failure.title}`);
                console.log(`   ${failure.error}`);
            });
        }
        
        console.log('='.repeat(60) + '\n');
        
        await browser.close();
        
        // Exit with appropriate code
        process.exit(results.failed > 0 ? 1 : 0);
        
    } catch (error) {
        console.error('❌ Test execution failed:', error);
        await browser.close();
        process.exit(1);
    }
}

runTests();
