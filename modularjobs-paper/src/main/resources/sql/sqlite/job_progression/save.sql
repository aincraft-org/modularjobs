INSERT INTO {table} (player_id, job_key, current_node_key, experience)
VALUES (?, ?, ?, ?)
ON CONFLICT(player_id, job_key) DO UPDATE SET
current_node_key = excluded.current_node_key,
experience = excluded.experience;
