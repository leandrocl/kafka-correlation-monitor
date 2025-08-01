# Spring Boot REST API

A comprehensive RESTful API built with Spring Boot 3.2.0, featuring HSQLDB persistence, AWS SQS/SNS integration, and OpenAPI documentation.

## Features

- **Spring Boot 3.2.0** with Java 21
- **HSQLDB** with persistent local storage
- **AWS SQS** integration (producer/consumer)
- **AWS SNS** integration (publisher/subscriber)
- **Health check endpoint** with database connectivity testing
- **OpenAPI/Swagger** documentation
- **Actuator** endpoints for monitoring

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- AWS CLI (optional, for AWS credentials)

## Quick Start

### 1. Clone and Build

```bash
cd spring-boot-rest-api
mvn clean install
```

### 2. Configure AWS Credentials (Optional)

If you want to use real AWS services, configure your credentials:

**Option A: Environment Variables**
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=us-east-1
```

**Option B: AWS CLI**
```bash
aws configure
```

**Option C: Application Properties**
Add to `src/main/resources/application.yml`:
```yaml
aws:
  access-key-id: your_access_key
  secret-access-key: your_secret_key
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Health Check
- **GET** `/api/v1/health`
- Tests database connectivity
- Returns HTTP 200 if healthy, 503 if unhealthy

### Echo Endpoint
- **GET** `/api/v1/echo?inputString=Hello World`
- Returns the input string in JSON format
- Example response: `{"responseString": "Hello World"}`

### AWS SQS Endpoints
- **POST** `/api/v1/sqs/send` - Send message to SQS queue
- **GET** `/api/v1/sqs/receive` - Receive messages from SQS queue

### AWS SNS Endpoints
- **POST** `/api/v1/sns/publish` - Publish message to SNS topic

## Documentation

### Swagger UI
- **URL**: `http://localhost:8080/swagger-ui.html`
- Interactive API documentation

### OpenAPI JSON
- **URL**: `http://localhost:8080/api-docs`
- Raw OpenAPI specification

### Actuator Endpoints
- **Health**: `http://localhost:8080/actuator/health`
- **Info**: `http://localhost:8080/actuator/info`
- **Metrics**: `http://localhost:8080/actuator/metrics`

## Configuration

### Database
- **Type**: HSQLDB with file persistence
- **Location**: `./data/restapi` (created automatically)
- **Username**: `sa`
- **Password**: (empty)

### AWS Configuration
- **Region**: `us-east-1` (configurable)
- **SQS Queue URL**: Configured in `application.yml`
- **SNS Topic ARN**: Configured in `application.yml`

## Project Structure

```
src/
├── main/
│   ├── java/com/example/restapi/
│   │   ├── RestApiApplication.java      # Main application class
│   │   ├── config/
│   │   │   └── AwsConfig.java          # AWS client configuration
│   │   ├── controller/
│   │   │   └── ApiController.java       # REST endpoints
│   │   └── service/
│   │       ├── SqsService.java          # SQS operations
│   │       └── SnsService.java          # SNS operations
│   └── resources/
│       └── application.yml              # Application configuration
└── test/
    └── java/com/example/restapi/       # Test classes
```

## Testing the API

### Health Check
```bash
curl http://localhost:8080/api/v1/health
```

### Echo Endpoint
```bash
curl "http://localhost:8080/api/v1/echo?inputString=Hello%20World"
```

### Send SQS Message
```bash
curl -X POST http://localhost:8080/api/v1/sqs/send \
  -H "Content-Type: application/json" \
  -d '{"message": "Test message"}'
```

### Publish SNS Message
```bash
curl -X POST http://localhost:8080/api/v1/sns/publish \
  -H "Content-Type: application/json" \
  -d '{"message": "Test notification"}'
```

## Development

### Running Tests
```bash
mvn test
```

### Building JAR
```bash
mvn clean package
```

### Running JAR
```bash
java -jar target/rest-api-1.0.0.jar
```

## Troubleshooting

### Database Issues
- Check if the `./data` directory is writable
- Verify HSQLDB is properly configured in `application.yml`

### AWS Issues
- Ensure AWS credentials are properly configured
- Check AWS region settings
- Verify SQS queue and SNS topic exist (if using real AWS)

### Port Issues
- Default port is 8080
- Change in `application.yml` if needed:
```yaml
server:
  port: 8081
```

## License

This project is for educational purposes. 