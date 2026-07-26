export const environment = {
  production: true,
  apiUrl: '',
  smtApiUrl: 'http://localhost:3001',
  keycloak: {
    tokenUrl: 'http://localhost:8180/realms/mikel-crm/protocol/openid-connect/token',
    clientId: 'mikel-crm-test',
    clientSecret: 'test-client-secret-dev',
  },
};
