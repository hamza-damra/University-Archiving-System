# Docker Deployment Guide

## Overview

This guide explains how to deploy the Archive System using Docker and Docker Compose with the new semester-based file system structure.

## File Storage Structure

The application uses a hierarchical file storage structure:

```
uploads/
├── {year}/
│   ├── {semester}/
│   │   ├── {professorId}/
│   │   │   ├── {courseCode}/
│   │   │   │   ├── {documentType}/
│   │   │   │   │   ├── file1.pdf
│   │   │   │   │   ├── file2.pdf
```

Example:
```
uploads/
├── 2024-2025/
│   ├── FIRST/
│   │   ├── prof_12345/
│   │   │   ├── CS101/
│   │   │   │   ├── SYLLABUS/
│   │   │   │   │   ├── syllabus_v1.pdf
│   │   │   │   ├── EXAM/
│   │   │   │   │   ├── midterm_exam.pdf
```

## Docker Compose Configuration

### Services

1. **MySQL Database** (`mysql`)
   - Image: `mysql:8.0`
   - Port: `3307:3306` (host:container)
   - Persistent storage via `mysql_data` volume
   - Health check enabled

2. **Spring Boot Application** (`app`)
   - Built from Dockerfile
   - Port: `8080:8080` (host:container)
   - Persistent file storage via `app_uploads` volume
   - Depends on MySQL service

### Volumes

- `mysql_data`: Stores MySQL database files
- `app_uploads`: Stores uploaded files in hierarchical structure

## Environment Variables

### Application Configuration

The following environment variables can be configured in `docker-compose.yml`:

#### File Upload Settings
- `APP_UPLOAD_BASE_PATH`: Base directory for uploads (default: `/app/uploads`)
- `APP_UPLOAD_ALLOWED_EXTENSIONS`: Comma-separated file extensions (default: `pdf,zip`)
- `APP_UPLOAD_MAX_FILE_COUNT`: Maximum files per upload (default: `10`)
- `APP_UPLOAD_MAX_TOTAL_SIZE_MB`: Maximum total size in MB (default: `50`)

#### Academic Settings
- `APP_ACADEMIC_DEFAULT_YEAR`: Default academic year (default: `2024-2025`)
- `APP_ACADEMIC_AUTO_CREATE_SEMESTERS`: Auto-create semesters (default: `true`)

#### Database Settings
- `SPRING_DATASOURCE_URL`: JDBC connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

## Deployment Steps

### 1. Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+

### 2. Build and Start Services

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Check service status
docker-compose ps
```

### 3. Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v
```

### 4. Rebuild Application

```bash
# Rebuild application after code changes
docker-compose build app
docker-compose up -d app
```

## Volume Management

### Backup Uploads Directory

```bash
# Create backup of uploads volume
docker run --rm -v archive-system_app_uploads:/data -v $(pwd):/backup alpine tar czf /backup/uploads-backup.tar.gz -C /data .
```

### Restore Uploads Directory

```bash
# Restore uploads from backup
docker run --rm -v archive-system_app_uploads:/data -v $(pwd):/backup alpine tar xzf /backup/uploads-backup.tar.gz -C /data
```

### Inspect Volume

```bash
# List files in uploads volume
docker run --rm -v archive-system_app_uploads:/data alpine ls -la /data
```

## Accessing the Application

- **Application URL**: http://localhost:8080
- **MySQL Port**: localhost:3307

## Troubleshooting

### Application Won't Start

1. Check MySQL is healthy:
   ```bash
   docker-compose ps mysql
   ```

2. View application logs:
   ```bash
   docker-compose logs app
   ```

3. Verify database connection:
   ```bash
   docker-compose exec mysql mysql -uroot -proot -e "SHOW DATABASES;"
   ```

### File Upload Issues

1. Check uploads directory permissions:
   ```bash
   docker-compose exec app ls -la /app/uploads
   ```

2. Verify volume mount:
   ```bash
   docker volume inspect archive-system_app_uploads
   ```

### Database Migration Issues

1. Check Flyway status:
   ```bash
   docker-compose logs app | grep -i flyway
   ```

2. Access MySQL directly:
   ```bash
   docker-compose exec mysql mysql -uroot -proot archive_system
   ```

## Production Considerations

### Security

1. **Change default passwords** in `docker-compose.yml`
2. **Use secrets management** for sensitive data
3. **Enable SSL/TLS** for database connections
4. **Configure firewall rules** to restrict access

### Performance

1. **Adjust connection pool** settings in application.properties
2. **Monitor volume disk usage**
3. **Configure MySQL memory** settings
4. **Use external volumes** for better I/O performance

### Backup Strategy

1. **Regular database backups** using mysqldump
2. **Periodic uploads volume backups**
3. **Test restore procedures**
4. **Store backups off-site**

## Configuration Files

- `docker-compose.yml`: Service orchestration
- `Dockerfile`: Application container build
- `src/main/resources/application.properties`: Application configuration

## Support

For issues or questions, refer to:
- Application logs: `docker-compose logs app`
- Database logs: `docker-compose logs mysql`
- System documentation: `ARCHITECTURE.md`
