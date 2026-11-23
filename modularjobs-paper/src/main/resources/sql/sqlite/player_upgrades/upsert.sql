INSERT INTO player_upgrades (player_id, job_key, total_skill_points, unlocked_nodes)
VALUES (?, ?, ?, ?)
ON CONFLICT(player_id, job_key) DO UPDATE SET
total_skill_points = excluded.total_skill_points,
unlocked_nodes = excluded.unlocked_nodes;
