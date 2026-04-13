export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'scope-enum': [
      2,
      'always',
      [
        'gateway',
        'auth',
        'book',
        'circulation',
        'reader',
        'system',
        'notification',
        'recommend',
        'chat',
        'analytics',
        'common',
        'web-admin',
        'infra',
        'docs',
      ],
    ],
    'scope-empty': [1, 'never'],
  },
};
