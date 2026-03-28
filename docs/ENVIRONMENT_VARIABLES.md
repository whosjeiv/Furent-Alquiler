# Furent - Environment Variables Documentation

This document describes all environment variables required for deploying Furent in production.

## Required Environment Variables

### Database Configuration

#### `MONGODB_URI`
- **Description**: MongoDB connection URI with authentication
- **Format**: `mongodb://username:password@host:port/database`
- **Example**: `mongodb://furent_user:SecurePass123@mongodb.example.com:27017/FurentDataBase`
- **Notes**: 
  - Must include authentication credentials for production
  - Supports MongoDB Atlas connection strings
  - Database name should be `FurentDataBase`

### Cache & Session Store

#### `REDIS_HOST`
- **Description**: Redis server hostname or IP address
- **Example**: `redis.example.com` or `10.0.1.50`
- **Notes**: Used for caching and session management

#### `REDIS_PORT`
- **Description**: Redis server port
- **Default**: `6379`
- **Example**: `6379`

#### `REDIS_PASSWORD`
- **Description**: Redis authentication password
- **Example**: `RedisSecurePassword123`
- **Notes**: Leave empty if Redis has no authentication configured (not recommended for production)

### Security & Authentication

#### `FURENT_ADMIN_PASSWORD`
- **Description**: Password for the initial admin user account
- **Example**: `Admin@Secure2024!`
- **Requirements**:
  - Minimum 12 characters
  - Mix of uppercase, lowercase, numbers, and symbols
  - Should be unique and not reused
- **Notes**: 
  - **CRITICAL**: This creates the default admin account on first startup
  - Change immediately after first login
  - Store securely (use secrets manager in production)

#### `JWT_SECRET`
- **Description**: Secret key for signing JWT authentication tokens
- **Format**: Base64-encoded string (minimum 256 bits)
- **Example**: `dGhpcyBpcyBhIHNlY3VyZSBrZXkgZm9yIGZ1cmVudCBzYWFzIHBsYXRmb3JtIDIwMjY=`
- **Generate**: `openssl rand -base64 32`
- **Notes**:
  - **CRITICAL**: Must be kept secret and secure
  - Changing this will invalidate all existing JWT tokens
  - Use a secrets manager in production

### Email Configuration

#### `MAIL_USERNAME`
- **Description**: SMTP email account username
- **Example**: `noreply@furent.com` or `notifications@example.com`
- **Notes**: 
  - Used for sending transactional emails
  - Supports Gmail, SendGrid, AWS SES, etc.

#### `MAIL_PASSWORD`
- **Description**: SMTP email account password or app-specific password
- **Example**: `wohbkdrcgxvdnezs` (Gmail app password)
- **Notes**:
  - For Gmail, use App Passwords (not account password)
  - For other providers, use API keys or app-specific passwords
  - Store securely

### File Storage

#### `UPLOAD_DIR`
- **Description**: Directory path for storing uploaded files
- **Example**: `/var/furent/uploads` or `/opt/furent/data/uploads`
- **Notes**:
  - Must have write permissions for the application user
  - Should be backed up regularly
  - Consider using cloud storage (S3, Azure Blob) for scalability
  - Ensure sufficient disk space

## Optional Environment Variables

### Payment Gateway (PayU)

#### `PAYU_API_KEY`
- **Description**: PayU payment gateway API key
- **Example**: `4Vj8eK4rpeq6scS930ZasY44uz`
- **Notes**: Required only if using PayU payment integration

#### `PAYU_MERCHANT_ID`
- **Description**: PayU merchant identifier
- **Example**: `508029`
- **Notes**: Provided by PayU during merchant registration

#### `PAYU_ACCOUNT_ID`
- **Description**: PayU account identifier
- **Example**: `512321`
- **Notes**: Specific to your PayU account

#### `PAYU_URL`
- **Description**: PayU gateway URL
- **Default**: `https://checkout.payulatam.com/checkout-web-gateway-payu/`
- **Notes**: 
  - Production: `https://checkout.payulatam.com/checkout-web-gateway-payu/`
  - Sandbox: `https://sandbox.checkout.payulatam.com/checkout-web-gateway-payu/`

#### `PAYU_ENABLED`
- **Description**: Enable/disable PayU payment integration
- **Values**: `true` or `false`
- **Default**: `false`

### Spring Profile

#### `SPRING_PROFILES_ACTIVE`
- **Description**: Active Spring profile
- **Values**: `dev`, `prod`
- **Default**: `dev`
- **Example**: `prod`
- **Notes**: Set to `prod` for production deployments

## Environment Variable Setup Examples

### Docker Compose

```yaml
version: '3.8'
services:
  furent:
    image: furent:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MONGODB_URI=mongodb://furent_user:password@mongodb:27017/FurentDataBase
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=redis_password
      - FURENT_ADMIN_PASSWORD=Admin@Secure2024!
      - JWT_SECRET=dGhpcyBpcyBhIHNlY3VyZSBrZXkgZm9yIGZ1cmVudCBzYWFzIHBsYXRmb3JtIDIwMjY=
      - MAIL_USERNAME=noreply@furent.com
      - MAIL_PASSWORD=mail_app_password
      - UPLOAD_DIR=/var/furent/uploads
    volumes:
      - uploads:/var/furent/uploads
```

### Kubernetes Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: furent-secrets
type: Opaque
stringData:
  MONGODB_URI: "mongodb://furent_user:password@mongodb:27017/FurentDataBase"
  REDIS_PASSWORD: "redis_password"
  FURENT_ADMIN_PASSWORD: "Admin@Secure2024!"
  JWT_SECRET: "dGhpcyBpcyBhIHNlY3VyZSBrZXkgZm9yIGZ1cmVudCBzYWFzIHBsYXRmb3JtIDIwMjY="
  MAIL_USERNAME: "noreply@furent.com"
  MAIL_PASSWORD: "mail_app_password"
```

### Shell Export (Linux/macOS)

```bash
export SPRING_PROFILES_ACTIVE=prod
export MONGODB_URI="mongodb://furent_user:password@localhost:27017/FurentDataBase"
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=redis_password
export FURENT_ADMIN_PASSWORD="Admin@Secure2024!"
export JWT_SECRET="dGhpcyBpcyBhIHNlY3VyZSBrZXkgZm9yIGZ1cmVudCBzYWFzIHBsYXRmb3JtIDIwMjY="
export MAIL_USERNAME=noreply@furent.com
export MAIL_PASSWORD=mail_app_password
export UPLOAD_DIR=/var/furent/uploads
```

### .env File (for local testing)

```env
SPRING_PROFILES_ACTIVE=prod
MONGODB_URI=mongodb://furent_user:password@localhost:27017/FurentDataBase
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password
FURENT_ADMIN_PASSWORD=Admin@Secure2024!
JWT_SECRET=dGhpcyBpcyBhIHNlY3VyZSBrZXkgZm9yIGZ1cmVudCBzYWFzIHBsYXRmb3JtIDIwMjY=
MAIL_USERNAME=noreply@furent.com
MAIL_PASSWORD=mail_app_password
UPLOAD_DIR=/var/furent/uploads
```

## Security Best Practices

1. **Never commit secrets to version control**
   - Use `.gitignore` for `.env` files
   - Use secrets managers (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault)

2. **Rotate credentials regularly**
   - Change passwords and keys every 90 days
   - Update JWT secret periodically (will invalidate sessions)

3. **Use strong passwords**
   - Minimum 12 characters
   - Mix of uppercase, lowercase, numbers, symbols
   - Use password generators

4. **Restrict access**
   - Limit who can view production secrets
   - Use role-based access control (RBAC)
   - Audit secret access logs

5. **Encrypt in transit and at rest**
   - Use TLS/SSL for all connections
   - Encrypt secrets in storage
   - Use encrypted environment variables in CI/CD

## Validation Checklist

Before deploying to production, verify:

- [ ] All required environment variables are set
- [ ] MongoDB connection is successful
- [ ] Redis connection is successful
- [ ] Admin password is strong and unique
- [ ] JWT secret is properly generated (base64, 256+ bits)
- [ ] Email credentials are valid and tested
- [ ] Upload directory exists and has write permissions
- [ ] Secrets are stored in a secrets manager (not plain text)
- [ ] Application starts successfully with `prod` profile
- [ ] Health endpoint returns healthy status

## Troubleshooting

### Application fails to start

1. Check all required environment variables are set
2. Verify MongoDB and Redis connectivity
3. Check application logs for specific errors
4. Ensure upload directory has proper permissions

### Email not sending

1. Verify SMTP credentials are correct
2. Check firewall allows outbound SMTP connections (port 587)
3. For Gmail, ensure "Less secure app access" is enabled or use App Passwords
4. Check email service logs

### JWT token errors

1. Verify JWT_SECRET is properly base64 encoded
2. Ensure JWT_SECRET hasn't changed (invalidates existing tokens)
3. Check token expiration settings

### File upload errors

1. Verify UPLOAD_DIR exists and has write permissions
2. Check disk space availability
3. Verify file size limits in application-prod.properties

## Support

For additional help:
- Review application logs: `/var/log/furent/`
- Check health endpoint: `http://your-domain/actuator/health`
- Contact support: support@furent.com
