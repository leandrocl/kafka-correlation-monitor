-- Add correlation columns to interesting_events table
ALTER TABLE interesting_events ADD COLUMN correlated_message VARCHAR(1000);
ALTER TABLE interesting_events ADD COLUMN is_correlated BOOLEAN DEFAULT FALSE;
ALTER TABLE interesting_events ADD COLUMN correlation_timestamp TIMESTAMP DEFAULT NULL;

-- Create index on key_of_interest_name for faster correlation lookups
CREATE INDEX idx_interesting_events_key_name_value ON interesting_events(key_of_interest_name, key_of_interest_value); 