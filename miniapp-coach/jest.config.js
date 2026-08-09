module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['@tarojs/test-utils-react'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1'
  },
  transform: {
    '^.+\\.(ts|tsx)$': 'ts-jest'
  },
  testMatch: [
    '<rootDir>/tests/unit/**/*.test.{ts,tsx}',
    '<rootDir>/src/**/*.test.{ts,tsx}'
  ],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.config.ts',
    '!src/**/*.d.ts'
  ],
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80
    }
  }
}
