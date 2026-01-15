module.exports = {
    testEnvironment: 'jsdom',
    roots: ['<rootDir>/src/test/frontend'],
    testMatch: ['**/__tests__/**/*.js', '**/?(*.)+(spec|test).js'],
    testPathIgnorePatterns: [
        '<rootDir>/src/test/frontend/e2e/'
    ],
    moduleNameMapper: {
        '^(\\.{1,2}/.*)\\.js$': '$1'
    },
    transform: {
        '^.+\\.js$': 'babel-jest'
    },
    setupFilesAfterEnv: ['<rootDir>/src/test/frontend/setupTests.js'],
    collectCoverageFrom: [
        'src/main/resources/static/js/**/*.js',
        '!**/node_modules/**',
        '!**/lib/**'
    ],
    coverageDirectory: 'coverage',
    verbose: true
};
