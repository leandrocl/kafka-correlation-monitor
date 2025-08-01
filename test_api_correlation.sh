#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"
API_BASE="$BASE_URL/api/v1"

echo -e "${BLUE}=== Kafka Correlation Monitor API Test Suite ===${NC}"
echo "Testing real API calls for events correlation and schedulers"
echo ""

# Function to print test results
print_result() {
    local test_name="$1"
    local status="$2"
    local message="$3"
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC} - $test_name: $message"
    else
        echo -e "${RED}✗ FAIL${NC} - $test_name: $message"
    fi
}

# Function to check if response contains expected content
check_response() {
    local response="$1"
    local expected="$2"
    local test_name="$3"
    
    if echo "$response" | grep -q "$expected"; then
        print_result "$test_name" "PASS" "Response contains expected content"
        return 0
    else
        print_result "$test_name" "FAIL" "Response does not contain expected content: '$expected'"
        echo "Actual response: $response"
        return 1
    fi
}

# Function to check HTTP status code
check_status() {
    local status="$1"
    local expected="$2"
    local test_name="$3"
    
    if [ "$status" = "$expected" ]; then
        print_result "$test_name" "PASS" "HTTP status $status"
        return 0
    else
        print_result "$test_name" "FAIL" "Expected HTTP status $expected, got $status"
        return 1
    fi
}

echo -e "${YELLOW}1. Testing Health Endpoint${NC}"
response=$(curl -s -w "%{http_code}" "$BASE_URL/actuator/health")
status_code="${response: -3}"
response_body="${response%???}"

check_status "$status_code" "200" "Health Check"
check_response "$response_body" "UP" "Health Status"

echo ""

echo -e "${YELLOW}2. Testing Kafka Producer Endpoint${NC}"
# Test sending a message to Kafka
kafka_response=$(curl -s -X POST "$API_BASE/kafka/produce" \
    -H "Content-Type: application/json" \
    -d '{
        "kafkaTopic": "test-topic",
        "message": "{\"userId\":\"user123\",\"message\":\"Test message for correlation\"}"
    }' \
    -w "%{http_code}")

kafka_status="${kafka_response: -3}"
kafka_body="${kafka_response%???}"

check_status "$kafka_status" "200" "Kafka Producer"
check_response "$kafka_body" "Message sent successfully" "Kafka Message Sent"

echo ""

echo -e "${YELLOW}3. Testing Interesting Events API - Initial State${NC}"
# Check initial state of interesting events
events_response=$(curl -s -w "%{http_code}" "$API_BASE/interesting-events?page=0&size=10")
events_status="${events_response: -3}"
events_body="${events_response%???}"

check_status "$events_status" "200" "Get Interesting Events"
echo "Initial events count: $(echo "$events_body" | jq '.events | length' 2>/dev/null || echo "0")"

echo ""

echo -e "${YELLOW}4. Testing Correlation by Sending Correlated Message${NC}"
# Send a correlated message that should match the previous event
correlated_response=$(curl -s -X POST "$API_BASE/kafka/produce" \
    -H "Content-Type: application/json" \
    -d '{
        "kafkaTopic": "test-topic-correlated",
        "message": "{\"correlationId\":\"user123\",\"correlatedData\":\"This should correlate with user123\"}"
    }' \
    -w "%{http_code}")

correlated_status="${correlated_response: -3}"
correlated_body="${correlated_response%???}"

check_status "$correlated_status" "200" "Correlated Kafka Producer"
check_response "$correlated_body" "Message sent successfully" "Correlated Message Sent"

echo ""

echo -e "${YELLOW}5. Testing Interesting Events API - After Correlation${NC}"
# Wait a moment for processing
sleep 5

events_after_response=$(curl -s -w "%{http_code}" "$API_BASE/interesting-events?page=0&size=10")
events_after_status="${events_after_response: -3}"
events_after_body="${events_after_response%???}"

check_status "$events_after_status" "200" "Get Interesting Events After Correlation"
echo "Events after correlation: $(echo "$events_after_body" | jq '.events | length' 2>/dev/null || echo "0")"

# Check if any events are correlated
correlated_count=$(echo "$events_after_body" | jq '[.events[] | select(.isCorrelated == true)] | length' 2>/dev/null || echo "0")
echo "Correlated events count: $correlated_count"

if [ "$correlated_count" -gt 0 ]; then
    print_result "Correlation Detection" "PASS" "Found $correlated_count correlated events"
else
    print_result "Correlation Detection" "FAIL" "No correlated events found"
fi

echo ""

echo -e "${YELLOW}6. Testing Multiple Events for Better Correlation Testing${NC}"
# Send multiple events to test correlation
echo "Sending multiple test events..."

# Event 1
curl -s -X POST "$API_BASE/kafka/produce" \
    -H "Content-Type: application/json" \
    -d '{
        "kafkaTopic": "test-topic",
        "message": "{\"userId\":\"user456\",\"message\":\"Second test message\"}"
    }' > /dev/null

# Event 2
curl -s -X POST "$API_BASE/kafka/produce" \
    -H "Content-Type: application/json" \
    -d '{
        "kafkaTopic": "test-topic",
        "message": "{\"userId\":\"user789\",\"message\":\"Third test message\"}"
    }' > /dev/null

# Event 3
curl -s -X POST "$API_BASE/kafka/produce" \
    -H "Content-Type: application/json" \
    -d '{
        "kafkaTopic": "test-topic",
        "message": "{\"userId\":\"user789\",\"message\":\"Fourth test message\"}"
    }' > /dev/null

sleep 3

# Send correlated messages
echo "Sending correlated messages..."

# Correlated message for user456
curl -s -X POST "$API_BASE/kafka/produce" \
    -H "Content-Type: application/json" \
    -d '{
        "kafkaTopic": "test-topic-correlated",
        "message": "{\"correlationId\":\"user456\",\"correlatedData\":\"Correlated with user456\"}"
    }' > /dev/null

# Correlated message for user789
curl -s -X POST "$API_BASE/kafka/produce" \
    -H "Content-Type: application/json" \
    -d '{
        "kafkaTopic": "test-topic-correlated",
        "message": "{\"correlationId\":\"user789\",\"correlatedData\":\"Correlated with user789\"}"
    }' > /dev/null

sleep 5

echo ""

echo -e "${YELLOW}7. Testing Interesting Events API - After Multiple Correlations${NC}"
final_events_response=$(curl -s -w "%{http_code}" "$API_BASE/interesting-events?page=0&size=20")
final_events_status="${final_events_response: -3}"
final_events_body="${final_events_response%???}"

check_status "$final_events_status" "200" "Get Final Interesting Events"

total_events=$(echo "$final_events_body" | jq '.events | length' 2>/dev/null || echo "0")
correlated_events=$(echo "$final_events_body" | jq '[.events[] | select(.isCorrelated == true)] | length' 2>/dev/null || echo "0")
uncorrelated_events=$(echo "$final_events_body" | jq '[.events[] | select(.isCorrelated == false)] | length' 2>/dev/null || echo "0")

echo "Total events: $total_events"
echo "Correlated events: $correlated_events"
echo "Uncorrelated events: $uncorrelated_events"

if [ "$correlated_events" -gt 0 ]; then
    print_result "Multiple Correlations" "PASS" "Successfully correlated $correlated_events events"
else
    print_result "Multiple Correlations" "FAIL" "No correlations detected"
fi

echo ""

echo -e "${YELLOW}8. Testing Scheduler Cleanup Functionality${NC}"
echo "Waiting for cleanup scheduler to run (60 seconds)..."
echo "Current correlated events before cleanup: $correlated_events"

# Wait for cleanup scheduler
sleep 65

# Check events after cleanup
cleanup_events_response=$(curl -s -w "%{http_code}" "$API_BASE/interesting-events?page=0&size=20")
cleanup_events_status="${cleanup_events_response: -3}"
cleanup_events_body="${cleanup_events_response%???}"

check_status "$cleanup_events_status" "200" "Get Events After Cleanup"

cleanup_total=$(echo "$cleanup_events_body" | jq '.events | length' 2>/dev/null || echo "0")
cleanup_correlated=$(echo "$cleanup_events_body" | jq '[.events[] | select(.isCorrelated == true)] | length' 2>/dev/null || echo "0")
cleanup_uncorrelated=$(echo "$cleanup_events_body" | jq '[.events[] | select(.isCorrelated == false)] | length' 2>/dev/null || echo "0")

echo "Events after cleanup:"
echo "  Total: $cleanup_total"
echo "  Correlated: $cleanup_correlated"
echo "  Uncorrelated: $cleanup_uncorrelated"

if [ "$cleanup_correlated" -eq 0 ]; then
    print_result "Cleanup Scheduler" "PASS" "All correlated events cleaned up"
else
    print_result "Cleanup Scheduler" "FAIL" "Still $cleanup_correlated correlated events remaining"
fi

echo ""

echo -e "${YELLOW}9. Testing Monitoring Scheduler${NC}"
echo "Testing monitoring of uncorrelated events..."

# Send some new events that won't be correlated
curl -s -X POST "$API_BASE/kafka/produce" \
    -H "Content-Type: application/json" \
    -d '{
        "kafkaTopic": "test-topic",
        "message": "{\"userId\":\"monitor1\",\"message\":\"Event for monitoring\"}"
    }' > /dev/null

curl -s -X POST "$API_BASE/kafka/produce" \
    -H "Content-Type: application/json" \
    -d '{
        "kafkaTopic": "test-topic",
        "message": "{\"userId\":\"monitor2\",\"message\":\"Another event for monitoring\"}"
    }' > /dev/null

sleep 3

# Check monitoring endpoint (if available)
monitor_response=$(curl -s -w "%{http_code}" "$API_BASE/interesting-events/stats")
monitor_status="${monitor_response: -3}"
monitor_body="${monitor_response%???}"

check_status "$monitor_status" "200" "Event Stats Endpoint"
echo "Total events count: $(echo "$monitor_body" | jq '.totalEvents' 2>/dev/null || echo "N/A")"

echo ""

echo -e "${YELLOW}10. Testing API Error Handling${NC}"

# Test invalid pagination parameters
invalid_pagination=$(curl -s -w "%{http_code}" "$API_BASE/interesting-events?page=-1&size=0")
invalid_status="${invalid_pagination: -3}"

check_status "$invalid_status" "400" "Invalid Pagination Parameters"

# Test non-existent event
not_found_response=$(curl -s -w "%{http_code}" "$API_BASE/interesting-events/999999")
not_found_status="${not_found_response: -3}"

check_status "$not_found_status" "404" "Non-existent Event"

echo ""


echo -e "${BLUE}=== Test Summary ===${NC}"
echo "All API tests completed. Check the application logs for detailed correlation and scheduler activity."
echo ""
echo "Key test areas covered:"
echo "✓ Health endpoint"
echo "✓ Kafka producer functionality"
echo "✓ Event correlation logic"
echo "✓ Multiple event correlation"
echo "✓ Cleanup scheduler"
echo "✓ Monitoring functionality"
echo "✓ Error handling"
echo ""
echo "Check the application logs (output.log) for detailed correlation and scheduler activity." 