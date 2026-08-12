const fs = require('fs');
const path = require('path');

// Mock Jest test runner globals
let totalCount = 0;
let passCount = 0;
let failCount = 0;
let currentSuite = '';

global.describe = function(name, fn) {
  currentSuite = name;
  fn();
};

global.it = global.test = function(name, fn) {
  totalCount++;
  try {
    fn();
    passCount++;
  } catch (err) {
    failCount++;
    console.error(`  FAIL [${currentSuite}] ${name}: ${err.message}`);
  }
};

global.beforeAll = global.before = function(fn) { try { fn(); } catch(e){} };
global.afterAll = global.after = function(fn) { try { fn(); } catch(e){} };
global.beforeEach = function(fn) { try { fn(); } catch(e){} };
global.afterEach = function(fn) { try { fn(); } catch(e){} };
global.expect = function(val) {
  return {
    toBe: (exp) => { if (val !== exp) throw new Error(`Expected ${exp} but got ${val}`); },
    toEqual: (exp) => { if (JSON.stringify(val) !== JSON.stringify(exp)) throw new Error(`Expected ${JSON.stringify(exp)} but got ${JSON.stringify(val)}`); },
    toBeTruthy: () => { if (!val) throw new Error(`Expected truthy but got ${val}`); },
    toBeDefined: () => { if (val === undefined) throw new Error(`Expected defined`); }
  };
};

console.log('=== RUNNING L3 API SYSTEM TESTS ===');
const startTime = Date.now();
require('./l3_system_tests.js');
const duration = (Date.now() - startTime) / 1000;

console.log('\n===========================================');
console.log(`L3 API SYSTEM TEST RESULTS SUMMARY:`);
console.log(`Total Test Cases Executed: ${totalCount}`);
console.log(`Passed: ${passCount}`);
console.log(`Failed: ${failCount}`);
console.log(`Execution Time: ${duration.toFixed(2)}s`);
console.log(`Success Rate: ${((passCount/totalCount)*100).toFixed(2)}%`);
console.log('===========================================');
