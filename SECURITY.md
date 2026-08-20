# Security Policy

## Reporting a vulnerability

Please do not open a public issue for security problems.

Report privately through GitHub: open a draft advisory at
https://github.com/joelkanyi/platypus/security/advisories/new

Include what you found, how to reproduce it, and the impact you expect. You will
get an acknowledgement, and a fix or mitigation plan once the report is triaged.

## Scope

Platypus talks to Bitbucket Cloud directly from the device. Repository content
never passes through any Platypus-operated server. The optional push relay, when
present, handles event metadata only. Reports about token handling, the auth
exchange, secure storage, or the trust boundary are especially welcome.
