// Imports every spec (registering its tests) and runs them into the page.
import { run } from './test-runner.js';
import './format.test.js';
import './api.test.js';
import './components.test.js';

run(document.getElementById('results'));
