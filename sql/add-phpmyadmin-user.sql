-- Define user, password, and database
-- Settings should be updated according to local requirements.
SET @username := 'phpmyadmin';
SET @password := 'phppass';
SET @database := '*';
SET @ip_range := '172.32.0.%';

-- Drop the user if it exists
SET @drop_user_stmt = CONCAT('DROP USER IF EXISTS ''', @username, '''@''', @ip_range, '''');
PREPARE stmt FROM @drop_user_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create the user
SET @create_user_stmt = CONCAT('CREATE USER ''', @username, '''@''', @ip_range, ''' IDENTIFIED BY ''', @password, '''');
PREPARE stmt FROM @create_user_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Grant privileges to the user
SET @grant_stmt = CONCAT('GRANT ALL PRIVILEGES ON ', @database, '.* TO ''', @username, '''@''', @ip_range, '''');
PREPARE stmt FROM @grant_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Optional: Flush privileges to ensure changes take effect
FLUSH PRIVILEGES;

-- Verify the created user and display grants
-- In the cass of unsuccessful operations above, an error will be displayed; otherwise, nothing will be shown.
SET @verify_stmt = CONCAT('SHOW GRANTS FOR ''', @username, '''@''', @ip_range, '''');
PREPARE stmt FROM @verify_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Instead, you can directly execute SHOW GRANTS
-- SHOW GRANTS FOR 'eventman'@'172.32.0.%';

-- End of script
