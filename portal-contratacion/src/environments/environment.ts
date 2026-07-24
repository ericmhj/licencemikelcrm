export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  keycloak: {
    tokenUrl: 'http://localhost:8180/realms/mikel-crm/protocol/openid-connect/token',
    clientId: 'mikel-crm-test',
    clientSecret: 'test-client-secret-dev',
  },
};
