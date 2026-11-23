INSERT INTO {table} (player_id, job_key, current_node_key, experience)
VALUES (?,?,?,?)
ON DUPLICATE KEY UPDATE current_node_key = VALUES(current_node_key), experience = VALUES(experience);
