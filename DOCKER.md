# Archive System - Docker Setup

## Quick Start

### Using Docker Compose (Recommended)

1. **Build and start all services:**
   ```bash
   docker-compose up -d
   ```

2. **View logs:**
   ```bash
   docker-compose logs -f app
   ```

3. **Stop services:**
   ```bash
   docker-compose down
   ```

4. **Stop and remove volumes (clean reset):**
   ```bash
   docker-compose down -v
   ```

### Manual Docker Build

1. **Build the image:**
   ```bash
   docker build -t archive-system:latest .
   ```

2. **Run with external MySQL:**
   ```bash
   docker run -d \
     --name archive-app \
     -p 8080:8080 \
     -e MYSQL_HOST=your-mysql-host \
     -e MYSQL_PORT=3306 \
     -e MYSQL_DATABASE=archive_system \
     -e MYSQL_USER=your-user \
     -e MYSQL_PASSWORD=your-password \
     -v $(pwd)/uploads:/app/uploads \
     archive-system:latest
   ```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring profile to use |
| `MYSQL_HOST` | `mysql` | MySQL host address |
| `MYSQL_PORT` | `3306` | MySQL port |
| `MYSQL_DATABASE` | `archive_system` | Database name |
| `MYSQL_USER` | `root` | Database user |
| `MYSQL_PASSWORD` | `root` | Database password |
| `FILE_UPLOAD_DIR` | `/app/uploads` | Directory for uploaded files |

## Access the Application

- **Web UI:** http://localhost:8080
- **API:** http://localhost:8080/api

## Default Credentials

After initial setup, you can login with the credentials created by DataInitializer:
- HOD: `hod.ahmad@alquds.edu` / `password123`
- Professor: `prof.omar@alquds.edu` / `password123`

## Production Deployment

For production, make sure to:

1. **Change default passwords** in environment variables
2. **Use strong database credentials**
3. **Configure SSL/TLS** for HTTPS
4. **Set up proper backup** for volumes
5. **Configure resource limits** in docker-compose.yml

### Example with resource limits:

```yaml
app:
  # ... other config
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '1'
        memory: 1G
```

## Troubleshooting

### Check container logs:
```bash
docker-compose logs -f app
docker-compose logs -f mysql
```

### Restart services:
```bash
docker-compose restart app
```

### Database connection issues:
```bash
# Check MySQL is healthy
docker-compose ps

# Connect to MySQL container
docker exec -it archive-mysql mysql -u root -p
```

### Clean rebuild:
```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```
